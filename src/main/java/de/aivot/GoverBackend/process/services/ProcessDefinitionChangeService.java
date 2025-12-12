package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionChangeEntity;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionChangeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessDefinitionChangeService implements EntityService<ProcessDefinitionChangeEntity, Long> {

    private final ProcessDefinitionChangeRepository processDefinitionChangeRepository;

    @Autowired
    public ProcessDefinitionChangeService(ProcessDefinitionChangeRepository processDefinitionChangeRepository) {
        this.processDefinitionChangeRepository = processDefinitionChangeRepository;
    }

    @Nonnull
    @Override
    public ProcessDefinitionChangeEntity create(@Nonnull ProcessDefinitionChangeEntity entity) throws ResponseException {
        entity.setId(null);
        return processDefinitionChangeRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessDefinitionChangeEntity> performList(@Nonnull Pageable pageable,
                                                           @Nullable Specification<ProcessDefinitionChangeEntity> specification,
                                                           @Nullable Filter<ProcessDefinitionChangeEntity> filter) throws ResponseException {
        return processDefinitionChangeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionChangeEntity> retrieve(@Nonnull Long id) throws ResponseException {
        return processDefinitionChangeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionChangeEntity> retrieve(@Nonnull Specification<ProcessDefinitionChangeEntity> specification) throws ResponseException {
        return processDefinitionChangeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Long id) {
        return processDefinitionChangeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessDefinitionChangeEntity> specification) {
        return processDefinitionChangeRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessDefinitionChangeEntity performUpdate(@Nonnull Long id,
                                                      @Nonnull ProcessDefinitionChangeEntity entity,
                                                      @Nonnull ProcessDefinitionChangeEntity existingEntity) throws ResponseException {
        existingEntity.setTimestamp(entity.getTimestamp());
        existingEntity.setUserId(entity.getUserId());
        existingEntity.setProcessDefinitionId(entity.getProcessDefinitionId());
        existingEntity.setProcessDefinitionVersion(entity.getProcessDefinitionVersion());
        existingEntity.setProcessDefinitionNodeId(entity.getProcessDefinitionNodeId());
        existingEntity.setProcessDefinitionEdgeId(entity.getProcessDefinitionEdgeId());
        existingEntity.setChangeType(entity.getChangeType());
        existingEntity.setDiff(entity.getDiff());
        existingEntity.setComment(entity.getComment());
        return processDefinitionChangeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessDefinitionChangeEntity entity) throws ResponseException {
        processDefinitionChangeRepository.delete(entity);
    }
}

