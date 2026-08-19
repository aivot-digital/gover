package de.aivot.prosuna.backend.department.services;

import de.aivot.prosuna.backend.department.entities.DepartmentMembershipEntity;
import de.aivot.prosuna.backend.department.filters.DepartmentMembershipFilter;
import de.aivot.prosuna.backend.department.repositories.DepartmentMembershipRepository;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.prosuna.backend.userRoles.services.UserRoleAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DepartmentMembershipService implements EntityService<DepartmentMembershipEntity, Integer> {
    private final DepartmentMembershipRepository repository;
    private final DepartmentRepository departmentRepository;
    private final UserService userService;
    private final UserRoleAssignmentService userRoleAssignmentService;

    @Autowired
    public DepartmentMembershipService(DepartmentMembershipRepository repository,
                                       DepartmentRepository departmentRepository,
                                       UserService userService,
                                       UserRoleAssignmentService userRoleAssignmentService) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
        this.userService = userService;
        this.userRoleAssignmentService = userRoleAssignmentService;
    }

    @Nonnull
    @Override
    public DepartmentMembershipEntity create(@Nonnull DepartmentMembershipEntity entity) throws ResponseException {
        entity.setId(null);

        var targetUser = userService
                .retrieve(entity.getUserId())
                .orElseThrow(() -> new ResponseException(HttpStatus.NOT_FOUND, "Die Mitarbeiter:in wurde nicht gefunden."));

        var targetDepartment = departmentRepository
                .findById(entity.getDepartmentId())
                .orElseThrow(() -> new ResponseException(HttpStatus.NOT_FOUND, "Die Organisationseinheit wurde nicht gefunden."));

        var spec = DepartmentMembershipFilter
                .create()
                .setDepartmentId(targetDepartment.getId())
                .setUserId(targetUser.getId())
                .build();

        if (exists(spec)) {
            throw new ResponseException(HttpStatus.CONFLICT, "Diese Mitarbeiter:in ist bereits Teil der Organisationseinheit.");
        }

        return repository.save(entity);
    }

    @Nonnull
    @Transactional(rollbackFor = ResponseException.class)
    public DepartmentMembershipEntity createWithRoles(@Nonnull DepartmentMembershipEntity entity,
                                                      @Nonnull List<Integer> roleIds) throws ResponseException {
        var membership = create(entity);

        // Initial role assignment belongs to membership creation. Later role changes still go through the
        // assignment endpoints and require the department membership update permission.
        createInitialRoleAssignments(membership.getId(), roleIds);

        return membership;
    }

    private void createInitialRoleAssignments(@Nonnull Integer membershipId,
                                              @Nonnull List<Integer> roleIds) throws ResponseException {
        for (var roleId : roleIds.stream().filter(Objects::nonNull).distinct().toList()) {
            userRoleAssignmentService.create(new UserRoleAssignmentEntity()
                    .setDepartmentMembershipId(membershipId)
                    .setTeamMembershipId(null)
                    .setUserRoleId(roleId));
        }
    }

    @Nonnull
    @Override
    public Page<DepartmentMembershipEntity> performList(@Nonnull Pageable pageable,
                                                        @Nullable Specification<DepartmentMembershipEntity> specification,
                                                        @Nullable Filter<DepartmentMembershipEntity> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<DepartmentMembershipEntity> retrieve(@Nonnull Integer id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<DepartmentMembershipEntity> retrieve(
            @Nonnull Specification<DepartmentMembershipEntity> specification
    ) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<DepartmentMembershipEntity> specification
    ) {
        return repository.exists(specification);
    }

    /**
     * @deprecated use checks with permissions instead
     */
    public boolean checkUserInDepartment(UserEntity user, Integer departmentId) {
        var spec = DepartmentMembershipFilter
                .create()
                .setUserId(user.getId())
                .setDepartmentId(departmentId)
                .build();

        return exists(spec);
    }

    /**
     * @deprecated use checks with permissions instead
     */
    public boolean checkUserNotInDepartment(UserEntity user, Integer departmentId) {
        return !checkUserInDepartment(user, departmentId);
    }

    @Nonnull
    @Override
    public DepartmentMembershipEntity performUpdate(@Nonnull Integer id,
                                                    @Nonnull DepartmentMembershipEntity entity,
                                                    @Nonnull DepartmentMembershipEntity existingEntity) throws ResponseException {
        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull DepartmentMembershipEntity entity) throws ResponseException {
        repository.delete(entity);
    }
}
