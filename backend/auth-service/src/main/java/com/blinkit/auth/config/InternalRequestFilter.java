package com.blinkit.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects any request that did NOT come through the API Gateway.
 *
 * The gateway injects X-Internal-Secret on every forwarded request.
 * Direct calls to this service's port (bypassing the gateway) won't
 * have this header and will receive 403 Forbidden.
 */
@Component
public class InternalRequestFilter extends OncePerRequestFilter {

    @Value("${internal.secret-key}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Allow actuator health checks (used by Eureka + monitoring)
        // Allow Swagger UI and OpenAPI spec (direct browser access for development)
        if (uri.startsWith("/actuator") ||
            uri.startsWith("/swagger-ui") ||
            uri.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("X-Internal-Secret");
        if (header == null || !header.equals(internalSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Access denied. Use the API Gateway.\",\"data\":null}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
