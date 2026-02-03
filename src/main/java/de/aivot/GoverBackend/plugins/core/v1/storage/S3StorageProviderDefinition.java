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
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.repositories.SecretRepository;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

@Component
public class S3StorageProviderDefinition implements StorageProviderDefinition<S3StorageProviderDefinition.Config>, PluginComponent {
    private final SecretRepository secretRepository;
    private final SecretService secretService;

    public S3StorageProviderDefinition(SecretRepository secretRepository, SecretService secretService) {
        this.secretRepository = secretRepository;
        this.secretService = secretService;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "S3 Speicheranbieter";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Speichert Dokumente auf einem S3-kompatiblen Speicher.";
    }

    @Nonnull
    @Override
    public String getKey() {
        return "s3_storage";
    }

    @Nonnull
    @Override
    public Integer getVersion() {
        return 1;
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
    public Class<S3StorageProviderDefinition.Config> getConfigClass() {
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
    public void testConnection(@Nonnull Config config) throws StorageException {
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
                 NoSuchAlgorithmException | IOException | InvalidResponseException | InvalidKeyException | InternalException e) {
            throw new StorageException(e, "Die Verbindung zum S3-kompatiblen Speicher konnte nicht hergestellt werden.");
        }

        if (!bucketExists) {
            throw new StorageException("Der angegebene Bucket '%s' existiert nicht.", config.bucket);
        }
    }

    @Nonnull
    @Override
    public StorageFolder rootFolder(@Nonnull Config config, boolean recursive) throws StorageException {
        return retrieveFolder(config, "/", recursive)
                .orElseThrow(() -> new StorageException("Das Stammverzeichnis existiert nicht."));
    }

    @Nonnull
    @Override
    public Optional<StorageFolder> retrieveFolder(@Nonnull Config config, @Nonnull String pathFromRoot, boolean recursive) {
        var client = getClient(config);

        var listObjectsArgs = ListObjectsArgs
                .builder()
                .bucket(config.bucket)
                .prefix(pathFromRoot)
                .recursive(recursive)
                .build();

        var objects = client.listObjects(listObjectsArgs);



        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public StorageFolder createFolder(@Nonnull Config config, @Nonnull String pathFromRoot) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public boolean folderExists(@Nonnull Config config, @Nonnull String path) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void deleteFolder(@Nonnull Config config, @Nonnull String path) throws StorageException {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public StorageDocument storeDocument(@Nonnull Config config, @Nonnull String path, @Nonnull byte[] data) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public Optional<StorageDocument> retrieveDocument(@Nonnull Config config, @Nonnull String path) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public InputStream retrieveDocumentContent(@Nonnull Config config, @Nonnull String pathFromRoot) throws StorageException {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public boolean documentExists(@Nonnull Config config, @Nonnull String path) {
        return false;
    }

    @Override
    public void deleteDocument(@Nonnull Config config, @Nonnull String path) {
        // TODO
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
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String endpoint;

        @InputElementPOJOBinding(id = "bucket", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bucket"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Name des Buckets, in dem die Dateien gespeichert werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String bucket;

        @InputElementPOJOBinding(id = "access_key", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Access Key"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Access Key für den Zugriff auf den S3-kompatiblen Speicher."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String accessKey;

        @InputElementPOJOBinding(id = "secret_key_secret", type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Secret Key"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das Geheimnis des Secret Keys für den Zugriff auf den S3-kompatiblen Speicher."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String secretKeySecret;
    }
}
