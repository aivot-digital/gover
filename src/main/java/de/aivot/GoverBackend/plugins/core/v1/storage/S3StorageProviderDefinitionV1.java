package de.aivot.GoverBackend.plugins.core.v1.storage;

import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.repositories.SecretRepository;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.storage.services.KnownExtensionsService;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class S3StorageProviderDefinitionV1 implements StorageProviderDefinition<S3StorageProviderDefinitionV1.Config> {
    private static final String ACCESS_DENIED_ERROR_CODE = "AccessDenied";

    private final SecretRepository secretRepository;
    private final SecretService secretService;
    private final KnownExtensionsService knownExtensionsService;

    // TODO: Maybe cache MinioClients for better performance

    public S3StorageProviderDefinitionV1(SecretRepository secretRepository,
                                         SecretService secretService,
                                         KnownExtensionsService knownExtensionsService) {
        this.secretRepository = secretRepository;
        this.secretService = secretService;
        this.knownExtensionsService = knownExtensionsService;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "s3_storage";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getName() {
        return "S3 Speicheranbieter";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Speichert Dokumente auf einem S3-kompatiblen Speicher.
                """;
    }

    @Nonnull
    @Override
    public Boolean getSupportsMetadataAttributes() {
        return true;
    }

    @Nullable
    @Override
    public ConfigLayoutElement getProviderConfigLayout() throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e);
        }

        layout
                .findChild("secret_key_secret", SelectInputElement.class)
                .ifPresent(field -> {
                    var options = secretRepository
                            .findAll()
                            .stream()
                            .map(secret -> RadioInputElementOption.of(
                                    secret.getKey().toString(),
                                    secret.getName()
                            ))
                            .toList();

                    field.setOptions(options);
                });

        return layout;
    }

    @Override
    public Class<S3StorageProviderDefinitionV1.Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public void initializeProvider(@Nonnull Config config) throws StorageException {
        // Nothing to do here
    }

    @Override
    public boolean shouldResync(@Nullable Config oldConfig, @Nonnull Config newConfig) {
        if (oldConfig == null) {
            return true;
        }
        return !oldConfig.endpoint.equals(newConfig.endpoint)
                || !oldConfig.bucket.equals(newConfig.bucket)
                || !oldConfig.accessKey.equals(newConfig.accessKey)
                || !oldConfig.secretKeySecret.equals(newConfig.secretKeySecret);
    }

    @Override
    public void testConnection(@Nonnull Config config, @Nonnull Boolean mustCheckWritable) throws StorageException {
        var client = getClient(config);

        var bucketTestRequest = BucketExistsArgs
                .builder()
                .bucket(config.bucket)
                .build();

        boolean bucketExists;
        try {
            bucketExists = client
                    .bucketExists(bucketTestRequest);
        } catch (ErrorResponseException | InsufficientDataException | XmlParserException | ServerException |
                 NoSuchAlgorithmException | IOException | InvalidResponseException | InvalidKeyException |
                 InternalException e) {
            throw new StorageException(e, "Die Verbindung zum S3-kompatiblen Speicher konnte nicht hergestellt werden.");
        }

        if (!bucketExists) {
            throw new StorageException("Der angegebene Bucket '%s' existiert nicht.", config.bucket);
        }

        if (mustCheckWritable) {
            String testObjectName = "permissions-check-temp";

            try {
                // 1. Attempt a 0-byte upload
                // This is the "PutObject" action
                client.putObject(
                        PutObjectArgs.builder()
                                .bucket(config.bucket)
                                .object(testObjectName)
                                .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                                .build()
                );

                // 2. Clean up immediately
                client.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(config.bucket)
                                .object(testObjectName)
                                .build()
                );
            } catch (ErrorResponseException e) {
                if (ACCESS_DENIED_ERROR_CODE.equals(e.errorResponse().code())) {
                    throw new StorageException("Der Zugriff auf den S3-kompatiblen Speicher ist nicht schreibbar. Bitte überprüfen Sie die Berechtigungen des Access Keys.");
                } else {
                    throw new StorageException(e, "Fehler beim Überprüfen der Schreibberechtigung im S3-kompatiblen Speicher.");
                }
            } catch (Exception e) {
                throw new StorageException(e, "Fehler beim Überprüfen der Schreibberechtigung im S3-kompatiblen Speicher.");
            }
        }
    }

    @Nonnull
    @Override
    public Optional<StorageFolder> retrieveFolder(@Nonnull Config config, @Nonnull String pathFromRoot, boolean recursive) {
        var client = getClient(config);

        var listObjectsArgs = ListObjectsArgs
                .builder()
                .bucket(config.bucket)
                .prefix(pathFromRoot.substring(1))
                .includeUserMetadata(true)
                .build();

        var objects = client
                .listObjects(listObjectsArgs);

        var folder = new StorageFolder(
                pathFromRoot,
                pathFromRoot.substring(1).isEmpty() ? "Root" : StringUtils.getLastPathSegment(pathFromRoot),
                new LinkedList<>(),
                new LinkedList<>(),
                recursive
        );

        objects.forEach(object -> {
            Item item;
            try {
                item = object.get();
            } catch (ErrorResponseException | InsufficientDataException | XmlParserException | ServerException |
                     NoSuchAlgorithmException | IOException | InvalidResponseException | InvalidKeyException |
                     InternalException e) {
                throw new RuntimeException(e);
            }

            if (item.isDir()) {
                retrieveFolder(config, "/" + item.objectName())
                        .ifPresent(folder::addSubfolder);
            } else {
                var metadata = new StorageItemMetadata();

                if (item.userMetadata() != null) {
                    metadata.putAll(item.userMetadata());
                }

                folder.addDocument(new StorageDocument(
                        "/" + item.objectName(),
                        StringUtils.getLastPathSegment(item.objectName()),
                        item.size(),
                        metadata
                ));
            }
        });

        if (folder.getDocuments().isEmpty() && folder.getSubfolders().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(folder);
    }

    @Nonnull
    @Override
    public StorageFolder createFolder(@Nonnull Config config, @Nonnull String pathFromRoot) {
        return new StorageFolder(
                toPrefixWithSlash(pathFromRoot),
                StringUtils.getLastPathSegment(pathFromRoot),
                new LinkedList<>(),
                new LinkedList<>(),
                false
        );
    }

    @Override
    public boolean folderExists(@Nonnull Config config, @Nonnull String path) {
        return true;
    }

    @Override
    public void deleteFolder(@Nonnull Config config, @Nonnull String path) throws StorageException {
        var client = getClient(config);

        var listArgs = ListObjectsArgs
                .builder()
                .bucket(config.bucket)
                .prefix(path.substring(1))
                .recursive(true)
                .build();

        List<DeleteObject> objectsToDelete = new LinkedList<>();
        client
                .listObjects(listArgs)
                .forEach((object) -> {
                    Item item;
                    try {
                        item = object.get();
                    } catch (ErrorResponseException | InsufficientDataException | XmlParserException | ServerException |
                             NoSuchAlgorithmException | IOException | InvalidResponseException | InvalidKeyException |
                             InternalException e) {
                        throw new StorageException(e, "Fehler beim Auflisten der Objekte im S3-kompatiblen Speicher.");
                    }

                    objectsToDelete.add(new DeleteObject(item.objectName()));
                });

        var removeArgs = RemoveObjectsArgs
                .builder()
                .bucket(config.bucket)
                .objects(objectsToDelete)
                .build();

        client
                .removeObjects(removeArgs);
    }

    @Nonnull
    @Override
    public StorageDocument storeDocument(@Nonnull Config config, @Nonnull String path, @Nonnull InputStream data, @Nonnull StorageItemMetadata metadata) {
        var mimeType = knownExtensionsService
                .determineMimeType(path)
                .orElse(StorageService.UNKNOWN_MIME_TYPE);

        var userMetadata = metadata
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toString()
                ));

        var putObjectArgs = PutObjectArgs
                .builder()
                .bucket(config.bucket)
                .object(path.substring(1))
                .stream(data, -1, 10 * 1024 * 1024)
                .userMetadata(userMetadata)
                .contentType(mimeType)
                .build();

        var client = getClient(config);

        try {
            client.putObject(putObjectArgs);
        } catch (ErrorResponseException | XmlParserException | ServerException | NoSuchAlgorithmException |
                 IOException | InvalidResponseException | InvalidKeyException | InternalException |
                 InsufficientDataException e) {
            throw new StorageException(e, "Das Dokument konnte nicht im S3-kompatiblen Speicher gespeichert werden.");
        }

        return retrieveDocument(config, path).orElseGet(() -> new StorageDocument(
                path,
                StringUtils.getLastPathSegment(path),
                0L,
                metadata
        ));
    }

    @Nonnull
    @Override
    public Optional<StorageDocument> retrieveDocument(@Nonnull Config config, @Nonnull String path) {
        var client = getClient(config);

        var statObjectArgs = StatObjectArgs
                .builder()
                .bucket(config.bucket)
                .object(path.substring(1))
                .build();

        StatObjectResponse response;
        try {
            response = client.statObject(statObjectArgs);
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return Optional.empty();
            }
            throw new StorageException(e, "Fehler beim Abrufen des Dokuments aus dem S3-kompatiblen Speicher.");
        } catch (InsufficientDataException | XmlParserException | ServerException | NoSuchAlgorithmException |
                 IOException | InvalidResponseException | InvalidKeyException | InternalException e) {
            throw new StorageException(e, "Fehler beim Abrufen des Dokuments aus dem S3-kompatiblen Speicher.");
        }

        var metadata = new StorageItemMetadata();
        if (response.userMetadata() != null) {
            metadata.putAll(response.userMetadata());
        }

        return Optional.of(new StorageDocument(
                path,
                StringUtils.getLastPathSegment(path),
                response.size(),
                metadata
        ));
    }

    @Nonnull
    @Override
    public InputStream retrieveDocumentContent(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        var client = getClient(config);

        var getObjectArgs = GetObjectArgs
                .builder()
                .bucket(config.bucket)
                .object(pathFromRoot.substring(1))
                .build();

        InputStream inputStream;
        try {
            inputStream = client.getObject(getObjectArgs);
        } catch (ErrorResponseException | InsufficientDataException | XmlParserException | ServerException |
                 NoSuchAlgorithmException | IOException | InvalidResponseException | InvalidKeyException |
                 InternalException e) {
            throw new StorageException(e, "Fehler beim Abrufen des Dokuments aus dem S3-kompatiblen Speicher.");
        }

        return inputStream;
    }

    @Override
    public boolean documentExists(@Nonnull Config config, @Nonnull String path) {
        var client = getClient(config);

        var statObjectArgs = StatObjectArgs
                .builder()
                .bucket(config.bucket)
                .object(path.substring(1))
                .build();

        try {
            client.statObject(statObjectArgs);
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new StorageException(e, "Fehler beim Überprüfen der Existenz des Dokuments im S3-kompatiblen Speicher.");
        } catch (InsufficientDataException | XmlParserException | ServerException | NoSuchAlgorithmException |
                 IOException | InvalidResponseException | InvalidKeyException | InternalException e) {
            throw new StorageException(e, "Fehler beim Überprüfen der Existenz des Dokuments im S3-kompatiblen Speicher.");
        }
    }

    @Override
    public void deleteDocument(@Nonnull Config config, @Nonnull String path) {
        var client = getClient(config);

        var removeObjectArgs = RemoveObjectArgs
                .builder()
                .bucket(config.bucket)
                .object(path.substring(1))
                .build();

        try {
            client.removeObject(removeObjectArgs);
        } catch (ErrorResponseException | InsufficientDataException | XmlParserException | ServerException |
                 NoSuchAlgorithmException | IOException | InvalidResponseException | InvalidKeyException |
                 InternalException e) {
            throw new StorageException(e, "Das Dokument konnte nicht aus dem S3-kompatiblen Speicher gelöscht werden.");
        }
    }

    private MinioClient getClient(@Nonnull Config config) {
        UUID secretUUID;
        try {
            secretUUID = UUID
                    .fromString(config.secretKeySecret);
        } catch (Exception e) {
            throw new StorageException("Der Secret Key ist ungültig.");
        }

        SecretEntity secret = secretService
                .retrieve(secretUUID)
                .orElseThrow(() -> new StorageException("Das Geheimnis für den Secret Key wurde nicht gefunden."));

        String storageSecretKey;
        try {
            storageSecretKey = secretService
                    .decrypt(secret);
        } catch (Exception e) {
            throw new StorageException(e, "Fehler beim Entschlüsseln des Geheimnisses für den Secret Key.");
        }

        return MinioClient
                .builder()
                .endpoint(config.endpoint)
                .credentials(config.accessKey, storageSecretKey)
                .build();
    }

    /**
     * Ensures the given path is prefixed with a single '/'.
     */
    private static String toPrefixWithSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }


    @LayoutElementPOJOBinding(id = "config", type = ElementType.ConfigLayout)
    public static class Config {
        @InputElementPOJOBinding(id = "endpoint", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Endpoint"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Die URL des S3-kompatiblen Speichers."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 8)
        })
        public String endpoint;

        @InputElementPOJOBinding(id = "bucket", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bucket"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Name des Buckets, in dem die Dateien gespeichert werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 4)
        })
        public String bucket;

        @InputElementPOJOBinding(id = "access_key", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Access Key"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Access Key für den Zugriff auf den S3-kompatiblen Speicher."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6)
        })
        public String accessKey;

        @InputElementPOJOBinding(id = "secret_key_secret", type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Secret Key"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das Geheimnis des Secret Keys für den Zugriff auf den S3-kompatiblen Speicher."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6)
        })
        public String secretKeySecret;
    }
}
