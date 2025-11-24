package de.aivot.GoverBackend.teams.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.teams.dtos.TeamMembershipRequestDTO;
import de.aivot.GoverBackend.teams.dtos.TeamMembershipResponseDTO;
import de.aivot.GoverBackend.teams.entities.TeamMembershipEntity;
import de.aivot.GoverBackend.teams.filters.TeamMembershipFilter;
import de.aivot.GoverBackend.teams.repositories.TeamMembershipRepository;
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
@RequestMapping("/api/team-memberships/")
public class TeamMembershipController {
    private final ScopedAuditService auditService;
    private final TeamMembershipRepository repository;

    @Autowired
    public TeamMembershipController(
            AuditService auditService,
            TeamMembershipRepository repository
    ) {
        this.auditService = auditService.createScopedAuditService(TeamMembershipController.class);
        this.repository = repository;
    }

    @GetMapping("")
    public Page<TeamMembershipResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid TeamMembershipFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return repository
                .findAll(filter.build(), pageable)
                .map(TeamMembershipResponseDTO::fromEntity);
    }

    @PostMapping("")
    public TeamMembershipResponseDTO create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid TeamMembershipRequestDTO createDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        TeamMembershipEntity result;
        try {
            result = repository
                    .save(createDTO.toEntity());
        } catch (Exception e) {
            throw ResponseException.badRequest("Fehler beim Speichern des Teams", e);
        }

        auditService.logAction(user, AuditAction.Create, TeamMembershipEntity.class, Map.of(
                "id", result.getId(),
                "teamId", result.getTeamId(),
                "userId", result.getUserId()
        ));

        return TeamMembershipResponseDTO.fromEntity(result);
    }

    @GetMapping("{id}/")
    public TeamMembershipResponseDTO retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        TeamMembershipEntity entity = repository
                .findById(id)
                .orElseThrow(ResponseException::notFound);
        return TeamMembershipResponseDTO.fromEntity(entity);
    }

    @PutMapping("{id}/")
    public TeamMembershipResponseDTO update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @RequestBody @Valid TeamMembershipRequestDTO updateDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        TeamMembershipEntity entityToUpdate = updateDTO.toEntity();
        entityToUpdate.setId(id);

        TeamMembershipEntity result;
        try {
            result = repository
                    .save(entityToUpdate);
        } catch (Exception e) {
            throw ResponseException.badRequest("Fehler beim Speichern des Teams", e);
        }

        auditService.logAction(user, AuditAction.Update, TeamMembershipEntity.class, Map.of(
                "id", result.getId(),
                "teamId", result.getTeamId(),
                "userId", result.getUserId()
        ));

        return TeamMembershipResponseDTO.fromEntity(result);
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

        var entity = repository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        repository
                .delete(entity);

        auditService.logAction(user, AuditAction.Delete, TeamMembershipEntity.class, Map.of(
                "id", entity.getId(),
                "teamId", entity.getTeamId(),
                "userId", entity.getUserId()
        ));
    }
}
