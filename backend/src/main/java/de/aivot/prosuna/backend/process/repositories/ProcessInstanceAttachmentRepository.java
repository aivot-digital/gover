package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProcessInstanceAttachmentRepository extends JpaRepository<ProcessInstanceAttachmentEntity, UUID>, JpaSpecificationExecutor<ProcessInstanceAttachmentEntity> {
    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceId(Long processInstanceId);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceIdAndRemovedAtIsNull(Long processInstanceId);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceIdAndFileName(Long processInstanceId,
                                                                                 String fileName);

    List<ProcessInstanceAttachmentEntity> findAllByAttachmentSetIdAndRemovedAtIsNullOrderByPositionAscKeyAsc(Integer attachmentSetId);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceTaskIdAndAttachmentSetIdIn(Long processInstanceTaskId,
                                                                                            Collection<Integer> attachmentSetIds);

    List<ProcessInstanceAttachmentEntity> findAllByProcessInstanceTaskIdAndRemovedAtIsNotNull(Long processInstanceTaskId);

    boolean existsByAttachmentSetId(Integer attachmentSetId);
}
