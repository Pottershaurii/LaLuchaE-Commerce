package com.lalucha.backend.controller;

import com.lalucha.backend.dto.LoginRequest;
import com.lalucha.backend.dto.RegisterRequest;
import com.lalucha.backend.dto.VerifyRequest;
import com.lalucha.backend.model.Rol;
import com.lalucha.backend.model.Usuario;
import com.lalucha.backend.repository.RolRepository;
import com.lalucha.backend.repository.UsuarioRepository;
import com.lalucha.backend.service.EmailService;
import com.lalucha.backend.service.JwtService;
import com.lalucha.backend.service.VerificationService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174",
    "http://localhost:5175"
})
public class AuthController {

    private static final String ROL_POR_DEFECTO = "CLIENTE";

    private final EmailService emailService;
    private final VerificationService verificationService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            EmailService emailService,
            VerificationService verificationService,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.emailService = emailService;
        this.verificationService = verificationService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Crea (o actualiza, si aun no verifico su correo) un usuario pendiente
     * y le envia un codigo de verificacion que vence en 15 minutos.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        if (esVacio(request.getEmail())) {
            return error("Debes ingresar un correo electrónico.");
        }
        if (esVacio(request.getPassword()) || request.getPassword().length() < 6) {
            return error("La contraseña debe tener al menos 6 caracteres.");
        }
        if (esVacio(request.resolverNombres())) {
            return error("Debes ingresar tu nombre.");
        }

        String email = normalizarEmail(request.getEmail());
        Optional<Usuario> existente = usuarioRepository.findByEmail(email);

        if (existente.isPresent() && existente.get().isEmailVerificado()) {
            return error("El correo ya se encuentra registrado.");
        }

        // Si ya existia sin verificar, se actualizan sus datos y se reenvia un codigo nuevo
        Usuario usuario = existente.orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setEmail(email);
            nuevo.setRol(buscarRol(ROL_POR_DEFECTO));
            return nuevo;
        });
        usuario.setNombres(request.resolverNombres());
        usuario.setApellidos(request.resolverApellidos());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setEmailVerificado(false);

        String codigo = verificationService.asignarCodigo(usuario);
        usuarioRepository.save(usuario);

        try {
            emailService.sendVerificationCode(email, codigo);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "No se pudo enviar el correo de verificación.");
            return ResponseEntity.internalServerError().body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Código de verificación enviado correctamente.");
        response.put("email", email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@Valid @RequestBody VerifyRequest request) {
        if (esVacio(request.getEmail()) || esVacio(request.getCode())) {
            return error("Debes ingresar el correo y el código de verificación.");
        }

        String email = normalizarEmail(request.getEmail());
        Optional<Usuario> userOpt = usuarioRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return error("No hay un registro pendiente para este correo.");
        }

        Usuario usuario = userOpt.get();

        if (usuario.isEmailVerificado()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "El correo ya estaba verificado. Puedes iniciar sesión.");
            return ResponseEntity.ok(response);
        }

        if (!verificationService.codigoCoincide(usuario, request.getCode())) {
            return error("El código de verificación es incorrecto.");
        }

        if (verificationService.codigoVencido(usuario)) {
            return error("El código de verificación venció. Regístrate de nuevo para recibir uno nuevo.");
        }

        verificationService.marcarVerificado(usuario);
        usuarioRepository.save(usuario);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Correo verificado correctamente. Usuario registrado en el sistema.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        if (esVacio(request.getEmail()) || esVacio(request.getPassword())) {
            return error("Debes ingresar tu correo y contraseña.");
        }

        String email = normalizarEmail(request.getEmail());
        Optional<Usuario> userOpt = usuarioRepository.findByEmail(email);

        if (userOpt.isEmpty()
                || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            return error("Las credenciales ingresadas son incorrectas.");
        }

        Usuario usuario = userOpt.get();

        if (!usuario.isActivo()) {
            return error("Tu cuenta se encuentra desactivada.");
        }

        if (!usuario.isEmailVerificado()) {
            return error("Debes verificar tu correo electrónico antes de iniciar sesión.");
        }

        usuario.setUltimoAcceso(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        String rol = usuario.getRol().getAuthority();
        String nombre = usuario.getNombreCompleto();

        // Generar Token JWT con el Rol y Nombre del usuario
        String token = jwtService.generateToken(usuario.getEmail(), rol, nombre);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Inicio de sesión exitoso.");
        response.put("token", token);
        response.put("nombre", nombre);
        response.put("email", usuario.getEmail());
        response.put("rol", rol);
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------

    private Rol buscarRol(String codigo) {
        return rolRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol " + codigo + " en la tabla roles. Ejecuta database/seed.sql."));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static ResponseEntity<Map<String, Object>> error(String mensaje) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", mensaje);
        return ResponseEntity.badRequest().body(response);
    }
}
