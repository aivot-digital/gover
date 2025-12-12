package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionEdgeEntity;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionEdgeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessDefinitionEdgeService implements EntityService<ProcessDefinitionEdgeEntity, Integer> {

    private final ProcessDefinitionEdgeRepository processDefinitionEdgeRepository;

    @Autowired
    public ProcessDefinitionEdgeService(ProcessDefinitionEdgeRepository processDefinitionEdgeRepository) {
        this.processDefinitionEdgeRepository = processDefinitionEdgeRepository;
    }

    @Nonnull
    @Override
    public ProcessDefinitionEdgeEntity create(@Nonnull ProcessDefinitionEdgeEntity entity) throws ResponseException {
        entity.setId(null);
        return processDefinitionEdgeRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessDefinitionEdgeEntity> performList(@Nonnull Pageable pageable,
                                                         @Nullable Specification<ProcessDefinitionEdgeEntity> specification,
                                                         @Nullable Filter<ProcessDefinitionEdgeEntity> filter) throws ResponseException {
        return processDefinitionEdgeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionEdgeEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return processDefinitionEdgeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionEdgeEntity> retrieve(@Nonnull Specification<ProcessDefinitionEdgeEntity> specification) throws ResponseException {
        return processDefinitionEdgeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return processDefinitionEdgeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessDefinitionEdgeEntity> specification) {
        return processDefinitionEdgeRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessDefinitionEdgeEntity performUpdate(@Nonnull Integer id,
                                                    @Nonnull ProcessDefinitionEdgeEntity entity,
                                                    @Nonnull ProcessDefinitionEdgeEntity existingEntity) throws ResponseException {
        existingEntity.setProcessDefinitionId(entity.getProcessDefinitionId());
        existingEntity.setProcessDefinitionVersion(entity.getProcessDefinitionVersion());
        existingEntity.setFromNodeId(entity.getFromNodeId());
        existingEntity.setToNodeId(entity.getToNodeId());
        existingEntity.setViaPort(entity.getViaPort());
        return processDefinitionEdgeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessDefinitionEdgeEntity entity) throws ResponseException {
        processDefinitionEdgeRepository.delete(entity);
    }
}

