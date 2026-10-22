package com.erp.inventory.interfaces.rest.advice;

import com.erp.inventory.domain.exception.BranchNotFoundException;
import com.erp.inventory.domain.exception.DomainException;
import com.erp.inventory.domain.exception.InsufficientStockException;
import com.erp.inventory.domain.exception.SkuAlreadyExistsException;
import com.erp.inventory.domain.exception.SparePartNotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@Hidden
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @ExceptionHandler({SparePartNotFoundException.class, BranchNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(DomainException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(SkuAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(DomainException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomain(DomainException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst().orElse("Validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(msg));
    }

    /**
     * La denegacion por rol es una decision de autorizacion, no un fallo de la
     * operacion: se responde 403 y no 500. Sin este manejador, el generico
     * la convertiria en un error del servidor, que dice al cliente que
     * reintente lo mismo.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody("INVENTORY_ACCESS_DENIED",
                        "El principal no tiene el rol que exige el endpoint"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("INVENTORY_UNAUTHENTICATED",
                        "La peticion requiere un token de acceso verificado"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Error interno del servidor: " + ex.getMessage()));
    }

    private Map<String, Object> errorBody(String message) {
        return errorBody("INVENTORY_ERROR", message);
    }

    private Map<String, Object> errorBody(String code, String message) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("code", code);
        body.put("error", message);
        return body;
    }
}
