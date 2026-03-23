package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessTestConfigEntity;
import de.aivot.GoverBackend.process.repositories.ProcessRepository;
import de.aivot.GoverBackend.process.repositories.ProcessTestConfigRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessTestConfigService implements EntityService<ProcessTestConfigEntity, Integer> {

    private final ProcessTestConfigRepository repository;

    @Autowired
    public ProcessTestConfigService(ProcessTestConfigRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public ProcessTestConfigEntity create(@Nonnull ProcessTestConfigEntity entity) throws ResponseException {
        entity.setId(null);
        return repository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessTestConfigEntity> performList(@Nonnull Pageable pageable,
                                           @Nullable Specification<ProcessTestConfigEntity> specification,
                                           @Nullable Filter<ProcessTestConfigEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessTestConfigEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessTestConfigEntity> retrieve(@Nonnull Specification<ProcessTestConfigEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessTestConfigEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessTestConfigEntity performUpdate(@Nonnull Integer id,
                                       @Nonnull ProcessTestConfigEntity entity,
                                       @Nonnull ProcessTestConfigEntity existingEntity) throws ResponseException {
        existingEntity.setName(entity.getName());
        existingEntity.setConfigs(entity.getConfigs());
        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessTestConfigEntity entity) throws ResponseException {
        repository.delete(entity);
    }
}
