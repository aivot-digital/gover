package de.aivot.prosuna.backend.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CsrfResponseHeaderFilter extends OncePerRequestFilter {
    private static final RequestMatcher PROBE_REQUESTS = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/actuator/health"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/actuator/health/liveness"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/actuator/health/readiness")
    );

    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) {
        // Resolving a deferred CSRF token writes to Redis; Kubernetes probes must not depend on it.
        return PROBE_REQUESTS.matches(request);
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
