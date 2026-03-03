package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.configs.DefaultStorageAssetsSystemConfigDefinition;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AssetService implements EntityService<AssetEntity, UUID> {
    private final GoverConfig goverConfig;
    private final StorageService storageService;
    private final SystemConfigRepository systemConfigRepository;
    private final AssetRepository repository;

    @Autowired
    public AssetService(GoverConfig goverConfig,
                        StorageService storageService,
                        SystemConfigRepository systemConfigRepository,
                        AssetRepository repository) {
        this.goverConfig = goverConfig;
        this.storageService = storageService;
        this.systemConfigRepository = systemConfigRepository;
        this.repository = repository;
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

        var extension = StringUtils
                .extractExtensionFromFileName(entity.getFilename())
                .orElse("dat");

        var folder = storageService.createFolder(defaultStorageProviderId, "/assets");
        var filePath = folder.resolvePath(String.format(
                "%s.%s",
                entity.getKey(),
                extension
        ));

        var doc = storageService
                .storeDocument(defaultStorageProviderId, filePath, entity.getFileBytes(), StorageItemMetadata.empty());

        entity
                .setStorageProviderId(defaultStorageProviderId)
                .setStoragePathFromRoot(doc.getPathFromRoot());

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

    public String createUrl(String assetKey) {
        return goverConfig.createUrl("/api/public/assets/" + assetKey);
    }

    public String createUrl(UUID assetKey) {
        return createUrl(assetKey.toString());
    }

    public String createUrl(AssetEntity asset) {
        return createUrl(asset.getKey());
    }
}
