package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.repositories.FormVersionWithDetailsRepository;
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
public class FormVersionWithDetailsService implements ReadEntityService<FormVersionWithDetailsEntity, FormVersionWithDetailsEntityId> {
    private final FormVersionWithDetailsRepository repository;

    @Autowired
    public FormVersionWithDetailsService(
            FormVersionWithDetailsRepository repository
    ) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public Page<FormVersionWithDetailsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<FormVersionWithDetailsEntity> specification,
            @Nullable Filter<FormVersionWithDetailsEntity> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<FormVersionWithDetailsEntity> retrieve(@Nonnull FormVersionWithDetailsEntityId id) {
        return repository.findById(id);
    }

    @Nonnull
    public Optional<FormVersionWithDetailsEntity> retrieve(@Nonnull Integer formId, @Nonnull Integer version) {
        return retrieve(FormVersionWithDetailsEntityId.of(formId, version));
    }

    @Nonnull
    @Override
    public Optional<FormVersionWithDetailsEntity> retrieve(@Nonnull Specification<FormVersionWithDetailsEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull FormVersionWithDetailsEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<FormVersionWithDetailsEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    public Optional<FormVersionWithDetailsEntity> findLatestForSlug(@Nonnull String slug) {
        return repository.findLatestForSlug(slug);
    }

    @Nonnull
    public Optional<FormVersionWithDetailsEntity> findLatestForSlugAndStatus(@Nonnull String slug, @Nonnull FormStatus status) {
        return repository.findLatestForSlugAndStatus(slug, status);
    }

    @Nonnull
    public Optional<FormVersionWithDetailsEntity> findVersionForSlugAndVersion(@Nonnull String slug, @Nonnull Integer version) {
        return repository.findVersionForSlugAndVersion(slug, version);
    }
}
