package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.VOrganizationalUnitMembershipWithDetailsEntity;
import de.aivot.GoverBackend.department.repositories.VOrganizationalUnitMembershipWithDetailsRepository;
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
public class VOrganizationalUnitMembershipWithDetailsService implements ReadEntityService<VOrganizationalUnitMembershipWithDetailsEntity, Integer> {
    private final VOrganizationalUnitMembershipWithDetailsRepository vOrganizationalUnitMembershipWithDetailsRepository;

    @Autowired
    public VOrganizationalUnitMembershipWithDetailsService(VOrganizationalUnitMembershipWithDetailsRepository vOrganizationalUnitMembershipWithDetailsRepository) {
        this.vOrganizationalUnitMembershipWithDetailsRepository = vOrganizationalUnitMembershipWithDetailsRepository;
    }

    @Nonnull
    @Override
    public Page<VOrganizationalUnitMembershipWithDetailsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VOrganizationalUnitMembershipWithDetailsEntity> specification,
            Filter<VOrganizationalUnitMembershipWithDetailsEntity> filter) {
        return vOrganizationalUnitMembershipWithDetailsRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VOrganizationalUnitMembershipWithDetailsEntity> retrieve(@Nonnull Integer id) {
        return vOrganizationalUnitMembershipWithDetailsRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<VOrganizationalUnitMembershipWithDetailsEntity> retrieve(
            @Nonnull Specification<VOrganizationalUnitMembershipWithDetailsEntity> specification
    ) {
        return vOrganizationalUnitMembershipWithDetailsRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return vOrganizationalUnitMembershipWithDetailsRepository
                .existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<VOrganizationalUnitMembershipWithDetailsEntity> specification
    ) {
        return vOrganizationalUnitMembershipWithDetailsRepository
                .exists(specification);
    }
}
