package com.lalucha.backend.service;

import com.lalucha.backend.dto.RegisterRequest;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {

    // Mapa para almacenar los códigos de verificación activos
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();
    
    // Mapa para almacenar los datos del usuario en proceso de registro
    private final Map<String, RegisterRequest> pendingUsers = new ConcurrentHashMap<>();
    
    private final SecureRandom random = new SecureRandom();

    /**
     * Genera un código de 6 dígitos y guarda la solicitud de registro de forma temporal.
     */
    public String generateCode(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        int numero = 100000 + random.nextInt(900000);
        String codigo = String.valueOf(numero);

        verificationCodes.put(email, codigo);
        pendingUsers.put(email, request);

        return codigo;
    }

    /**
     * Valida si el código ingresado coincide con el enviado al correo.
     */
    public boolean verifyCode(String email, String codigo) {
        String codigoGuardado = verificationCodes.get(email);
        return codigoGuardado != null && codigoGuardado.equals(codigo);
    }

    /**
     * Obtiene la información del usuario pendiente de verificación.
     */
    public RegisterRequest getPendingUser(String email) {
        return pendingUsers.get(email);
    }

    /**
     * Limpia de la memoria temporal el código y la solicitud procesada.
     */
    public void clear(String email) {
        verificationCodes.remove(email);
        pendingUsers.remove(email);
    }
}