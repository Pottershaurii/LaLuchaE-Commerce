package com.lalucha.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mapea lalucha.asignados (database/incidentes.sql): grupos de soporte por nivel.
 * 1 Help Desk (nivel 1), 2 Especialista de base de datos (nivel 2),
 * 3 Equipo de desarrollo (nivel 3).
 */
@Entity
@Table(name = "asignados")
public class Asignado {

    /** Todo ticket nuevo entra al primer nivel. */
    public static final short HELP_DESK = 1;

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(nullable = false)
    private Short nivel;

    @Column(length = 255)
    private String descripcion;

    public Asignado() {
    }

    public Short getId() { return id; }

    public String getNombre() { return nombre; }

    public Short getNivel() { return nivel; }

    public String getDescripcion() { return descripcion; }
}
