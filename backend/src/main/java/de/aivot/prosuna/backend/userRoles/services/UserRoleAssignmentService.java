package de.aivot.prosuna.backend.userRoles.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.prosuna.backend.userRoles.repositories.UserRoleAssignmentRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
public class UserRoleAssignmentService implements EntityService<UserRoleAssignmentEntity, Integer> {
    private final UserRoleAssignmentRepository repository;

    @Autowired
    public UserRoleAssignmentService(UserRoleAssignmentRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public UserRoleAssignmentEntity create(@Nonnull UserRoleAssignmentEntity entity) throws ResponseException {
        entity.setId(null);
        return repository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull UserRoleAssignmentEntity entity) throws ResponseException {
        repository.delete(entity);
    }

    @Nullable
    @Override
    public Page<UserRoleAssignmentEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<UserRoleAssignmentEntity> specification, @Nullable Filter<UserRoleAssignmentEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public UserRoleAssignmentEntity performUpdate(@Nonnull Integer id, @Nonnull UserRoleAssignmentEntity entity, @Nonnull UserRoleAssignmentEntity existingEntity) throws ResponseException {
        // This entity has no updatable fields
        return repository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<UserRoleAssignmentEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<UserRoleAssignmentEntity> retrieve(@Nonnull Specification<UserRoleAssignmentEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<UserRoleAssignmentEntity> specification) {
        return repository.exists(specification);
    }

    public record RoleChanges(@Nonnull List<UserRoleAssignmentEntity> created,
                              @Nonnull List<UserRoleAssignmentEntity> deleted) {
    }

    @Nonnull
    public RoleChanges replaceDepartmentMembershipRoles(@Nonnull Integer membershipId,
                                                        @Nonnull Collection<Integer> roleIds) throws ResponseException {
        return replaceRoles(repository.findAllByDepartmentMembershipId(membershipId), roleIds, roleId -> new UserRoleAssignmentEntity()
                .setDepartmentMembershipId(membershipId)
                .setUserRoleId(roleId));
    }

    @Nonnull
    public RoleChanges replaceTeamMembershipRoles(@Nonnull Integer membershipId,
                                                  @Nonnull Collection<Integer> roleIds) throws ResponseException {
        return replaceRoles(repository.findAllByTeamMembershipId(membershipId), roleIds, roleId -> new UserRoleAssignmentEntity()
                .setTeamMembershipId(membershipId)
                .setUserRoleId(roleId));
    }

    @Nonnull
    private RoleChanges replaceRoles(@Nonnull List<UserRoleAssignmentEntity> current,
                                     @Nonnull Collection<Integer> roleIds,
                                     @Nonnull Function<Integer, UserRoleAssignmentEntity> newAssignment) throws ResponseException {
        var wanted = roleIds.stream().filter(Objects::nonNull).distinct().toList();
        var deleted = current.stream().filter(assignment -> !wanted.contains(assignment.getUserRoleId())).toList();
        var kept = current.stream().map(UserRoleAssignmentEntity::getUserRoleId).toList();

        for (var assignment : deleted) {
            deleteEntity(assignment);
        }

        var created = new ArrayList<UserRoleAssignmentEntity>();
        for (var roleId : wanted) {
            if (!kept.contains(roleId)) {
                created.add(create(newAssignment.apply(roleId)));
            }
        }
        return new RoleChanges(created, deleted);
    }
}
