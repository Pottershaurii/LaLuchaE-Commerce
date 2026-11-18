package com.lalucha.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mapea lalucha.prioridades (database/incidentes.sql).
 * Matriz impacto x urgencia: P1 Critico, P2 Alto, P3 Normal, P4 Bajo,
 * cada una con su tiempo objetivo de resolucion en horas (SLA).
 */
@Entity
@Table(name = "prioridades")
public class Prioridad {

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 2)
    private String codigo;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(nullable = false, length = 10)
    private String impacto;

    @Column(nullable = false, length = 10)
    private String urgencia;

    @Column(name = "horas_objetivo", nullable = false)
    private Integer horasObjetivo;

    public Prioridad() {
    }

    public Short getId() { return id; }

    public String getCodigo() { return codigo; }

    public String getNombre() { return nombre; }

    public String getImpacto() { return impacto; }

    public String getUrgencia() { return urgencia; }

    public Integer getHorasObjetivo() { return horasObjetivo; }
}
