package de.aivot.gover.backend.storage.services;

import com.beust.jcommander.Strings;
import de.aivot.gover.backend.av.services.AVService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.storage.exceptions.StorageException;
import de.aivot.gover.backend.storage.entities.StorageIndexItemEntity;
import de.aivot.gover.backend.storage.entities.StorageIndexItemEntityId;
import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.models.StorageDocument;
import de.aivot.gover.backend.storage.models.StorageFolder;
import de.aivot.gover.backend.storage.models.StorageItemMetadata;
import de.aivot.gover.backend.storage.models.StorageProviderDefinition;
import de.aivot.gover.backend.storage.repositories.StorageIndexItemRepository;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import de.aivot.gover.backend.storage.utils.StoragePathUtils;
import de.aivot.gover.backend.utils.NumberUtils;
import de.aivot.gover.backend.utils.PaginationUtils;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

@Service
public class StorageService {
    public static final String FOLDER_MIME_TYPE = "inode/directory";
    public static final String UNKNOWN_MIME_TYPE = "application/octet-stream";

    private final StorageProviderRepository storageProviderRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;
    private final StorageProviderConfigurationService storageProviderConfigurationService;
    private final StorageIndexItemRepository storageIndexItemRepository;
    private final KnownExtensionsService knownExtensionsService;
    private final AVService avService;

    @Autowired
    public StorageService(StorageProviderRepository storageProviderRepository,
                          StorageProviderDefinitionService storageProviderDefinitionService,
                          StorageProviderConfigurationService storageProviderConfigurationService,
                          StorageIndexItemRepository storageIndexItemRepository,
                          KnownExtensionsService knownExtensionsService,
                          AVService avService) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
        this.storageProviderConfigurationService = storageProviderConfigurationService;
        this.storageIndexItemRepository = storageIndexItemRepository;
        this.knownExtensionsService = knownExtensionsService;
        this.avService = avService;
    }

    @Nonnull
    public StorageFolder createFolder(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var normalizedPath = normalizeInputFolderPath(path);
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

        StorageFolder createdFolder;
        try {
            createdFolder = definition
                    .createFolder(config, normalizedPath);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }

        var createdFolderPathFromRoot = normalizeInputFolderPath(createdFolder.getPathFromRoot());
        var createdFolderPathParts = StringUtils
                .getPathSegments(createdFolderPathFromRoot);

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
                        Instant.now(),
                        Instant.now()
                );
                storageIndexItemRepository
                        .save(indexItem);
            }
        }

        return createdFolder;
    }

    public Optional<StorageFolder> getFolder(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        return getFolder(providerId, path, false);
    }

    public Optional<StorageFolder> getFolder(@Nonnull Integer providerId,
                                             @Nonnull String path,
                                             boolean recursive) throws ResponseException {
        var normalizedPath = normalizeInputFolderPath(path);
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        try {
            return definition.retrieveFolder(config, normalizedPath, recursive);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }
    }

    @Nonnull
    public StorageFolder getFolderTreeFromIndex(@Nonnull Integer providerId) throws ResponseException {
        var provider = retrieveProvider(providerId);
        var indexedItems = storageIndexItemRepository.findAllByStorageProviderIdAndDirectoryIsTrue(provider.getId());

        Map<String, StorageFolder> foldersByPath = new HashMap<>();
        var rootFolder = new StorageFolder("/", "Root", new LinkedList<>(), new LinkedList<>(), true);
        foldersByPath.put("/", rootFolder);

        for (var item : indexedItems) {
            if (item.getMissing()) {
                continue;
            }

            var folderPath = normalizeIndexFolderPath(item.getPathFromRoot());
            foldersByPath.computeIfAbsent(folderPath, p -> new StorageFolder(
                    p,
                    "/".equals(p) ? "Root" : StringUtils.getLastPathSegment(p),
                    new LinkedList<>(),
                    new LinkedList<>(),
                    true
            ));
        }

        foldersByPath
                .keySet()
                .stream()
                .sorted(Comparator.comparingInt(String::length))
                .filter(path -> !"/".equals(path))
                .forEach(path -> {
                    var folder = foldersByPath.get(path);
                    var parentPath = getParentFolderPath(path);
                    var parentFolder = foldersByPath.computeIfAbsent(parentPath, p -> new StorageFolder(
                            p,
                            "/".equals(p) ? "Root" : StringUtils.getLastPathSegment(p),
                            new LinkedList<>(),
                            new LinkedList<>(),
                            true
                    ));

                    var alreadyAdded = parentFolder.getSubfolders()
                            .stream()
                            .anyMatch(subfolder -> subfolder.getPathFromRoot().equals(folder.getPathFromRoot()));
                    if (!alreadyAdded) {
                        parentFolder.addSubfolder(folder);
                    }
                });

        return rootFolder;
    }

    @Nonnull
    public Page<StorageIndexItemEntity> searchIndexItems(@Nonnull Integer providerId,
                                                         @Nullable String search,
                                                         boolean includeMissing,
                                                         @Nonnull Pageable pageable) {
        var effectivePageable = createSearchPageable(pageable);

        if (StringUtils.isNullOrEmpty(search)) {
            return Page.empty(effectivePageable);
        }

        var searchPattern = "%" + search.trim().toLowerCase() + "%";
        Specification<StorageIndexItemEntity> specification = (root, query, builder) -> builder.and(
                builder.equal(root.get("storageProviderId").as(Integer.class), providerId),
                includeMissing
                        ? builder.conjunction()
                        : builder.equal(root.get("missing").as(Boolean.class), false),
                builder.or(
                        builder.like(builder.lower(root.get("filename").as(String.class)), searchPattern),
                        builder.like(builder.lower(root.get("pathFromRoot").as(String.class)), searchPattern)
                )
        );

        return storageIndexItemRepository.findAll(specification, effectivePageable);
    }

    public void deleteFolder(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var normalizedPath = normalizeInputFolderPath(path);
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

        try {
            definition.deleteFolder(config, normalizedPath);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }

        storageIndexItemRepository.deleteFolderTree(provider.getId(), normalizedPath);
    }

    @Nonnull
    public StorageFolder moveFolder(@Nonnull Integer providerId,
                                    @Nonnull String sourcePath,
                                    @Nonnull String targetPath) throws ResponseException {
        var normalizedSourcePath = normalizeInputFolderPath(sourcePath);
        var normalizedTargetPath = normalizeInputFolderPath(targetPath);
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        if (provider.getReadOnlyStorage()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) ist schreibgeschützt. Es können keine Ordner verschoben werden.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

        StorageFolder movedFolder;
        try {
            movedFolder = definition.moveFolder(config, normalizedSourcePath, normalizedTargetPath);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }
        var targetFolderPath = normalizeInputFolderPath(movedFolder.getPathFromRoot());
        StorageFolder targetFolderTree;
        try {
            targetFolderTree = definition
                    .retrieveFolder(config, targetFolderPath, true)
                    .orElse(movedFolder);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }

        upsertFolderTreeIndex(provider, targetFolderTree);

        if (!normalizedSourcePath.equals(targetFolderPath)) {
            storageIndexItemRepository.deleteFolderTree(provider.getId(), normalizedSourcePath);
        }

        return movedFolder;
    }

    @Nonnull
    public StorageFolder copyFolder(@Nonnull Integer providerId,
                                    @Nonnull String sourcePath,
                                    @Nonnull String targetPath) throws ResponseException {
        var normalizedSourcePath = normalizeInputFolderPath(sourcePath);
        var normalizedTargetPath = normalizeInputFolderPath(targetPath);
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        if (provider.getReadOnlyStorage()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) ist schreibgeschützt. Es können keine Ordner kopiert werden.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

        StorageFolder copiedFolder;
        try {
            copiedFolder = definition.copyFolder(config, normalizedSourcePath, normalizedTargetPath);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }
        var targetFolderPath = normalizeInputFolderPath(copiedFolder.getPathFromRoot());
        StorageFolder targetFolderTree;
        try {
            targetFolderTree = definition
                    .retrieveFolder(config, targetFolderPath, true)
                    .orElse(copiedFolder);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }

        upsertFolderTreeIndex(provider, targetFolderTree);

        return copiedFolder;
    }

    public Optional<StorageDocument> getDocument(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var normalizedPath = normalizeInputDocumentPath(path);
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        Optional<StorageDocument> doc;
        try {
            doc = definition.retrieveDocument(config, normalizedPath);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }

        doc.ifPresent(d -> {
            d.setMetadata(filterMetadataByRegisteredAttributes(provider, d.getMetadata()));
        });

        return doc;
    }

    public InputStream getDocumentContent(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var normalizedPath = normalizeInputDocumentPath(path);
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        try {
            return definition.retrieveDocumentContent(config, normalizedPath);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }
    }

    public StorageDocument storeDocument(@Nonnull Integer providerId,
                                         @Nonnull String path,
                                         @Nonnull InputStream content,
                                         @Nonnull StorageItemMetadata metadata) throws ResponseException {
        var normalizedPath = normalizeInputDocumentPath(path);
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
                                "Der Speicheranbieter %s (ID %d) erlaubt Dateien mit einer maximalen Größe von %s MB. Die übermittelte Datei überschreitet dieses Limit.",
                                StringUtils.quote(provider.getName()),
                                provider.getId(),
                                NumberUtils.formatGermanNumber(provider.getMaxFileSizeInMegabytes(), 2)
                        );
            }
            throw ResponseException.internalServerError(e, "Der Inhalt des Dokuments %s konnte nicht gelesen werden.", StringUtils.quote(normalizedPath));
        }

        avService.testFile(new ByteArrayInputStream(contentBytes), normalizedPath);

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
                    .storeDocument(config, normalizedPath, new ByteArrayInputStream(contentBytes), filteredMetadata);
        } catch (StorageException e) {
            if (isCausedByMaxFileSizeExceeded(e)) {
                throw ResponseException
                        .badRequest(
                                "Der Speicheranbieter %s (ID %d) erlaubt Dateien mit einer maximalen Größe von %s MB. Die übermittelte Datei überschreitet dieses Limit.",
                                StringUtils.quote(provider.getName()),
                                provider.getId(),
                                NumberUtils.formatGermanNumber(provider.getMaxFileSizeInMegabytes(), 2)
                        );
            }
            throw wrapStorageException(e);
        }

        var createdDocumentFilteredMetadata = filterMetadataByRegisteredAttributes(provider, createdDocument.getMetadata());
        createdDocument.setMetadata(createdDocumentFilteredMetadata);

        // Index the effective persisted path returned by the provider.
        upsertDocumentIndexItem(provider, createdDocument);

        return createdDocument;
    }

    public StorageDocument storeDocument(@Nonnull Integer providerId,
                                         @Nonnull String path,
                                         @Nonnull byte[] content,
                                         @Nonnull StorageItemMetadata metadata) throws ResponseException {
        return storeDocument(providerId, path, new ByteArrayInputStream(content), metadata);
    }

    @Nonnull
    public StorageDocument updateDocumentMetadata(@Nonnull Integer providerId,
                                                  @Nonnull String path,
                                                  @Nonnull StorageItemMetadata metadata) throws ResponseException {
        var normalizedPath = normalizeInputDocumentPath(path);
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        if (provider.getReadOnlyStorage()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) ist schreibgeschützt. Die Metadaten von Dokumenten können nicht aktualisiert werden.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

        if (!definition.getSupportsMetadataAttributes()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) unterstützt keine Metadatenattribute.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

        var filteredMetadata = filterMetadataByRegisteredAttributes(provider, metadata);

        StorageDocument updatedDocument;
        try {
            updatedDocument = definition.updateDocumentMetadata(config, normalizedPath, filteredMetadata);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }

        var updatedDocumentFilteredMetadata = filterMetadataByRegisteredAttributes(provider, updatedDocument.getMetadata());
        updatedDocument.setMetadata(updatedDocumentFilteredMetadata);

        var normalizedUpdatedPath = normalizeInputDocumentPath(updatedDocument.getPathFromRoot());
        var normalizedSourcePath = normalizedPath;
        if (!normalizedSourcePath.equals(normalizedUpdatedPath)) {
            storageIndexItemRepository.moveDocumentPath(provider.getId(), normalizedSourcePath, normalizedUpdatedPath);
        }
        upsertDocumentIndexItem(provider, updatedDocument);

        return updatedDocument;
    }

    @Nonnull
    public StorageDocument moveDocument(@Nonnull Integer providerId,
                                        @Nonnull String sourcePath,
                                        @Nonnull String targetPath) throws ResponseException {
        var normalizedSourcePath = normalizeInputDocumentPath(sourcePath);
        var normalizedTargetPath = normalizeInputDocumentPath(targetPath);
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        if (provider.getReadOnlyStorage()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) ist schreibgeschützt. Es können keine Dokumente verschoben werden.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

        StorageDocument movedDocument;
        try {
            movedDocument = definition.moveDocument(config, normalizedSourcePath, normalizedTargetPath);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }
        var movedDocumentFilteredMetadata = filterMetadataByRegisteredAttributes(provider, movedDocument.getMetadata());
        movedDocument.setMetadata(movedDocumentFilteredMetadata);

        var movedDocumentPath = normalizeInputDocumentPath(movedDocument.getPathFromRoot());
        if (!normalizedSourcePath.equals(movedDocumentPath)) {
            storageIndexItemRepository.moveDocumentPath(provider.getId(), normalizedSourcePath, movedDocumentPath);
        }
        upsertDocumentIndexItem(provider, movedDocument);

        return movedDocument;
    }

    @Nonnull
    public StorageDocument copyDocument(@Nonnull Integer providerId,
                                        @Nonnull String sourcePath,
                                        @Nonnull String targetPath) throws ResponseException {
        var normalizedSourcePath = normalizeInputDocumentPath(sourcePath);
        var normalizedTargetPath = normalizeInputDocumentPath(targetPath);
        var provider = retrieveProvider(providerId);
        var definition = retrieveDefinition(provider);
        var config = createConfig(provider, definition);

        if (provider.getReadOnlyStorage()) {
            throw ResponseException
                    .badRequest(
                            "Der Speicheranbieter %s (ID %d) ist schreibgeschützt. Es können keine Dokumente kopiert werden.",
                            StringUtils.quote(provider.getName()),
                            provider.getId()
                    );
        }

        StorageDocument copiedDocument;
        try {
            copiedDocument = definition.copyDocument(config, normalizedSourcePath, normalizedTargetPath);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }
        var copiedDocumentFilteredMetadata = filterMetadataByRegisteredAttributes(provider, copiedDocument.getMetadata());
        copiedDocument.setMetadata(copiedDocumentFilteredMetadata);

        upsertDocumentIndexItem(provider, copiedDocument);

        return copiedDocument;
    }

    public void deleteDocument(@Nonnull Integer providerId, @Nonnull String path) throws ResponseException {
        var normalizedPath = normalizeInputDocumentPath(path);
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

        try {
            definition.deleteDocument(config, normalizedPath);
        } catch (StorageException e) {
            throw wrapStorageException(e);
        }

        storageIndexItemRepository
                .deleteById(StorageIndexItemEntityId.of(
                        provider.getId(),
                        normalizedPath
                ));
    }

    private void upsertDocumentIndexItem(@Nonnull StorageProviderEntity provider,
                                         @Nonnull StorageDocument document) throws ResponseException {
        var normalizedPath = normalizeInputDocumentPath(document.getPathFromRoot());

        var indexItem = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                normalizedPath,
                false,
                document.getName(),
                document.getSizeInBytes(),
                knownExtensionsService
                        .determineMimeType(document.getName())
                        .orElse(UNKNOWN_MIME_TYPE),
                false,
                document.getMetadata(),
                Instant.now(),
                Instant.now()
        );
        storageIndexItemRepository.save(indexItem);
    }

    private void upsertFolderTreeIndex(@Nonnull StorageProviderEntity provider,
                                       @Nonnull StorageFolder folderTree) throws ResponseException {
        upsertFolderIndexItem(provider, folderTree);
        for (var document : folderTree.getDocuments()) {
            var filteredMetadata = filterMetadataByRegisteredAttributes(provider, document.getMetadata());
            document.setMetadata(filteredMetadata);
            upsertDocumentIndexItem(provider, document);
        }
        for (var subfolder : folderTree.getSubfolders()) {
            upsertFolderTreeIndex(provider, subfolder);
        }
    }

    private void upsertFolderIndexItem(@Nonnull StorageProviderEntity provider,
                                       @Nonnull StorageFolder folder) throws ResponseException {
        var normalizedPath = normalizeInputFolderPath(folder.getPathFromRoot());
        var indexItem = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                normalizedPath,
                true,
                StringUtils.getLastPathSegment(normalizedPath),
                0L,
                FOLDER_MIME_TYPE,
                false,
                StorageItemMetadata.empty(),
                Instant.now(),
                Instant.now()
        );
        storageIndexItemRepository.save(indexItem);
    }

    @Nonnull
    private static String normalizeInputDocumentPath(@Nonnull String path) throws ResponseException {
        try {
            return StoragePathUtils.normalizeDocumentPath(path);
        } catch (StorageException e) {
            throw ResponseException.badRequest(e.getMessage());
        }
    }

    @Nonnull
    private static String normalizeInputFolderPath(@Nonnull String path) throws ResponseException {
        try {
            return StoragePathUtils.normalizeFolderPath(path);
        } catch (StorageException e) {
            throw ResponseException.badRequest(e.getMessage());
        }
    }

    @Nonnull
    private static String normalizeIndexFolderPath(@Nonnull String path) {
        var normalizedPath = path.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        if (!normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath + "/";
        }
        return normalizedPath;
    }

    @Nonnull
    private static String getParentFolderPath(@Nonnull String folderPath) {
        var normalizedPath = normalizeIndexFolderPath(folderPath);
        if ("/".equals(normalizedPath)) {
            return "/";
        }

        var withoutTrailingSlash = normalizedPath.substring(0, normalizedPath.length() - 1);
        var lastSlash = withoutTrailingSlash.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return withoutTrailingSlash.substring(0, lastSlash + 1);
    }

    @Nonnull
    private static Pageable createSearchPageable(@Nonnull Pageable pageable) {
        var filteredPageable = PaginationUtils.filterSorting(
                pageable,
                "directory",
                "filename",
                "pathFromRoot",
                "mimeType",
                "sizeInBytes",
                "missing",
                "created",
                "updated"
        );

        if (filteredPageable.getSort().isSorted()) {
            return filteredPageable;
        }

        return PageRequest.of(
                filteredPageable.getPageNumber(),
                filteredPageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("directory"),
                        Sort.Order.asc("filename"),
                        Sort.Order.asc("pathFromRoot")
                )
        );
    }

    @Nonnull
    private static ResponseException wrapStorageException(@Nonnull StorageException e) {
        return ResponseException.internalServerError(e, e.getMessage());
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
        return storageProviderConfigurationService
                .mapToConfig(provider, definition);
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
