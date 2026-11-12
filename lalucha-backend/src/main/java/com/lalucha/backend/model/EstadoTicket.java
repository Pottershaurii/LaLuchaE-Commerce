package com.lalucha.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mapea lalucha.estados_ticket (database/incidentes.sql).
 * Ciclo de vida: 1 Abierto, 2 En diagnostico, 3 Resuelto, 4 Cerrado.
 */
@Entity
@Table(name = "estados_ticket")
public class EstadoTicket {

    /** Estado con el que nace todo ticket. */
    public static final short ABIERTO = 1;

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 40)
    private String nombre;

    /** true = el ticket sigue en seguimiento y se muestra en el panel. */
    @Column(nullable = false)
    private boolean activo;

    public EstadoTicket() {
    }

    public Short getId() { return id; }

    public String getNombre() { return nombre; }

    public boolean isActivo() { return activo; }
}
