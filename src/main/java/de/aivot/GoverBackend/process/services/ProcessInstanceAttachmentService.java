package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessInstanceAttachmentService implements EntityService<ProcessInstanceAttachmentEntity, UUID> {

    private final ProcessInstanceAttachmentRepository processInstanceAttachmentRepository;
    private final StorageService storageService;
    private final SystemConfigRepository systemConfigRepository;
    private final ProcessInstanceRepository processInstanceRepository;

    @Autowired
    public ProcessInstanceAttachmentService(ProcessInstanceAttachmentRepository processInstanceAttachmentRepository,
                                            StorageService storageService,
                                            SystemConfigRepository systemConfigRepository,
                                            ProcessInstanceRepository processInstanceRepository) {
        this.processInstanceAttachmentRepository = processInstanceAttachmentRepository;
        this.storageService = storageService;
        this.systemConfigRepository = systemConfigRepository;
        this.processInstanceRepository = processInstanceRepository;
    }

    @Nonnull
    @Override
    public ProcessInstanceAttachmentEntity create(@Nonnull ProcessInstanceAttachmentEntity entity) throws ResponseException {
        // Set the key to a new random UUID, to ensure that the client cannot specify the key and that it is always unique.
        entity.setKey(UUID.randomUUID());

        // region Store the attachment in the default storage provider

        var defaultStorageProviderId = systemConfigRepository
                .findById(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY)
                .map(SystemConfigEntity::getValue)
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .orElse(null);

        if (defaultStorageProviderId == null) {
            throw ResponseException.internalServerError("Es wurde kein Standard-Speicheranbieter für Prozess-Anhänge konfiguriert.");
        }

        var processInstance = processInstanceRepository
                .findById(entity.getProcessInstanceId())
                        .orElseThrow(() -> ResponseException
                                .badRequest(
                                        "Die Prozess-Instanz mit der ID %s existiert nicht.",
                                        entity.getProcessInstanceId()
                                )
                        );

        var extension = StringUtils
                .extractExtensionFromFileName(entity.getFileName())
                .orElse("dat");

        var folderPath = String.format(
                "/proc-%d/%s/attachments",
                processInstance.getProcessId(),
                processInstance.getAccessKey()
        );

        var folder = storageService
                .createFolder(defaultStorageProviderId, folderPath);

        var filePath = folder.resolvePath(String.format(
                "%s.%s",
                entity.getKey(),
                extension
        ));

        var doc = storageService
                .storeDocument(defaultStorageProviderId, filePath, entity.getFileBytes());

        entity.setStorageProviderId(defaultStorageProviderId);
        entity.setStoragePathFromRoot(doc.getPathFromRoot());

        // endregion

        return processInstanceAttachmentRepository.save(entity);
    }

    @Nullable
    @Override
    public Page<ProcessInstanceAttachmentEntity> performList(@Nonnull Pageable pageable,
                                                             @Nullable Specification<ProcessInstanceAttachmentEntity> specification,
                                                             @Nullable Filter<ProcessInstanceAttachmentEntity> filter) throws ResponseException {
        return processInstanceAttachmentRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAttachmentEntity> retrieve(@Nonnull UUID key) throws ResponseException {
        return processInstanceAttachmentRepository.findById(key);
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAttachmentEntity> retrieve(@Nonnull Specification<ProcessInstanceAttachmentEntity> specification) throws ResponseException {
        return processInstanceAttachmentRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull UUID key) {
        return processInstanceAttachmentRepository.existsById(key);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceAttachmentEntity> specification) {
        return processInstanceAttachmentRepository.exists(specification);
    }

    @Nonnull
    @Override
    public ProcessInstanceAttachmentEntity performUpdate(@Nonnull UUID key,
                                                         @Nonnull ProcessInstanceAttachmentEntity entity,
                                                         @Nonnull ProcessInstanceAttachmentEntity existingEntity) throws ResponseException {
        existingEntity.setProcessInstanceId(entity.getProcessInstanceId());
        existingEntity.setProcessInstanceTaskId(entity.getProcessInstanceTaskId());
        existingEntity.setUploadedByUserId(entity.getUploadedByUserId());
        return processInstanceAttachmentRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceAttachmentEntity entity) throws ResponseException {
        // Delete the attachment from the storage provider
        storageService.deleteDocument(entity.getStorageProviderId(), entity.getStoragePathFromRoot());

        // Delete the attachment from the database
        processInstanceAttachmentRepository.delete(entity);
    }
}

