package de.aivot.prosuna.backend.security;

import de.aivot.prosuna.backend.system.controllers.AuthController;
import de.aivot.prosuna.backend.user.repositories.UserApiKeyRepository;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.*;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    /**
     * State-changing requests that need a CSRF token: every unsafe method except for the anonymous public endpoints, the
     * actuator and requests authenticated by an API key in the authorization header.
     */
    private static final RequestMatcher CSRF_PROTECTION_MATCHER = new AndRequestMatcher(
            CsrfFilter.DEFAULT_CSRF_MATCHER,
            new NegatedRequestMatcher(new OrRequestMatcher(
                    PathPatternRequestMatcher.withDefaults().matcher("/api/public/**"),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/actuator/**"),
                    request -> ApiKeyJwtDecoder.resolveApiKey(request) != null
            ))
    );

    /**
     * Anonymous health checks: the overall status and the Kubernetes probes. HEAD is allowed as well as GET, because the
     * app container waits for the API with HEAD requests and external monitors may use them too.
     */
    static final RequestMatcher HEALTH_CHECK_REQUESTS = new OrRequestMatcher(
            Stream.of("/api/actuator/health", "/api/actuator/health/liveness", "/api/actuator/health/readiness")
                    .flatMap(path -> Stream.<RequestMatcher>of(
                            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, path),
                            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.HEAD, path)
                    ))
                    .toList()
    );

    private final RedisCsrfTokenRepository csrfTokenRepository;
    private final CsrfResponseHeaderFilter csrfResponseHeaderFilter;
    private final JwtDecoder jwtDecoder;
    private final UserApiKeyRepository userApiKeyRepository;

    @Autowired
    public SecurityConfiguration(
            RedisCsrfTokenRepository csrfTokenRepository,
            CsrfResponseHeaderFilter csrfResponseHeaderFilter,
            JwtDecoder jwtDecoder,
            // Lazy, so slice tests that import this configuration without JPA repositories still start
            @Lazy UserApiKeyRepository userApiKeyRepository
    ) {
        this.csrfTokenRepository = csrfTokenRepository;
        this.csrfResponseHeaderFilter = csrfResponseHeaderFilter;
        this.jwtDecoder = jwtDecoder;
        this.userApiKeyRepository = userApiKeyRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        // The resource server exempts every request that carries a bearer token from CSRF protection,
                        // because bearer tokens are usually sent in a header. Prosuna reads the staff token from the
                        // access cookie, which browsers attach automatically, so these requests need CSRF protection.
                        // The resource server adds its exemption to the configured matcher, so the matcher of the
                        // filter is replaced after all configurers ran.
                        .withObjectPostProcessor(new ObjectPostProcessor<CsrfFilter>() {
                            @Override
                            public <O extends CsrfFilter> O postProcess(O filter) {
                                filter.setRequireCsrfProtectionMatcher(CSRF_PROTECTION_MATCHER);
                                return filter;
                            }
                        })
                )

                .authorizeHttpRequests(
                        requests -> requests
                                .requestMatchers("/api/public/**").permitAll()
                                .requestMatchers(HEALTH_CHECK_REQUESTS).permitAll()
                                .requestMatchers("/api/auth/login").permitAll()
                                .requestMatchers("/api/auth/refresh").permitAll()
                                .requestMatchers("/api/auth/logout").permitAll()
                                .requestMatchers("/api/auth/oidc-callback").permitAll()
                                .requestMatchers("/api/error").permitAll()
                                .anyRequest().authenticated()
                )

                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                        .decoder(new ApiKeyJwtDecoder(jwtDecoder, userApiKeyRepository))
                ))

                .addFilterAfter(csrfResponseHeaderFilter, CsrfFilter.class);

        return http.build();
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver(@Nonnull JwtDecoder decoder) {
        return new BearerResolver(AuthController.ACCESS_COOKIE_NAME, decoder);
    }

    private record BearerResolver(@Nonnull String cookieName,
                                  @Nonnull JwtDecoder decoder) implements BearerTokenResolver {
        @Override
        public String resolve(@Nonnull HttpServletRequest request) {
            // An API key in the header wins over the access cookie, so the CSRF exemption of API key requests can
            // never apply to a request that is authenticated by the cookie
            var apiKey = ApiKeyJwtDecoder.resolveApiKey(request);
            if (apiKey != null) {
                return apiKey;
            }

            Cookie[] cookies = request.getCookies();

            if (cookies == null) {
                return null;
            }

            return Arrays
                    .stream(cookies)
                    .filter(cookie -> cookie.getName().equals(cookieName))
                    .map(Cookie::getValue)
                    .findFirst()
                    // API keys are only accepted from the authorization header, never from the access cookie
                    .filter(token -> !token.startsWith(ApiKeyJwtDecoder.API_KEY_PREFIX))
                    .orElse(null);
        }
    }
}
