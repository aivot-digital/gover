package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessInstanceService implements EntityService<ProcessInstanceEntity, Long> {

    private final ProcessInstanceRepository processInstanceRepository;

    @Autowired
    public ProcessInstanceService(ProcessInstanceRepository processInstanceRepository) {
        this.processInstanceRepository = processInstanceRepository;
    }

    @Nonnull
    @Override
    public ProcessInstanceEntity create(@Nonnull ProcessInstanceEntity entity) throws ResponseException {
        entity.setId(null);
        entity.setAccessKey(UUID.randomUUID());
        return processInstanceRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceEntity> performList(@Nonnull Pageable pageable,
                                                   @Nullable Specification<ProcessInstanceEntity> specification,
                                                   @Nullable Filter<ProcessInstanceEntity> filter) throws ResponseException {
        return processInstanceRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceEntity> retrieve(@Nonnull Long id) throws ResponseException {
        return processInstanceRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceEntity> retrieve(@Nonnull Specification<ProcessInstanceEntity> specification) throws ResponseException {
        return processInstanceRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Long id) {
        return processInstanceRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceEntity> specification) {
        return processInstanceRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessInstanceEntity performUpdate(@Nonnull Long id,
                                              @Nonnull ProcessInstanceEntity entity,
                                              @Nonnull ProcessInstanceEntity existingEntity) throws ResponseException {
        // Update fields as needed, example:
        existingEntity.setAccessKey(entity.getAccessKey());
        existingEntity.setProcessDefinitionId(entity.getProcessDefinitionId());
        existingEntity.setProcessDefinitionVersion(entity.getProcessDefinitionVersion());
        existingEntity.setStatus(entity.getStatus());
        existingEntity.setStatusOverride(entity.getStatusOverride());
        existingEntity.setAssignedFileNumbers(entity.getAssignedFileNumbers());
        existingEntity.setDeliveryChannels(entity.getDeliveryChannels());
        existingEntity.setTags(entity.getTags());
        existingEntity.setStarted(entity.getStarted());
        existingEntity.setUpdated(entity.getUpdated());
        existingEntity.setFinished(entity.getFinished());
        existingEntity.setRuntime(entity.getRuntime());
        existingEntity.setInitialPayload(entity.getInitialPayload());
        existingEntity.setInitialNodeId(entity.getInitialNodeId());
        return processInstanceRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceEntity entity) throws ResponseException {
        processInstanceRepository.delete(entity);
    }
}

