package de.aivot.GoverBackend.services.storages;

import de.aivot.GoverBackend.models.config.StorageConfig;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.SubmissionAttachment;
import de.aivot.GoverBackend.repositories.SubmissionAttachmentRepository;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class SubmissionStorageService {
    private final StorageService storageService;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final StorageConfig storageConfig;

    @Autowired
    public SubmissionStorageService(
            StorageService storageService,
            SubmissionAttachmentRepository submissionAttachmentRepository,
            StorageConfig storageConfig) {
        this.storageService = storageService;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.storageConfig = storageConfig;
    }

    public byte[] getAttachmentData(Submission submission, SubmissionAttachment submissionAttachment) {
        return storageService.getFile(getAttachmentPath(submission, submissionAttachment, true));
    }

    public void saveAttachment(Submission submission, SubmissionAttachment submissionAttachment, byte[] data) {
        storageService.writeFile(getAttachmentPath(submission, submissionAttachment, false), data, submissionAttachment.getContentType());
    }

    public void deleteSubmission(Submission submission) throws IOException {
        deleteAttachments(submission);
        if (storageService.isLocalStorageEnabled()) {
            FileUtils.deleteDirectory(new File(storageConfig.getLocalStoragePath() + "/submissions/" + submission.getId()));
        }
    }

    private void deleteAttachments(Submission submission) {
        submissionAttachmentRepository
                .findAllBySubmissionId(submission.getId())
                .forEach(submissionAttachment -> deleteAttachment(submission, submissionAttachment));
    }

    private void deleteAttachment(Submission submission, SubmissionAttachment submissionAttachment) {
        storageService.deleteFile(getAttachmentPath(submission, submissionAttachment, true));
    }

    private String getAttachmentPath(Submission submission, SubmissionAttachment submissionAttachment, boolean checkExistence) {
        String storagePrefix = "submissions/";

        String path = storagePrefix + submission.getId() + "/attachments/" + submissionAttachment.getId();
        // This path resembles the old path structure, which is deprecated
        String deprecatedPath = storagePrefix + submission.getId() + "/" + submissionAttachment.getId();

        // If the file checking is not enabled, return the path. This is useful for saving files
        if (!checkExistence) {
            return path;
        }

        // If the file exists in the new path, return the new path
        if (storageService.testLocalFileExists(path)) {
            return path;
        }

        // If the file does not exist in the default path, return the deprecated path
        return deprecatedPath;
    }
}
