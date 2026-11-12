package com.lalucha.backend.repository;

import com.lalucha.backend.model.EstadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstadoTicketRepository extends JpaRepository<EstadoTicket, Short> {
}
