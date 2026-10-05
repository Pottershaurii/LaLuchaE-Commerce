package com.lalucha.backend.config;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OWASP A03 - Injection.
 * Las peticiones que no cumplen las reglas de los DTOs (@Valid) se rechazan
 * con 400 antes de llegar a la logica de negocio o a la base de datos,
 * indicando que campo fallo, sin exponer trazas internas del servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("success", false);
        respuesta.put("message", "Los datos enviados no son validos.");
        respuesta.put("errores", errores);
        return ResponseEntity.badRequest().body(respuesta);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonMalFormado(HttpMessageNotReadableException ex) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("success", false);
        respuesta.put("message", "El cuerpo de la peticion no es un JSON valido.");
        return ResponseEntity.badRequest().body(respuesta);
    }
}
