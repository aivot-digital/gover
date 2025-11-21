package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitMembershipEntity;
import de.aivot.GoverBackend.department.filters.OrganizationalUnitMembershipFilter;
import de.aivot.GoverBackend.department.repositories.OrganizationalUnitMembershipRepository;
import de.aivot.GoverBackend.department.repositories.OrganizationalUnitRepository;
import de.aivot.GoverBackend.enums.SubmissionStatus;
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
public class OrganizationalUnitMembershipService implements EntityService<OrganizationalUnitMembershipEntity, Integer> {
    private final OrganizationalUnitMembershipRepository repository;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final SubmissionRepository submissionRepository;
    private final UserService userService;

    @Autowired
    public OrganizationalUnitMembershipService(
            OrganizationalUnitMembershipRepository repository,
            OrganizationalUnitRepository organizationalUnitRepository,
            SubmissionRepository submissionRepository,
            UserService userService
    ) {
        this.repository = repository;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.submissionRepository = submissionRepository;
        this.userService = userService;
    }

    @Nonnull
    @Override
    public OrganizationalUnitMembershipEntity create(@Nonnull OrganizationalUnitMembershipEntity entity) throws ResponseException {
        entity.setId(null);

        var targetUser = userService
                .retrieve(entity.getUserId())
                .orElseThrow(() -> new ResponseException(HttpStatus.NOT_FOUND, "Die Mitarbeiter:in wurde nicht gefunden."));

        var targetDepartment = organizationalUnitRepository
                .findById(entity.getOrganizationalUnitId())
                .orElseThrow(() -> new ResponseException(HttpStatus.NOT_FOUND, "Der Fachbereich wurde nicht gefunden."));

        var spec = OrganizationalUnitMembershipFilter
                .create()
                .setOrganizationalUnitId(targetDepartment.getId())
                .setUserId(targetUser.getId())
                .build();

        if (exists(spec)) {
            throw new ResponseException(HttpStatus.CONFLICT, "Diese Mitarbeiter:in ist bereits teil des Fachbereichs.");
        }

        return repository.save(entity);
    }

    @Nonnull
    @Override
    public Page<OrganizationalUnitMembershipEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<OrganizationalUnitMembershipEntity> specification,
            Filter<OrganizationalUnitMembershipEntity> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<OrganizationalUnitMembershipEntity> retrieve(@Nonnull Integer id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<OrganizationalUnitMembershipEntity> retrieve(
            @Nonnull Specification<OrganizationalUnitMembershipEntity> specification
    ) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<OrganizationalUnitMembershipEntity> specification
    ) {
        return repository.exists(specification);
    }

    public boolean checkUserInDepartment(UserEntity user, Integer departmentId) {
        var spec = OrganizationalUnitMembershipFilter
                .create()
                .setUserId(user.getId())
                .setOrganizationalUnitId(departmentId)
                .build();

        return exists(spec);
    }

    public boolean checkUserNotInDepartment(UserEntity user, Integer departmentId) {
        return !checkUserInDepartment(user, departmentId);
    }

    @Nonnull
    @Override
    public OrganizationalUnitMembershipEntity performUpdate(
            @Nonnull Integer id,
            @Nonnull OrganizationalUnitMembershipEntity entity,
            @Nonnull OrganizationalUnitMembershipEntity existingEntity
    ) throws ResponseException {
        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull OrganizationalUnitMembershipEntity entity) throws ResponseException {
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
