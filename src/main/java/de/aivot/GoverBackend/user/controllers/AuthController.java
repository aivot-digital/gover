package de.aivot.GoverBackend.user.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

@RestController
@RequestMapping("/api/auth/")
public class AuthController {
    private static final String AUTH_PATH = "/protocol/openid-connect/auth";

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerURI;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @GetMapping("login/")
    public void list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull HttpServletResponse response,
            @Nonnull @RequestParam String redirect
    ) throws ResponseException, IOException {
        if (jwt != null) {
            UserService
                    .fromJWT(jwt)
                    .orElseThrow(ResponseException::unauthorized);

            response
                    .sendRedirect(redirect);
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
