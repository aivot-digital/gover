package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEventEntity;
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
public class ProcessInstanceEventService implements EntityService<ProcessInstanceEventEntity, Long> {

    private final ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository;

    @Autowired
    public ProcessInstanceEventService(ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository) {
        this.processInstanceHistoryEventRepository = processInstanceHistoryEventRepository;
    }

    @Nonnull
    @Override
    public ProcessInstanceEventEntity create(@Nonnull ProcessInstanceEventEntity entity) throws ResponseException {
        entity.setId(null);
        return processInstanceHistoryEventRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceEventEntity> performList(@Nonnull Pageable pageable,
                                                        @Nullable Specification<ProcessInstanceEventEntity> specification,
                                                        @Nullable Filter<ProcessInstanceEventEntity> filter) throws ResponseException {
        return processInstanceHistoryEventRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceEventEntity> retrieve(@Nonnull Long id) throws ResponseException {
        return processInstanceHistoryEventRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceEventEntity> retrieve(@Nonnull Specification<ProcessInstanceEventEntity> specification) throws ResponseException {
        return processInstanceHistoryEventRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Long id) {
        return processInstanceHistoryEventRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceEventEntity> specification) {
        return processInstanceHistoryEventRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessInstanceEventEntity performUpdate(@Nonnull Long id,
                                                    @Nonnull ProcessInstanceEventEntity entity,
                                                    @Nonnull ProcessInstanceEventEntity existingEntity) throws ResponseException {
        // No updatable fields for now
        return existingEntity;
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceEventEntity entity) throws ResponseException {
        processInstanceHistoryEventRepository.delete(entity);
    }
}

