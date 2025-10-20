package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.dtos.FormDetailsResponseDTO;
import de.aivot.GoverBackend.form.filters.FormVersionWithMembershipFilter;
import de.aivot.GoverBackend.form.services.FormVersionWithMembershipService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RestController
@RequestMapping("/api/form-versions/")
public class FormVersionController {
    private final FormVersionWithMembershipService formVersionWithMembershipService;

    @Autowired
    public FormVersionController(FormVersionWithMembershipService formVersionWithMembershipService) {
        this.formVersionWithMembershipService = formVersionWithMembershipService;
    }

    @GetMapping("")
    public Page<FormDetailsResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid FormVersionWithMembershipFilter filter
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user is not a global admin
        if (filter.getUserId() == null) {
            filter.setUserId(user.getId());
        }

        return formVersionWithMembershipService
                .list(pageable, filter)
                .map(FormDetailsResponseDTO::fromEntity);
    }
}
