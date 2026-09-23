package com.lalucha.backend.controller;

import com.lalucha.backend.dto.RegisterRequest;
import com.lalucha.backend.dto.VerifyRequest;
import com.lalucha.backend.service.EmailService;
import com.lalucha.backend.service.VerificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174",
    "http://localhost:5175"
})
public class AuthController {

    private final EmailService emailService;

    private final VerificationService verificationService;

    public AuthController(
            EmailService emailService,
            VerificationService verificationService) {

        this.emailService = emailService;
        this.verificationService =
                verificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody RegisterRequest request) {

        Map<String, Object> response =
                new HashMap<>();

        try {

            if (request.getEmail() == null
                    || request.getEmail().isBlank()) {

                response.put("success", false);
                response.put(
                        "message",
                        "Debes ingresar un correo electrónico."
                );

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }

            String email =
                    request.getEmail()
                            .trim()
                            .toLowerCase();

            String code =
                    verificationService
                            .generateCode(email);

            emailService.sendVerificationCode(
                    email,
                    code
            );

            response.put("success", true);

            response.put(
                    "message",
                    "Código de verificación enviado correctamente."
            );

            response.put("email", email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("success", false);

            response.put(
                    "message",
                    "No se pudo enviar el correo de verificación."
            );

            return ResponseEntity
                    .internalServerError()
                    .body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody VerifyRequest request) {

        Map<String, Object> response =
                new HashMap<>();

        boolean valid =
                verificationService.verifyCode(
                        request.getEmail(),
                        request.getCode()
                );

        if (!valid) {

            response.put("success", false);

            response.put(
                    "message",
                    "El código de verificación es incorrecto."
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }

        response.put("success", true);

        response.put(
                "message",
                "Correo verificado correctamente."
        );

        return ResponseEntity.ok(response);
    }
}