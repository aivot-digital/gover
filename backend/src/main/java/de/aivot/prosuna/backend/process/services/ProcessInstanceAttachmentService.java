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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ProcessInstanceAttachmentService implements ReadEntityService<ProcessInstanceAttachmentEntity, UUID>,
        CreateEntityService<ProcessInstanceAttachmentEntity>,
        DeleteEntityService<ProcessInstanceAttachmentEntity, UUID> {
    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceAttachmentService.class);
    private static final Duration DELETION_RETRY_DELAY = Duration.ofMinutes(5);

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
            throw ResponseException.internalServerError("Es wurde kein Standard-Speicheranbieter für Vorgangsanlagen konfiguriert.");
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
        // JPA merge may return a different instance and does not copy transient metadata.
        savedEntity.setSizeInBytes(doc.getSizeInBytes());
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
                    "Anlage erstellt",
                    String.format("Die Anlage %s wurde erstellt.", StringUtils.quote(attachment.getFileName())),
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
        var page = processInstanceAttachmentRepository.findAll(specification, pageable);
        populateFileSizes(page.getContent());
        return page;
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAttachmentEntity> retrieve(@Nonnull UUID key) throws ResponseException {
        var attachment = processInstanceAttachmentRepository.findById(key);
        attachment.ifPresent(entity -> populateFileSizes(List.of(entity)));
        return attachment;
    }

    @Nonnull
    @Override
    public Optional<ProcessInstanceAttachmentEntity> retrieve(@Nonnull Specification<ProcessInstanceAttachmentEntity> specification) throws ResponseException {
        var attachment = processInstanceAttachmentRepository.findOne(specification);
        attachment.ifPresent(entity -> populateFileSizes(List.of(entity)));
        return attachment;
    }

    @Nonnull
    public List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceIdAndFileName(@Nonnull Long processInstanceId,
                                                                                         @Nonnull String fileName) {
        var attachments = processInstanceAttachmentRepository
                .findAllByProcessInstanceIdAndFileName(processInstanceId, fileName);
        populateFileSizes(attachments);
        return attachments;
    }

    @Nonnull
    public List<ProcessInstanceAttachmentEntity> findAllByAttachmentSetId(@Nonnull Integer attachmentSetId) {
        var attachments = processInstanceAttachmentRepository
                .findAllByAttachmentSetIdAndRemovedAtIsNullOrderByPositionAscKeyAsc(attachmentSetId);
        populateFileSizes(attachments);
        return attachments;
    }

    private void populateFileSizes(@Nonnull List<ProcessInstanceAttachmentEntity> attachments) {
        if (attachments.isEmpty()) {
            return;
        }

        // Resolve metadata in one database query instead of contacting the storage provider for every file.
        var sizes = new HashMap<UUID, Long>();
        processInstanceAttachmentRepository.findSizesByKeys(attachments.stream()
                        .map(ProcessInstanceAttachmentEntity::getKey)
                        .toList())
                .forEach(item -> sizes.put(item.getKey(), item.getSizeInBytes()));
        attachments.forEach(attachment -> attachment.setSizeInBytes(sizes.get(attachment.getKey())));
    }

    /**
     * Returns the attachments that a task uploaded into the given attachment sets and that are not flagged as removed.
     */
    @Nonnull
    public List<ProcessInstanceAttachmentEntity> findAllUploadsOfTask(@Nonnull Long processInstanceTaskId,
                                                                      @Nonnull Collection<Integer> attachmentSetIds) {
        return findAllUploadsOfTaskIncludingRemoved(processInstanceTaskId, attachmentSetIds)
                .stream()
                .filter(attachment -> attachment.getRemovedAt() == null)
                .toList();
    }

    /**
     * Returns the attachments that a task uploaded into the given attachment sets, including those flagged as removed.
     */
    @Nonnull
    public List<ProcessInstanceAttachmentEntity> findAllUploadsOfTaskIncludingRemoved(@Nonnull Long processInstanceTaskId,
                                                                                      @Nonnull Collection<Integer> attachmentSetIds) {
        if (attachmentSetIds.isEmpty()) {
            return List.of();
        }

        return processInstanceAttachmentRepository
                .findAllByProcessInstanceTaskIdAndAttachmentSetIdIn(processInstanceTaskId, attachmentSetIds);
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
            // A restarted task may run again, but the attachments due for deletion stay deleted.
            if (attachment.getDeletionDueAt() != null) {
                continue;
            }

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
     * The attachments are only marked as due for deletion in the current transaction, so a rollback keeps them. Their
     * rows and files are deleted after the commit, and {@link #retryOverdueAttachmentDeletions} retries deletions that
     * failed or never ran, e.g. because the storage provider was unavailable or the application stopped.
     *
     * @param userId the user who ended the task, or {@code null} if the system ended it
     */
    public void deleteRemovedUploadsOfTask(@Nonnull ProcessInstanceTaskEntity task,
                                           @Nullable String userId) {
        var removedAttachments = processInstanceAttachmentRepository
                .findAllByProcessInstanceTaskIdAndRemovedAtIsNotNullAndDeletionDueAtIsNull(task.getId());
        if (removedAttachments.isEmpty()) {
            return;
        }

        var now = Instant.now();
        for (var attachment : removedAttachments) {
            processInstanceAttachmentRepository.save(attachment.setDeletionDueAt(now));
            logAttachmentDeletionEvent(attachment, userId);
        }

        var keys = removedAttachments
                .stream()
                .map(ProcessInstanceAttachmentEntity::getKey)
                .toList();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    keys.forEach(ProcessInstanceAttachmentService.this::deleteDueAttachment);
                }
            });
        } else {
            keys.forEach(this::deleteDueAttachment);
        }
    }

    /**
     * Retries the deletion of due attachments whose deletion failed or never ran after the commit.
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void retryOverdueAttachmentDeletions() {
        // Recently due attachments are still being deleted after their commit.
        processInstanceAttachmentRepository
                .findTop100ByDeletionDueAtLessThanOrderByDeletionDueAtAsc(Instant.now().minus(DELETION_RETRY_DELAY))
                .forEach(attachment -> deleteDueAttachment(attachment.getKey()));
    }

    /**
     * Deletes a due attachment, its file and its attachment set once it is empty in a new transaction. If the file
     * cannot be deleted, the transaction rolls back and the attachment stays due for the next retry.
     */
    private void deleteDueAttachment(@Nonnull UUID attachmentKey) {
        try {
            Objects.requireNonNull(fileDeletionTransaction).executeWithoutResult(status -> {
                // Absent if the instance or another run deleted it in the meantime.
                var attachment = processInstanceAttachmentRepository
                        .findByKeyAndDeletionDueAtIsNotNull(attachmentKey)
                        .orElse(null);
                if (attachment == null) {
                    return;
                }

                // The storage index still has a foreign key from this row; flush its deletion first.
                processInstanceAttachmentRepository.delete(attachment);
                processInstanceAttachmentRepository.flush();

                try {
                    storageService.deleteDocument(attachment.getStorageProviderId(), attachment.getStoragePathFromRoot());
                } catch (ResponseException e) {
                    throw new IllegalStateException(e);
                }

                var attachmentSetId = attachment.getAttachmentSetId();
                if (!processInstanceAttachmentRepository.existsByAttachmentSetId(attachmentSetId)) {
                    Objects.requireNonNull(processInstanceAttachmentSetRepository).deleteById(attachmentSetId);
                }
            });
        } catch (RuntimeException e) {
            logger.warn("Failed to delete the removed attachment {}. The deletion is retried later.", attachmentKey, e);
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
                        true,
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
