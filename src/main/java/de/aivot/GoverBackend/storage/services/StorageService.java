package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntityId;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.storage.repositories.StorageIndexItemRepository;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Optional;

@Service
public class StorageService {
    public static final String FOLDER_MIME_TYPE = "inode/directory";
    public static final String UNKNOWN_MIME_TYPE = "application/octet-stream";

    private final StorageProviderRepository storageProviderRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;
    private final StorageIndexItemRepository storageIndexItemRepository;
    private final KnownExtensionsService knownExtensionsService;

    @Autowired
    public StorageService(StorageProviderRepository storageProviderRepository, StorageProviderDefinitionService storageProviderDefinitionService, StorageIndexItemRepository storageIndexItemRepository, KnownExtensionsService knownExtensionsService) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
        this.storageIndexItemRepository = storageIndexItemRepository;
        this.knownExtensionsService = knownExtensionsService;
    }

    @Nonnull
    public StorageFolder createFolder(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        var createdFolder = definition.createFolder(config, path);

        var indexItem = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                path,
                true,
                StringUtils.getLastPathSegment(path),
                FOLDER_MIME_TYPE
        );
        storageIndexItemRepository
                .save(indexItem);

        return createdFolder;
    }

    public Optional<StorageFolder> getFolder(Integer providerId, String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        return definition.retrieveFolder(config, path);
    }

    public void deleteFolder(Integer providerId, String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        definition.deleteFolder(config, path);

        storageIndexItemRepository
                .deleteById(StorageIndexItemEntityId.of(
                        provider.getId(),
                        path
                ));
    }

    public Optional<StorageDocument> getDocument(Integer providerId, String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        return definition.retrieveDocument(config, path);
    }

    public InputStream getDocumentContent(Integer providerId, String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        return definition.retrieveDocumentContent(config, path);
    }

    public StorageDocument storeDocument(Integer providerId, String path, byte[] content) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        var createdDocument = definition.storeDocument(config, path, content);

        var indexItem = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                path,
                false,
                createdDocument.getName(),
                knownExtensionsService
                        .determineMimeType(createdDocument.getName())
                        .orElse(UNKNOWN_MIME_TYPE)
        );
        storageIndexItemRepository
                .save(indexItem);

        return createdDocument;
    }

    public void deleteDocument(Integer providerId, String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        definition.deleteDocument(config, path);

        storageIndexItemRepository
                .deleteById(StorageIndexItemEntityId.of(
                        provider.getId(),
                        path
                ));
    }

    private StorageProviderEntity retrieveProvider(Integer providerId) throws ResponseException {
        return storageProviderRepository
                .findById(providerId)
                .orElseThrow(() -> ResponseException
                        .internalServerError(
                                "Der Speicheranbieter mit der ID %d konnte nicht gefunden werden.",
                                providerId
                        ));
    }

    private <T> StorageProviderDefinition<T> retrieveDefinition(StorageProviderEntity provider) throws ResponseException {
        var definition = storageProviderDefinitionService
                .retrieveProviderDefinition(provider.getStorageProviderDefinitionKey(), provider.getStorageProviderDefinitionVersion())
                .orElseThrow(() -> ResponseException
                        .internalServerError(
                                "Die Definition des Speicheranbieters %s (ID %d) mit dem Schlüssel %s (Version %d) konnte nicht gefunden werden.",
                                StringUtils.quote(provider.getName()),
                                provider.getId(),
                                StringUtils.quote(provider.getStorageProviderDefinitionKey()),
                                provider.getStorageProviderDefinitionVersion()
                        ));

        return (StorageProviderDefinition<T>) definition;
    }

    private <T> T createConfig(StorageProviderEntity provider, StorageProviderDefinition<T> definition) throws ResponseException {
        try {
            return ElementPOJOMapper
                    .mapToPOJO(provider.getConfiguration(), definition.getConfigClass());
        } catch (Exception e) {
            throw ResponseException
                    .internalServerError(
                            e,
                            "Die Konfiguration des Speicheranbieters %s (ID %d) konnte nicht geladen werden. Die folgende Fehlermeldung wurde protokolliert: %s",
                            StringUtils.quote(provider.getName()),
                            provider.getId(),
                            e.getMessage()
                    );
        }
    }
}
