package de.aivot.GoverBackend.providerLink.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.providerLink.entities.ProviderLink;
import de.aivot.GoverBackend.providerLink.repositories.ProviderLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class ProviderLinkService implements EntityService<ProviderLink, Integer> {
    private final ProviderLinkRepository repository;

    @Autowired
    public ProviderLinkService(
            ProviderLinkRepository repository
    ) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public ProviderLink create(@Nonnull ProviderLink entity) throws ResponseException {
        entity.setId(null);
        return repository.save(entity);
    }

    @Nonnull
    @Override
    public Page<ProviderLink> performList(@Nonnull Pageable pageable, @Nullable Specification<ProviderLink> specification, Filter<ProviderLink> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public ProviderLink performUpdate(@Nonnull Integer id, @Nonnull ProviderLink entity, @Nonnull ProviderLink existingEntity) throws ResponseException {
        existingEntity.setText(entity.getText());
        existingEntity.setLink(entity.getLink());

        return repository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<ProviderLink> retrieve(@Nonnull Integer id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProviderLink> retrieve(@Nonnull Specification<ProviderLink> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProviderLink> specification) {
        return repository.exists(specification);
    }

    @Override
    public void performDelete(@Nonnull ProviderLink entity) throws ResponseException {
        repository.delete(entity);
    }
}
