package de.aivot.prosuna.backend.teams.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.teams.entities.TeamMembershipEntity;
import de.aivot.prosuna.backend.teams.filters.TeamMembershipFilter;
import de.aivot.prosuna.backend.teams.repositories.TeamMembershipRepository;
import de.aivot.prosuna.backend.teams.repositories.TeamRepository;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import de.aivot.prosuna.backend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.prosuna.backend.userRoles.services.UserRoleAssignmentService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TeamMembershipService implements EntityService<TeamMembershipEntity, Integer> {

    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final UserRoleAssignmentService userRoleAssignmentService;

    @Autowired
    public TeamMembershipService(TeamMembershipRepository teamMembershipRepository,
                                 TeamRepository teamRepository,
                                 UserRepository userRepository,
                                 UserRoleAssignmentService userRoleAssignmentService) {
        this.teamMembershipRepository = teamMembershipRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.userRoleAssignmentService = userRoleAssignmentService;
    }

    @Nonnull
    @Override
    public TeamMembershipEntity create(@Nonnull TeamMembershipEntity entity) throws ResponseException {
        entity.setId(null);

        // Check if the team exists
        if (!teamRepository.existsById(entity.getTeamId()))  {
            throw ResponseException.badRequest("Das angegebene Team existiert nicht.");
        }

        // Check if user exists
        if (!userRepository.existsById(entity.getUserId()))  {
            throw ResponseException.badRequest("Der angegebene Benutzer existiert nicht.");
        }

        var spec = TeamMembershipFilter
                .create()
                .setTeamId(entity.getTeamId())
                .setUserId(entity.getUserId())
                .build();

        if (exists(spec)) {
            throw ResponseException.conflict("Diese Mitarbeiter:in ist bereits Teil des Teams.");
        }

        return teamMembershipRepository.save(entity);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public TeamMembershipEntity createWithRoles(@Nonnull TeamMembershipEntity entity,
                                                @Nonnull List<Integer> roleIds) throws ResponseException {
        var membership = create(entity);

        // Initial role assignment belongs to membership creation. Later role changes still go through the
        // assignment endpoints and require the team membership update permission.
        createInitialRoleAssignments(membership.getId(), roleIds);

        return membership;
    }

    private void createInitialRoleAssignments(@Nonnull Integer membershipId,
                                              @Nonnull List<Integer> roleIds) throws ResponseException {
        for (var roleId : roleIds.stream().filter(Objects::nonNull).distinct().toList()) {
            userRoleAssignmentService.create(new UserRoleAssignmentEntity()
                    .setDepartmentMembershipId(null)
                    .setTeamMembershipId(membershipId)
                    .setUserRoleId(roleId));
        }
    }

    @Nullable
    @Override
    public Page<TeamMembershipEntity> performList(@Nonnull Pageable pageable,
                                                  @Nullable Specification<TeamMembershipEntity> specification,
                                                  @Nullable Filter<TeamMembershipEntity> filter) throws ResponseException {
        return teamMembershipRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<TeamMembershipEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return teamMembershipRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<TeamMembershipEntity> retrieve(@Nonnull Specification<TeamMembershipEntity> specification) throws ResponseException {
        return teamMembershipRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return teamMembershipRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<TeamMembershipEntity> specification) {
        return teamMembershipRepository.exists(specification);
    }

    @Nonnull
    @Override
    public TeamMembershipEntity performUpdate(@Nonnull Integer id,
                                              @Nonnull TeamMembershipEntity entity,
                                              @Nonnull TeamMembershipEntity existingEntity) throws ResponseException {
        return existingEntity;
    }

    @Override
    public void performDelete(@Nonnull TeamMembershipEntity entity) throws ResponseException {
        teamMembershipRepository.delete(entity);
    }
}
