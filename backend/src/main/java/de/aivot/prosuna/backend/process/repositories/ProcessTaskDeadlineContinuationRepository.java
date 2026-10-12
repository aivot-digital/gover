package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessTaskDeadlineContinuationEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ProcessTaskDeadlineContinuationRepository extends JpaRepository<ProcessTaskDeadlineContinuationEntity, Long> {
    @Nonnull
    @Query("""
            select continuation from ProcessTaskDeadlineContinuationEntity continuation
            join ProcessInstanceEntity instance on instance.id = continuation.processInstanceId
            where continuation.id > :afterId
              and (continuation.lastDispatched is null or continuation.lastDispatched <= :dispatchedBefore)
              and instance.status in :instanceStatuses
            order by continuation.id
            """)
    List<ProcessTaskDeadlineContinuationEntity> findDispatchable(@Param("afterId") long afterId,
                                                                 @Nonnull @Param("dispatchedBefore") Instant dispatchedBefore,
                                                                 @Nonnull @Param("instanceStatuses") Collection<ProcessInstanceStatus> instanceStatuses,
                                                                 @Nonnull Pageable page);

    // Updates nothing if a consumer has already deleted the entry, unlike saving the detached entity.
    @Modifying
    @Transactional
    @Query("update ProcessTaskDeadlineContinuationEntity continuation set continuation.lastDispatched = :dispatched where continuation.id = :id")
    void markDispatched(@Param("id") long id, @Nonnull @Param("dispatched") Instant dispatched);
}
