package de.aivot.GoverBackend.services.storages;

import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.StorageConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.ErrorResponse;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static de.aivot.GoverBackend.TestConstants.TEST_FILE_DIRECTORY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class StorageServiceTest {
    @Mock
    private StorageConfig storageConfig;

    @Mock
    private MinioClient storageClient;

    @InjectMocks
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsRemoteStorageEnabled() {
        when(storageConfig.getRemoteEndpoint()).thenReturn("http://localhost:9000");
        when(storageConfig.getRemoteAccessKey()).thenReturn("accessKey");
        when(storageConfig.getRemoteSecretKey()).thenReturn("secretKey");

        when(storageConfig.remoteStorageEnabled()).thenReturn(true);
        assertTrue(storageService.isRemoteStorageEnabled());

        when(storageConfig.remoteStorageEnabled()).thenReturn(false);
        assertFalse(storageService.isRemoteStorageEnabled());
    }

    @Test
    void testIsLocalStorageEnabled() {
        when(storageConfig.localStorageEnabled()).thenReturn(true);
        assertTrue(storageService.isLocalStorageEnabled());

        when(storageConfig.localStorageEnabled()).thenReturn(false);
        assertFalse(storageService.isLocalStorageEnabled());
    }

    @Test
    void testRemoteStorageBucketExists() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        when(storageConfig.remoteStorageEnabled()).thenReturn(true);
        when(storageConfig.getRemoteEndpoint()).thenReturn("http://localhost:9000");
        when(storageConfig.getRemoteAccessKey()).thenReturn("accessKey");
        when(storageConfig.getRemoteSecretKey()).thenReturn("secretKey");
        when(storageConfig.getRemoteBucket()).thenReturn("test-bucket");

        storageService.storageClient = storageClient;

        when(storageClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        assertDoesNotThrow(storageService::testRemoteStorageBucketExists);

        when(storageClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        assertThrows(RuntimeException.class, storageService::testRemoteStorageBucketExists);
    }

    @Test
    void testGetRemoteFileUrl() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ResponseException {
        when(storageConfig.getRemoteBucket()).thenReturn("test-bucket");

        storageService.storageClient = storageClient;

        when(storageClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("http://localhost:9000/test-bucket/test-file");
        assertEquals("http://localhost:9000/test-bucket/test-file", storageService.getRemoteFileUrl("test-file", "test-file", "application/json"));

        when(storageClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenThrow(new IOException());
        assertThrows(RuntimeException.class, () -> storageService.getRemoteFileUrl("test-file", "test-file", "application/json"));

        when(storageClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenThrow(new ErrorResponseException(new ErrorResponse(), null, ""));
        assertThrows(NotFoundException.class, () -> storageService.getRemoteFileUrl("test-file", "test-file", "application/json"));
    }

    @Test
    void testWriteFile() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ResponseException {
        final String testFileName = "test-file";
        final byte[] testFileBytes = "test-content".getBytes();
        final String testFileContentType = "text/plain";

        // Test local write file
        when(storageConfig.getLocalStoragePath()).thenReturn(TEST_FILE_DIRECTORY);

        storageService.writeFile(testFileName, testFileBytes, testFileContentType);
        assertTrue(new File(TEST_FILE_DIRECTORY + "/" + testFileName).exists());
        Files.delete(new File(TEST_FILE_DIRECTORY + "/" + testFileName).toPath());

        // Test remote write file
        storageService.storageClient = storageClient;

        when(storageClient.putObject(any(PutObjectArgs.class))).thenThrow(IOException.class);
        assertThrows(RuntimeException.class, () -> storageService.writeFile(testFileName, testFileBytes, testFileContentType));
    }

    @Test
    void testDeleteFile() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ResponseException {
        final String testFileName = "test-file";
        final byte[] testFileBytes = "test-content".getBytes();

        FileUtils.writeByteArrayToFile(new File(TEST_FILE_DIRECTORY + "/" + testFileName), testFileBytes);

        // Test local delete file
        when(storageConfig.getLocalStoragePath()).thenReturn(TEST_FILE_DIRECTORY);

        storageService.deleteFile(testFileName);
        assertFalse(new File(TEST_FILE_DIRECTORY + "/" + testFileName).exists());

        // Test remote write file
        storageService.storageClient = storageClient;

        doThrow(IOException.class).when(storageClient).removeObject(any(RemoveObjectArgs.class));
        assertThrows(RuntimeException.class, () -> storageService.deleteFile(testFileName));
    }

    @Test
    void testGetFile() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, ResponseException {
        final String testFileName = "test-file";
        final byte[] testFileBytes = "test-content".getBytes();

        FileUtils.writeByteArrayToFile(new File(TEST_FILE_DIRECTORY + "/" + testFileName), testFileBytes);

        // Test local get file
        when(storageConfig.getLocalStoragePath()).thenReturn(TEST_FILE_DIRECTORY);
        assertArrayEquals(testFileBytes, storageService.getFile(testFileName));

        // Test remote get file
        storageService.storageClient = storageClient;

        when(storageClient.getObject(any(GetObjectArgs.class))).thenThrow(IOException.class);
        assertThrows(RuntimeException.class, () -> storageService.getFile(testFileName));
    }

    @Test
    void testTestLocalFileExists() throws IOException, ResponseException {
        final String testFileName = "test-file";
        final byte[] testFileBytes = "test-content".getBytes();

        // Delete test file directory if it exists
        if (new File(TEST_FILE_DIRECTORY).exists()) {
            FileUtils.deleteDirectory(new File(TEST_FILE_DIRECTORY));
        }

        // Set local storage path
        when(storageConfig.getLocalStoragePath()).thenReturn(TEST_FILE_DIRECTORY);

        // Create test file
        FileUtils.writeByteArrayToFile(new File(TEST_FILE_DIRECTORY + "/" + testFileName), testFileBytes);
        assertTrue(storageService.testFileExists(testFileName));

        // Delete test file
        Files.delete(new File(TEST_FILE_DIRECTORY + "/" + testFileName).toPath());
        assertFalse(storageService.testFileExists(testFileName));

        // Delete test file directory
        Files.delete(new File(TEST_FILE_DIRECTORY).toPath());
    }
}