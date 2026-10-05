package com.lalucha.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    // OWASP A03: formato de correo estricto; un payload como "' OR '1'='1" se rechaza con 400
    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no tiene un formato valido.")
    @Size(max = 180, message = "El correo no puede superar 180 caracteres.")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria.")
    @Size(max = 72, message = "La contrasena no puede superar 72 caracteres.")
    private String password;

    public LoginRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
