package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, Long>, JpaSpecificationExecutor<ProcessInstanceEntity> {
    List<ProcessInstanceEntity> findAllByStatus(ProcessInstanceStatus status);

    List<ProcessInstanceEntity> findAllByStatusAndKeepUntilLessThanEqual(ProcessInstanceStatus status,
                                                                         LocalDateTime keepUntil);

    List<ProcessInstanceEntity> findAllByCreatedForTestClaimId(Integer createdForTestClaimId);
}
