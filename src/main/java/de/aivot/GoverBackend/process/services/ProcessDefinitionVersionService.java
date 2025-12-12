package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionVersionEntityId;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionVersionRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessDefinitionVersionService implements EntityService<ProcessDefinitionVersionEntity, ProcessDefinitionVersionEntityId> {

    private final ProcessDefinitionVersionRepository processDefinitionVersionRepository;

    @Autowired
    public ProcessDefinitionVersionService(ProcessDefinitionVersionRepository processDefinitionVersionRepository) {
        this.processDefinitionVersionRepository = processDefinitionVersionRepository;
    }

    @Nonnull
    @Override
    public ProcessDefinitionVersionEntity create(@Nonnull ProcessDefinitionVersionEntity entity) throws ResponseException {
        // Fetch the latest version number for the given process definition
        Integer latestVersionNumber = processDefinitionVersionRepository
                .maxVersionForProcessDefinition(entity.getProcessDefinitionId())
                .orElse(0);

        // Set the new version number to be one greater than the latest version number
        entity
                .setProcessDefinitionVersion(latestVersionNumber + 1);

        return processDefinitionVersionRepository
                .save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessDefinitionVersionEntity> performList(@Nonnull Pageable pageable,
                                                            @Nullable Specification<ProcessDefinitionVersionEntity> specification,
                                                            @Nullable Filter<ProcessDefinitionVersionEntity> filter) throws ResponseException {
        return processDefinitionVersionRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionVersionEntity> retrieve(@Nonnull ProcessDefinitionVersionEntityId id) throws ResponseException {
        return processDefinitionVersionRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionVersionEntity> retrieve(@Nonnull Specification<ProcessDefinitionVersionEntity> specification) throws ResponseException {
        return processDefinitionVersionRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull ProcessDefinitionVersionEntityId id) {
        return processDefinitionVersionRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessDefinitionVersionEntity> specification) {
        return processDefinitionVersionRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessDefinitionVersionEntity performUpdate(@Nonnull ProcessDefinitionVersionEntityId id,
                                                        @Nonnull ProcessDefinitionVersionEntity entity,
                                                        @Nonnull ProcessDefinitionVersionEntity existingEntity) throws ResponseException {
        existingEntity.setStatus(entity.getStatus());
        existingEntity.setRetentionTimeUnit(entity.getRetentionTimeUnit());
        existingEntity.setRetentionTimeAmount(entity.getRetentionTimeAmount());
        return processDefinitionVersionRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessDefinitionVersionEntity entity) throws ResponseException {
        processDefinitionVersionRepository.delete(entity);
    }
}

