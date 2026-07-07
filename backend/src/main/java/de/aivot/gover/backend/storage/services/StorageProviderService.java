package de.aivot.gover.backend.storage.services;

import de.aivot.gover.backend.asset.configs.DefaultStorageAssetsSystemConfigDefinition;
import de.aivot.gover.backend.config.services.SystemConfigService;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.lib.services.EntityService;
import de.aivot.gover.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.enums.StorageProviderStatus;
import de.aivot.gover.backend.storage.models.StorageProviderDefinition;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import java.util.Optional;

@Service
public class StorageProviderService implements EntityService<StorageProviderEntity, Integer> {
    private final StorageProviderRepository storageProviderRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;
    private final StorageProviderConfigurationService storageProviderConfigurationService;
    private final RabbitTemplate rabbitTemplate;
    private final DataSize maxFileSize;
    private final SystemConfigService systemConfigService;

    @Autowired
    public StorageProviderService(StorageProviderRepository storageProviderRepository,
                                  StorageProviderDefinitionService storageProviderDefinitionService,
                                  StorageProviderConfigurationService storageProviderConfigurationService,
                                  RabbitTemplate rabbitTemplate,
                                  @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize,
                                  SystemConfigService systemConfigService) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
        this.storageProviderConfigurationService = storageProviderConfigurationService;
        this.rabbitTemplate = rabbitTemplate;
        this.maxFileSize = maxFileSize;
        this.systemConfigService = systemConfigService;
    }

    @Nonnull
    @Override
    public StorageProviderEntity create(@Nonnull StorageProviderEntity entity) throws ResponseException {
        // Retrieve the payment provider definition
        var def = storageProviderDefinitionService
                .retrieveProviderDefinition(entity.getStorageProviderDefinitionKey(), entity.getStorageProviderDefinitionVersion())
                .orElseThrow(() -> new ResponseException(HttpStatus.BAD_REQUEST, "Der ausgewählte Speicheranbieter ist nicht vorhanden"));

        // Validate the configuration of the provider
        validateProvider(def, entity);

        // Validate of the systemwide max file size is exceeded
        validateMaxFileSize(entity);

        // Ensure the ID is null for creation
        entity.setId(null);

        // Set initial status
        entity.setStatus(StorageProviderStatus.SyncPending);

        // Save and return the storage provider entity
        var res = storageProviderRepository.save(entity);

        // Trigger synchronization
        rabbitTemplate.convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, res.getId());

        return res;
    }

    @NotNull
    @Override
    public Page<StorageProviderEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<StorageProviderEntity> specification,
            @Nullable Filter<StorageProviderEntity> filter) {
        return storageProviderRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<StorageProviderEntity> retrieve(
            @Nonnull Integer key
    ) {
        return storageProviderRepository
                .findById(key);
    }

    @Nonnull
    @Override
    public Optional<StorageProviderEntity> retrieve(
            @Nonnull Specification<StorageProviderEntity> specification
    ) {
        return storageProviderRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return storageProviderRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<StorageProviderEntity> specification) {
        return storageProviderRepository.exists(specification);
    }

    @Nonnull
    @Override
    public StorageProviderEntity performUpdate(
            @Nonnull Integer id,
            @Nonnull StorageProviderEntity entity,
            @Nonnull StorageProviderEntity existingEntity
    ) throws ResponseException {
        validateMaxFileSize(entity);

        if (existingEntity.getSystemProvider()) {
            if (entity.getStatus() == StorageProviderStatus.SyncPending) {
                var res = storageProviderRepository.save(
                        existingEntity
                                .setStatus(StorageProviderStatus.SyncPending)
                );
                rabbitTemplate.convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, res.getId());

                return res;
            } else {
                throw ResponseException.badRequest("Dieser Speicheranbieter kann nicht bearbeitet werden");
            }
        }

        var def = storageProviderDefinitionService
                .retrieveProviderDefinition(
                        existingEntity.getStorageProviderDefinitionKey(),
                        existingEntity.getStorageProviderDefinitionVersion()
                )
                .orElseThrow(() -> ResponseException.badRequest(
                        "Die Speicheranbieter-Definition mit dem Schlüssel " +
                                existingEntity.getStorageProviderDefinitionKey() +
                                " und der Version " +
                                existingEntity.getStorageProviderDefinitionVersion() +
                                " ist nicht vorhanden")
                );

        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());
        existingEntity.setMaxFileSizeInBytes(entity.getMaxFileSizeInBytes());
        existingEntity.setReadOnlyStorage(entity.getReadOnlyStorage());
        existingEntity.setTestProvider(entity.getTestProvider());
        existingEntity.setMetadataAttributes(entity.getMetadataAttributes());

        var shouldResync = shouldResync(def, existingEntity, entity);

        if (shouldResync) {
            existingEntity.setStatus(StorageProviderStatus.SyncPending);
        }

        // Assign this here to prevent resync check failing due to overwritten data
        existingEntity.setConfiguration(entity.getConfiguration());

        // Validate the metadata and configuration
        validateProvider(def, existingEntity);

        var res = storageProviderRepository
                .save(existingEntity);

        if (entity.getStatus() == StorageProviderStatus.SyncPending) {
            existingEntity.setStatusMessage(null); // Reset the status message
        }

        if (shouldResync) {
            rabbitTemplate.convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, res.getId());
        }

        return res;
    }

    private <T> boolean shouldResync(@Nonnull StorageProviderDefinition<T> def,
                                     @Nonnull StorageProviderEntity existing,
                                     @Nonnull StorageProviderEntity updated) throws ResponseException {
        var existingConfig = storageProviderConfigurationService
                .mapToConfig(existing, def);
        var updatedConfig = storageProviderConfigurationService
                .mapToConfig(updated, def);
        return def.shouldResync(existingConfig, updatedConfig);
    }

    @Override
    public void performDelete(
            @Nonnull StorageProviderEntity entity
    ) throws ResponseException {
        if (entity.getSystemProvider()) {
            throw ResponseException
                    .badRequest("Dieser Speicheranbieter kann nicht gelöscht werden, da es sich um einen Systemanbieter handelt");
        }

        testSystemConfigRelated(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY, entity, "Vorgangsdokumente");
        testSystemConfigRelated(DefaultStorageAssetsSystemConfigDefinition.DEFAULT_STORAGE_ASSETS_KEY, entity, "Dokument & Medien");

        // TODO: Check if any file of this storage provider is used in a process or process instance.

        storageProviderRepository.delete(entity);
    }

    private void testSystemConfigRelated(@Nonnull String key, @Nonnull StorageProviderEntity entity, @Nonnull String label) throws ResponseException {
        // Check if the storage provider is the default for process attachments and prevents its deletion
        var defaultAttachmentStorageIdRaw = systemConfigService
                .getValue(key);

        if (defaultAttachmentStorageIdRaw instanceof String defaultAttachmentStorageIdStr) {
            Integer defaultAttachmentStorageId;
            try {
                defaultAttachmentStorageId = Integer
                        .parseInt(defaultAttachmentStorageIdStr);
            } catch (NumberFormatException e) {
                throw ResponseException.internalServerError(
                        e,
                        "Die Systemkonfiguration %s enthält eine ungültige ID für den Standard-Speicheranbieter für %s: %s.",
                        StringUtils.quote(key),
                        StringUtils.quote(label),
                        StringUtils.quote(defaultAttachmentStorageIdStr)
                );
            }

            if (defaultAttachmentStorageId.equals(entity.getId())) {
                throw ResponseException
                        .badRequest(
                                "Dieser Speicheranbieter kann nicht gelöscht werden, da es sich um den Standardanbieter für %s handelt.",
                                StringUtils.quote(label)
                        );
            }
        }
    }

    private void validateMaxFileSize(@Nonnull StorageProviderEntity entity) throws ResponseException {
        if (entity.getMaxFileSizeInBytes() > maxFileSize.toBytes()) {
            var message = "Die maximale Dateigröße des Speicheranbieters darf die systemweite Grenze für Uploads nicht überschreiten. Systemweit gelten %d Megabyte."
                    .formatted(maxFileSize.toMegabytes());
            var derivedRuntimeData = new DerivedRuntimeElementData();
            derivedRuntimeData.putError("maxFileSizeInBytes", message);
            throw ResponseException.badRequest(message, derivedRuntimeData);
        }
    }

    private <T> void validateProvider(@Nonnull StorageProviderDefinition<T> def,
                                      @Nonnull StorageProviderEntity entity) throws ResponseException {
        // Check if the configuration is valid
        var config = storageProviderConfigurationService
                .mapToConfig(entity, def);
        def.validateConfiguration(entity, config);

        // Check if the metadata attributes are valid
        def.validateMetadataAttributes(entity.getMetadataAttributes());
    }
}
