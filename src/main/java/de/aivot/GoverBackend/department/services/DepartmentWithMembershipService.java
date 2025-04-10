package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.DepartmentWithMembershipEntity;
import de.aivot.GoverBackend.department.entities.DepartmentWithMembershipEntityId;
import de.aivot.GoverBackend.department.repositories.DepartmentWithMembershipRepository;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class DepartmentWithMembershipService implements ReadEntityService<DepartmentWithMembershipEntity, DepartmentWithMembershipEntityId> {
    private final DepartmentWithMembershipRepository repository;

    @Autowired
    public DepartmentWithMembershipService(DepartmentWithMembershipRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<DepartmentWithMembershipEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<DepartmentWithMembershipEntity> specification,
            Filter<DepartmentWithMembershipEntity> filter
    ) {
        return repository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<DepartmentWithMembershipEntity> retrieve(@Nonnull DepartmentWithMembershipEntityId id) {
        return repository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<DepartmentWithMembershipEntity> retrieve(
            @Nonnull Specification<DepartmentWithMembershipEntity> specification
    ) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull DepartmentWithMembershipEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<DepartmentWithMembershipEntity> specification
    ) {
        return repository.exists(specification);
    }
}
