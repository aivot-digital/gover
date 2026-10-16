package de.aivot.prosuna.backend.security;

import de.aivot.prosuna.backend.system.controllers.AuthController;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    /**
     * State-changing requests that need a CSRF token: every unsafe method except for the anonymous public endpoints and
     * the actuator.
     */
    private static final RequestMatcher CSRF_PROTECTION_MATCHER = new AndRequestMatcher(
            CsrfFilter.DEFAULT_CSRF_MATCHER,
            new NegatedRequestMatcher(new OrRequestMatcher(
                    PathPatternRequestMatcher.withDefaults().matcher("/api/public/**"),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/actuator/**")
            ))
    );

    private final RedisCsrfTokenRepository csrfTokenRepository;
    private final CsrfResponseHeaderFilter csrfResponseHeaderFilter;

    @Autowired
    public SecurityConfiguration(
            RedisCsrfTokenRepository csrfTokenRepository,
            CsrfResponseHeaderFilter csrfResponseHeaderFilter
    ) {
        this.csrfTokenRepository = csrfTokenRepository;
        this.csrfResponseHeaderFilter = csrfResponseHeaderFilter;
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
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/actuator/health",
                                        "/api/actuator/health/liveness",
                                        "/api/actuator/health/readiness"
                                ).permitAll()
                                .requestMatchers("/api/auth/login").permitAll()
                                .requestMatchers("/api/auth/refresh").permitAll()
                                .requestMatchers("/api/auth/logout").permitAll()
                                .requestMatchers("/api/auth/oidc-callback").permitAll()
                                .requestMatchers("/api/error").permitAll()
                                .anyRequest().authenticated()
                )

                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))

                .addFilterAfter(csrfResponseHeaderFilter, CsrfFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverterForKeycloak() {
        Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter = jwt -> {
            // All of this is deprecated since 20.04.2026
            Map<String, Collection<String>> realmAccess = jwt.getClaim("realm_access");
            Collection<String> roles = realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        };

        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver(@Nonnull JwtDecoder decoder) {
        return new BearerResolver(AuthController.ACCESS_COOKIE_NAME, decoder);
    }

    private record BearerResolver(@Nonnull String cookieName,
                                  @Nonnull JwtDecoder decoder) implements BearerTokenResolver {
        @Override
        public String resolve(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();

            if (cookies == null) {
                return null;
            }

            return Arrays
                    .stream(cookies)
                    .filter(cookie -> cookie.getName().equals(cookieName))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
    }
}
