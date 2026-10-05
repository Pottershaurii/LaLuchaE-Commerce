package com.lalucha.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VerifyRequest {

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no tiene un formato valido.")
    @Size(max = 180, message = "El correo no puede superar 180 caracteres.")
    private String email;

    // Solo se aceptan exactamente 6 digitos
    @NotBlank(message = "El codigo es obligatorio.")
    @Pattern(regexp = "^\\d{6}$", message = "El codigo debe tener exactamente 6 digitos.")
    private String code;

    public VerifyRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
