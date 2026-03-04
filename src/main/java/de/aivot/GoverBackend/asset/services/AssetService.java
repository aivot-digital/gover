package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.dtos.AssetFolderGroupResponseDTO;
import de.aivot.GoverBackend.asset.dtos.AssetResponseDTO;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.filters.AssetFilter;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.repositories.StorageIndexItemRepository;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssetService implements EntityService<AssetEntity, UUID> {
    private final GoverConfig goverConfig;
    private final AssetRepository repository;
    private final AssetWithMetadataService assetWithMetadataService;
    private final StorageIndexItemRepository storageIndexItemRepository;
    private final StorageProviderRepository storageProviderRepository;
    private final StorageService storageService;

    @Autowired
    public AssetService(GoverConfig goverConfig,
                        AssetRepository repository,
                        AssetWithMetadataService assetWithMetadataService,
                        StorageIndexItemRepository storageIndexItemRepository,
                        StorageProviderRepository storageProviderRepository,
                        StorageService storageService) {
        this.goverConfig = goverConfig;
        this.repository = repository;
        this.assetWithMetadataService = assetWithMetadataService;
        this.storageIndexItemRepository = storageIndexItemRepository;
        this.storageProviderRepository = storageProviderRepository;
        this.storageService = storageService;
    }

    @Nonnull
    @Override
    public AssetEntity create(@Nonnull AssetEntity entity) throws ResponseException {
        if (entity.getFileBytes() == null) {
            throw ResponseException.badRequest("Der Inhalt des Assets fehlt.");
        }

        return create(entity, new ByteArrayInputStream(entity.getFileBytes()), entity.getStorageProviderId(), StorageItemMetadata.empty());
    }

    @Nonnull
    public AssetEntity create(@Nonnull AssetEntity entity, @Nonnull byte[] fileBytes) throws ResponseException {
        return create(entity, fileBytes, requireStorageProviderId(entity.getStorageProviderId()));
    }

    @Nonnull
    public AssetEntity create(@Nonnull AssetEntity entity,
                              @Nonnull byte[] fileBytes,
                              @Nonnull Integer requestedStorageProviderId) throws ResponseException {
        return create(entity, new ByteArrayInputStream(fileBytes), requestedStorageProviderId, StorageItemMetadata.empty());
    }

    @Nonnull
    public AssetEntity create(@Nonnull AssetEntity entity,
                              @Nonnull byte[] fileBytes,
                              @Nonnull Integer requestedStorageProviderId,
                              @Nullable StorageItemMetadata metadata) throws ResponseException {
        return create(entity, new ByteArrayInputStream(fileBytes), requestedStorageProviderId, metadata);
    }

    @Nonnull
    public AssetEntity create(@Nonnull AssetEntity entity,
                              @Nonnull InputStream fileContent,
                              @Nonnull Integer requestedStorageProviderId) throws ResponseException {
        return create(entity, fileContent, requestedStorageProviderId, StorageItemMetadata.empty());
    }

    @Nonnull
    public AssetEntity create(@Nonnull AssetEntity entity,
                              @Nonnull InputStream fileContent,
                              @Nonnull Integer requestedStorageProviderId,
                              @Nullable StorageItemMetadata metadata) throws ResponseException {
        var requestedFilename = entity.getFilename();
        var requestedContentType = entity.getContentType();

        var storageProviderId = resolveStorageProviderId(requestedStorageProviderId);
        var storagePathFromRoot = resolveAssetStoragePathForCreate(entity);

        if (repository.existsByStorageProviderIdAndStoragePathFromRoot(storageProviderId, storagePathFromRoot)) {
            throw ResponseException.conflict(
                    "Es existiert bereits ein Asset im Speicheranbieter %d mit dem Pfad %s.",
                    storageProviderId,
                    StringUtils.quote(storagePathFromRoot)
            );
        }

        var document = storageService
                .storeDocument(storageProviderId, storagePathFromRoot, fileContent, StorageItemMetadata.empty());

        entity.setKey(entity.getKey() != null ? entity.getKey() : UUID.randomUUID());
        entity.setStorageProviderId(storageProviderId);
        entity.setStoragePathFromRoot(document.getPathFromRoot());
        entity.setFilename(null);
        entity.setContentType(null);
        try {
            var createdAsset = repository.save(entity);
            syncStorageIndexItem(createdAsset, requestedFilename, requestedContentType, metadata);
            return createdAsset;
        } catch (DataIntegrityViolationException e) {
            throw ResponseException.conflict(
                    "Es existiert bereits ein Asset im Speicheranbieter %d mit dem Pfad %s.",
                    storageProviderId,
                    StringUtils.quote(storagePathFromRoot)
            );
        }
    }

    @Override
    public void performDelete(@Nonnull AssetEntity asset) throws ResponseException {
        storageService.deleteDocument(asset.getStorageProviderId(), asset.getStoragePathFromRoot());
        repository.delete(asset);
    }

    @Nonnull
    public Page<AssetResponseDTO> listWithMetadata(@Nonnull Pageable pageable,
                                                   @Nullable AssetFilter filter) {
        return assetWithMetadataService
                .list(pageable, filter)
                .map(AssetResponseDTO::fromViewEntity);
    }

    @Nonnull
    @Override
    public Page<AssetEntity> performList(@Nonnull Pageable pageable,
                                         @Nonnull Specification<AssetEntity> specification,
                                         Filter<AssetEntity> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public AssetEntity performUpdate(@Nonnull UUID id,
                                     @Nonnull AssetEntity entity,
                                     @Nonnull AssetEntity existingEntity
    ) throws ResponseException {
        existingEntity.setPrivate(entity.getPrivate());
        existingEntity.setFilename(null);
        existingEntity.setContentType(null);
        return repository.save(existingEntity);
    }

    @Nonnull
    public AssetEntity update(@Nonnull UUID id,
                              @Nonnull AssetEntity entity,
                              @Nullable StorageItemMetadata metadata) throws ResponseException {
        var existingAsset = retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (repository.existsByStorageProviderIdAndStoragePathFromRootAndKeyNot(
                existingAsset.getStorageProviderId(),
                existingAsset.getStoragePathFromRoot(),
                existingAsset.getKey()
        )) {
            throw ResponseException.conflict(
                    "Für den Speicheranbieter %d und den Pfad %s existiert bereits ein anderes Asset.",
                    existingAsset.getStorageProviderId(),
                    StringUtils.quote(existingAsset.getStoragePathFromRoot())
            );
        }

        var updatedAsset = update(id, entity);
        syncStorageIndexItem(updatedAsset, entity.getFilename(), entity.getContentType(), metadata);
        return updatedAsset;
    }

    @Nonnull
    @Override
    public Optional<AssetEntity> retrieve(@Nonnull UUID id) {
        return repository.findById(id);
    }

    @Nonnull
    public Optional<AssetResponseDTO> retrieveResponse(@Nonnull UUID id) {
        return assetWithMetadataService
                .retrieve(id)
                .map(AssetResponseDTO::fromViewEntity);
    }

    @Nonnull
    public Optional<AssetEntity> retrieveByPath(@Nonnull Integer storageProviderId,
                                                @Nonnull String storagePathFromRoot) throws ResponseException {
        return repository.findByStorageProviderIdAndStoragePathFromRoot(
                storageProviderId,
                normalizeAssetStoragePath(storagePathFromRoot)
        );
    }

    @Nonnull
    public Optional<AssetResponseDTO> retrieveResponseByPath(@Nonnull Integer storageProviderId,
                                                             @Nonnull String storagePathFromRoot) throws ResponseException {
        return retrieveByPath(storageProviderId, storagePathFromRoot)
                .flatMap(asset -> retrieveResponse(asset.getKey()));
    }

    @Nonnull
    public AssetResponseDTO createWithResponse(@Nonnull AssetEntity entity,
                                               @Nonnull InputStream fileContent,
                                               @Nonnull Integer requestedStorageProviderId,
                                               @Nullable StorageItemMetadata metadata) throws ResponseException {
        var createdAsset = create(entity, fileContent, requestedStorageProviderId, metadata);
        return retrieveResponse(createdAsset.getKey())
                .orElseThrow(() -> ResponseException.internalServerError("Das neu erstellte Asset konnte nicht gelesen werden."));
    }

    @Nonnull
    public AssetResponseDTO updateByPath(@Nonnull Integer storageProviderId,
                                         @Nonnull String storagePathFromRoot,
                                         @Nonnull AssetEntity entity,
                                         @Nullable StorageItemMetadata metadata) throws ResponseException {
        var existingAsset = retrieveByPath(storageProviderId, storagePathFromRoot)
                .orElseThrow(ResponseException::notFound);

        var updatedAsset = update(existingAsset.getKey(), entity, metadata);
        return retrieveResponse(updatedAsset.getKey())
                .orElseThrow(() -> ResponseException.internalServerError("Das aktualisierte Asset konnte nicht gelesen werden."));
    }

    @Nonnull
    public AssetResponseDTO deleteByPath(@Nonnull Integer storageProviderId,
                                         @Nonnull String storagePathFromRoot) throws ResponseException {
        var existingAsset = retrieveByPath(storageProviderId, storagePathFromRoot)
                .orElseThrow(ResponseException::notFound);

        var existingAssetResponse = retrieveResponse(existingAsset.getKey())
                .orElseThrow(ResponseException::notFound);

        delete(existingAsset.getKey());
        return existingAssetResponse;
    }

    @Nonnull
    @Override
    public Optional<AssetEntity> retrieve(@Nonnull Specification<AssetEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<AssetEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    public List<AssetFolderGroupResponseDTO> listGroupedByStorageProvider(@Nonnull String rawFolderPath) {
        var folderPath = normalizeFolderPath(rawFolderPath);

        var storageProviderNamesById = storageProviderRepository
                .findAllById(assetWithMetadataService.findDistinctStorageProviderIds())
                .stream()
                .collect(Collectors.toMap(
                        provider -> provider.getId(),
                        provider -> provider.getName()
                ));

        return storageProviderNamesById
                .keySet()
                .stream()
                .map(providerId -> {
                    var folders = storageIndexItemRepository
                            .listAllInFolder(providerId, "^" + escapeRegex(folderPath) + "([^/]+/$)", false)
                            .stream()
                            .filter(folder -> Boolean.TRUE.equals(folder.getDirectory()))
                            .toList();

                    var files = assetWithMetadataService
                            .listAllInFolder(providerId, folderPath)
                            .stream()
                            .map(AssetResponseDTO::fromViewEntity)
                            .toList();

                    if (folders.isEmpty() && files.isEmpty()) {
                        return null;
                    }

                    return new AssetFolderGroupResponseDTO(
                            providerId,
                            storageProviderNamesById.getOrDefault(providerId, String.valueOf(providerId)),
                            folders,
                            files
                    );
                })
                .filter(group -> group != null)
                .sorted(Comparator.comparing(AssetFolderGroupResponseDTO::storageProviderName))
                .toList();
    }

    public String createUrl(String assetKey) {
        return goverConfig.createUrl("/api/public/assets/" + assetKey);
    }

    public String createUrl(UUID assetKey) {
        return createUrl(assetKey.toString());
    }

    public String createUrl(AssetEntity asset) {
        return createUrl(asset.getKey());
    }

    @Nonnull
    private static String normalizeFolderPath(@Nonnull String rawPath) {
        var normalized = rawPath.trim();
        if (normalized.isEmpty()) {
            return "/";
        }

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }

        return normalized;
    }

    @Nonnull
    private static String escapeRegex(@Nonnull String input) {
        return input
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("|", "\\|")
                .replace("?", "\\?")
                .replace("*", "\\*")
                .replace("+", "\\+")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}");
    }

    @Nonnull
    private Integer resolveStorageProviderId(@Nonnull Integer requestedStorageProviderId) throws ResponseException {
        var storageProviderId = requireStorageProviderId(requestedStorageProviderId);

        var storageProvider = storageProviderRepository
                .findById(storageProviderId)
                .orElseThrow(() -> ResponseException.badRequest("Der angegebene Speicheranbieter mit der ID %d wurde nicht gefunden.", storageProviderId));

        if (storageProvider.getType() != StorageProviderType.Assets) {
            throw ResponseException.badRequest("Der Speicheranbieter mit der ID %d ist nicht für Assets konfiguriert.", storageProviderId);
        }

        return storageProviderId;
    }

    @Nonnull
    private static Integer requireStorageProviderId(@Nullable Integer storageProviderId) throws ResponseException {
        if (storageProviderId == null) {
            throw ResponseException.badRequest("Die ID des Speicheranbieters ist erforderlich.");
        }
        return storageProviderId;
    }

    public InputStream getAssetContent(AssetEntity asset) throws ResponseException {
        if (asset.getStorageProviderId() == null || StringUtils.isNullOrEmpty(asset.getStoragePathFromRoot())) {
            throw ResponseException.internalServerError("Das Asset %s hat keinen gültigen Speicherpfad.", asset.getKey());
        }

        return storageService.getDocumentContent(
                asset.getStorageProviderId(),
                asset.getStoragePathFromRoot()
        );
    }

    public byte[] getAssetContentBytes(AssetEntity asset) throws ResponseException {
        try (var assetContent = getAssetContent(asset)) {
            return assetContent.readAllBytes();
        } catch (IOException e) {
            throw ResponseException.internalServerError(e, "Der Inhalt des Assets %s konnte nicht gelesen werden.", asset.getKey());
        }
    }

    private void syncStorageIndexItem(@Nonnull AssetEntity asset,
                                      @Nullable String filename,
                                      @Nullable String contentType,
                                      @Nullable StorageItemMetadata metadata) throws ResponseException {
        var indexItem = storageIndexItemRepository
                .findByStorageProviderIdAndPathFromRootAndDirectoryIsFalse(asset.getStorageProviderId(), asset.getStoragePathFromRoot())
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Der Speicherindex-Eintrag für das Asset %s wurde nicht gefunden.",
                        asset.getKey()
                ));

        if (!StringUtils.isNullOrEmpty(filename)) {
            indexItem.setFilename(filename);
        }
        if (!StringUtils.isNullOrEmpty(contentType)) {
            indexItem.setMimeType(contentType);
        }
        if (metadata != null) {
            indexItem.setMetadata(metadata);
        }

        storageIndexItemRepository.save(indexItem);
    }

    @Nonnull
    private static String resolveAssetStoragePathForCreate(@Nonnull AssetEntity entity) throws ResponseException {
        var providedPath = entity.getStoragePathFromRoot();
        if (StringUtils.isNullOrEmpty(providedPath)) {
            throw ResponseException.badRequest("Der Speicherpfad des Assets ist erforderlich.");
        }

        return normalizeAssetStoragePath(providedPath);
    }

    @Nonnull
    private static String normalizeAssetStoragePath(@Nonnull String rawPath) throws ResponseException {
        var normalized = rawPath.trim();

        if (normalized.isEmpty()) {
            throw ResponseException.badRequest("Der Speicherpfad des Assets darf nicht leer sein.");
        }

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        if (normalized.endsWith("/")) {
            throw ResponseException.badRequest("Der Speicherpfad des Assets muss auf eine Datei verweisen und darf nicht auf / enden.");
        }

        if ("/".equals(normalized)) {
            throw ResponseException.badRequest("Der Speicherpfad des Assets darf nicht auf das Wurzelverzeichnis zeigen.");
        }

        return normalized;
    }

}
