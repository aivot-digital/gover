package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import de.aivot.GoverBackend.department.filters.DepartmentMembershipFilter;
import de.aivot.GoverBackend.department.repositories.DepartmentMembershipRepository;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.submission.filters.SubmissionFilter;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class DepartmentMembershipService implements EntityService<DepartmentMembershipEntity, Integer> {
    private final DepartmentMembershipRepository repository;
    private final DepartmentRepository departmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserService userService;

    @Autowired
    public DepartmentMembershipService(
            DepartmentMembershipRepository repository,
            DepartmentRepository departmentRepository,
            SubmissionRepository submissionRepository,
            UserService userService
    ) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
        this.submissionRepository = submissionRepository;
        this.userService = userService;
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
                .orElseThrow(() -> new ResponseException(HttpStatus.NOT_FOUND, "Der Fachbereich wurde nicht gefunden."));

        var spec = DepartmentMembershipFilter
                .create()
                .setDepartmentId(targetDepartment.getId())
                .setUserId(targetUser.getId())
                .build();

        if (exists(spec)) {
            throw new ResponseException(HttpStatus.CONFLICT, "Diese Mitarbeiter:in ist bereits teil des Fachbereichs.");
        }

        return repository.save(entity);
    }

    @Nonnull
    @Override
    public Page<DepartmentMembershipEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<DepartmentMembershipEntity> specification,
            Filter<DepartmentMembershipEntity> filter) {
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

    public boolean checkUserInDepartment(UserEntity user, Integer departmentId) {
        var spec = DepartmentMembershipFilter
                .create()
                .setUserId(user.getId())
                .setDepartmentId(departmentId)
                .build();

        return exists(spec);
    }

    public boolean checkUserNotInDepartment(UserEntity user, Integer departmentId) {
        return !checkUserInDepartment(user, departmentId);
    }

    public boolean checkUserInDepartment(UserEntity user, Integer departmentId, UserRole role) {
        var spec = DepartmentMembershipFilter
                .create()
                .setUserId(user.getId())
                .setDepartmentId(departmentId)
                .setRole(role)
                .build();

        return exists(spec);
    }

    public boolean checkUserNotInDepartment(UserEntity user, Integer departmentId, UserRole role) {
        return !checkUserInDepartment(user, departmentId, role);
    }

    @Nonnull
    @Override
    public DepartmentMembershipEntity performUpdate(
            @Nonnull Integer id,
            @Nonnull DepartmentMembershipEntity entity,
            @Nonnull DepartmentMembershipEntity existingEntity
    ) throws ResponseException {
        existingEntity.setRole(entity.getRole());
        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull DepartmentMembershipEntity entity) throws ResponseException {
        var submissionSpec = SubmissionFilter
                .create()
                // TODO: @mobaetzel - Check if this is correct
                .setAssigneeId(entity.getUserId())
                .setStatus(SubmissionStatus.OpenForManualWork)
                .build();

        if (submissionRepository.exists(submissionSpec)) {
            throw new ResponseException(HttpStatus.CONFLICT, "Die Mitarbeiter:in ist noch als Bearbeiter:in für offene Anträge eingetragen.");
        }

        repository.delete(entity);
    }
}
