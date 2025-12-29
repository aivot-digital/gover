package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlPresetEntity;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceAccessControlPresetRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessInstanceAccessControlPresetService implements EntityService<ProcessInstanceAccessControlPresetEntity, Integer> {
    private final ProcessInstanceAccessControlPresetRepository repository;

    public ProcessInstanceAccessControlPresetService(ProcessInstanceAccessControlPresetRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public ProcessInstanceAccessControlPresetEntity create(@Nonnull ProcessInstanceAccessControlPresetEntity entity) throws ResponseException {
        entity.setId(null);
        return repository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceAccessControlPresetEntity> performList(@Nonnull Pageable pageable,
                                                                      @Nullable Specification<ProcessInstanceAccessControlPresetEntity> specification,
                                                                      @Nullable Filter<ProcessInstanceAccessControlPresetEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAccessControlPresetEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAccessControlPresetEntity> retrieve(@Nonnull Specification<ProcessInstanceAccessControlPresetEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceAccessControlPresetEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessInstanceAccessControlPresetEntity performUpdate(@Nonnull Integer id,
                                                                  @Nonnull ProcessInstanceAccessControlPresetEntity entity,
                                                                  @Nonnull ProcessInstanceAccessControlPresetEntity existingEntity) throws ResponseException {
        // Update fields as needed, example:
        existingEntity.setPermissions(entity.getPermissions());

        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceAccessControlPresetEntity entity) throws ResponseException {
        repository.delete(entity);
    }
}

