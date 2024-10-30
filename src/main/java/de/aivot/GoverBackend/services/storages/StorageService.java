package de.aivot.GoverBackend.services.storages;

import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.config.StorageConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Component
public class StorageService {
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

    public void testRemoteStorageBucketExists() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (this.storageClient == null) {
            throw new RuntimeException("Remote storage is not initialized");
        }

        var bucketTestRequest = BucketExistsArgs
                .builder()
                .bucket(storageConfig.getRemoteBucket())
                .build();

        var bucketExists = this.storageClient.bucketExists(bucketTestRequest);
        if (!bucketExists) {
            throw new RuntimeException("Bucket " + storageConfig.getRemoteBucket() + " does not exist");
        }
    }

    public String getRemoteFileUrl(String path, String filename, String contentType) {
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
            throw new RuntimeException(e);
        } catch (ErrorResponseException e) {
            throw new NotFoundException();
        }
        return url;
    }

    public void writeFile(String path, byte[] data, String contentType) {
        if (this.storageClient == null) {
            writeLocalFile(path, data);
        } else {
            writeRemoteFile(path, data, contentType);
        }
    }

    private void writeLocalFile(String path, byte[] data) {
        var pathToFile = resolvePath(path);
        var parent = pathToFile.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Files.write(pathToFile, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeRemoteFile(String path, byte[] data, String contentType) {
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
            throw new RuntimeException(e);
        }
    }

    public void deleteFile(String path) {
        if (this.storageClient == null) {
            deleteLocalFile(path);
        } else {
            deleteRemoteFile(path);
        }
    }

    private void deleteLocalFile(String path) {
        try {
            Files.delete(resolvePath(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteRemoteFile(String path) {
        var removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(storageConfig.getRemoteBucket())
                .object(path)
                .build();
        try {
            storageClient.removeObject(removeObjectArgs);
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getFile(String path) {
        if (this.storageClient == null) {
            return getLocalFile(path);
        } else {
            return getRemoteFile(path);
        }
    }

    private byte[] getLocalFile(String path) {
        try {
            return Files.readAllBytes(resolvePath(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getRemoteFile(String path) {
        var getObjectArgs = GetObjectArgs.builder()
                .bucket(storageConfig.getRemoteBucket())
                .object(path)
                .build();
        try {
            var object = storageClient.getObject(getObjectArgs);
            return object.readAllBytes();
        } catch (InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException | XmlParserException e) {
            throw new RuntimeException(e);
        } catch (ErrorResponseException e) {
            throw new NotFoundException();
        }
    }

    /**
     * TODO: Create a function to test the remote file exists
     */
    public boolean testLocalFileExists(String path) {
        var resolvedPath = resolvePath(path);
        return Files.exists(resolvedPath);
    }

    private Path resolvePath(String path) {
        return Path.of(storageConfig.getLocalStoragePath(), path);
    }
}
