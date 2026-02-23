package de.aivot.GoverBackend.storage.services;

import com.beust.jcommander.Strings;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntityId;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.storage.repositories.StorageIndexItemRepository;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
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
    public StorageService(StorageProviderRepository storageProviderRepository,
                          StorageProviderDefinitionService storageProviderDefinitionService,
                          StorageIndexItemRepository storageIndexItemRepository,
                          KnownExtensionsService knownExtensionsService) {
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

        var createdFolder = definition
                .createFolder(config, path);

        var createdFolderPathParts = StringUtils
                .getPathSegments(createdFolder.getPathFromRoot());

        for (int i = 0; i < createdFolderPathParts.size(); i++) {
            var createdFolderPath = Strings
                    .join("/", createdFolderPathParts.subList(0, i + 1)) + "/";

            var exists = storageIndexItemRepository
                    .existsById(StorageIndexItemEntityId.of(provider.getId(), createdFolderPath));

            if (!exists) {
                var indexItem = new StorageIndexItemEntity(
                        provider.getId(),
                        provider.getType(),
                        createdFolderPath,
                        true,
                        StringUtils.getLastPathSegment(createdFolderPath),
                        FOLDER_MIME_TYPE,
                        false,
                        StorageItemMetadata.empty(),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
                storageIndexItemRepository
                        .save(indexItem);
            }
        }

        return createdFolder;
    }

    public Optional<StorageFolder> getFolder(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        return definition.retrieveFolder(config, path);
    }

    public void deleteFolder(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        definition.deleteFolder(config, path);

        // TODO: Find all subfolders and documents and delete them as well

        storageIndexItemRepository
                .deleteById(StorageIndexItemEntityId.of(
                        provider.getId(),
                        path
                ));
    }

    public Optional<StorageDocument> getDocument(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        var doc = definition.retrieveDocument(config, path);

        doc.ifPresent(d -> {
            var filteredMetadata = new StorageItemMetadata();
            if (definition.getSupportsMetadataAttributes()) {
                for (var ma : provider.getMetadataAttributes()) {
                    if (d.getMetadata().containsKey(ma.getKey())) {
                        filteredMetadata.put(
                                ma.getKey(),
                                d.getMetadata().get(ma.getKey())
                        );
                    }
                }
            }
            d.setMetadata(filteredMetadata);
        });

        return doc;
    }

    public InputStream getDocumentContent(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        return definition.retrieveDocumentContent(config, path);
    }

    public StorageDocument storeDocument(@Nonnull Integer providerId,
                                         @Nonnull String path,
                                         @Nonnull byte[] content,
                                         @Nonnull StorageItemMetadata metadata) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        // Only respect metadata attributes if the provider definition supports them.
        // Additionally, filter out any metadata attributes that are not supported by the provider definition.
        var filteredMetadata = new StorageItemMetadata();
        if (definition.getSupportsMetadataAttributes()) {
            for (var ma : provider.getMetadataAttributes()) {
                if (metadata.containsKey(ma.getKey())) {
                    filteredMetadata.put(
                            ma.getKey(),
                            metadata.get(ma.getKey())
                    );
                }
            }
        }

        var createdDocument = definition
                .storeDocument(config, path, content, filteredMetadata);

        var indexItem = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                path,
                false,
                createdDocument.getName(),
                knownExtensionsService
                        .determineMimeType(createdDocument.getName())
                        .orElse(UNKNOWN_MIME_TYPE),
                false,
                createdDocument.getMetadata(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        storageIndexItemRepository
                .save(indexItem);

        return createdDocument;
    }

    public void deleteDocument(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
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

    private StorageProviderEntity retrieveProvider(@Nonnull Integer providerId) throws ResponseException {
        return storageProviderRepository
                .findById(providerId)
                .orElseThrow(() -> ResponseException
                        .internalServerError(
                                "Der Speicheranbieter mit der ID %d konnte nicht gefunden werden.",
                                providerId
                        ));
    }

    private <T> StorageProviderDefinition<T> retrieveDefinition(@Nonnull StorageProviderEntity provider) throws ResponseException {
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

    private <T> T createConfig(@Nonnull StorageProviderEntity provider, @Nonnull StorageProviderDefinition<T> definition) throws ResponseException {
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
