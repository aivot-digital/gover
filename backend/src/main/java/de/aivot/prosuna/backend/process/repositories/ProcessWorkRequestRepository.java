package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessWorkRequestEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessWorkRequestKind;
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

public interface ProcessWorkRequestRepository extends JpaRepository<ProcessWorkRequestEntity, Long> {
    @Nonnull
    @Query("""
            select request.id from ProcessWorkRequestEntity request
            join ProcessInstanceEntity instance on instance.id = request.processInstanceId
            where request.id > :afterId
              and (request.lastDispatched is null or request.lastDispatched <= :dispatchedBefore)
              and instance.status in :instanceStatuses
            order by request.id
            """)
    List<Long> findDispatchableIds(@Param("afterId") long afterId,
                                   @Nonnull @Param("dispatchedBefore") Instant dispatchedBefore,
                                   @Nonnull @Param("instanceStatuses") Collection<ProcessInstanceStatus> instanceStatuses,
                                   @Nonnull Pageable page);

    /**
     * Records a publication unless another dispatcher has published the request since it was selected or a consumer
     * has already deleted it. Returns whether this caller may publish the request.
     */
    @Modifying
    @Transactional
    @Query("""
            update ProcessWorkRequestEntity request set request.lastDispatched = :dispatched
            where request.id = :id and (request.lastDispatched is null or request.lastDispatched <= :dispatchedBefore)
            """)
    int claimDispatch(@Param("id") long id, @Nonnull @Param("dispatchedBefore") Instant dispatchedBefore,
                      @Nonnull @Param("dispatched") Instant dispatched);

    boolean existsByKindAndProcessInstanceId(@Nonnull ProcessWorkRequestKind kind, @Nonnull Long processInstanceId);

    boolean existsByKindAndTaskId(@Nonnull ProcessWorkRequestKind kind, @Nonnull Long taskId);

    @Nonnull
    @Query("select request.id from ProcessWorkRequestEntity request where request.processInstanceId = :instanceId order by request.id")
    List<Long> findIdsByProcessInstanceId(@Nonnull @Param("instanceId") Long processInstanceId);
}
