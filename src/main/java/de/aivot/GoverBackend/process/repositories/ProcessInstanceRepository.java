package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, Long>, JpaSpecificationExecutor<ProcessInstanceEntity> {
    List<ProcessInstanceEntity> findAllByStatus(ProcessInstanceStatus status);

    List<ProcessInstanceEntity> findAllByStatusAndKeepUntilLessThanEqual(ProcessInstanceStatus status,
                                                                         Instant keepUntil,
                                                                         Pageable pageable);

    long countByStatusAndKeepUntilLessThanEqual(ProcessInstanceStatus status,
                                                Instant keepUntil);

    List<ProcessInstanceEntity> findAllByCreatedForTestClaimId(Integer createdForTestClaimId);

    long countAllByStatusIs(ProcessInstanceStatus status);
}
