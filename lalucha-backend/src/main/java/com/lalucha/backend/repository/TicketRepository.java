package com.lalucha.backend.repository;

import com.lalucha.backend.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Panel del personal: todos los tickets en seguimiento, primero los mas criticos. */
    @Query("select t from Ticket t where t.estado.activo = true "
            + "order by t.prioridad.id asc, t.createdAt desc")
    List<Ticket> listarActivos();

    /** Panel del cliente: solo los tickets que el mismo reporto. */
    @Query("select t from Ticket t where t.estado.activo = true and t.usuarioReporta.email = :email "
            + "order by t.prioridad.id asc, t.createdAt desc")
    List<Ticket> listarActivosDe(@Param("email") String email);
}
