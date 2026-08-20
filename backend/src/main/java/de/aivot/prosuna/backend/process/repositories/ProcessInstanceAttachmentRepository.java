package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ProcessInstanceAttachmentRepository extends JpaRepository<ProcessInstanceAttachmentEntity, UUID>, JpaSpecificationExecutor<ProcessInstanceAttachmentEntity> {
    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceId(Long processInstanceId);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceIdAndFileName(Long processInstanceId,
                                                                                 String fileName);

    List<ProcessInstanceAttachmentEntity> findAllByAttachmentSetIdOrderByPositionAscKeyAsc(Integer attachmentSetId);
}
