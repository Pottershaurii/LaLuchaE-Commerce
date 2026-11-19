package com.lalucha.backend.controller;

import com.lalucha.backend.model.Ticket;
import com.lalucha.backend.service.TicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Receptor del webhook de Alertmanager (Sesion 30 - Monitoreo y gestion de eventos).
 * Cada alerta que pasa a "firing" abre un ticket en el modulo de incidentes, con la
 * prioridad de la regla (P1-P4) y asignado al Help Desk. Las alertas resueltas no
 * cierran el ticket: lo cierra el personal despues de verificar.
 *
 * Autenticacion: header "Authorization: Alerta <token>". El token se define en
 * ALERTAS_WEBHOOK_TOKEN y en monitoring/secretos/alertas_token (Zero Hardcoding).
 */
@RestController
@RequestMapping("/api/incidentes/alertas")
public class AlertaController {

    private static final String PREFIJO = "Alerta ";

    private final TicketService servicio;
    private final byte[] token;
    private final String usuarioSistema;

    public AlertaController(TicketService servicio,
                            @Value("${lalucha.alertas.token:}") String token,
                            @Value("${lalucha.alertas.usuario:}") String usuarioSistema) {
        this.servicio = servicio;
        this.token = token.trim().getBytes(StandardCharsets.UTF_8);
        this.usuarioSistema = usuarioSistema.trim();
    }

    /** Una alerta dentro de la notificacion de Alertmanager (solo los campos que usamos). */
    public record AlertaEntrante(String status, Map<String, String> labels, Map<String, String> annotations,
                                 String startsAt, String fingerprint) {
    }

    /** Cuerpo del webhook de Alertmanager (version 4). */
    public record NotificacionAlertmanager(String status, List<AlertaEntrante> alerts) {
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> recibir(
            @RequestHeader(value = "Authorization", required = false) String autorizacion,
            @RequestBody NotificacionAlertmanager notificacion) {

        if (token.length == 0 || usuarioSistema.isEmpty()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "La integracion de alertas no esta configurada.");
        }
        if (!autorizado(autorizacion)) {
            return error(HttpStatus.UNAUTHORIZED, "Token de alertas invalido.");
        }

        List<String> creados = new ArrayList<>();
        int omitidas = 0;
        List<AlertaEntrante> alertas = notificacion.alerts() == null ? List.of() : notificacion.alerts();
        for (AlertaEntrante alerta : alertas) {
            if (!"firing".equalsIgnoreCase(alerta.status())) {
                omitidas++;
                continue;
            }
            Optional<Ticket> ticket = servicio.registrarAlerta(usuarioSistema, alerta.labels(),
                    alerta.annotations(), alerta.startsAt(), alerta.fingerprint());
            if (ticket.isPresent()) {
                creados.add(ticket.get().getCodigo());
            } else {
                omitidas++;
            }
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("creados", creados);
        cuerpo.put("omitidas", omitidas);
        return ResponseEntity.ok(cuerpo);
    }

    /** Configuracion incoherente (p. ej. el usuario de sistema no existe): 400, Alertmanager no reintenta. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> datoNoValido(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Comparacion en tiempo constante para no filtrar el token por diferencias de tiempo. */
    private boolean autorizado(String autorizacion) {
        if (autorizacion == null || !autorizacion.startsWith(PREFIJO)) {
            return false;
        }
        byte[] recibido = autorizacion.substring(PREFIJO.length()).trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(recibido, token);
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("success", false);
        cuerpo.put("message", mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
