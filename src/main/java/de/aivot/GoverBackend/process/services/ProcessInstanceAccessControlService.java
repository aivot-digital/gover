package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.GoverBackend.process.repositories.ProcessAccessControlRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceAccessControlRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessInstanceAccessControlService implements EntityService<ProcessInstanceAccessControlEntity, Integer> {
    private final ProcessInstanceAccessControlRepository repository;

    public ProcessInstanceAccessControlService(ProcessInstanceAccessControlRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public ProcessInstanceAccessControlEntity create(@Nonnull ProcessInstanceAccessControlEntity entity) throws ResponseException {
        entity.setId(null);
        return repository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceAccessControlEntity> performList(@Nonnull Pageable pageable,
                                                        @Nullable Specification<ProcessInstanceAccessControlEntity> specification,
                                                        @Nullable Filter<ProcessInstanceAccessControlEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAccessControlEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAccessControlEntity> retrieve(@Nonnull Specification<ProcessInstanceAccessControlEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceAccessControlEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessInstanceAccessControlEntity performUpdate(@Nonnull Integer id,
                                                    @Nonnull ProcessInstanceAccessControlEntity entity,
                                                    @Nonnull ProcessInstanceAccessControlEntity existingEntity) throws ResponseException {
        // Update fields as needed, example:
        existingEntity.setPermissions(entity.getPermissions());

        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceAccessControlEntity entity) throws ResponseException {
        repository.delete(entity);
    }
}

