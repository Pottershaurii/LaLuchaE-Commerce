package com.lalucha.backend.controller;

import com.lalucha.backend.dto.LoginRequest;
import com.lalucha.backend.dto.RegisterRequest;
import com.lalucha.backend.dto.VerifyRequest;
import com.lalucha.backend.model.Usuario;
import com.lalucha.backend.repository.UsuarioRepository;
import com.lalucha.backend.service.EmailService;
import com.lalucha.backend.service.JwtService;
import com.lalucha.backend.service.VerificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

    private final EmailService emailService;
    private final VerificationService verificationService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            EmailService emailService,
            VerificationService verificationService,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.emailService = emailService;
        this.verificationService = verificationService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                response.put("success", false);
                response.put("message", "Debes ingresar un correo electrónico.");
                return ResponseEntity.badRequest().body(response);
            }

            String email = request.getEmail().trim().toLowerCase();

            if (usuarioRepository.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "El correo ya se encuentra registrado.");
                return ResponseEntity.badRequest().body(response);
            }

            String code = verificationService.generateCode(request);
            emailService.sendVerificationCode(email, code);

            response.put("success", true);
            response.put("message", "Código de verificación enviado correctamente.");
            response.put("email", email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "No se pudo enviar el correo de verificación.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody VerifyRequest request) {
        Map<String, Object> response = new HashMap<>();
        String email = request.getEmail().trim().toLowerCase();

        boolean valid = verificationService.verifyCode(email, request.getCode());

        if (!valid) {
            response.put("success", false);
            response.put("message", "El código de verificación es incorrecto.");
            return ResponseEntity.badRequest().body(response);
        }

        RegisterRequest pendingUser = verificationService.getPendingUser(email);
        if (pendingUser != null) {
            // Guardar usuario con contraseña encriptada usando BCrypt
            Usuario nuevoUsuario = new Usuario(
                pendingUser.getNombre(),
                email,
                passwordEncoder.encode(pendingUser.getPassword()),
                "ROLE_CLIENTE",
                true
            );
            usuarioRepository.save(nuevoUsuario);
            verificationService.clear(email);
        }

        response.put("success", true);
        response.put("message", "Correo verificado correctamente. Usuario registrado en el sistema.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (request.getEmail() == null || request.getPassword() == null) {
            response.put("success", false);
            response.put("message", "Debes ingresar tu correo y contraseña.");
            return ResponseEntity.badRequest().body(response);
        }

        String email = request.getEmail().trim().toLowerCase();
        Optional<Usuario> userOpt = usuarioRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Las credenciales ingresadas son incorrectas.");
            return ResponseEntity.badRequest().body(response);
        }

        Usuario usuario = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            response.put("success", false);
            response.put("message", "Las credenciales ingresadas son incorrectas.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!usuario.isVerificado()) {
            response.put("success", false);
            response.put("message", "Debes verificar tu correo electrónico antes de iniciar sesión.");
            return ResponseEntity.badRequest().body(response);
        }

        // Generar Token JWT con el Rol y Nombre del usuario
        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol(), usuario.getNombre());

        response.put("success", true);
        response.put("message", "Inicio de sesión exitoso.");
        response.put("token", token);
        response.put("nombre", usuario.getNombre());
        response.put("email", usuario.getEmail());
        response.put("rol", usuario.getRol());

        return ResponseEntity.ok(response);
    }
}