package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessEdgeEntity;
import de.aivot.GoverBackend.process.repositories.ProcessEdgeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessEdgeService implements EntityService<ProcessEdgeEntity, Integer> {

    private final ProcessEdgeRepository processDefinitionEdgeRepository;

    @Autowired
    public ProcessEdgeService(ProcessEdgeRepository processDefinitionEdgeRepository) {
        this.processDefinitionEdgeRepository = processDefinitionEdgeRepository;
    }

    @Nonnull
    @Override
    public ProcessEdgeEntity create(@Nonnull ProcessEdgeEntity entity) throws ResponseException {
        entity.setId(null);
        return processDefinitionEdgeRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessEdgeEntity> performList(@Nonnull Pageable pageable,
                                               @Nullable Specification<ProcessEdgeEntity> specification,
                                               @Nullable Filter<ProcessEdgeEntity> filter) throws ResponseException {
        return processDefinitionEdgeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessEdgeEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return processDefinitionEdgeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessEdgeEntity> retrieve(@Nonnull Specification<ProcessEdgeEntity> specification) throws ResponseException {
        return processDefinitionEdgeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return processDefinitionEdgeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessEdgeEntity> specification) {
        return processDefinitionEdgeRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessEdgeEntity performUpdate(@Nonnull Integer id,
                                           @Nonnull ProcessEdgeEntity entity,
                                           @Nonnull ProcessEdgeEntity existingEntity) throws ResponseException {
        existingEntity.setProcessId(entity.getProcessId());
        existingEntity.setProcessVersion(entity.getProcessVersion());
        existingEntity.setFromNodeId(entity.getFromNodeId());
        existingEntity.setToNodeId(entity.getToNodeId());
        existingEntity.setViaPort(entity.getViaPort());
        return processDefinitionEdgeRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessEdgeEntity entity) throws ResponseException {
        processDefinitionEdgeRepository.delete(entity);
    }
}

