package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.enums.SystemAssetKey;
import de.aivot.GoverBackend.models.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Component
public class BlobService {
    @Autowired
    public MinioConfig minioConfig;

    public String storeData(String prefix, String filename, String path) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        MinioClient minioClient = getClient();

        prepareBucket(minioClient);

        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(prefix + "/" + filename)
                        .filename(path)
                        .build());

        return String.format("%s/%s/%s/%s", minioConfig.getUrl(), minioConfig.getBucket(), prefix, filename);
    }

    public String storeData(String prefix, String filename, byte[] file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        Path tmpPath = Paths.get("/tmp/" + filename);
        Files.write(tmpPath, file);
        String url = storeData(prefix, filename, tmpPath.toString());
        Files.delete(tmpPath);
        return url;
    }

    public String getAssetLink(SystemAssetKey key) {
        return String.format("%s/%s/assets/%s", minioConfig.getUrl(), minioConfig.getBucket(), key.getKey());
    }

    public String getCodeLink(Long id) {
        return String.format("%s/%s/code/%d.js", minioConfig.getUrl(), minioConfig.getBucket(), id);
    }

    private MinioClient getClient() {
        return MinioClient.builder()
                .endpoint(minioConfig.getUrl())
                .credentials(minioConfig.getAccess(), minioConfig.getSecret())
                .build();
    }

    private void prepareBucket(MinioClient minioClient) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs
                            .builder()
                            .bucket(minioConfig.getBucket())
                            .config(String.format("{\"Version\": \"2012-10-17\", \"Statement\": [{\"Action\": \"s3:GetObject\", \"Effect\": \"Allow\", \"Principal\": \"*\", \"Resource\": \"arn:aws:s3:::%s/*\"}]}", minioConfig.getBucket()))
                            .build());
        }

    }
}
