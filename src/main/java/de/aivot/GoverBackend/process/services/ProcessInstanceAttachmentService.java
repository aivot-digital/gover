package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceAttachmentRepository;
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
public class ProcessInstanceAttachmentService implements EntityService<ProcessInstanceAttachmentEntity, UUID> {

    private final ProcessInstanceAttachmentRepository processInstanceAttachmentRepository;

    @Autowired
    public ProcessInstanceAttachmentService(ProcessInstanceAttachmentRepository processInstanceAttachmentRepository) {
        this.processInstanceAttachmentRepository = processInstanceAttachmentRepository;
    }

    @Nonnull
    @Override
    public ProcessInstanceAttachmentEntity create(@Nonnull ProcessInstanceAttachmentEntity entity) throws ResponseException {
        entity.setKey(null);
        return processInstanceAttachmentRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceAttachmentEntity> performList(@Nonnull Pageable pageable,
                                                            @Nullable Specification<ProcessInstanceAttachmentEntity> specification,
                                                            @Nullable Filter<ProcessInstanceAttachmentEntity> filter) throws ResponseException {
        return processInstanceAttachmentRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAttachmentEntity> retrieve(@Nonnull UUID key) throws ResponseException {
        return processInstanceAttachmentRepository.findById(key);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAttachmentEntity> retrieve(@Nonnull Specification<ProcessInstanceAttachmentEntity> specification) throws ResponseException {
        return processInstanceAttachmentRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull UUID key) {
        return processInstanceAttachmentRepository.existsById(key);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceAttachmentEntity> specification) {
        return processInstanceAttachmentRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessInstanceAttachmentEntity performUpdate(@Nonnull UUID key,
                                                        @Nonnull ProcessInstanceAttachmentEntity entity,
                                                        @Nonnull ProcessInstanceAttachmentEntity existingEntity) throws ResponseException {
        existingEntity.setProcessInstanceId(entity.getProcessInstanceId());
        existingEntity.setProcessInstanceTaskId(entity.getProcessInstanceTaskId());
        existingEntity.setUploadedByUserId(entity.getUploadedByUserId());
        existingEntity.setUploadedAt(entity.getUploadedAt());
        return processInstanceAttachmentRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceAttachmentEntity entity) throws ResponseException {
        processInstanceAttachmentRepository.delete(entity);
    }
}

