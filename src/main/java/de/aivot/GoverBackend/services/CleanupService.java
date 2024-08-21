package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.repositories.FormRepository;
import de.aivot.GoverBackend.repositories.SubmissionAttachmentRepository;
import de.aivot.GoverBackend.repositories.SubmissionRepository;
import de.aivot.GoverBackend.services.mail.ExceptionMailService;
import de.aivot.GoverBackend.services.storages.SubmissionStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
public class CleanupService {
    private final SubmissionStorageService submissionStorageService;
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final ExceptionMailService exceptionMailService;

    @Autowired
    public CleanupService(
            SubmissionStorageService submissionStorageService,
            FormRepository formRepository,
            SubmissionRepository submissionRepository,
            SubmissionAttachmentRepository submissionAttachmentRepository,
            ExceptionMailService exceptionMailService
    ) {
        this.submissionStorageService = submissionStorageService;
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.exceptionMailService = exceptionMailService;
    }

    @Scheduled(
            cron = "0 0 * * * *",
            zone = "Europe/Paris"
    )
    public void cleanSubmissions() {
        var archivedSubmission = submissionRepository.findAllByArchivedIsNotNull();

        for (var submission : archivedSubmission) {
            try {
                var optForm = formRepository
                        .findById(submission.getFormId());
                if (optForm.isPresent()) {
                    var form = optForm.get();

                    var deletionWeeks = form.getSubmissionDeletionWeeks();
                    if (deletionWeeks == null || deletionWeeks < 1) {
                        deletionWeeks = 4;
                    }

                    var expirationDate = submission.getArchived().plusWeeks(deletionWeeks);
                    if (expirationDate.isBefore(LocalDateTime.now())) {
                        submissionStorageService
                                .deleteSubmission(submission);
                        submissionRepository
                                .delete(submission);
                    }
                }
            } catch (Exception e) {
                exceptionMailService.send(e);
            }
        }
    }
}
