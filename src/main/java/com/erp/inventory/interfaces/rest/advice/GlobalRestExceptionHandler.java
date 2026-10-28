package com.erp.inventory.interfaces.rest.advice;

import com.erp.inventory.domain.exception.BranchNotFoundException;
import com.erp.inventory.domain.exception.DomainException;
import com.erp.inventory.domain.exception.InsufficientStockException;
import com.erp.inventory.domain.exception.SkuAlreadyExistsException;
import com.erp.inventory.domain.exception.SparePartNotFoundException;
import com.erp.inventory.domain.exception.TenantContextMissingException;
import com.erp.inventory.infrastructure.persistence.rls.TenantContextHolder;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.UUID;

/**
 * Global REST exception handler mapping domain and framework exceptions to RFC 9457 {@code ProblemDetail}.
 *
 * <p>Provides a <strong>stable, machine-readable error code</strong> per family: error messages
 * can be refined without breaking client contracts, whereas code values remain stable. Error
 * responses also include the tenant context when available.
 *
 * <p>Internal 500 errors do not expose internal exception classes or stack traces to callers:
 * diagnostics are logged for operator observability.
 */
@Hidden
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

    private static final String TIPO_BASE = "https://erp-automotive.pe/errors/";

    @ExceptionHandler(TenantContextMissingException.class)
    public ProblemDetail handleTenantContextMissing(TenantContextMissingException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(SkuAlreadyExistsException.class)
    public ProblemDetail handleSkuAlreadyExists(SkuAlreadyExistsException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({SparePartNotFoundException.class, BranchNotFoundException.class})
    public ProblemDetail handleNotFound(DomainException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Role denial represents an authorization decision: mapped to HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return problema("INVENTORY_ACCESS_DENIED", HttpStatus.FORBIDDEN,
                "El principal no tiene el rol que exige el endpoint", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, WebRequest request) {
        return problema("INVENTORY_UNAUTHENTICATED", HttpStatus.UNAUTHORIZED,
                "La peticion requiere un token de acceso verificado", request);
    }

    /**
     * Non-existent endpoints map to HTTP 404 Not Found to prevent clients
     * from mistaking route errors for temporary server outages.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, WebRequest request) {
        return problema("INVENTORY_NOT_FOUND", HttpStatus.NOT_FOUND,
                "El recurso solicitado no existe", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Peticion no valida");
        return problema("INVENTORY_VALIDATION_ERROR", HttpStatus.BAD_REQUEST, mensaje, request);
    }

    /**
     * Catches any unhandled exceptions, logging stack traces for operational diagnostics
     * while returning a sanitized generic problem detail response.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex, WebRequest request) {
        log.error("Error no previsto en {} {}", request.getDescription(false), ex.getMessage(), ex);
        return problema("INVENTORY_INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
                "Se ha producido un error interno", request);
    }

    private ProblemDetail problema(String codigo, HttpStatus estado, String detalle, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(estado, detalle);
        problem.setTitle(estado.getReasonPhrase());
        problem.setType(URI.create(TIPO_BASE + codigo.toLowerCase().replace('_', '-')));
        problem.setProperty("code", codigo);

        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            problem.setProperty("tenant_id", tenantId.toString());
        }
        return problem;
    }
}
