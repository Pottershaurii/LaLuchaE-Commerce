package com.lalucha.backend;

import com.jayway.jsonpath.JsonPath;
import com.lalucha.backend.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evidencia de mitigacion OWASP Top 10 contra la BD real (database/schema.sql + seed.sql).
 * Usuarios del seed: ivan.p@example.net (ADMIN) y juan.perez@example.com (CLIENTE),
 * ambos con contrasena Password123!
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadOwaspIntegrationTest {

    private static final String ADMIN = "ivan.p@example.net";
    private static final String CLIENTE = "juan.perez@example.com";
    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String login(String email, String password) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    // =====================================================================
    // A01 - Broken Access Control
    // =====================================================================
    @Nested
    @DisplayName("A01 - Control de acceso en endpoints")
    class A01ControlDeAcceso {

        @Test
        @DisplayName("Sin token: ruta protegida responde 401")
        void sinToken_rutaProtegida_401() throws Exception {
            mockMvc.perform(get("/api/cliente/perfil"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Debes iniciar sesion para acceder a este recurso."));
        }

        @Test
        @DisplayName("Sin token: el checkout ya no es publico (401)")
        void sinToken_checkout_401() throws Exception {
            mockMvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token inventado: 401 token invalido")
        void tokenInventado_401() throws Exception {
            mockMvc.perform(get("/api/cliente/perfil").header("Authorization", "Bearer abc.def.ghi"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Token invalido o expirado."));
        }

        @Test
        @DisplayName("Token falsificado con otra clave y rol ADMIN: 401")
        void tokenFirmadoConOtraClave_401() throws Exception {
            String falso = Jwts.builder()
                    .setSubject(CLIENTE)
                    .claim("rol", "ROLE_ADMIN")
                    .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(Keys.hmacShaKeyFor("clave-del-atacante-con-mas-de-32-caracteres!".getBytes(StandardCharsets.UTF_8)),
                            SignatureAlgorithm.HS256)
                    .compact();

            mockMvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + falso))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Token invalido o expirado."));
        }

        @Test
        @DisplayName("Token vencido: 401")
        void tokenVencido_401() throws Exception {
            String vencido = Jwts.builder()
                    .setSubject(ADMIN)
                    .claim("rol", "ROLE_ADMIN")
                    .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                    .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                    .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                    .compact();

            mockMvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + vencido))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("CLIENTE accede a su perfil: 200 y solo ve sus datos")
        void cliente_perfil_200() throws Exception {
            String token = login(CLIENTE, PASSWORD);

            mockMvc.perform(get("/api/cliente/perfil").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(CLIENTE))
                    .andExpect(jsonPath("$.rol").value("CLIENTE"));
        }

        @Test
        @DisplayName("CLIENTE intenta entrar a ruta de ADMIN: 403")
        void cliente_rutaAdmin_403() throws Exception {
            String token = login(CLIENTE, PASSWORD);

            mockMvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("No tienes permisos para acceder a este recurso."));
        }

        @Test
        @DisplayName("ADMIN entra a ruta de ADMIN: 200 sin exponer contrasenas")
        void admin_rutaAdmin_200() throws Exception {
            String token = login(ADMIN, PASSWORD);

            mockMvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").exists())
                    .andExpect(content().string(not(containsString("password"))))
                    .andExpect(content().string(not(containsString("$2a$"))));
        }
    }

    // =====================================================================
    // A03 - Injection
    // =====================================================================
    @Nested
    @DisplayName("A03 - Validacion de entradas e inyeccion SQL")
    class A03Inyeccion {

        @Test
        @DisplayName("Payload SQL en el correo del login: 400 por validacion del DTO")
        void login_payloadSqlEnCorreo_400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"' OR '1'='1' --\",\"password\":\"x\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.email").value("El correo no tiene un formato valido."))
                    .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        @DisplayName("Payload SQL en la contrasena: se trata como texto y no inicia sesion")
        void login_payloadSqlEnPassword_noAutentica() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + CLIENTE + "\",\"password\":\"' OR '1'='1' --\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Las credenciales ingresadas son incorrectas."))
                    .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        @DisplayName("Consulta parametrizada: el payload no altera la consulta ni borra tablas")
        void repositorio_consultaParametrizada() {
            long antes = usuarioRepository.count();

            assertThat(usuarioRepository.findByEmail(CLIENTE + "' OR '1'='1")).isEmpty();
            assertThat(usuarioRepository.findByEmail("x'; DROP TABLE lalucha.usuarios; --")).isEmpty();
            assertThat(usuarioRepository.existsByEmail("' OR 1=1 --")).isFalse();

            // La tabla sigue existiendo y con los mismos registros
            assertThat(usuarioRepository.count()).isEqualTo(antes);
        }

        @Test
        @DisplayName("Registro con datos invalidos: 400 con el detalle por campo")
        void registro_datosInvalidos_400() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"<script>alert(1)</script>\",\"email\":\"no-es-correo\",\"password\":\"123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.nombre").value("El nombre solo puede contener letras y espacios."))
                    .andExpect(jsonPath("$.errores.email").value("El correo no tiene un formato valido."))
                    .andExpect(jsonPath("$.errores.password").value("La contrasena debe tener entre 6 y 72 caracteres."));
        }

        @Test
        @DisplayName("Codigo de verificacion con caracteres no numericos: 400")
        void verificacion_codigoInvalido_400() throws Exception {
            mockMvc.perform(post("/api/auth/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + CLIENTE + "\",\"code\":\"1' OR '1\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.code").value("El codigo debe tener exactamente 6 digitos."));
        }

        @Test
        @DisplayName("JSON mal formado: 400 sin exponer trazas del servidor")
        void jsonMalFormado_400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("El cuerpo de la peticion no es un JSON valido."))
                    .andExpect(content().string(not(containsString("Exception"))));
        }
    }
}
