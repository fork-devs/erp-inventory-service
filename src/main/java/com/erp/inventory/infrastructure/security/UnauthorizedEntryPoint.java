package com.erp.inventory.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authentication entry point returning <strong>401 Unauthorized</strong> for unauthenticated requests.
 *
 * <p>Without this entry point, Spring Security defaults to 403 Forbidden for anonymous requests,
 * confusing clients by mischaracterizing unauthenticated access as unauthorized access.
 */
@Component
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(UnauthorizedEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws java.io.IOException {
        log.warn("Peticion sin principal verificado: {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://erp-automotive.pe/errors/security");
        body.put("title", "Unauthorized");
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("detail", "La peticion requiere un token de acceso verificado");
        body.put("timestamp", Instant.now().toString());

        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body));
    }
}
