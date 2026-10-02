package com.lalucha.backend.dto;

/**
 * Datos de registro.
 * El frontend actual envia "nombre" (nombre completo). Tambien se aceptan
 * "nombres" y "apellidos" por separado; si llegan, tienen prioridad.
 */
public class RegisterRequest {

    private String nombre;
    private String nombres;
    private String apellidos;
    private String email;
    private String password;

    public RegisterRequest() {
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    /** Nombres a guardar: "nombres" si viene, si no la primera palabra de "nombre". */
    public String resolverNombres() {
        if (nombres != null && !nombres.isBlank()) {
            return nombres.trim();
        }
        String completo = nombre == null ? "" : nombre.trim();
        int espacio = completo.indexOf(' ');
        return espacio < 0 ? completo : completo.substring(0, espacio);
    }

    /** Apellidos a guardar: "apellidos" si viene, si no el resto de "nombre". */
    public String resolverApellidos() {
        if (apellidos != null && !apellidos.isBlank()) {
            return apellidos.trim();
        }
        if (nombres != null && !nombres.isBlank()) {
            return "";
        }
        String completo = nombre == null ? "" : nombre.trim();
        int espacio = completo.indexOf(' ');
        return espacio < 0 ? "" : completo.substring(espacio + 1).trim();
    }
}
