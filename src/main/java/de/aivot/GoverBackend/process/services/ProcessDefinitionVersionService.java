package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.repositories.ProcessVersionRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessDefinitionVersionService implements EntityService<ProcessVersionEntity, ProcessVersionEntityId> {

    private final ProcessVersionRepository processDefinitionVersionRepository;

    @Autowired
    public ProcessDefinitionVersionService(ProcessVersionRepository processDefinitionVersionRepository) {
        this.processDefinitionVersionRepository = processDefinitionVersionRepository;
    }

    @Nonnull
    @Override
    public ProcessVersionEntity create(@Nonnull ProcessVersionEntity entity) throws ResponseException {
        // Fetch the latest version number for the given process definition
        Integer latestVersionNumber = processDefinitionVersionRepository
                .maxVersionForProcessDefinition(entity.getProcessId())
                .orElse(0);

        // Set the new version number to be one greater than the latest version number
        entity
                .setProcessVersion(latestVersionNumber + 1);

        return processDefinitionVersionRepository
                .save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessVersionEntity> performList(@Nonnull Pageable pageable,
                                                  @Nullable Specification<ProcessVersionEntity> specification,
                                                  @Nullable Filter<ProcessVersionEntity> filter) throws ResponseException {
        return processDefinitionVersionRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessVersionEntity> retrieve(@Nonnull ProcessVersionEntityId id) throws ResponseException {
        return processDefinitionVersionRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessVersionEntity> retrieve(@Nonnull Specification<ProcessVersionEntity> specification) throws ResponseException {
        return processDefinitionVersionRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull ProcessVersionEntityId id) {
        return processDefinitionVersionRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessVersionEntity> specification) {
        return processDefinitionVersionRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessVersionEntity performUpdate(@Nonnull ProcessVersionEntityId id,
                                              @Nonnull ProcessVersionEntity entity,
                                              @Nonnull ProcessVersionEntity existingEntity) throws ResponseException {
        existingEntity.setStatus(entity.getStatus());
        existingEntity.setPublicTitle(entity.getPublicTitle());
        return processDefinitionVersionRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessVersionEntity entity) throws ResponseException {
        processDefinitionVersionRepository.delete(entity);
    }
}

