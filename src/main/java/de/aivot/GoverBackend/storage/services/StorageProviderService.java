package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderStatus;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StorageProviderService implements EntityService<StorageProviderEntity, Integer> {
    private final StorageProviderRepository storageProviderRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public StorageProviderService(StorageProviderRepository storageProviderRepository,
                                  StorageProviderDefinitionService storageProviderDefinitionService, RabbitTemplate rabbitTemplate) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Nonnull
    @Override
    public StorageProviderEntity create(@Nonnull StorageProviderEntity entity) throws ResponseException {
        // Retrieve the payment provider definition
        storageProviderDefinitionService
                .retrieveProviderDefinition(entity.getStorageProviderDefinitionKey(), entity.getStorageProviderDefinitionVersion())
                .orElseThrow(() -> new ResponseException(HttpStatus.BAD_REQUEST, "Der ausgewählte Speicheranbieter ist nicht vorhanden"));

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
        existingEntity.setConfiguration(entity.getConfiguration());

        var shouldResync = shouldResync(def,
                existingEntity.getConfiguration(),
                entity.getConfiguration());

        if (shouldResync) {
            existingEntity.setStatus(StorageProviderStatus.SyncPending);
        }

        var res = storageProviderRepository
                .save(existingEntity);

        if (shouldResync) {
            rabbitTemplate.convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, res.getId());
        }

        return res;
    }

    private static <T> boolean shouldResync(StorageProviderDefinition<T> def, ElementData existing, ElementData updated) throws ResponseException {
        T existingConfig;
        try {
            existingConfig = ElementPOJOMapper.mapToPOJO(existing, def.getConfigClass());
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Fehler beim Verarbeiten der Speicheranbieter-Konfiguration für die Speicheranbieter-Definition %s (%s v%d)",
                    StringUtils.quote(def.getName()),
                    StringUtils.quote(def.getKey()),
                    def.getVersion()
            );
        }

        T updatedConfig;
        try {
            updatedConfig = ElementPOJOMapper.mapToPOJO(updated, def.getConfigClass());
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Fehler beim Verarbeiten der Speicheranbieter-Konfiguration für die Speicheranbieter-Definition %s (%s v%d)",
                    StringUtils.quote(def.getName()),
                    StringUtils.quote(def.getKey()),
                    def.getVersion()
            );
        }

        return def.shouldResync(existingConfig, updatedConfig);

    }

    @Override
    public void performDelete(
            @Nonnull StorageProviderEntity entity
    ) throws ResponseException {
        storageProviderRepository.delete(entity);
    }
}
