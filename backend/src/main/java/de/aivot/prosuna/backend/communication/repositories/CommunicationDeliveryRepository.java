package de.aivot.prosuna.backend.communication.repositories;

import de.aivot.prosuna.backend.communication.entities.CommunicationDeliveryEntity;
import jakarta.annotation.Nonnull;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationDeliveryRepository extends JpaRepository<CommunicationDeliveryEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from CommunicationDeliveryEntity d where d.id = :id")
    Optional<CommunicationDeliveryEntity> findByIdForUpdate(@Param("id") @Nonnull UUID id);

    @Query(value = "select id from communication_deliveries where next_check_at <= :now order by next_check_at limit 100", nativeQuery = true)
    List<UUID> findDueIds(@Param("now") @Nonnull Instant now);

    Optional<CommunicationDeliveryEntity> findByTaskId(@Nonnull Long taskId);

    @Query(value = "select * from communication_deliveries where next_work is not null order by created limit 100", nativeQuery = true)
    List<CommunicationDeliveryEntity> findPendingWork();

    long deleteByProcessInstanceIdIsNullAndCreatedBefore(@Nonnull Instant cutoff);
}
