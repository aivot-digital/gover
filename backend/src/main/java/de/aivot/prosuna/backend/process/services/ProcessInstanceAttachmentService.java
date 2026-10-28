package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.CreateEntityService;
import de.aivot.prosuna.backend.lib.services.DeleteEntityService;
import de.aivot.prosuna.backend.lib.services.ReadEntityService;
import de.aivot.prosuna.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceAttachmentSetRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ProcessInstanceAttachmentService implements ReadEntityService<ProcessInstanceAttachmentEntity, UUID>,
        CreateEntityService<ProcessInstanceAttachmentEntity>,
        DeleteEntityService<ProcessInstanceAttachmentEntity, UUID> {
    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceAttachmentService.class);

    private final ProcessInstanceAttachmentRepository processInstanceAttachmentRepository;
    @Nullable
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final StorageService storageService;
    private final SystemConfigRepository systemConfigRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    @Nullable
    private final ProcessInstanceAttachmentSetRepository processInstanceAttachmentSetRepository;
    @Nullable
    private final TransactionTemplate fileDeletionTransaction;

    @Autowired
    public ProcessInstanceAttachmentService(ProcessInstanceAttachmentRepository processInstanceAttachmentRepository,
                                            StorageService storageService,
                                            SystemConfigRepository systemConfigRepository,
                                            ProcessInstanceRepository processInstanceRepository,
                                            @Nullable ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                                            @Nullable ProcessInstanceAttachmentSetRepository processInstanceAttachmentSetRepository,
                                            @Nullable PlatformTransactionManager transactionManager) {
        this.processInstanceAttachmentRepository = processInstanceAttachmentRepository;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.storageService = storageService;
        this.systemConfigRepository = systemConfigRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceAttachmentSetRepository = processInstanceAttachmentSetRepository;
        if (transactionManager != null) {
            // Files are deleted after the commit, when data access would otherwise join the committed transaction.
            this.fileDeletionTransaction = new TransactionTemplate(transactionManager);
            this.fileDeletionTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        } else {
            this.fileDeletionTransaction = null;
        }
    }

    protected ProcessInstanceAttachmentService(ProcessInstanceAttachmentRepository processInstanceAttachmentRepository,
                                               StorageService storageService,
                                               SystemConfigRepository systemConfigRepository,
                                               ProcessInstanceRepository processInstanceRepository) {
        this(processInstanceAttachmentRepository, storageService, systemConfigRepository, processInstanceRepository, null, null, null);
    }

    @Nonnull
    @Override
    public ProcessInstanceAttachmentEntity create(@Nonnull ProcessInstanceAttachmentEntity entity) throws ResponseException {
        // Set the key to a new random UUID, to ensure that the client cannot specify the key and that it is always unique.
        entity.setKey(UUID.randomUUID());
        if (StringUtils.isNullOrEmpty(entity.getOriginalFileName())) {
            entity.setOriginalFileName(entity.getFileName());
        }

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
                                "Der Vorgang mit der ID %s existiert nicht.",
                                entity.getProcessInstanceId()
                        )
                );

        var extension = StringUtils
                .extractExtensionFromFileName(entity.getFileName())
                .orElse("dat");

        // Storage paths are visible to everyone who may browse the storage provider. They must therefore never contain
        // secrets such as the access key of the process instance, which grants customers access to the instance.
        var folderPath = String.format(
                "/proc-%d/%d/attachments",
                processInstance.getProcessId(),
                processInstance.getId()
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
        if (processNodeExecutionLoggerFactory == null) {
            return;
        }

        var details = new LinkedHashMap<String, Object>();
        details.put("attachmentKey", attachment.getKey());
        details.put("fileName", attachment.getFileName());
        details.put("originalFileName", attachment.getOriginalFileName());
        details.put("group", attachment.getGroup());
        details.put("position", attachment.getPosition());
        details.put("attachmentSetId", attachment.getAttachmentSetId());
        details.put("processInstanceId", attachment.getProcessInstanceId());
        details.put("processInstanceTaskId", attachment.getProcessInstanceTaskId());
        details.put("storageProviderId", attachment.getStorageProviderId());
        details.put("storagePathFromRoot", attachment.getStoragePathFromRoot());
        details.put("uploadedByUserId", attachment.getUploadedByUserId());

        processNodeExecutionLoggerFactory
                .create(attachment.getProcessInstanceId(), attachment.getProcessInstanceTaskId(), attachment.getUploadedByUserId(), null)
                .saveEvent(
                    ProcessNodeExecutionLogLevel.Info,
                    attachment.getUploadedByUserId() == null,
                    true,
                    false,
                    "Anhang erstellt",
                    String.format("Der Anhang %s wurde erstellt.", StringUtils.quote(attachment.getFileName())),
                    details,
                    Instant.now(),
                    null,
                    null,
                    null
                );
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
                .findAllByAttachmentSetIdAndRemovedAtIsNullOrderByPositionAscKeyAsc(attachmentSetId);
    }

    /**
     * Flags the attachments that a task uploaded into the given attachment sets once its upload fields no longer
     * reference them, and clears the flag when they reference them again. Referenced attachments take the position of
     * their item, so the order of a set follows the upload fields. The flagged attachments are deleted when the task
     * ends, see {@link #deleteRemovedUploadsOfTask}.
     *
     * @param attachmentPositions the positions of referenced attachments; attachments without one keep theirs
     */
    public void updateRemovedUploadsOfTask(@Nonnull Long processInstanceTaskId,
                                           @Nonnull Collection<Integer> attachmentSetIds,
                                           @Nonnull Set<UUID> referencedAttachmentKeys,
                                           @Nonnull Map<UUID, Integer> attachmentPositions) {
        if (attachmentSetIds.isEmpty()) {
            return;
        }

        var now = Instant.now();
        var attachments = processInstanceAttachmentRepository
                .findAllByProcessInstanceTaskIdAndAttachmentSetIdIn(processInstanceTaskId, attachmentSetIds);
        for (var attachment : attachments) {
            var removedAt = referencedAttachmentKeys.contains(attachment.getKey())
                    ? null
                    : Objects.requireNonNullElse(attachment.getRemovedAt(), now);
            var position = attachmentPositions.getOrDefault(attachment.getKey(), attachment.getPosition());

            if (!Objects.equals(removedAt, attachment.getRemovedAt()) || !position.equals(attachment.getPosition())) {
                processInstanceAttachmentRepository.save(attachment.setRemovedAt(removedAt).setPosition(position));
            }
        }
    }

    /**
     * Deletes the attachments that the upload fields of a task no longer referenced when it ended, and the attachment
     * sets that no attachment is left in. Attachments of other tasks are never flagged, so documents that were
     * submitted before stay in the instance.
     * <p>
     * The files are deleted from their storage provider only after the transaction commits, so a rollback keeps both
     * the attachments and their files.
     *
     * @param userId the user who ended the task, or {@code null} if the system ended it
     */
    public void deleteRemovedUploadsOfTask(@Nonnull ProcessInstanceTaskEntity task,
                                           @Nullable String userId) {
        var removedAttachments = processInstanceAttachmentRepository
                .findAllByProcessInstanceTaskIdAndRemovedAtIsNotNull(task.getId());
        if (removedAttachments.isEmpty()) {
            return;
        }

        // The storage index still has a foreign key from these rows; flush their deletion first.
        processInstanceAttachmentRepository.deleteAll(removedAttachments);
        processInstanceAttachmentRepository.flush();

        var attachmentSetIds = removedAttachments
                .stream()
                .map(ProcessInstanceAttachmentEntity::getAttachmentSetId)
                .distinct()
                .toList();
        for (var attachmentSetId : attachmentSetIds) {
            if (!processInstanceAttachmentRepository.existsByAttachmentSetId(attachmentSetId)) {
                Objects.requireNonNull(processInstanceAttachmentSetRepository).deleteById(attachmentSetId);
            }
        }

        for (var attachment : removedAttachments) {
            logAttachmentDeletionEvent(attachment, userId);
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteStoredDocuments(removedAttachments);
                }
            });
        } else {
            deleteStoredDocuments(removedAttachments);
        }
    }

    private void deleteStoredDocuments(@Nonnull List<ProcessInstanceAttachmentEntity> attachments) {
        for (var attachment : attachments) {
            try {
                Objects.requireNonNull(fileDeletionTransaction).executeWithoutResult(status -> {
                    try {
                        storageService.deleteDocument(attachment.getStorageProviderId(), attachment.getStoragePathFromRoot());
                    } catch (ResponseException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (RuntimeException e) {
                // The attachment is already deleted, so only its file remains in the storage provider.
                logger.warn(
                        "Failed to delete the file {} of the removed attachment {} from storage provider {}",
                        attachment.getStoragePathFromRoot(),
                        attachment.getKey(),
                        attachment.getStorageProviderId(),
                        e
                );
            }
        }
    }

    private void logAttachmentDeletionEvent(@Nonnull ProcessInstanceAttachmentEntity attachment,
                                            @Nullable String userId) {
        if (processNodeExecutionLoggerFactory == null) {
            return;
        }

        var details = new LinkedHashMap<String, Object>();
        details.put("attachmentKey", attachment.getKey());
        details.put("fileName", attachment.getFileName());
        details.put("originalFileName", attachment.getOriginalFileName());
        details.put("attachmentSetId", attachment.getAttachmentSetId());
        details.put("processInstanceId", attachment.getProcessInstanceId());
        details.put("processInstanceTaskId", attachment.getProcessInstanceTaskId());
        details.put("removedAt", String.valueOf(attachment.getRemovedAt()));

        processNodeExecutionLoggerFactory
                .create(attachment.getProcessInstanceId(), attachment.getProcessInstanceTaskId(), userId, null)
                .saveEvent(
                        ProcessNodeExecutionLogLevel.Info,
                        userId == null,
                        true,
                        false,
                        "Anhang gelöscht",
                        String.format(
                                "Der Anhang %s wurde gelöscht, weil er vor dem Ende der Aufgabe aus dem Anlagen-Feld entfernt wurde.",
                                StringUtils.quote(attachment.getFileName())
                        ),
                        details,
                        Instant.now(),
                        null,
                        null,
                        null
                );
    }

    @Override
    public boolean exists(@Nonnull UUID key) {
        return processInstanceAttachmentRepository.existsById(key);
    }

    @Override
    public boolean exists(@Nonnull Specification<ProcessInstanceAttachmentEntity> specification) {
        return processInstanceAttachmentRepository.exists(specification);
    }

    @Override
    public void performDelete(@Nonnull ProcessInstanceAttachmentEntity entity) throws ResponseException {
        // The storage index still has a foreign key from this row; flush its deletion first.
        processInstanceAttachmentRepository.delete(entity);
        processInstanceAttachmentRepository.flush();

        // Delete the attachment from the storage provider
        storageService
                .deleteDocument(entity.getStorageProviderId(), entity.getStoragePathFromRoot());
    }
}
