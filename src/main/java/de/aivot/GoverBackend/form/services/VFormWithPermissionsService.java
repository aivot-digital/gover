package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntityId;
import de.aivot.GoverBackend.form.repositories.VFormWithPermissionsRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
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
import java.util.function.Function;

@Service
public class VFormWithPermissionsService implements ReadEntityService<VFormWithPermissionsEntity, VFormWithPermissionsEntityId> {
    private final VFormWithPermissionsRepository repository;

    @Autowired
    public VFormWithPermissionsService(
            VFormWithPermissionsRepository repository
    ) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<VFormWithPermissionsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<VFormWithPermissionsEntity> specification,
            @Nullable Filter<VFormWithPermissionsEntity> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VFormWithPermissionsEntity> retrieve(@Nonnull VFormWithPermissionsEntityId id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<VFormWithPermissionsEntity> retrieve(@Nonnull Specification<VFormWithPermissionsEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull VFormWithPermissionsEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<VFormWithPermissionsEntity> specification) {
        return repository.exists(specification);
    }

    public void checkUserPermission(@Nonnull Integer formId,
                                    @Nonnull String userId,
                                    @Nonnull Function<VFormWithPermissionsEntity, Boolean> permissionGetter,
                                    @Nonnull String permissionName) throws ResponseException {
        var id = new VFormWithPermissionsEntityId(formId, userId);

        var entityOpt = retrieve(id);

        var hasAccess = entityOpt
                .map(permissionGetter)
                .orElse(false);

        if (!hasAccess) {
            throw ResponseException.noPermission(permissionName);
        }
    }
}
