package de.aivot.GoverBackend.services.storages;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.StorageConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Component
public class StorageService {
    private final Logger logger = LoggerFactory.getLogger(StorageService.class);

    private final StorageConfig storageConfig;
    // This is protected for testing purposes
    protected MinioClient storageClient;

    @Autowired
    public StorageService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;

        if (storageConfig.remoteStorageEnabled()) {
            this.storageClient = MinioClient
                    .builder()
                    .endpoint(storageConfig.getRemoteEndpoint())
                    .credentials(storageConfig.getRemoteAccessKey(), storageConfig.getRemoteSecretKey())
                    .build();
        } else {
            this.storageClient = null;
        }
    }

    public boolean isRemoteStorageEnabled() {
        return storageConfig.remoteStorageEnabled();
    }

    public boolean isLocalStorageEnabled() {
        return storageConfig.localStorageEnabled();
    }

    public void testRemoteStorageBucketExists() throws ResponseException {
        if (this.storageClient == null) {
            logger.error("Remote storage is not initialized");
            throw ResponseException.internalServerError("Remote storage is not initialized");
        }

        var bucketTestRequest = BucketExistsArgs
                .builder()
                .bucket(storageConfig.getRemoteBucket())
                .build();

        boolean bucketExists;
        try {
            bucketExists = this.storageClient
                    .bucketExists(bucketTestRequest);
        } catch (ErrorResponseException | InsufficientDataException | XmlParserException | ServerException | NoSuchAlgorithmException | IOException | InvalidResponseException | InvalidKeyException | InternalException e) {
            logger.error("Error while checking if bucket exists", e);
            throw ResponseException.internalServerError(e);
        }

        if (!bucketExists) {
            logger.error("Bucket {} does not exist", storageConfig.getRemoteBucket());
            throw ResponseException.internalServerError("Bucket " + storageConfig.getRemoteBucket() + " does not exist");
        }
    }

    public String getRemoteFileUrl(String path, String filename, String contentType) throws ResponseException {
        var extraQueryParams = new HashMap<String, String>();
        extraQueryParams.put("response-content-disposition", "inline; filename=" + filename);
        extraQueryParams.put("response-content-type", contentType);

        var presignedObjectUrlQuery = GetPresignedObjectUrlArgs
                .builder()
                .method(Method.GET)
                .bucket(storageConfig.getRemoteBucket())
                .object(path)
                .extraQueryParams(extraQueryParams)
                .expiry(2, TimeUnit.HOURS)
                .build();

        String url;
        try {
            url = this.storageClient.getPresignedObjectUrl(presignedObjectUrlQuery);
        } catch (InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException | ServerException e) {
            logger.error("Error while generating presigned URL", e);
            throw ResponseException.internalServerError(e);
        } catch (ErrorResponseException e) {
            logger.error("Error while generating presigned URL. Not found", e);
            throw ResponseException.notFound("Die Datei mit dem Pfad %s existiert nicht.", path);
        }
        return url;
    }

    public String getRemoteFileDownloadUrl(String path, String filename, String contentType) throws ResponseException {
        var extraQueryParams = new HashMap<String, String>();
        extraQueryParams.put("response-content-disposition", "attachment; filename=" + filename);
        extraQueryParams.put("response-content-type", contentType);

        var presignedObjectUrlQuery = GetPresignedObjectUrlArgs
                .builder()
                .method(Method.GET)
                .bucket(storageConfig.getRemoteBucket())
                .object(path)
                .extraQueryParams(extraQueryParams)
                .expiry(2, TimeUnit.HOURS)
                .build();

        String url;
        try {
            url = this.storageClient.getPresignedObjectUrl(presignedObjectUrlQuery);
        } catch (InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException | ServerException e) {
            logger.error("Error while generating presigned URL", e);
            throw ResponseException.internalServerError(e);
        } catch (ErrorResponseException e) {
            logger.error("Error while generating presigned URL. Not found", e);
            throw ResponseException.notFound("Die Datei mit dem Pfad %s existiert nicht.", path);
        }
        return url;
    }

    public void writeFile(String path, byte[] data, String contentType) throws ResponseException {
        if (this.storageClient == null) {
            writeLocalFile(path, data);
        } else {
            writeRemoteFile(path, data, contentType);
        }
    }

    private void writeLocalFile(String path, byte[] data) throws ResponseException {
        var pathToFile = resolvePath(path);
        var parent = pathToFile.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                logger.error("Error while creating directories", e);
                throw ResponseException.internalServerError(e);
            }
        }

        try {
            Files.write(pathToFile, data);
        } catch (IOException e) {
            logger.error("Error while writing file", e);
            throw ResponseException.internalServerError(e);
        }
    }

    private void writeRemoteFile(String path, byte[] data, String contentType) throws ResponseException {
        var inputStream = new ByteArrayInputStream(data);
        var putObjectQuery = PutObjectArgs
                .builder()
                .bucket(storageConfig.getRemoteBucket())
                .object(path)
                .stream(inputStream, data.length, -1)
                .contentType(contentType)
                .build();
        try {
            storageClient.putObject(putObjectQuery);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            logger.error("Error while writing file", e);
            throw ResponseException.internalServerError(e);
        }
    }

    public void deleteFile(String path) throws ResponseException {
        if (this.storageClient == null) {
            deleteLocalFile(path);
        } else {
            deleteRemoteFile(path);
        }
    }

    private void deleteLocalFile(String path) throws ResponseException {
        try {
            Files.delete(resolvePath(path));
        } catch (NoSuchFileException e) {
            // Ignore, file does not exist
        } catch (IOException e) {
            logger.error("Error while deleting file", e);
            throw ResponseException.internalServerError(e);
        }
    }

    private void deleteRemoteFile(String path) throws ResponseException {
        var exists = testRemoteFileExists(path);
        if (!exists) {
            // Ignore, file does not exist
            return;
        }

        var removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(storageConfig.getRemoteBucket())
                .object(path)
                .build();
        try {
            storageClient.removeObject(removeObjectArgs);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            logger.error("Error while deleting file", e);
            throw ResponseException.internalServerError(e);
        }
    }

    public byte[] getFile(String path) throws ResponseException {
        if (this.storageClient == null) {
            return getLocalFile(path);
        } else {
            return getRemoteFile(path);
        }
    }

    private byte[] getLocalFile(String path) throws ResponseException {
        try {
            return Files.readAllBytes(resolvePath(path));
        } catch (IOException e) {
            logger.error("Error while reading file", e);
            throw ResponseException.notFound("Die Datei mit dem Pfad %s existiert nicht.", path);
        }
    }

    private byte[] getRemoteFile(String path) throws ResponseException {
        var getObjectArgs = GetObjectArgs.builder()
                .bucket(storageConfig.getRemoteBucket())
                .object(path)
                .build();
        try {
            var object = storageClient.getObject(getObjectArgs);
            return object.readAllBytes();
        } catch (InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            logger.error("Error while reading file", e);
            throw ResponseException.internalServerError(e);
        } catch (ErrorResponseException e) {
            logger.error("Error while reading file. Not found", e);
            throw ResponseException.notFound("Die Datei mit dem Pfad %s existiert nicht.", path);
        }
    }

    public boolean testFileExists(String path) throws ResponseException {
        if (this.storageClient == null) {
            return testLocalFileExists(path);
        } else {
            return testRemoteFileExists(path);
        }
    }

    private boolean testLocalFileExists(String path) {
        var resolvedPath = resolvePath(path);
        return Files.exists(resolvedPath);
    }

    private boolean testRemoteFileExists(String path) throws ResponseException {
        var testObjectArgs = StatObjectArgs
                .builder()
                .bucket(storageConfig.getRemoteBucket())
                .object(path)
                .build();
        try {
            storageClient.statObject(testObjectArgs);
        } catch (ServerException | InsufficientDataException | ErrorResponseException | InternalException | XmlParserException | InvalidResponseException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            logger.error("Error while checking if file exists", e);
            throw ResponseException.internalServerError(e);
        }

        return true;
    }

    private Path resolvePath(String path) {
        return Path.of(storageConfig.getLocalStoragePath(), path);
    }
}
