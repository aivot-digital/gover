package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionEntity;
import de.aivot.GoverBackend.process.repositories.ProcessDefinitionRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessDefinitionService implements EntityService<ProcessDefinitionEntity, Integer> {

    private final ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    public ProcessDefinitionService(ProcessDefinitionRepository processDefinitionRepository) {
        this.processDefinitionRepository = processDefinitionRepository;
    }

    @Nonnull
    @Override
    public ProcessDefinitionEntity create(@Nonnull ProcessDefinitionEntity entity) throws ResponseException {
        entity.setId(null);
        return processDefinitionRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessDefinitionEntity> performList(@Nonnull Pageable pageable,
                                                     @Nullable Specification<ProcessDefinitionEntity> specification,
                                                     @Nullable Filter<ProcessDefinitionEntity> filter) throws ResponseException {
        return processDefinitionRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return processDefinitionRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessDefinitionEntity> retrieve(@Nonnull Specification<ProcessDefinitionEntity> specification) throws ResponseException {
        return processDefinitionRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return processDefinitionRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessDefinitionEntity> specification) {
        return processDefinitionRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessDefinitionEntity performUpdate(@Nonnull Integer id,
                                                 @Nonnull ProcessDefinitionEntity entity,
                                                 @Nonnull ProcessDefinitionEntity existingEntity) throws ResponseException {
        existingEntity.setName(entity.getName());
        existingEntity.setDepartmentId(entity.getDepartmentId());
        return processDefinitionRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessDefinitionEntity entity) throws ResponseException {
        processDefinitionRepository.delete(entity);
    }
}

