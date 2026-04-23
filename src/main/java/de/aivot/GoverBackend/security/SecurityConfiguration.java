package de.aivot.GoverBackend.security;

import de.aivot.GoverBackend.system.controllers.AuthController;
import de.aivot.GoverBackend.system.properties.CORSProperties;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final CORSProperties corsProperties;

    @Autowired
    public SecurityConfiguration(CORSProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(
                        requests -> requests
                                .requestMatchers("/api/public/**").permitAll()
                                .requestMatchers("/api/actuator/health").permitAll()
                                .requestMatchers("/api/auth/login").permitAll()
                                .requestMatchers("/api/auth/refresh").permitAll()
                                .requestMatchers("/api/auth/logout").permitAll()
                                .requestMatchers("/api/auth/oidc-callback").permitAll()
                                .anyRequest().authenticated()
                )

                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

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

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var source = new UrlBasedCorsConfigurationSource();

        if (corsProperties.isEnabled()) {
            var configuration = new CorsConfiguration();

            for (String origin : corsProperties.getAllowedOrigins()) {
                configuration.addAllowedOrigin(origin);
            }

            configuration.setAllowedMethods(Arrays.asList(CORSProperties.DEFAULT_ALLOWED_METHODS));
            configuration.setAllowedHeaders(List.of("*"));

            source.registerCorsConfiguration("/**", configuration);
        }

        return source;
    }
}
