package com.lalucha.backend;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sesion 30 - Monitoreo y gestion de eventos.
 * 1) /actuator/prometheus publica metricas reales sin pedir token (Ficha 01).
 * 2) El webhook de Alertmanager convierte una alerta critica en un ticket P1 (Ficha 04).
 * El token de esta prueba solo existe aqui; en produccion viene de ALERTAS_WEBHOOK_TOKEN.
 */
@SpringBootTest(properties = {
        "lalucha.alertas.token=token-de-prueba-local",
        "lalucha.alertas.usuario=ivan.p@example.net"
})
@AutoConfigureMockMvc
class AlertaWebhookIntegrationTest {

    private static final String AUTORIZACION = "Alerta token-de-prueba-local";

    @Autowired
    private MockMvc mockMvc;

    private static String notificacion(String estado, String huella) {
        return """
                {"version":"4","status":"%s","receiver":"tickets-lalucha",
                 "alerts":[{"status":"%s",
                   "labels":{"alertname":"CpuCritica","severity":"critical","priority":"P1","modulo":"Tienda web"},
                   "annotations":{"summary":"CPU del API sobre 85%%","description":"Prueba automatica del webhook."},
                   "startsAt":"2026-11-19T21:00:00Z","fingerprint":"%s"}]}
                """.formatted(estado, estado, huella);
    }

    private String loginAdmin() throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ivan.p@example.net\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    @Test
    @DisplayName("/actuator/prometheus responde 200 con metricas de CPU y memoria, sin token")
    void metricasPrometheusPublicas() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("process_cpu_usage")))
                .andExpect(content().string(containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(containsString("hikaricp_connections")));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Sin token o con token incorrecto el webhook responde 401")
    void webhookSinToken_401() throws Exception {
        mockMvc.perform(post("/api/incidentes/alertas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificacion("firing", "sin-token")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/incidentes/alertas")
                        .header("Authorization", "Alerta otro-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificacion("firing", "token-malo")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Una alerta critica crea un ticket P1 y no se duplica mientras siga activo")
    void alertaCritica_creaTicketP1() throws Exception {
        String huella = "prueba-" + System.nanoTime();

        String json = mockMvc.perform(post("/api/incidentes/alertas")
                        .header("Authorization", AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificacion("firing", huella)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creados", hasSize(1)))
                .andExpect(jsonPath("$.creados[0]", matchesPattern("INC-\\d{3,}")))
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(json, "$.creados[0]");

        // El personal lo ve en el panel con prioridad P1, asignado al Help Desk
        mockMvc.perform(get("/api/incidentes").header("Authorization", loginAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == '" + codigo + "')].prioridad", hasItem("P1")))
                .andExpect(jsonPath("$[?(@.codigo == '" + codigo + "')].asignado", hasItem("Help Desk")))
                .andExpect(jsonPath("$[?(@.codigo == '" + codigo + "')].titulo",
                        hasItem("[Alerta] CPU del API sobre 85%")));

        // Alertmanager repite la notificacion: no se abre un segundo ticket
        mockMvc.perform(post("/api/incidentes/alertas")
                        .header("Authorization", AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificacion("firing", huella)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creados", hasSize(0)))
                .andExpect(jsonPath("$.omitidas").value(1));
    }

    @Test
    @DisplayName("Una alerta resuelta no abre ticket")
    void alertaResuelta_noCreaTicket() throws Exception {
        mockMvc.perform(post("/api/incidentes/alertas")
                        .header("Authorization", AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificacion("resolved", "resuelta-" + System.nanoTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creados", hasSize(0)))
                .andExpect(jsonPath("$.omitidas").value(1));
    }
}
