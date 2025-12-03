package de.aivot.GoverBackend.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth/")
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {
    private static final String AUTH_PATH = "/protocol/openid-connect/auth";

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerURI;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @GetMapping("login/")
    @Operation(
            summary = "Login",
            description = "Redirects the user to the authentication provider login page or directly to the specified redirect URL if already authenticated."
    )
    public void redirectToLogin(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull HttpServletResponse response,
            @Nonnull @RequestParam(required = true) String redirect
    ) throws IOException {
        if (jwt != null) {
            response.sendRedirect(redirect);
        }

        var uri = UriComponentsBuilder
                .fromUriString(issuerURI)
                .path(AUTH_PATH)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirect)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .build()
                .toString();

        response
                .sendRedirect(uri);
    }
}
