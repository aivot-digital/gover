package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.VDepartmentMembershipWithPermissionsEntity;
import de.aivot.GoverBackend.department.repositories.VDepartmentMembershipWithPermissionsRepository;
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
public class VDepartmentMembershipWithPermissionsService implements ReadEntityService<VDepartmentMembershipWithPermissionsEntity, Integer> {
    private final VDepartmentMembershipWithPermissionsRepository vDepartmentMembershipWithDetailsRepository;

    @Autowired
    public VDepartmentMembershipWithPermissionsService(VDepartmentMembershipWithPermissionsRepository vDepartmentMembershipWithDetailsRepository) {
        this.vDepartmentMembershipWithDetailsRepository = vDepartmentMembershipWithDetailsRepository;
    }

    @Nonnull
    @Override
    public Page<VDepartmentMembershipWithPermissionsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VDepartmentMembershipWithPermissionsEntity> specification,
            Filter<VDepartmentMembershipWithPermissionsEntity> filter) {
        return vDepartmentMembershipWithDetailsRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VDepartmentMembershipWithPermissionsEntity> retrieve(@Nonnull Integer id) {
        return vDepartmentMembershipWithDetailsRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<VDepartmentMembershipWithPermissionsEntity> retrieve(
            @Nonnull Specification<VDepartmentMembershipWithPermissionsEntity> specification
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
            @Nonnull Specification<VDepartmentMembershipWithPermissionsEntity> specification
    ) {
        return vDepartmentMembershipWithDetailsRepository
                .exists(specification);
    }
}
