package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.config.entities.SystemConfigEntity;
import de.aivot.gover.backend.config.repositories.SystemConfigRepository;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.lib.services.EntityService;
import de.aivot.gover.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.gover.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.gover.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.gover.backend.storage.models.StorageItemMetadata;
import de.aivot.gover.backend.storage.services.StorageService;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessInstanceAttachmentService implements EntityService<ProcessInstanceAttachmentEntity, UUID> {
    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceAttachmentService.class);

    private final ProcessInstanceAttachmentRepository processInstanceAttachmentRepository;
    @Nullable
    private final ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository;
    private final StorageService storageService;
    private final SystemConfigRepository systemConfigRepository;
    private final ProcessInstanceRepository processInstanceRepository;

    @Autowired
    public ProcessInstanceAttachmentService(ProcessInstanceAttachmentRepository processInstanceAttachmentRepository,
                                            StorageService storageService,
                                            SystemConfigRepository systemConfigRepository,
                                            ProcessInstanceRepository processInstanceRepository,
                                            ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository) {
        this.processInstanceAttachmentRepository = processInstanceAttachmentRepository;
        this.processInstanceHistoryEventRepository = processInstanceHistoryEventRepository;
        this.storageService = storageService;
        this.systemConfigRepository = systemConfigRepository;
        this.processInstanceRepository = processInstanceRepository;
    }

    protected ProcessInstanceAttachmentService(ProcessInstanceAttachmentRepository processInstanceAttachmentRepository,
                                               StorageService storageService,
                                               SystemConfigRepository systemConfigRepository,
                                               ProcessInstanceRepository processInstanceRepository) {
        this(processInstanceAttachmentRepository, storageService, systemConfigRepository, processInstanceRepository, null);
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
                .storeDocument(defaultStorageProviderId,
                        filePath,
                        entity.getFileBytes(),
                        StorageItemMetadata.empty()); // TODO: Think of and specify metadata

        entity.setStorageProviderId(defaultStorageProviderId);
        entity.setStoragePathFromRoot(doc.getPathFromRoot());

        // endregion

        var savedEntity = processInstanceAttachmentRepository.save(entity);
        logAttachmentCreationEvent(savedEntity);
        return savedEntity;
    }

    private void logAttachmentCreationEvent(@Nonnull ProcessInstanceAttachmentEntity attachment) {
        if (processInstanceHistoryEventRepository == null) {
            return;
        }

        var details = new LinkedHashMap<String, Object>();
        details.put("attachmentKey", attachment.getKey());
        details.put("fileName", attachment.getFileName());
        details.put("attachmentSetId", attachment.getAttachmentSetId());
        details.put("processInstanceId", attachment.getProcessInstanceId());
        details.put("processInstanceTaskId", attachment.getProcessInstanceTaskId());
        details.put("storageProviderId", attachment.getStorageProviderId());
        details.put("storagePathFromRoot", attachment.getStoragePathFromRoot());
        details.put("uploadedByUserId", attachment.getUploadedByUserId());

        try {
            processInstanceHistoryEventRepository.save(new ProcessInstanceEventEntity(
                    null,
                    attachment.getProcessInstanceId(),
                    attachment.getProcessInstanceTaskId(),
                    ProcessNodeExecutionLogLevel.Info,
                    attachment.getUploadedByUserId() == null,
                    true,
                    "Anhang erstellt",
                    String.format("Der Anhang %s wurde erstellt.", StringUtils.quote(attachment.getFileName())),
                    details,
                    Instant.now(),
                    attachment.getUploadedByUserId()
            ));
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("Failed to persist process attachment creation event")
                    .setCause(e)
                    .addKeyValue("processInstanceId", attachment.getProcessInstanceId())
                    .addKeyValue("processInstanceTaskId", attachment.getProcessInstanceTaskId())
                    .addKeyValue("attachmentKey", attachment.getKey())
                    .log();
        }
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

    @Nonnull
    public List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceIdAndFileName(@Nonnull Long processInstanceId,
                                                                                         @Nonnull String fileName) {
        return processInstanceAttachmentRepository
                .findAllByProcessInstanceIdAndFileName(processInstanceId, fileName);
    }

    @Nonnull
    public List<ProcessInstanceAttachmentEntity> findAllByAttachmentSetId(@Nonnull Integer attachmentSetId) {
        return processInstanceAttachmentRepository
                .findAllByAttachmentSetId(attachmentSetId);
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
        existingEntity.setAttachmentSetId(entity.getAttachmentSetId());
        existingEntity.setUploadedByUserId(entity.getUploadedByUserId());
        return processInstanceAttachmentRepository.save(existingEntity);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceAttachmentEntity entity) throws ResponseException {
        // Delete the attachment from the database
        processInstanceAttachmentRepository.delete(entity);

        // Delete the attachment from the storage provider
        storageService
                .deleteDocument(entity.getStorageProviderId(), entity.getStoragePathFromRoot());
    }
}
