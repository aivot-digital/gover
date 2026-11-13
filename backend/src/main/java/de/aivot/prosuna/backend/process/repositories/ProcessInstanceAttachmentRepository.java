package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.models.ProcessAttachmentSizeProjection;
import jakarta.annotation.Nonnull;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessInstanceAttachmentRepository extends JpaRepository<ProcessInstanceAttachmentEntity, UUID>, JpaSpecificationExecutor<ProcessInstanceAttachmentEntity> {
    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceId(Long processInstanceId);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceIdAndRemovedAtIsNull(Long processInstanceId);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceIdAndFileName(Long processInstanceId,
                                                                                 String fileName);

    @Nonnull
    @Query("""
            SELECT attachment.key AS key, item.sizeInBytes AS sizeInBytes
            FROM ProcessInstanceAttachmentEntity attachment
            JOIN StorageIndexItemEntity item
              ON item.storageProviderId = attachment.storageProviderId
             AND item.pathFromRoot = attachment.storagePathFromRoot
            WHERE attachment.key IN :keys
            """)
    List<ProcessAttachmentSizeProjection> findSizesByKeys(@Nonnull @Param("keys") List<UUID> keys);

    List<ProcessInstanceAttachmentEntity> findAllByAttachmentSetIdAndRemovedAtIsNullOrderByPositionAscKeyAsc(Integer attachmentSetId);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceTaskIdAndAttachmentSetIdIn(Long processInstanceTaskId,
                                                                                            Collection<Integer> attachmentSetIds);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceTaskIdAndRemovedAtIsNotNullAndDeletionDueAtIsNull(Long processInstanceTaskId);

    List<ProcessInstanceAttachmentEntity> findTop100ByDeletionDueAtLessThanOrderByDeletionDueAtAsc(Instant deletionDueBefore);

    /** Locks a due attachment, so only one run deletes its file. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProcessInstanceAttachmentEntity> findByKeyAndDeletionDueAtIsNotNull(UUID key);

    boolean existsByAttachmentSetId(Integer attachmentSetId);
}
