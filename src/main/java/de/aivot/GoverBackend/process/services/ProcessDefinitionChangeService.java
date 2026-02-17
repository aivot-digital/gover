package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessChangeEntity;
import de.aivot.GoverBackend.process.repositories.ProcessChangeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessDefinitionChangeService implements EntityService<ProcessChangeEntity, Long> {

    private final ProcessChangeRepository processDefinitionChangeRepository;

    @Autowired
    public ProcessDefinitionChangeService(ProcessChangeRepository processDefinitionChangeRepository) {
        this.processDefinitionChangeRepository = processDefinitionChangeRepository;
    }

    @Nonnull
    @Override
    public ProcessChangeEntity create(@Nonnull ProcessChangeEntity entity) throws ResponseException {
        entity.setId(null);
        return processDefinitionChangeRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessChangeEntity> performList(@Nonnull Pageable pageable,
                                                 @Nullable Specification<ProcessChangeEntity> specification,
                                                 @Nullable Filter<ProcessChangeEntity> filter) throws ResponseException {
        return processDefinitionChangeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessChangeEntity> retrieve(@Nonnull Long id) throws ResponseException {
        return processDefinitionChangeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessChangeEntity> retrieve(@Nonnull Specification<ProcessChangeEntity> specification) throws ResponseException {
        return processDefinitionChangeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Long id) {
        return processDefinitionChangeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessChangeEntity> specification) {
        return processDefinitionChangeRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessChangeEntity performUpdate(@Nonnull Long id,
                                             @Nonnull ProcessChangeEntity entity,
                                             @Nonnull ProcessChangeEntity existingEntity) throws ResponseException {
        existingEntity.setTimestamp(entity.getTimestamp());
        existingEntity.setUserId(entity.getUserId());
        existingEntity.setProcessId(entity.getProcessId());
        existingEntity.setProcessVersion(entity.getProcessVersion());
        existingEntity.setProcessNodeId(entity.getProcessNodeId());
        existingEntity.setProcessEdgeId(entity.getProcessEdgeId());
        existingEntity.setChangeType(entity.getChangeType());
        existingEntity.setDiff(entity.getDiff());
        existingEntity.setComment(entity.getComment());
        return processDefinitionChangeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessChangeEntity entity) throws ResponseException {
        processDefinitionChangeRepository.delete(entity);
    }
}

