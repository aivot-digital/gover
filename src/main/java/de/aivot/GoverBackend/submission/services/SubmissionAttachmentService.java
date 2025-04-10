package de.aivot.GoverBackend.submission.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.DeleteEntityService;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.services.storages.SubmissionStorageService;
import de.aivot.GoverBackend.submission.entities.SubmissionAttachment;
import de.aivot.GoverBackend.submission.repositories.SubmissionAttachmentRepository;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;

@Service
public class SubmissionAttachmentService implements ReadEntityService<SubmissionAttachment, String>, DeleteEntityService<SubmissionAttachment, String> {
    private final SubmissionAttachmentRepository repository;
    private final SubmissionStorageService submissionStorageService;
    private final SubmissionRepository submissionRepository;

    @Autowired
    public SubmissionAttachmentService(
            SubmissionAttachmentRepository repository,
            SubmissionStorageService submissionStorageService,
            SubmissionRepository submissionRepository
    ) {
        this.repository = repository;
        this.submissionStorageService = submissionStorageService;
        this.submissionRepository = submissionRepository;
    }

    @Nonnull
    @Override
    public Page<SubmissionAttachment> performList(@Nonnull Pageable pageable, @Nullable Specification<SubmissionAttachment> specification, Filter<SubmissionAttachment> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<SubmissionAttachment> retrieve(@Nonnull String id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<SubmissionAttachment> retrieve(@Nonnull Specification<SubmissionAttachment> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull String id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<SubmissionAttachment> specification) {
        return repository.exists(specification);
    }

    @Override
    public void performDelete(@Nonnull SubmissionAttachment entity) throws ResponseException {
        var submission = submissionRepository
                .findById(entity.getSubmissionId())
                .orElseThrow(() -> new ResponseException(HttpStatus.NOT_FOUND, "Der zugehörige Antrag konnte nicht gefunden werden"));

        repository.delete(entity);

        try {
            submissionStorageService.deleteSubmission(submission);
        } catch (IOException e) {
            throw new ResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Die Anlage zum Antrag konnte nicht gelöscht werden", e.getMessage(), e);
        }
    }
}
