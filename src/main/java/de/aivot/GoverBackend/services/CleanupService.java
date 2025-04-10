package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.services.storages.SubmissionStorageService;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.filters.SubmissionFilter;
import de.aivot.GoverBackend.submission.services.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@EnableScheduling
public class CleanupService {
    private final ScopedAuditService auditService;

    private final SubmissionService submissionService;
    private final FormService formService;
    private final SubmissionStorageService submissionStorageService;
    private final ExceptionMailService exceptionMailService;

    @Autowired
    public CleanupService(
            AuditService auditService,
            SubmissionService submissionService,
            FormService formService,
            SubmissionStorageService submissionStorageService,
            ExceptionMailService exceptionMailService
    ) {
        this.auditService = auditService.createScopedAuditService(CleanupService.class);

        this.submissionService = submissionService;
        this.formService = formService;
        this.submissionStorageService = submissionStorageService;
        this.exceptionMailService = exceptionMailService;
    }

    @Scheduled(
            cron = "0 0 * * * *",
            zone = "Europe/Paris"
    )
    public void cleanSubmissions() {
        var archivedSubmissionSpec = SubmissionFilter
                .create()
                .setStatus(SubmissionStatus.Archived);

        Page<Submission> archivedSubmission;
        try {
            archivedSubmission = submissionService
                    .list(null, archivedSubmissionSpec);
        } catch (ResponseException e) {
            exceptionMailService.send(e);
            return;
        }

        for (var submission : archivedSubmission.getContent()) {
            try {
                var form = formService
                        .retrieve(submission.getFormId())
                        .orElse(null);

                if (form == null) {
                    auditService.logError("Form with id " + submission.getFormId() + "not found for submission: " + submission.getId(), Map.of(
                            "submissionId", submission.getId(),
                            "formId", submission.getFormId()
                    ));
                    continue;
                }

                var deletionWeeks = form.getSubmissionDeletionWeeks();
                if (deletionWeeks == null || deletionWeeks < 1) {
                    deletionWeeks = 4;
                }

                var expirationDate = submission.getArchived().plusWeeks(deletionWeeks);
                if (expirationDate.isBefore(LocalDateTime.now())) {
                    submissionStorageService
                            .deleteSubmission(submission);
                    submissionService
                            .performDelete(submission);
                }
            } catch (Exception e) {
                exceptionMailService.send(e);
            }
        }
    }
}
