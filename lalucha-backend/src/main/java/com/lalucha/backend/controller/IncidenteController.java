package com.lalucha.backend.controller;

import com.lalucha.backend.model.Asignado;
import com.lalucha.backend.model.Ticket;
import com.lalucha.backend.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Modulo de gestion de incidentes (Semana 14 - Sesion 28).
 * - POST  /api/incidentes            cualquier usuario autenticado reporta un incidente
 * - GET   /api/incidentes            tickets activos (el personal ve todos; el cliente, los suyos)
 * - GET   /api/incidentes/catalogos  prioridades, estados, grupos de soporte y modulos
 * - PATCH /api/incidentes/{id}       cambiar estado o reasignar (solo STAFF o ADMIN)
 */
@RestController
@RequestMapping("/api/incidentes")
public class IncidenteController {

    private final TicketService servicio;

    public IncidenteController(TicketService servicio) {
        this.servicio = servicio;
    }

    /** Datos del formulario de reporte. */
    public record NuevoTicket(
            @NotBlank(message = "El titulo es obligatorio.")
            @Size(max = 150, message = "El titulo admite hasta 150 caracteres.")
            String titulo,

            @NotBlank(message = "Indica el modulo afectado.")
            String modulo,

            @NotBlank(message = "Describe el error.")
            @Size(max = 4000, message = "La descripcion admite hasta 4000 caracteres.")
            String descripcion,

            @NotBlank(message = "La prioridad es obligatoria.")
            @Pattern(regexp = "P[1-4]", message = "La prioridad debe ser P1, P2, P3 o P4.")
            String prioridad) {
    }

    /** Cambio que hace el personal de soporte. */
    public record CambioTicket(Short estadoId, Short asignadoId) {
    }

    /** Lo que se muestra del ticket en el panel. */
    public record TicketVista(
            Long id,
            String codigo,
            String titulo,
            String modulo,
            String descripcion,
            Short estadoId,
            String estado,
            String prioridad,
            String prioridadNombre,
            Integer horasObjetivo,
            Short asignadoId,
            String asignado,
            Short nivel,
            String reportadoPor,
            OffsetDateTime fechaCreacion,
            OffsetDateTime fechaActualizacion) {

        static TicketVista de(Ticket t) {
            Asignado a = t.getAsignado();
            return new TicketVista(
                    t.getId(),
                    t.getCodigo(),
                    t.getTitulo(),
                    t.getModulo(),
                    t.getDescripcion(),
                    t.getEstado().getId(),
                    t.getEstado().getNombre(),
                    t.getPrioridad().getCodigo(),
                    t.getPrioridad().getNombre(),
                    t.getPrioridad().getHorasObjetivo(),
                    a == null ? null : a.getId(),
                    a == null ? null : a.getNombre(),
                    a == null ? null : a.getNivel(),
                    t.getUsuarioReporta().getNombreCompleto(),
                    t.getCreatedAt(),
                    t.getUpdatedAt());
        }
    }

    @GetMapping("/catalogos")
    public Map<String, Object> catalogos() {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("prioridades", servicio.prioridades());
        datos.put("estados", servicio.estados());
        datos.put("asignados", servicio.asignados());
        datos.put("modulos", TicketService.MODULOS);
        return datos;
    }

    @PostMapping
    public ResponseEntity<TicketVista> registrar(@Valid @RequestBody NuevoTicket datos,
                                                 Authentication autenticacion) {
        Ticket ticket = servicio.registrar(autenticacion.getName(), datos.titulo(), datos.modulo(),
                datos.descripcion(), datos.prioridad());
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketVista.de(ticket));
    }

    @GetMapping
    public List<TicketVista> activos(Authentication autenticacion) {
        return servicio.listarActivos(autenticacion.getName(), esPersonal(autenticacion))
                .stream()
                .map(TicketVista::de)
                .toList();
    }

    @PatchMapping("/{id}")
    public TicketVista actualizar(@PathVariable Long id, @RequestBody CambioTicket cambio) {
        return TicketVista.de(servicio.actualizar(id, cambio.estadoId(), cambio.asignadoId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> datoNoValido(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(NoSuchElementException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private static boolean esPersonal(Authentication autenticacion) {
        return autenticacion.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_STAFF".equals(a.getAuthority()));
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("success", false);
        cuerpo.put("message", mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
