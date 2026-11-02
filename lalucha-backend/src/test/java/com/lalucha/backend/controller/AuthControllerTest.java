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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de AuthController con Mockito: las dependencias (repositorios,
 * correo, BCrypt y JWT) se simulan, asi se prueba solo la logica del controlador.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String CORREO = "juan.perez@example.com";

    @Mock private EmailService emailService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private final VerificationService verificationService = new VerificationService();
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(emailService, verificationService, usuarioRepository,
                rolRepository, passwordEncoder, jwtService);
    }

    private static Usuario usuario(boolean verificado, boolean activo) {
        Rol rol = new Rol();
        rol.setCodigo("CLIENTE");
        Usuario u = new Usuario();
        u.setRol(rol);
        u.setEmail(CORREO);
        u.setNombres("Juan");
        u.setApellidos("Perez");
        u.setPasswordHash("hash-bcrypt");
        u.setEmailVerificado(verificado);
        u.setActivo(activo);
        return u;
    }

    private static String mensaje(ResponseEntity<Map<String, Object>> r) {
        return String.valueOf(r.getBody().get("message"));
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        private LoginRequest request(String email, String clave) {
            LoginRequest r = new LoginRequest();
            r.setEmail(email);
            r.setPassword(clave);
            return r;
        }

        @Test
        @DisplayName("Normaliza el correo y entrega un JWT con el rol del usuario")
        void loginCorrecto() {
            Usuario u = usuario(true, true);
            when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("Password123!", "hash-bcrypt")).thenReturn(true);
            when(jwtService.generateToken(CORREO, "ROLE_CLIENTE", "Juan Perez")).thenReturn("token.jwt.firmado");

            ResponseEntity<Map<String, Object>> r = controller.login(request("  Juan.Perez@Example.com ", "Password123!"));

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(r.getBody()).containsEntry("token", "token.jwt.firmado").containsEntry("rol", "ROLE_CLIENTE");
            assertThat(u.getUltimoAcceso()).isNotNull();
            verify(usuarioRepository).save(u);
        }

        @Test
        @DisplayName("Usa el mismo mensaje si el correo no existe o la clave es incorrecta")
        void credencialesIncorrectas() {
            when(usuarioRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());
            ResponseEntity<Map<String, Object>> sinUsuario = controller.login(request("nadie@example.com", "x"));

            when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(usuario(true, true)));
            when(passwordEncoder.matches("mala", "hash-bcrypt")).thenReturn(false);
            ResponseEntity<Map<String, Object>> claveMala = controller.login(request(CORREO, "mala"));

            assertThat(sinUsuario.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(claveMala.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(sinUsuario)).isEqualTo(mensaje(claveMala)).contains("incorrectas");
            verify(jwtService, never()).generateToken(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("No entrega token a una cuenta desactivada")
        void cuentaDesactivada() {
            when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(usuario(true, false)));
            when(passwordEncoder.matches("Password123!", "hash-bcrypt")).thenReturn(true);

            ResponseEntity<Map<String, Object>> r = controller.login(request(CORREO, "Password123!"));

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(r)).contains("desactivada");
            verify(jwtService, never()).generateToken(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Exige verificar el correo antes de iniciar sesion")
        void correoSinVerificar() {
            when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(usuario(false, true)));
            when(passwordEncoder.matches("Password123!", "hash-bcrypt")).thenReturn(true);

            ResponseEntity<Map<String, Object>> r = controller.login(request(CORREO, "Password123!"));

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(r)).contains("verificar");
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Registro {

        private RegisterRequest request() {
            RegisterRequest r = new RegisterRequest();
            r.setNombre("Ana Torres");
            r.setEmail("Ana@Example.com");
            r.setPassword("Secreta123");
            return r;
        }

        @Test
        @DisplayName("Crea el usuario con clave cifrada y rol CLIENTE y envia el codigo")
        void registroNuevo() {
            Rol cliente = new Rol();
            cliente.setCodigo("CLIENTE");
            when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
            when(rolRepository.findByCodigo("CLIENTE")).thenReturn(Optional.of(cliente));
            when(passwordEncoder.encode("Secreta123")).thenReturn("hash-nuevo");

            ResponseEntity<Map<String, Object>> r = controller.register(request());

            ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(guardado.capture());
            Usuario u = guardado.getValue();
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(u.getEmail()).isEqualTo("ana@example.com");
            assertThat(u.getPasswordHash()).isEqualTo("hash-nuevo").isNotEqualTo("Secreta123");
            assertThat(u.getRol().getCodigo()).isEqualTo("CLIENTE");
            assertThat(u.isEmailVerificado()).isFalse();
            assertThat(u.getNombres()).isEqualTo("Ana");
            assertThat(u.getApellidos()).isEqualTo("Torres");
            verify(emailService).sendVerificationCode(eq("ana@example.com"), eq(u.getTokenVerificacion()));
        }

        @Test
        @DisplayName("No permite registrar un correo ya verificado")
        void correoYaRegistrado() {
            when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario(true, true)));

            ResponseEntity<Map<String, Object>> r = controller.register(request());

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(r)).contains("ya se encuentra registrado");
            verify(usuarioRepository, never()).save(any());
            verify(emailService, never()).sendVerificationCode(anyString(), anyString());
        }

        @Test
        @DisplayName("Responde 500 sin exponer detalles si el correo no se puede enviar")
        void falloDeCorreo() {
            Rol cliente = new Rol();
            cliente.setCodigo("CLIENTE");
            when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
            when(rolRepository.findByCodigo("CLIENTE")).thenReturn(Optional.of(cliente));
            when(passwordEncoder.encode("Secreta123")).thenReturn("hash-nuevo");
            doThrow(new RuntimeException("SMTP no disponible"))
                    .when(emailService).sendVerificationCode(anyString(), anyString());

            ResponseEntity<Map<String, Object>> r = controller.register(request());

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(mensaje(r)).contains("No se pudo enviar").doesNotContain("SMTP");
        }
    }

    @Nested
    @DisplayName("POST /api/auth/verify")
    class Verificacion {

        private VerifyRequest request(String codigo) {
            VerifyRequest r = new VerifyRequest();
            r.setEmail(CORREO);
            r.setCode(codigo);
            return r;
        }

        @Test
        @DisplayName("Verifica la cuenta con el codigo correcto y vigente")
        void codigoCorrecto() {
            Usuario u = usuario(false, true);
            u.setTokenVerificacion("123456");
            u.setTokenExpiraEn(OffsetDateTime.now().plusMinutes(10));
            when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(u));

            ResponseEntity<Map<String, Object>> r = controller.verify(request("123456"));

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(u.isEmailVerificado()).isTrue();
            verify(usuarioRepository).save(u);
        }

        @Test
        @DisplayName("Rechaza un codigo incorrecto")
        void codigoIncorrecto() {
            Usuario u = usuario(false, true);
            u.setTokenVerificacion("123456");
            u.setTokenExpiraEn(OffsetDateTime.now().plusMinutes(10));
            when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(u));

            ResponseEntity<Map<String, Object>> r = controller.verify(request("000000"));

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(u.isEmailVerificado()).isFalse();
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza un codigo vencido")
        void codigoVencido() {
            Usuario u = usuario(false, true);
            u.setTokenVerificacion("123456");
            u.setTokenExpiraEn(OffsetDateTime.now().minusMinutes(1));
            when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(u));

            ResponseEntity<Map<String, Object>> r = controller.verify(request("123456"));

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(r)).contains("venci");
        }
    }
}
