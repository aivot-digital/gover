package de.aivot.GoverBackend.submission.services;

import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.filters.SubmissionAttachmentFilter;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubmissionService implements EntityService<Submission, String> {
    private final SubmissionRepository repository;
    private final SubmissionAttachmentService attachmentService;

    @Autowired
    public SubmissionService(
            SubmissionRepository repository,
            SubmissionAttachmentService attachmentService
    ) {
        this.repository = repository;
        this.attachmentService = attachmentService;
    }

    @Nonnull
    @Override
    public Submission create(@Nonnull Submission entity) throws ResponseException {
        entity.setId(UUID.randomUUID().toString());
        return repository.save(entity);
    }

    @Nonnull
    @Override
    public Page<Submission> performList(@Nonnull Pageable pageable, @Nullable Specification<Submission> specification, Filter<Submission> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<Submission> retrieve(@Nonnull String id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<Submission> retrieve(@Nonnull Specification<Submission> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull String id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<Submission> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    @Override
    public Submission performUpdate(@Nonnull String id, @Nonnull Submission entity, @Nonnull Submission existingEntity) throws ResponseException {
        if (existingEntity.getArchived() != null) {
            throw new ResponseException(HttpStatus.CONFLICT, "Der Antrag wurde bereits archiviert und kann nicht mehr bearbeitet werden.");
        }

        existingEntity.setFileNumber(entity.getFileNumber());

        // TODO: Check if assignee exists and is allowed to be assigned

        existingEntity.setAssigneeId(entity.getAssigneeId());
        if (existingEntity.getAssigneeId() != null && existingEntity.getStatus() == SubmissionStatus.OpenForManualWork) {
            existingEntity.setStatus(SubmissionStatus.ManualWorkingOn);
        }

        if (StringUtils.isNullOrEmpty(existingEntity.getAssigneeId()) && existingEntity.getStatus() == SubmissionStatus.ManualWorkingOn) {
            existingEntity.setAssigneeId(null);
            existingEntity.setStatus(SubmissionStatus.OpenForManualWork);
        }

        if (entity.getArchived() != null) {
            existingEntity.setArchived(entity.getArchived());
            existingEntity.setStatus(SubmissionStatus.Archived);
        }

        if (entity.getDestinationId() == null && existingEntity.getDestinationId() != null) {
            existingEntity.setDestinationId(null);
            existingEntity.setDestinationSuccess(null);
            existingEntity.setDestinationResult(null);

            if (existingEntity.getAssigneeId() != null) {
                existingEntity.setStatus(SubmissionStatus.ManualWorkingOn);
            } else {
                existingEntity.setStatus(SubmissionStatus.OpenForManualWork);
            }
        }

        return repository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull Submission entity) throws ResponseException {
        var spec = SubmissionAttachmentFilter
                .create()
                .setSubmissionId(entity.getId());

        var attachments = attachmentService
                .list(null, spec);

        for (var attachment : attachments) {
            attachmentService.performDelete(attachment);
        }

        repository.delete(entity);
    }
}
