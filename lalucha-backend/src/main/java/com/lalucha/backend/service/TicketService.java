package com.lalucha.backend.service;

import com.lalucha.backend.model.Asignado;
import com.lalucha.backend.model.EstadoTicket;
import com.lalucha.backend.model.Prioridad;
import com.lalucha.backend.model.Ticket;
import com.lalucha.backend.model.Usuario;
import com.lalucha.backend.repository.AsignadoRepository;
import com.lalucha.backend.repository.EstadoTicketRepository;
import com.lalucha.backend.repository.PrioridadRepository;
import com.lalucha.backend.repository.TicketRepository;
import com.lalucha.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Gestion de incidentes (ITIL 4): registro, seguimiento y cambio de estado de tickets.
 * Todo ticket nace "Abierto" y asignado al Help Desk (nivel 1); el personal lo
 * diagnostica, lo escala a nivel 2 o 3 si hace falta, lo resuelve y lo cierra.
 */
@Service
public class TicketService {

    /** Modulos de la plataforma que se pueden indicar como afectados. */
    public static final List<String> MODULOS = List.of(
            "Tienda web",
            "Inicio de sesión y registro",
            "Carrito y pedidos",
            "Pagos",
            "Inventario",
            "Base de datos",
            "Panel de administración",
            "Otro");

    private final TicketRepository tickets;
    private final PrioridadRepository prioridades;
    private final EstadoTicketRepository estados;
    private final AsignadoRepository asignados;
    private final UsuarioRepository usuarios;

    public TicketService(TicketRepository tickets,
                         PrioridadRepository prioridades,
                         EstadoTicketRepository estados,
                         AsignadoRepository asignados,
                         UsuarioRepository usuarios) {
        this.tickets = tickets;
        this.prioridades = prioridades;
        this.estados = estados;
        this.asignados = asignados;
        this.usuarios = usuarios;
    }

    @Transactional
    public Ticket registrar(String emailReporta, String titulo, String modulo,
                            String descripcion, String codigoPrioridad) {
        if (!MODULOS.contains(modulo)) {
            throw new IllegalArgumentException("El modulo indicado no existe.");
        }
        Usuario usuario = usuarios.findByEmail(emailReporta)
                .orElseThrow(() -> new IllegalArgumentException("El usuario de la sesion no existe."));
        Prioridad prioridad = prioridades.findByCodigo(codigoPrioridad)
                .orElseThrow(() -> new IllegalArgumentException("La prioridad debe ser P1, P2, P3 o P4."));

        Ticket ticket = new Ticket();
        ticket.setTitulo(titulo.trim());
        ticket.setModulo(modulo);
        ticket.setDescripcion(descripcion.trim());
        ticket.setPrioridad(prioridad);
        ticket.setEstado(buscarEstado(EstadoTicket.ABIERTO));
        ticket.setAsignado(buscarAsignado(Asignado.HELP_DESK));
        ticket.setUsuarioReporta(usuario);

        // La BD completa el codigo INC-### y la fecha de creacion
        return tickets.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<Ticket> listarActivos(String email, boolean esPersonal) {
        return esPersonal ? tickets.listarActivos() : tickets.listarActivosDe(email);
    }

    /** Cambio de estado o reasignacion (solo personal: STAFF o ADMIN, ver SecurityConfig). */
    @Transactional
    public Ticket actualizar(Long id, Short estadoId, Short asignadoId) {
        Ticket ticket = tickets.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe el ticket " + id + "."));
        if (estadoId != null) {
            ticket.setEstado(buscarEstado(estadoId));
        }
        if (asignadoId != null) {
            ticket.setAsignado(buscarAsignado(asignadoId));
        }
        return tickets.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<Prioridad> prioridades() {
        return prioridades.findAll();
    }

    @Transactional(readOnly = true)
    public List<EstadoTicket> estados() {
        return estados.findAll();
    }

    @Transactional(readOnly = true)
    public List<Asignado> asignados() {
        return asignados.findAll();
    }

    private EstadoTicket buscarEstado(Short id) {
        return estados.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estado no valido."));
    }

    private Asignado buscarAsignado(Short id) {
        return asignados.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grupo de soporte no valido."));
    }
}
