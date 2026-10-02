package com.lalucha.backend;

import com.lalucha.backend.model.Usuario;
import com.lalucha.backend.repository.UsuarioRepository;
import com.lalucha.backend.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el flujo registro -> verificacion -> login contra la BD real
 * creada con database/schema.sql + database/seed.sql (ver workflow backend-ci).
 * El envio de correo se simula para capturar el codigo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private EmailService emailService;

    private String emailUnico() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private String registrar(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Maria Jose Nonato\",\"email\":\"" + email
                                + "\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationCode(eq(email), codigo.capture());
        return codigo.getValue();
    }

    @Test
    void registroVerificacionYLogin() throws Exception {
        String email = emailUnico();
        String codigo = registrar(email);

        Usuario pendiente = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(pendiente.isEmailVerificado()).isFalse();
        assertThat(pendiente.getNombres()).isEqualTo("Maria");
        assertThat(pendiente.getApellidos()).isEqualTo("Jose Nonato");
        assertThat(pendiente.getRol().getCodigo()).isEqualTo("CLIENTE");
        assertThat(pendiente.getTokenExpiraEn()).isAfter(OffsetDateTime.now());

        // Login antes de verificar: rechazado
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("ROLE_CLIENTE"))
                .andExpect(jsonPath("$.nombre").value("Maria Jose Nonato"));
    }

    @Test
    void codigoVencidoEsRechazado() throws Exception {
        String email = emailUnico();
        String codigo = registrar(email);

        Usuario pendiente = usuarioRepository.findByEmail(email).orElseThrow();
        pendiente.setTokenExpiraEn(OffsetDateTime.now().minusMinutes(1));
        usuarioRepository.save(pendiente);

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + codigo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginConUsuarioDelSeed() throws Exception {
        // Usuario admin de database/seed.sql (password documentado: Password123!)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ivan.p@example.net\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ROLE_ADMIN"));
    }
}
