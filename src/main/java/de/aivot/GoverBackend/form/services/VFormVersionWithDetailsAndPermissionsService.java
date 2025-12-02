package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsAndPermissionsEntity;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsAndPermissionsEntityId;
import de.aivot.GoverBackend.form.repositories.VFormVersionWithDetailsAndPermissionsRepository;
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
public class VFormVersionWithDetailsAndPermissionsService implements ReadEntityService<VFormVersionWithDetailsAndPermissionsEntity, VFormVersionWithDetailsAndPermissionsEntityId> {
    private final VFormVersionWithDetailsAndPermissionsRepository repository;

    @Autowired
    public VFormVersionWithDetailsAndPermissionsService(
            VFormVersionWithDetailsAndPermissionsRepository repository
    ) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<VFormVersionWithDetailsAndPermissionsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VFormVersionWithDetailsAndPermissionsEntity> specification,
            @Nullable Filter<VFormVersionWithDetailsAndPermissionsEntity> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VFormVersionWithDetailsAndPermissionsEntity> retrieve(@Nonnull VFormVersionWithDetailsAndPermissionsEntityId id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<VFormVersionWithDetailsAndPermissionsEntity> retrieve(@Nonnull Specification<VFormVersionWithDetailsAndPermissionsEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull VFormVersionWithDetailsAndPermissionsEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<VFormVersionWithDetailsAndPermissionsEntity> specification) {
        return repository.exists(specification);
    }
}
