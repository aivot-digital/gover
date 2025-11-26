package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.VDepartmentUserRoleAssignmentWithDetailsEntity;
import de.aivot.GoverBackend.department.repositories.VDepartmentUserRoleAssignmentWithDetailsRepository;
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
public class VDepartmentUserRoleAssignmentWithDetailsService implements ReadEntityService<VDepartmentUserRoleAssignmentWithDetailsEntity, Integer> {
    private final VDepartmentUserRoleAssignmentWithDetailsRepository vDepartmentMembershipWithDetailsRepository;

    @Autowired
    public VDepartmentUserRoleAssignmentWithDetailsService(VDepartmentUserRoleAssignmentWithDetailsRepository vDepartmentMembershipWithDetailsRepository) {
        this.vDepartmentMembershipWithDetailsRepository = vDepartmentMembershipWithDetailsRepository;
    }

    @Nonnull
    @Override
    public Page<VDepartmentUserRoleAssignmentWithDetailsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VDepartmentUserRoleAssignmentWithDetailsEntity> specification,
            Filter<VDepartmentUserRoleAssignmentWithDetailsEntity> filter) {
        return vDepartmentMembershipWithDetailsRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VDepartmentUserRoleAssignmentWithDetailsEntity> retrieve(@Nonnull Integer id) {
        return vDepartmentMembershipWithDetailsRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<VDepartmentUserRoleAssignmentWithDetailsEntity> retrieve(
            @Nonnull Specification<VDepartmentUserRoleAssignmentWithDetailsEntity> specification
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
            @Nonnull Specification<VDepartmentUserRoleAssignmentWithDetailsEntity> specification
    ) {
        return vDepartmentMembershipWithDetailsRepository
                .exists(specification);
    }
}
