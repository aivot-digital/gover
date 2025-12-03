package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.VDepartmentMembershipWithDetailsEntity;
import de.aivot.GoverBackend.department.repositories.VDepartmentMembershipWithDetailsRepository;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;

@Service
public class VDepartmentMembershipWithDetailsService implements ReadEntityService<VDepartmentMembershipWithDetailsEntity, Integer> {
    private final VDepartmentMembershipWithDetailsRepository vDepartmentMembershipWithDetailsRepository;

    @Autowired
    public VDepartmentMembershipWithDetailsService(VDepartmentMembershipWithDetailsRepository vDepartmentMembershipWithDetailsRepository) {
        this.vDepartmentMembershipWithDetailsRepository = vDepartmentMembershipWithDetailsRepository;
    }

    @Nonnull
    @Override
    public Page<VDepartmentMembershipWithDetailsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VDepartmentMembershipWithDetailsEntity> specification,
            Filter<VDepartmentMembershipWithDetailsEntity> filter) {
        return vDepartmentMembershipWithDetailsRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VDepartmentMembershipWithDetailsEntity> retrieve(@Nonnull Integer id) {
        return vDepartmentMembershipWithDetailsRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<VDepartmentMembershipWithDetailsEntity> retrieve(
            @Nonnull Specification<VDepartmentMembershipWithDetailsEntity> specification
    ) {
        return vDepartmentMembershipWithDetailsRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return vDepartmentMembershipWithDetailsRepository
                .existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<VDepartmentMembershipWithDetailsEntity> specification
    ) {
        return vDepartmentMembershipWithDetailsRepository
                .exists(specification);
    }
}
