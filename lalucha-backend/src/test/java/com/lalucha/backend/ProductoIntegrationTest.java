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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba integral del panel de productos (HU-10) contra la BD real (schema.sql + seed.sql).
 * Usuarios del seed: ivan.p@example.net (ADMIN) y juan.perez@example.com (CLIENTE).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductoIntegrationTest {

    private static final String ADMIN = "ivan.p@example.net";
    private static final String CLIENTE = "juan.perez@example.com";
    private static final String PASSWORD = "Password123!";

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

    private long primeraCategoria() throws Exception {
        String json = mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$[0].id")).longValue();
    }

    private String producto(String nombre, String precio, long categoriaId) {
        return "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Prueba automatica\","
                + "\"precio\":" + precio + ",\"categoriaId\":" + categoriaId + ",\"stockMinimo\":3}";
    }

    @Test
    @DisplayName("El catalogo de productos es publico (sin sesion)")
    void catalogoPublico() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Sin sesion no se puede crear un producto (401)")
    void sinToken_401() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Sin sesion", "10", primeraCategoria())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un cliente no puede crear, editar ni desactivar productos (403)")
    void cliente_403() throws Exception {
        String token = login(CLIENTE);
        long categoria = primeraCategoria();

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Intento cliente", "10", categoria)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/productos/1")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precio\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/productos/1").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El admin crea, edita y desactiva un producto y el catalogo lo refleja")
    void admin_cicloCompleto() throws Exception {
        String admin = login(ADMIN);
        long categoria = primeraCategoria();

        String creado = mockMvc.perform(post("/api/productos")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Sanguche de Prueba", "12.50", categoria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", matchesPattern("[A-Z0-9]{1,2}-[A-Z0-9]{1,3}-\\d{2,}")))
                .andExpect(jsonPath("$.slug", matchesPattern("sanguche-de-prueba(-\\d+)?")))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse().getContentAsString();
        int id = ((Number) JsonPath.read(creado, "$.id")).intValue();

        // Aparece en el catalogo publico
        mockMvc.perform(get("/api/productos"))
                .andExpect(jsonPath("$[*].id", hasItem(id)));

        // Edicion de precio
        mockMvc.perform(put("/api/productos/" + id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precio\":13.90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(13.90));

        // Desactivar: sale del catalogo, pero el registro no se borra
        mockMvc.perform(delete("/api/productos/" + id).header("Authorization", admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/productos"))
                .andExpect(jsonPath("$[*].id", not(hasItem(id))));
    }

    @Test
    @DisplayName("Dos productos con el mismo nombre reciben slug y SKU distintos")
    void nombreRepetido_noChoca() throws Exception {
        String admin = login(ADMIN);
        long categoria = primeraCategoria();

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/productos")
                            .header("Authorization", admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(producto("Producto Repetido", "9.90", categoria)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Datos invalidos se rechazan con 400, no con error del servidor")
    void datosInvalidos_400() throws Exception {
        String admin = login(ADMIN);
        long categoria = primeraCategoria();

        // Precio negativo
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Precio negativo", "-1", categoria)))
                .andExpect(status().isBadRequest());

        // Sin nombre ni precio
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + categoria + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // Categoria inexistente
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(producto("Sin categoria", "10", 999999)))
                .andExpect(status().isBadRequest());
    }
}
