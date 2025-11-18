package de.aivot.GoverBackend.teams.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.teams.entities.TeamEntity;
import de.aivot.GoverBackend.teams.filters.TeamFilter;
import de.aivot.GoverBackend.teams.repositories.TeamRepository;
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
@RequestMapping("/api/teams/")
public class TeamController {
    private final ScopedAuditService auditService;
    private final TeamRepository teamRepository;

    @Autowired
    public TeamController(
            AuditService auditService,
            TeamRepository teamRepository
    ) {
        this.auditService = auditService.createScopedAuditService(TeamController.class);
        this.teamRepository = teamRepository;
    }

    @GetMapping("")
    public Page<TeamEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid TeamFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return teamRepository
                .findAll(filter.build(), pageable);
    }

    @PostMapping("")
    public TeamEntity create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid TeamEntity createDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        TeamEntity result;
        try {
            result = teamRepository
                    .save(createDTO);
        } catch (Exception e) {
            throw ResponseException.badRequest("Fehler beim Speichern des Teams", e);
        }

        auditService.logAction(user, AuditAction.Create, TeamEntity.class, Map.of(
                "id", result.getId(),
                "name", result.getName()
        ));

        return result;
    }

    @GetMapping("{id}/")
    public TeamEntity retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return teamRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    public TeamEntity update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @RequestBody @Valid TeamEntity updateDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        // Ensure the entity exists
        updateDTO.setId(id);

        TeamEntity result;
        try {
            result = teamRepository
                    .save(updateDTO);
        } catch (Exception e) {
            throw ResponseException.badRequest("Fehler beim Speichern des Teams", e);
        }

        auditService.logAction(user, AuditAction.Update, TeamEntity.class, Map.of(
                "id", result.getId(),
                "name", result.getName()
        ));

        return result;
    }

    @DeleteMapping("{id}/")
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = teamRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        teamRepository
                .delete(entity);

        auditService.logAction(user, AuditAction.Delete, TeamEntity.class, Map.of(
                "id", entity.getId(),
                "name", entity.getName()
        ));

    }
}
