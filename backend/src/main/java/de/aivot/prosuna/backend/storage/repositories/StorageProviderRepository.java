package de.aivot.prosuna.backend.storage.repositories;

import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageProviderRepository extends JpaRepository<StorageProviderEntity, Integer>, JpaSpecificationExecutor<StorageProviderEntity> {
    List<StorageProviderEntity> findAllByStorageProviderDefinitionKey(String storageProviderDefinitionKey);

    List<StorageProviderEntity> findAllByType(StorageProviderType type);

    @Query(value = """
            SELECT * FROM storage_providers 
            WHERE read_only_storage = false 
              AND type IN (:types) 
              AND metadata_attributes IS NOT NULL 
              AND metadata_attributes NOT IN ('[]'::jsonb, '{}'::jsonb, 'null'::jsonb)
            """, nativeQuery = true)
    List<StorageProviderEntity> findAllByMetadataAttributesNotEmptyAndReadOnlyStorageIsFalseAndTypeIsIn(
            @Param("types") Collection<StorageProviderType> types
    );

    /** Locks the provider, so a run can record its result only while it still owns the synchronization. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select provider from StorageProviderEntity provider where provider.id = :id")
    Optional<StorageProviderEntity> findByIdForUpdate(@Param("id") Integer id);

    /**
     * Requests a synchronization of a provider. Clears the persistence context, so the provider is read again with
     * the new status. Returns whether the provider exists.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
            update StorageProviderEntity provider
            set provider.status = de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.SyncPending,
                provider.statusMessage = null, provider.updated = :now
            where provider.id = :id
            """)
    int requestSync(@Nonnull @Param("id") Integer id, @Nonnull @Param("now") Instant now);

    /** Requests a synchronization of all providers, e.g. for the nightly synchronization. */
    @Modifying
    @Transactional
    @Query("""
            update StorageProviderEntity provider
            set provider.status = de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.SyncPending,
                provider.updated = :now
            """)
    int requestSyncOfAll(@Nonnull @Param("now") Instant now);

    /**
     * Starts a requested synchronization as the given run. While another run is active, the request stays pending
     * until that run releases it. A run that started before {@code staleBefore} is considered interrupted, e.g. by a
     * restart, and is taken over. Returns whether this caller may synchronize the provider.
     */
    @Modifying
    @Transactional
    @Query("""
            update StorageProviderEntity provider
            set provider.status = de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.Syncing,
                provider.statusMessage = null, provider.syncRunId = :runId, provider.syncStarted = :now,
                provider.updated = :now
            where provider.id = :id
              and provider.status in (de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.SyncPending,
                                      de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.Syncing)
              and (provider.syncRunId is null or provider.syncStarted is null or provider.syncStarted <= :staleBefore)
            """)
    int claimSync(@Nonnull @Param("id") Integer id, @Nonnull @Param("runId") UUID runId,
                  @Nonnull @Param("now") Instant now, @Nonnull @Param("staleBefore") Instant staleBefore);

    /** Records a failed synchronization unless the provider was requested again or another run took it over. */
    @Modifying
    @Transactional
    @Query("""
            update StorageProviderEntity provider
            set provider.status = de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.SyncFailed,
                provider.statusMessage = :message, provider.syncRunId = null, provider.syncStarted = null,
                provider.updated = :now
            where provider.id = :id
              and provider.syncRunId = :runId
              and provider.status = de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.Syncing
            """)
    int failSync(@Nonnull @Param("id") Integer id, @Nonnull @Param("runId") UUID runId,
                 @Nullable @Param("message") String message, @Nonnull @Param("now") Instant now);

    /** Releases a run whose result was superseded, so the pending request can be claimed again. */
    @Modifying
    @Transactional
    @Query("""
            update StorageProviderEntity provider set provider.syncRunId = null, provider.syncStarted = null
            where provider.id = :id and provider.syncRunId = :runId
            """)
    int releaseSync(@Nonnull @Param("id") Integer id, @Nonnull @Param("runId") UUID runId);

    @Nonnull
    @Query("""
            select provider.id from StorageProviderEntity provider
            where provider.status = de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.SyncPending
              and provider.updated <= :requestedBefore
            order by provider.id
            """)
    List<Integer> findPendingSyncIds(@Nonnull @Param("requestedBefore") Instant requestedBefore);

    /**
     * Records that a pending request is published again unless another application instance has just done so.
     * Returns whether this caller may publish it.
     */
    @Modifying
    @Transactional
    @Query("""
            update StorageProviderEntity provider set provider.updated = :now
            where provider.id = :id
              and provider.status = de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.SyncPending
              and provider.updated <= :requestedBefore
            """)
    int claimSyncRedispatch(@Nonnull @Param("id") Integer id, @Nonnull @Param("requestedBefore") Instant requestedBefore,
                            @Nonnull @Param("now") Instant now);

    /** Returns synchronizations whose run started before {@code startedBefore} and is considered interrupted. */
    @Nonnull
    @Query("""
            select provider.id from StorageProviderEntity provider
            where provider.status = de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.Syncing
              and (provider.syncRunId is null or provider.syncStarted is null or provider.syncStarted <= :startedBefore)
            order by provider.id
            """)
    List<Integer> findInterruptedSyncIds(@Nonnull @Param("startedBefore") Instant startedBefore);
}
