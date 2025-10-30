package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithMembershipEntityId;
import de.aivot.GoverBackend.form.filters.FormVersionWithMembershipFilter;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionWithMembershipRepository;
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
public class FormVersionWithMembershipService implements ReadEntityService<FormVersionWithMembershipEntity, FormVersionWithMembershipEntityId> {
    private final FormVersionWithMembershipRepository repository;
    private final FormVersionRepository versionRepository;

    @Autowired
    public FormVersionWithMembershipService(
            FormVersionWithMembershipRepository repository,
            FormVersionRepository formVersionRepository) {
        this.repository = repository;
        this.versionRepository = formVersionRepository;
    }

    @Nonnull
    @Override
    public Page<FormVersionWithMembershipEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<FormVersionWithMembershipEntity> specification,
            @Nullable Filter<FormVersionWithMembershipEntity> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<FormVersionWithMembershipEntity> retrieve(@Nonnull FormVersionWithMembershipEntityId id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<FormVersionWithMembershipEntity> retrieve(@Nonnull Specification<FormVersionWithMembershipEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull FormVersionWithMembershipEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<FormVersionWithMembershipEntity> specification) {
        return repository.exists(specification);
    }

    public Optional<FormVersionWithMembershipEntity> retrieveLatest(FormVersionWithMembershipFilter filter) {
        if (filter.getId() == null) {
            return Optional.empty();
        }

        var maxVersion = versionRepository
                .maxVersionForFormId(filter.getId());

        if (maxVersion.isEmpty()) {
            return Optional.empty();
        }

        filter.setVersion(maxVersion.get());

        return repository.findOne(filter.build());
    }
}
