package de.aivot.GoverBackend.storage.services;

import com.beust.jcommander.Strings;
import de.aivot.GoverBackend.av.services.AVService;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
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
    private final AVService avService;

    @Autowired
    public StorageService(StorageProviderRepository storageProviderRepository,
                          StorageProviderDefinitionService storageProviderDefinitionService,
                          StorageIndexItemRepository storageIndexItemRepository,
                          KnownExtensionsService knownExtensionsService,
                          AVService avService) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
        this.storageIndexItemRepository = storageIndexItemRepository;
        this.knownExtensionsService = knownExtensionsService;
        this.avService = avService;
    }

    @Nonnull
    public StorageFolder createFolder(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        if (provider.getReadOnlyStorage()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) ist schreibgeschützt. Es können keine Ordner erstellt werden.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

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
                        0L,
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

        if (provider.getReadOnlyStorage()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) ist schreibgeschützt. Es können keine Ordner gelöscht werden.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

        definition.deleteFolder(config, path);

        var normalizedFolderPath = path.trim();
        if (!normalizedFolderPath.startsWith("/")) {
            normalizedFolderPath = "/" + normalizedFolderPath;
        }
        if (!normalizedFolderPath.endsWith("/")) {
            normalizedFolderPath = normalizedFolderPath + "/";
        }

        storageIndexItemRepository.deleteFolderTree(provider.getId(), normalizedFolderPath);
    }

    public Optional<StorageDocument> getDocument(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        var doc = definition.retrieveDocument(config, path);

        doc.ifPresent(d -> {
            d.setMetadata(filterMetadataByRegisteredAttributes(provider, d.getMetadata()));
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
                                         @Nonnull InputStream content,
                                         @Nonnull StorageItemMetadata metadata) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        // Check if the provider is read-only before doing any other checks, to avoid unnecessary processing.
        if (provider.getReadOnlyStorage()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) ist schreibgeschützt. Es können keine Dokumente gespeichert werden.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

        byte[] contentBytes;
        try (var limitedContent = withProviderFileSizeLimit(provider, content);
             var contentBuffer = new ByteArrayOutputStream()) {
            limitedContent.transferTo(contentBuffer);
            contentBytes = contentBuffer.toByteArray();
        } catch (IOException e) {
            if (isCausedByMaxFileSizeExceeded(e)) {
                throw ResponseException
                        .badRequest(
                                "Der Speicheranbieter %s (ID %d) erlaubt Dateien mit einer maximalen Größe von %d Bytes. Die übermittelte Datei überschreitet dieses Limit.",
                                StringUtils.quote(provider.getName()),
                                provider.getId(),
                                provider.getMaxFileSizeInBytes()
                        );
            }
            throw ResponseException.internalServerError(e, "Der Inhalt des Dokuments %s konnte nicht gelesen werden.", StringUtils.quote(path));
        }

        avService.testFile(new ByteArrayInputStream(contentBytes), path);

        // Only respect metadata attributes if the provider definition supports them.
        // Additionally, filter out any metadata attributes that are not supported by the provider definition.
        var filteredMetadata = filterMetadataByRegisteredAttributes(provider, metadata);
        if (!definition.getSupportsMetadataAttributes()) {
            filteredMetadata = StorageItemMetadata.empty();
        }

        // Store the document in the storage provider.
        StorageDocument createdDocument;
        try {
            createdDocument = definition
                    .storeDocument(config, path, new ByteArrayInputStream(contentBytes), filteredMetadata);
        } catch (StorageException e) {
            if (isCausedByMaxFileSizeExceeded(e)) {
                throw ResponseException
                        .badRequest(
                                "Der Speicheranbieter %s (ID %d) erlaubt Dateien mit einer maximalen Größe von %d Bytes. Die übermittelte Datei überschreitet dieses Limit.",
                                StringUtils.quote(provider.getName()),
                                provider.getId(),
                                provider.getMaxFileSizeInBytes()
                        );
            }
            throw e;
        }

        var createdDocumentFilteredMetadata = filterMetadataByRegisteredAttributes(provider, createdDocument.getMetadata());
        createdDocument.setMetadata(createdDocumentFilteredMetadata);

        // Create or update the index item for the stored document.
        var indexItem = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                path,
                false,
                createdDocument.getName(),
                createdDocument.getSizeInBytes(),
                knownExtensionsService
                        .determineMimeType(createdDocument.getName())
                        .orElse(UNKNOWN_MIME_TYPE),
                false,
                createdDocumentFilteredMetadata,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        storageIndexItemRepository
                .save(indexItem);

        return createdDocument;
    }

    public StorageDocument storeDocument(@Nonnull Integer providerId,
                                         @Nonnull String path,
                                         @Nonnull byte[] content,
                                         @Nonnull StorageItemMetadata metadata) throws ResponseException {
        return storeDocument(providerId, path, new ByteArrayInputStream(content), metadata);
    }

    public void deleteDocument(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        if (provider.getReadOnlyStorage()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) ist schreibgeschützt. Es können keine Dokumente gelöscht werden.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

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

    private static StorageItemMetadata filterMetadataByRegisteredAttributes(@Nonnull StorageProviderEntity provider,
                                                                            @Nonnull StorageItemMetadata metadata) {
        var filteredMetadata = new StorageItemMetadata();

        for (var metadataAttribute : provider.getMetadataAttributes()) {
            var key = metadataAttribute.getKey();
            if (metadata.containsKey(key)) {
                filteredMetadata.put(key, metadata.get(key));
            }
        }

        return filteredMetadata;
    }

    private static InputStream withProviderFileSizeLimit(@Nonnull StorageProviderEntity provider,
                                                         @Nonnull InputStream content) {
        var maxFileSize = provider.getMaxFileSizeInBytes() != null ? provider.getMaxFileSizeInBytes() : 0L;
        if (maxFileSize <= 0) {
            return content;
        }

        return new MaxFileSizeLimitedInputStream(content, maxFileSize);
    }

    private static boolean isCausedByMaxFileSizeExceeded(@Nonnull Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof MaxFileSizeExceededIOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class MaxFileSizeExceededIOException extends IOException {
        private MaxFileSizeExceededIOException() {
            super("InputStream exceeds configured max file size.");
        }
    }

    private static final class MaxFileSizeLimitedInputStream extends FilterInputStream {
        private final long maxFileSizeInBytes;
        private long bytesRead;

        private MaxFileSizeLimitedInputStream(@Nonnull InputStream in, long maxFileSizeInBytes) {
            super(in);
            this.maxFileSizeInBytes = maxFileSizeInBytes;
            this.bytesRead = 0;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                incrementAndValidate(1);
            }
            return value;
        }

        @Override
        public int read(@Nonnull byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) {
                incrementAndValidate(read);
            }
            return read;
        }

        private void incrementAndValidate(int delta) throws IOException {
            bytesRead += delta;
            if (bytesRead > maxFileSizeInBytes) {
                throw new MaxFileSizeExceededIOException();
            }
        }
    }
}
