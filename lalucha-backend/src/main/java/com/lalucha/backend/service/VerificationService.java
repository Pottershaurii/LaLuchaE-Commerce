package com.lalucha.backend.service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class VerificationService {

    private final Map<String, String> verificationCodes =
            new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    public String generateCode(String email) {

        int numero = 100000 + random.nextInt(900000);

        String codigo = String.valueOf(numero);

        verificationCodes.put(email, codigo);

        return codigo;
    }

    public boolean verifyCode(String email, String codigo) {

        String codigoGuardado = verificationCodes.get(email);

        if (codigoGuardado == null) {
            return false;
        }

        boolean valido = codigoGuardado.equals(codigo);

        if (valido) {
            verificationCodes.remove(email);
        }

        return valido;
    }
}