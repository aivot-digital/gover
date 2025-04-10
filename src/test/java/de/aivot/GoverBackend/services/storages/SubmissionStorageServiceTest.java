package de.aivot.GoverBackend.services.storages;

import de.aivot.GoverBackend.models.config.StorageConfig;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.entities.SubmissionAttachment;
import de.aivot.GoverBackend.submission.repositories.SubmissionAttachmentRepository;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;

import static de.aivot.GoverBackend.TestConstants.TEST_FILE_DIRECTORY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SubmissionStorageServiceTest {
    @Mock
    private StorageConfig storageConfig;

    @InjectMocks
    private StorageService storageService;

    @Mock
    private SubmissionAttachmentRepository submissionAttachmentRepository;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(storageConfig.localStorageEnabled()).thenReturn(true);
        when(storageConfig.getLocalStoragePath()).thenReturn(TEST_FILE_DIRECTORY);
    }

    @Test
    void testGetAttachmentData() throws IOException {
        if (new File(TEST_FILE_DIRECTORY).exists()) {
            FileUtils.deleteDirectory(new File(TEST_FILE_DIRECTORY));
        }

        Submission submission = new Submission();
        submission.setId("1");

        SubmissionAttachment submissionAttachment = new SubmissionAttachment();
        submissionAttachment.setId("1");

        byte[] data = new byte[]{1, 2, 3, 4, 5};

        var submissionStorageService = new SubmissionStorageService(storageService, submissionAttachmentRepository, storageConfig);

        FileUtils.writeByteArrayToFile(new File(TEST_FILE_DIRECTORY + "/submissions/1/attachments/1"), data);
        byte[] result = submissionStorageService.getAttachmentData(submission, submissionAttachment);
        assertArrayEquals(data, result);

        FileUtils.deleteDirectory(new File(TEST_FILE_DIRECTORY + "/submissions/1/attachments"));

        FileUtils.writeByteArrayToFile(new File(TEST_FILE_DIRECTORY + "/submissions/1/1"), data);
        result = submissionStorageService.getAttachmentData(submission, submissionAttachment);
        assertArrayEquals(data, result);

        FileUtils.deleteDirectory(new File(TEST_FILE_DIRECTORY + "/submissions/1"));
        assertThrows(Exception.class, () -> submissionStorageService.getAttachmentData(submission, submissionAttachment));

        FileUtils.deleteDirectory(new File(TEST_FILE_DIRECTORY));
    }

    @Test
    void testSaveAttachment() throws IOException {
        Submission submission = new Submission();
        submission.setId("1");

        SubmissionAttachment submissionAttachment = new SubmissionAttachment();
        submissionAttachment.setId("1");

        byte[] data = new byte[]{1, 2, 3, 4, 5};

        var submissionStorageService = new SubmissionStorageService(storageService, submissionAttachmentRepository, storageConfig);

        submissionStorageService.saveAttachment(submission, submissionAttachment, data);

        assertTrue(new File(TEST_FILE_DIRECTORY + "/submissions/1/attachments/1").exists());
        assertArrayEquals(data, storageService.getFile("submissions/1/attachments/1"));

        FileUtils.deleteDirectory(new File(TEST_FILE_DIRECTORY));
    }

    @Test
    void testDeleteSubmission() throws IOException {
        Submission submission = new Submission();
        submission.setId("1");

        SubmissionAttachment submissionAttachment = new SubmissionAttachment();
        submissionAttachment.setId("1");

        byte[] data = new byte[]{1, 2, 3, 4, 5};

        var submissionStorageService = new SubmissionStorageService(storageService, submissionAttachmentRepository, storageConfig);

        submissionStorageService.saveAttachment(submission, submissionAttachment, data);

        submissionStorageService.deleteSubmission(submission);

        assertFalse(new File(TEST_FILE_DIRECTORY + "/submissions/1/attachments/1").exists());
        assertFalse(new File(TEST_FILE_DIRECTORY + "/submissions/1").exists());

        FileUtils.deleteDirectory(new File(TEST_FILE_DIRECTORY));
    }
}