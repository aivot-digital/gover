package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.configs.DefaultStorageAssetsSystemConfigDefinition;
import de.aivot.GoverBackend.asset.dtos.AssetFolderGroupResponseDTO;
import de.aivot.GoverBackend.asset.dtos.AssetResponseDTO;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.repositories.SystemConfigRepository;
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
import org.springframework.stereotype.Service;

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
    private final SystemConfigRepository systemConfigRepository;
    private final AssetRepository repository;
    private final StorageIndexItemRepository storageIndexItemRepository;
    private final StorageProviderRepository storageProviderRepository;
    private final StorageService storageService;

    @Autowired
    public AssetService(GoverConfig goverConfig,
                        SystemConfigRepository systemConfigRepository,
                        AssetRepository repository,
                        StorageIndexItemRepository storageIndexItemRepository,
                        StorageProviderRepository storageProviderRepository,
                        StorageService storageService) {
        this.goverConfig = goverConfig;
        this.repository = repository;
        this.storageIndexItemRepository = storageIndexItemRepository;
        this.storageProviderRepository = storageProviderRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.storageService = storageService;
    }

    @Nonnull
    @Override
    public AssetEntity create(@Nonnull AssetEntity entity) throws ResponseException {
        entity.setKey(UUID.randomUUID());

        var defaultStorageProviderId = systemConfigRepository
                .findById(DefaultStorageAssetsSystemConfigDefinition.DEFAULT_STORAGE_ASSETS_KEY)
                .flatMap(SystemConfigEntity::getValueAsInteger)
                .orElse(null);

        if (defaultStorageProviderId == null) {
            throw ResponseException.internalServerError("Es wurde kein Standard-Speicheranbieter für Assets konfiguriert.");
        }

        if (entity.getFileBytes() == null) {
            throw ResponseException.badRequest("Der Inhalt des Assets fehlt.");
        }

        var doc = storageService.storeDocument(defaultStorageProviderId,
                entity.getStoragePathFromRoot(),
                entity.getFileBytes(),
                StorageItemMetadata.empty());

        entity
                .setStorageProviderId(defaultStorageProviderId)
                .setStoragePathFromRoot(doc.getPathFromRoot());

        return repository.save(entity);
    }

    @Nonnull
    public AssetEntity create(@Nonnull AssetEntity entity, @Nonnull byte[] fileBytes) throws ResponseException {
        entity.setKey(UUID.randomUUID());
        var defaultStorageProviderId = resolveStorageProviderId(null);
        return create(entity, fileBytes, defaultStorageProviderId);
    }

    @Nonnull
    public AssetEntity create(@Nonnull AssetEntity entity,
                              @Nonnull byte[] fileBytes,
                              @Nullable Integer requestedStorageProviderId) throws ResponseException {
        entity.setKey(UUID.randomUUID());

        var storageProviderId = resolveStorageProviderId(requestedStorageProviderId);

        var pathFromRoot = "/" + entity.getKey();

        var document = storageService
                .storeDocument(storageProviderId, pathFromRoot, fileBytes, StorageItemMetadata.empty());

        entity.setStorageProviderId(storageProviderId);
        entity.setStoragePathFromRoot(document.getPathFromRoot());

        return repository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull AssetEntity asset) throws ResponseException {
        storageService.deleteDocument(asset.getStorageProviderId(), asset.getStoragePathFromRoot());
        repository.delete(asset);
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
        existingEntity.setFilename(entity.getFilename());
        existingEntity.setPrivate(entity.getPrivate());
        return repository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<AssetEntity> retrieve(@Nonnull UUID id) {
        return repository.findById(id);
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
                .findAllById(repository.findDistinctStorageProviderIds())
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

                    var files = repository
                            .listAllInFolder(providerId, folderPath)
                            .stream()
                            .map(AssetResponseDTO::fromEntity)
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
    private Integer resolveStorageProviderId(@Nullable Integer requestedStorageProviderId) throws ResponseException {
        var storageProviderId = requestedStorageProviderId != null
                ? requestedStorageProviderId
                : systemConfigRepository
                .findById(DefaultStorageAssetsSystemConfigDefinition.DEFAULT_STORAGE_ASSETS_KEY)
                .flatMap(config -> config.getValueAsInteger())
                .orElseThrow(() -> ResponseException.internalServerError("Es wurde kein Standard-Speicheranbieter für Assets konfiguriert."));

        var storageProvider = storageProviderRepository
                .findById(storageProviderId)
                .orElseThrow(() -> ResponseException.badRequest("Der angegebene Speicheranbieter mit der ID %d wurde nicht gefunden.", storageProviderId));

        if (storageProvider.getType() != StorageProviderType.Assets) {
            throw ResponseException.badRequest("Der Speicheranbieter mit der ID %d ist nicht für Assets konfiguriert.", storageProviderId);
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

}
