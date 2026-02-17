package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessAccessControlEntity;
import de.aivot.GoverBackend.process.repositories.ProcessAccessControlRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessAccessControlService implements EntityService<ProcessAccessControlEntity, Integer> {
    private final ProcessAccessControlRepository repository;

    public ProcessAccessControlService(ProcessAccessControlRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public ProcessAccessControlEntity create(@Nonnull ProcessAccessControlEntity entity) throws ResponseException {
        entity.setId(null);
        return repository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessAccessControlEntity> performList(@Nonnull Pageable pageable,
                                                        @Nullable Specification<ProcessAccessControlEntity> specification,
                                                        @Nullable Filter<ProcessAccessControlEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessAccessControlEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessAccessControlEntity> retrieve(@Nonnull Specification<ProcessAccessControlEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessAccessControlEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessAccessControlEntity performUpdate(@Nonnull Integer id,
                                                    @Nonnull ProcessAccessControlEntity entity,
                                                    @Nonnull ProcessAccessControlEntity existingEntity) throws ResponseException {
        // Update fields as needed, example:
        existingEntity.setPermissions(entity.getPermissions());

        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessAccessControlEntity entity) throws ResponseException {
        repository.delete(entity);
    }
}

