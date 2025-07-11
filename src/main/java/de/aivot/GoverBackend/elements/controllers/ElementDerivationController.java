package de.aivot.GoverBackend.elements.controllers;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RestController
@RequestMapping("/api/elements/")
public class ElementDerivationController {
    private final ElementDerivationService elementDerivationService;

    public ElementDerivationController(ElementDerivationService elementDerivationService) {
        this.elementDerivationService = elementDerivationService;
    }

    @PostMapping("derive/")
    public ElementData derive(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ElementDerivationRequest request
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return elementDerivationService
                .derive(request);
    }
}
