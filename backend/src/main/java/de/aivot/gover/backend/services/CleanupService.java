package de.aivot.gover.backend.services;

import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.enums.SubmissionStatus;
import de.aivot.gover.backend.form.services.FormVersionService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.mail.services.ExceptionMailService;
import de.aivot.gover.backend.services.storages.SubmissionStorageService;
import de.aivot.gover.backend.submission.entities.Submission;
import de.aivot.gover.backend.submission.filters.SubmissionFilter;
import de.aivot.gover.backend.submission.services.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * @deprecated
 */
@Deprecated
@Component
@EnableScheduling
public class CleanupService {
    private final ScopedAuditService auditService;

    private final SubmissionService submissionService;
    private final SubmissionStorageService submissionStorageService;
    private final ExceptionMailService exceptionMailService;
    private final FormVersionService formVersionService;

    @Autowired
    public CleanupService(AuditService auditService,
                          SubmissionService submissionService,
                          SubmissionStorageService submissionStorageService,
                          ExceptionMailService exceptionMailService,
                          FormVersionService formVersionService) {
        this.auditService = auditService.createScopedAuditService(CleanupService.class, "Systemwartung");

        this.submissionService = submissionService;
        this.submissionStorageService = submissionStorageService;
        this.exceptionMailService = exceptionMailService;
        this.formVersionService = formVersionService;
    }

    @Scheduled(
            cron = "0 0 * * * *",
            zone = "${gover.timezone}"
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
                var form = formVersionService
                        .retrieve(submission.getFormId(), submission.getFormVersion())
                        .orElse(null);

                if (form == null) {
                    auditService.create()
                            .setTriggerType("Error")
                            .setMessage(
                                    "Das Formular mit der ID " + submission.getFormId() +
                                            " wurde für den archivierten Antrag mit der ID " + submission.getId() +
                                            " nicht gefunden; der Löschlauf wurde für diesen Antrag übersprungen."
                            )
                            .setMetadata(Map.of(
                                    "submissionId", submission.getId(),
                                    "formId", submission.getFormId()
                            )).log();
                    continue;
                }

                var deletionWeeks = form.getSubmissionRetentionWeeks();
                if (deletionWeeks == null || deletionWeeks < 1) {
                    deletionWeeks = 4;
                }

                var expirationDate = submission.getArchived().plus(Duration.ofDays(deletionWeeks * 7L));
                if (expirationDate.isBefore(Instant.now())) {
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
