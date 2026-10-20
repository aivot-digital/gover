package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.models.ProcessAttachmentSizeProjection;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProcessInstanceAttachmentRepository extends JpaRepository<ProcessInstanceAttachmentEntity, UUID>, JpaSpecificationExecutor<ProcessInstanceAttachmentEntity> {
    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceId(Long processInstanceId);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceIdAndFileName(Long processInstanceId,
                                                                                 String fileName);

    List<ProcessInstanceAttachmentEntity> findAllByAttachmentSetIdOrderByPositionAscKeyAsc(Integer attachmentSetId);

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
}
