package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.VOrganizationalUnitShadowedEntity;
import de.aivot.GoverBackend.department.repositories.VOrganizationalUnitShadowedRepository;
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
public class VOrganizationalUnitShadowedService implements ReadEntityService<VOrganizationalUnitShadowedEntity, Integer> {
    private final VOrganizationalUnitShadowedRepository vOrganizationalUnitShadowedRepository;

    @Autowired
    public VOrganizationalUnitShadowedService(VOrganizationalUnitShadowedRepository vOrganizationalUnitShadowedRepository) {
        this.vOrganizationalUnitShadowedRepository = vOrganizationalUnitShadowedRepository;
    }

    @Nonnull
    @Override
    public Page<VOrganizationalUnitShadowedEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VOrganizationalUnitShadowedEntity> specification,
            Filter<VOrganizationalUnitShadowedEntity> filter) {
        return vOrganizationalUnitShadowedRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VOrganizationalUnitShadowedEntity> retrieve(@Nonnull Integer id) {
        return vOrganizationalUnitShadowedRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<VOrganizationalUnitShadowedEntity> retrieve(
            @Nonnull Specification<VOrganizationalUnitShadowedEntity> specification
    ) {
        return vOrganizationalUnitShadowedRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return vOrganizationalUnitShadowedRepository
                .existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<VOrganizationalUnitShadowedEntity> specification
    ) {
        return vOrganizationalUnitShadowedRepository
                .exists(specification);
    }
}
