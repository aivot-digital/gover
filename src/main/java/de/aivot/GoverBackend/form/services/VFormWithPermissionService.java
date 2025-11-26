package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.entities.VFormWithPermissionEntity;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionEntityId;
import de.aivot.GoverBackend.form.repositories.VFormWithPermissionRepository;
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
import java.util.function.Function;

@Service
public class VFormWithPermissionService implements ReadEntityService<VFormWithPermissionEntity, VFormWithPermissionEntityId> {
    private final VFormWithPermissionRepository repository;

    @Autowired
    public VFormWithPermissionService(
            VFormWithPermissionRepository repository
    ) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<VFormWithPermissionEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VFormWithPermissionEntity> specification,
            @Nullable Filter<VFormWithPermissionEntity> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VFormWithPermissionEntity> retrieve(@Nonnull VFormWithPermissionEntityId id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<VFormWithPermissionEntity> retrieve(@Nonnull Specification<VFormWithPermissionEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull VFormWithPermissionEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<VFormWithPermissionEntity> specification) {
        return repository.exists(specification);
    }

    public boolean checkUserPermission(Integer formId, String userId, Function<VFormWithPermissionEntity, Boolean> permissionGetter) {
        var id = new VFormWithPermissionEntityId(formId, userId);

        var entityOpt = retrieve(id);

        return entityOpt
                .map(permissionGetter)
                .orElse(false);
    }
}
