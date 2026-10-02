package com.lalucha.backend.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Mapea la tabla lalucha.roles (ver database/schema.sql).
 * Codigos sembrados: ADMIN, CLIENTE, STAFF.
 */
@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    // La BD lo llena con DEFAULT NOW()
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Rol() {
    }

    public Long getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    /** Nombre de autoridad para Spring Security / JWT, p. ej. "ROLE_CLIENTE". */
    public String getAuthority() {
        return "ROLE_" + codigo;
    }
}
