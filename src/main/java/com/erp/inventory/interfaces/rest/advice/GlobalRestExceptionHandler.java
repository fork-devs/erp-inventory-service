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
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
import java.util.Locale;
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

    private final MessageSource messageSource;

    public GlobalRestExceptionHandler() {
        this(null);
    }

    public GlobalRestExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private String getMessage(String code, String defaultMessage, Object... args) {
        if (messageSource == null) {
            return defaultMessage;
        }
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

    @ExceptionHandler(TenantContextMissingException.class)
    public ProblemDetail handleTenantContextMissing(TenantContextMissingException ex, WebRequest request) {
        String detail = getMessage("error.inventory.tenant-context-missing.detail", ex.getMessage());
        return problema(ex.getCode(), HttpStatus.BAD_REQUEST, detail, request);
    }

    @ExceptionHandler(SkuAlreadyExistsException.class)
    public ProblemDetail handleSkuAlreadyExists(SkuAlreadyExistsException ex, WebRequest request) {
        String detail = getMessage("error.inventory.sku-already-exists.detail", ex.getMessage());
        return problema(ex.getCode(), HttpStatus.CONFLICT, detail, request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex, WebRequest request) {
        String detail = getMessage("error.inventory.insufficient-stock.detail", ex.getMessage());
        return problema(ex.getCode(), HttpStatus.CONFLICT, detail, request);
    }

    @ExceptionHandler({SparePartNotFoundException.class, BranchNotFoundException.class})
    public ProblemDetail handleNotFound(DomainException ex, WebRequest request) {
        String key = ex instanceof SparePartNotFoundException
                ? "error.inventory.spare-part-not-found.detail"
                : "error.inventory.branch-not-found.detail";
        String detail = getMessage(key, ex.getMessage());
        return problema(ex.getCode(), HttpStatus.NOT_FOUND, detail, request);
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
        String detail = getMessage("error.inventory.access-denied.detail",
                "El principal no tiene el rol que exige el endpoint");
        return problema("INVENTORY_ACCESS_DENIED", HttpStatus.FORBIDDEN, detail, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, WebRequest request) {
        String detail = getMessage("error.inventory.unauthenticated.detail",
                "La peticion requiere un token de acceso verificado");
        return problema("INVENTORY_UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, detail, request);
    }

    /**
     * Non-existent endpoints map to HTTP 404 Not Found to prevent clients
     * from mistaking route errors for temporary server outages.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, WebRequest request) {
        String detail = getMessage("error.inventory.not-found.detail", "El recurso solicitado no existe");
        return problema("INVENTORY_NOT_FOUND", HttpStatus.NOT_FOUND, detail, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse(getMessage("error.inventory.validation-error.detail", "Peticion no valida"));
        ProblemDetail problem = problema("INVENTORY_VALIDATION_ERROR", HttpStatus.BAD_REQUEST, mensaje, request);
        problem.setTitle(getMessage("error.inventory.validation-error.title", HttpStatus.BAD_REQUEST.getReasonPhrase()));
        return problem;
    }

    /**
     * Catches any unhandled exceptions, logging stack traces for operational diagnostics
     * while returning a sanitized generic problem detail response.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex, WebRequest request) {
        log.error("Error no previsto en {} {}", request.getDescription(false), ex.getMessage(), ex);
        String detail = getMessage("error.inventory.internal-error.detail", "Se ha producido un error interno");
        return problema("INVENTORY_INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, detail, request);
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
