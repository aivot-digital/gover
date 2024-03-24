package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.mail.ExceptionMailService;
import de.aivot.GoverBackend.mail.MailService;
import de.aivot.GoverBackend.repositories.FormRepository;
import de.aivot.GoverBackend.repositories.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@EnableScheduling
public class CleanupService {
    private final SubmissionStorageService storageService;
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;
    private final ExceptionMailService exceptionMailService;

    @Autowired
    public CleanupService(
            SubmissionStorageService storageService,
            FormRepository formRepository,
            SubmissionRepository submissionRepository,
            ExceptionMailService exceptionMailService
    ) {
        this.storageService = storageService;
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
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
                        storageService.deleteSubmissionFolder(submission.getId());
                        submissionRepository.delete(submission);
                    }
                }
            } catch (IOException e) {
                exceptionMailService.send(e);
            }
        }
    }
}
