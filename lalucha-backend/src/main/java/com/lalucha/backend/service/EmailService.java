package com.lalucha.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(
            String recipientEmail,
            String code) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);

        message.setTo(recipientEmail);

        message.setSubject(
                "Código de verificación - La Lucha"
        );

        message.setText(
                "¡Bienvenido a La Lucha Sanguchería!\n\n"
                        + "Tu código de verificación es:\n\n"
                        + code
                        + "\n\n"
                        + "Ingresa este código en la plataforma "
                        + "para verificar tu correo electrónico.\n\n"
                        + "Si no realizaste este registro, "
                        + "puedes ignorar este mensaje."
        );

        mailSender.send(message);

        System.out.println(
                "Correo enviado correctamente a: "
                        + recipientEmail
        );
    }
}