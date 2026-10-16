package de.aivot.prosuna.backend.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CsrfResponseHeaderFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        // Resolving a deferred CSRF token writes to Redis; health checks and Kubernetes probes must not depend on it.
        return SecurityConfiguration.HEALTH_CHECK_REQUESTS.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            response.setHeader(RedisCsrfTokenRepository.HEADER_NAME, csrfToken.getToken());
        }

        filterChain.doFilter(request, response);
    }
}
