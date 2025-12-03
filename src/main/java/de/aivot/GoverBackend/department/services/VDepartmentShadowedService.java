package de.aivot.GoverBackend.department.services;

import de.aivot.GoverBackend.department.entities.VDepartmentShadowedEntity;
import de.aivot.GoverBackend.department.repositories.VDepartmentShadowedRepository;
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
public class VDepartmentShadowedService implements ReadEntityService<VDepartmentShadowedEntity, Integer> {
    private final VDepartmentShadowedRepository vDepartmentShadowedRepository;

    @Autowired
    public VDepartmentShadowedService(VDepartmentShadowedRepository vDepartmentShadowedRepository) {
        this.vDepartmentShadowedRepository = vDepartmentShadowedRepository;
    }

    @Nonnull
    @Override
    public Page<VDepartmentShadowedEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VDepartmentShadowedEntity> specification,
            Filter<VDepartmentShadowedEntity> filter) {
        return vDepartmentShadowedRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VDepartmentShadowedEntity> retrieve(@Nonnull Integer id) {
        return vDepartmentShadowedRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<VDepartmentShadowedEntity> retrieve(
            @Nonnull Specification<VDepartmentShadowedEntity> specification
    ) {
        return vDepartmentShadowedRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return vDepartmentShadowedRepository
                .existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<VDepartmentShadowedEntity> specification
    ) {
        return vDepartmentShadowedRepository
                .exists(specification);
    }
}
