package com.lalucha.backend;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba integral del modulo de incidentes contra la BD real (schema.sql + seed.sql):
 * simula el reporte de un fallo critico y su seguimiento por el personal.
 * Usuarios del seed: ivan.p@example.net (ADMIN) y juan.perez@example.com (CLIENTE).
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidenteIntegrationTest {

    private static final String ADMIN = "ivan.p@example.net";
    private static final String CLIENTE = "juan.perez@example.com";
    private static final String PASSWORD = "Password123!";

    private static final String FALLO_CRITICO = """
            {"titulo":"La pasarela de pagos no responde",
             "modulo":"Pagos",
             "descripcion":"Al confirmar el pago aparece error 502 y el pedido queda pendiente.",
             "prioridad":"P1"}
            """;

    @Autowired
    private MockMvc mockMvc;

    private String login(String email) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    private String reportarFalloCritico(String token) throws Exception {
        return mockMvc.perform(post("/api/incidentes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FALLO_CRITICO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Sin sesion no se puede reportar un incidente (401)")
    void sinToken_401() throws Exception {
        mockMvc.perform(post("/api/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FALLO_CRITICO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un fallo critico genera ticket INC-###, estado Abierto, prioridad P1 y fecha")
    void reporteCritico_generaTicket() throws Exception {
        String token = login(CLIENTE);

        mockMvc.perform(post("/api/incidentes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FALLO_CRITICO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", matchesPattern("INC-\\d{3,}")))
                .andExpect(jsonPath("$.estado").value("Abierto"))
                .andExpect(jsonPath("$.prioridad").value("P1"))
                .andExpect(jsonPath("$.horasObjetivo").value(4))
                .andExpect(jsonPath("$.asignado").value("Help Desk"))
                .andExpect(jsonPath("$.fechaCreacion", notNullValue()));
    }

    @Test
    @DisplayName("Datos invalidos se rechazan con 400 antes de llegar a la BD")
    void datosInvalidos_400() throws Exception {
        String token = login(CLIENTE);

        mockMvc.perform(post("/api/incidentes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"modulo\":\"Pagos\",\"descripcion\":\"x\",\"prioridad\":\"P9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").exists())
                .andExpect(jsonPath("$.errores.prioridad").exists());

        mockMvc.perform(post("/api/incidentes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Error\",\"modulo\":\"Inexistente\",\"descripcion\":\"x\",\"prioridad\":\"P3\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("El cliente no puede cambiar el estado de un ticket (403)")
    void cliente_noCambiaEstado_403() throws Exception {
        String token = login(CLIENTE);
        Integer id = JsonPath.read(reportarFalloCritico(token), "$.id");

        mockMvc.perform(patch("/api/incidentes/" + id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":4}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El personal ve el ticket en el panel y lo pasa a diagnostico con nivel 2")
    void personal_seguimiento() throws Exception {
        String ticket = reportarFalloCritico(login(CLIENTE));
        String codigo = JsonPath.read(ticket, "$.codigo");
        Integer id = JsonPath.read(ticket, "$.id");
        String admin = login(ADMIN);

        mockMvc.perform(get("/api/incidentes").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem(codigo)));

        mockMvc.perform(patch("/api/incidentes/" + id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":2,\"asignadoId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoId").value(2))
                .andExpect(jsonPath("$.asignado").value("Especialista de base de datos"))
                .andExpect(jsonPath("$.nivel").value(2));
    }

    @Test
    @DisplayName("Los catalogos traen las 4 prioridades y los 4 estados")
    void catalogos() throws Exception {
        mockMvc.perform(get("/api/incidentes/catalogos").header("Authorization", login(CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridades.length()").value(4))
                .andExpect(jsonPath("$.estados.length()").value(4))
                .andExpect(jsonPath("$.asignados.length()").value(3));
    }
}
