package de.aivot.gover.backend.process.repositories;

import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProcessInstanceAttachmentSetRepository extends JpaRepository<ProcessInstanceAttachmentSetEntity, Integer>, JpaSpecificationExecutor<ProcessInstanceAttachmentSetEntity> {
    List<ProcessInstanceAttachmentSetEntity> findAllByProcessInstanceId(Long processInstanceId);

    List<ProcessInstanceAttachmentSetEntity> findAllByProcessInstanceIdAndDataKey(Long processInstanceId,
                                                                                   String dataKey);

    Optional<ProcessInstanceAttachmentSetEntity> findFirstByProcessInstanceIdAndProcessInstanceTaskIdAndDataKeyOrderByIdDesc(Long processInstanceId,
                                                                                                                              Long processInstanceTaskId,
                                                                                                                              String dataKey);
}
