package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessInstanceTaskService implements EntityService<ProcessInstanceTaskEntity, Long> {

    private final ProcessInstanceTaskRepository processInstanceTaskRepository;

    @Autowired
    public ProcessInstanceTaskService(ProcessInstanceTaskRepository processInstanceTaskRepository) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
    }

    @Nonnull
    @Override
    public ProcessInstanceTaskEntity create(@Nonnull ProcessInstanceTaskEntity entity) throws ResponseException {
        entity.setId(null);
        return processInstanceTaskRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceTaskEntity> performList(@Nonnull Pageable pageable,
                                                       @Nullable Specification<ProcessInstanceTaskEntity> specification,
                                                       @Nullable Filter<ProcessInstanceTaskEntity> filter) throws ResponseException {
        return processInstanceTaskRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceTaskEntity> retrieve(@Nonnull Long id) throws ResponseException {
        return processInstanceTaskRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceTaskEntity> retrieve(@Nonnull Specification<ProcessInstanceTaskEntity> specification) throws ResponseException {
        return processInstanceTaskRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Long id) {
        return processInstanceTaskRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceTaskEntity> specification) {
        return processInstanceTaskRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessInstanceTaskEntity performUpdate(@Nonnull Long id,
                                                  @Nonnull ProcessInstanceTaskEntity entity,
                                                  @Nonnull ProcessInstanceTaskEntity existingEntity) throws ResponseException {
        existingEntity.setAccessKey(entity.getAccessKey());
        existingEntity.setProcessInstanceId(entity.getProcessInstanceId());
        existingEntity.setProcessDefinitionId(entity.getProcessDefinitionId());
        existingEntity.setProcessDefinitionVersion(entity.getProcessDefinitionVersion());
        existingEntity.setProcessDefinitionNodeId(entity.getProcessDefinitionNodeId());
        existingEntity.setStarted(entity.getStarted());
        existingEntity.setUpdated(entity.getUpdated());
        existingEntity.setFinished(entity.getFinished());
        existingEntity.setRuntime(entity.getRuntime());
        existingEntity.setNodeData(entity.getNodeData());
        existingEntity.setProcessData(entity.getProcessData());
        existingEntity.setAssignedUserId(entity.getAssignedUserId());
        return processInstanceTaskRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceTaskEntity entity) throws ResponseException {
        processInstanceTaskRepository.delete(entity);
    }
}

