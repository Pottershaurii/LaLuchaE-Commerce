package com.lalucha.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de registro.
 * El frontend actual envia "nombre" (nombre completo). Tambien se aceptan
 * "nombres" y "apellidos" por separado; si llegan, tienen prioridad.
 */
public class RegisterRequest {

    // OWASP A03: los nombres solo aceptan letras, espacios, punto, apostrofe y guion
    @Size(max = 240, message = "El nombre no puede superar 240 caracteres.")
    @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$", message = "El nombre solo puede contener letras y espacios.")
    private String nombre;

    @Size(max = 120, message = "Los nombres no pueden superar 120 caracteres.")
    @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$", message = "Los nombres solo pueden contener letras y espacios.")
    private String nombres;

    @Size(max = 120, message = "Los apellidos no pueden superar 120 caracteres.")
    @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$", message = "Los apellidos solo pueden contener letras y espacios.")
    private String apellidos;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no tiene un formato valido.")
    @Size(max = 180, message = "El correo no puede superar 180 caracteres.")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria.")
    @Size(min = 6, max = 72, message = "La contrasena debe tener entre 6 y 72 caracteres.")
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
