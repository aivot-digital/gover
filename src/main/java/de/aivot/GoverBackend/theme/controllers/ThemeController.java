package de.aivot.GoverBackend.theme.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.theme.dtos.ThemeRequestDTO;
import de.aivot.GoverBackend.theme.dtos.ThemeResponseDTO;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.theme.filters.ThemeFilter;
import de.aivot.GoverBackend.theme.services.ThemeService;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@RestController
@RequestMapping("/api/themes/")
public class ThemeController {
    private final ScopedAuditService auditService;
    private final ThemeService service;

    @Autowired
    public ThemeController(AuditService auditService,
                           ThemeService service) {
        this.auditService = auditService.createScopedAuditService(ThemeController.class);
        this.service = service;
    }

    @GetMapping("")
    public Page<ThemeResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid ThemeFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return service
                .list(pageable, filter)
                .map(ThemeResponseDTO::fromEntity);
    }

    @PostMapping("")
    public ThemeResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody ThemeRequestDTO newThemeRequest
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asGlobalAdmin()
                .orElseThrow(ResponseException::forbidden);

        var newTheme = newThemeRequest
                .toEntity();

        var createdTheme = service
                .create(newTheme);

        auditService.logAction(user, AuditAction.Create, ThemeEntity.class, Map.of(
                "id", createdTheme.getId(),
                "name", createdTheme.getName()
        ));

        return ThemeResponseDTO
                .fromEntity(createdTheme);
    }

    @GetMapping("{id}/")
    public ThemeResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return service
                .retrieve(id)
                .map(ThemeResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }


    @PutMapping("{id}/")
    public ThemeResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody ThemeRequestDTO changeThemeRequest
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asGlobalAdmin()
                .orElseThrow(ResponseException::forbidden);

        var changedTheme = changeThemeRequest
                .toEntity();

        var updatedTheme = service
                .update(id, changedTheme);

        auditService.logAction(user, AuditAction.Update, ThemeEntity.class, Map.of(
                "id", updatedTheme.getId(),
                "name", updatedTheme.getName()
        ));

        return ThemeResponseDTO
                .fromEntity(updatedTheme);
    }

    @DeleteMapping("{id}/")
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asGlobalAdmin()
                .orElseThrow(ResponseException::forbidden);

        var deletedTheme = service
                .delete(id);

        auditService.logAction(user, AuditAction.Delete, ThemeEntity.class, Map.of(
                "id", deletedTheme.getId(),
                "name", deletedTheme.getName()
        ));
    }
}
