package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceHistoryEventEntity;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessInstanceHistoryEventService implements EntityService<ProcessInstanceHistoryEventEntity, Long> {

    private final ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository;

    @Autowired
    public ProcessInstanceHistoryEventService(ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository) {
        this.processInstanceHistoryEventRepository = processInstanceHistoryEventRepository;
    }

    @Nonnull
    @Override
    public ProcessInstanceHistoryEventEntity create(@Nonnull ProcessInstanceHistoryEventEntity entity) throws ResponseException {
        entity.setId(null);
        return processInstanceHistoryEventRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceHistoryEventEntity> performList(@Nonnull Pageable pageable,
                                                              @Nullable Specification<ProcessInstanceHistoryEventEntity> specification,
                                                              @Nullable Filter<ProcessInstanceHistoryEventEntity> filter) throws ResponseException {
        return processInstanceHistoryEventRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceHistoryEventEntity> retrieve(@Nonnull Long id) throws ResponseException {
        return processInstanceHistoryEventRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceHistoryEventEntity> retrieve(@Nonnull Specification<ProcessInstanceHistoryEventEntity> specification) throws ResponseException {
        return processInstanceHistoryEventRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Long id) {
        return processInstanceHistoryEventRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceHistoryEventEntity> specification) {
        return processInstanceHistoryEventRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessInstanceHistoryEventEntity performUpdate(@Nonnull Long id,
                                                          @Nonnull ProcessInstanceHistoryEventEntity entity,
                                                          @Nonnull ProcessInstanceHistoryEventEntity existingEntity) throws ResponseException {
        existingEntity.setTimestamp(entity.getTimestamp());
        existingEntity.setTriggeringUserId(entity.getTriggeringUserId());
        existingEntity.setProcessInstanceId(entity.getProcessInstanceId());
        existingEntity.setProcessInstanceTaskId(entity.getProcessInstanceTaskId());
        return processInstanceHistoryEventRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceHistoryEventEntity entity) throws ResponseException {
        processInstanceHistoryEventRepository.delete(entity);
    }
}

