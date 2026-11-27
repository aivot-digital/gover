package de.aivot.prosuna.backend.codeLists.repositories;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The synchronization status updates are conditional, so concurrent requests, duplicate messages and multiple
 * application instances cannot synchronize a list twice or overwrite a newer request. A synchronization is claimed
 * by a run, and only that run can record its result.
 */
public interface CodeListRepository extends JpaRepository<CodeListEntity, String>, JpaSpecificationExecutor<CodeListEntity> {
    /** Records a synchronization request for a list with a synchronized source. Returns whether one was recorded. */
    @Modifying
    @Transactional
    @Query("""
            update CodeListEntity codeList
            set codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.SyncPending,
                codeList.statusMessage = null, codeList.syncKeepOutdated = :keepOutdated, codeList.updated = :now
            where codeList.key = :key
              and codeList.sourceType in (de.aivot.prosuna.backend.codeLists.enums.CodeListSourceType.XRepository,
                                          de.aivot.prosuna.backend.codeLists.enums.CodeListSourceType.Asset)
            """)
    int requestSync(@Nonnull @Param("key") String key, @Param("keepOutdated") boolean keepOutdated,
                    @Nonnull @Param("now") Instant now);

    /**
     * Starts a requested synchronization as the given run. While another run is active, the request stays pending
     * until that run releases it. A run that started before {@code staleBefore} is considered interrupted, e.g. by a
     * restart, and is taken over. Returns whether this caller may synchronize the list.
     */
    @Modifying
    @Transactional
    @Query("""
            update CodeListEntity codeList
            set codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.Syncing,
                codeList.statusMessage = null, codeList.syncRunId = :runId, codeList.syncStarted = :now,
                codeList.updated = :now
            where codeList.key = :key
              and codeList.status in (de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.SyncPending,
                                      de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.Syncing)
              and (codeList.syncRunId is null or codeList.syncStarted is null or codeList.syncStarted <= :staleBefore)
            """)
    int claimSync(@Nonnull @Param("key") String key, @Nonnull @Param("runId") UUID runId,
                  @Nonnull @Param("now") Instant now, @Nonnull @Param("staleBefore") Instant staleBefore);

    /** Locks the list, so a run can record its result only while it still owns the synchronization. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select codeList from CodeListEntity codeList where codeList.key = :key")
    Optional<CodeListEntity> findByKeyForUpdate(@Nonnull @Param("key") String key);

    /** Records a failed synchronization unless the list was requested again or another run took it over. */
    @Modifying
    @Transactional
    @Query("""
            update CodeListEntity codeList
            set codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.SyncFailed,
                codeList.statusMessage = :message, codeList.syncRunId = null, codeList.syncStarted = null,
                codeList.updated = :now
            where codeList.key = :key
              and codeList.syncRunId = :runId
              and codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.Syncing
            """)
    int failSync(@Nonnull @Param("key") String key, @Nonnull @Param("runId") UUID runId,
                 @Nullable @Param("message") String message, @Nonnull @Param("now") Instant now);

    /** Releases a run whose result was superseded, so the pending request can be claimed again. */
    @Modifying
    @Transactional
    @Query("""
            update CodeListEntity codeList set codeList.syncRunId = null, codeList.syncStarted = null
            where codeList.key = :key and codeList.syncRunId = :runId
            """)
    int releaseSync(@Nonnull @Param("key") String key, @Nonnull @Param("runId") UUID runId);

    @Nonnull
    @Query("""
            select codeList.key from CodeListEntity codeList
            where codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.SyncPending
              and codeList.updated <= :requestedBefore
            order by codeList.key
            """)
    List<String> findPendingSyncKeys(@Nonnull @Param("requestedBefore") Instant requestedBefore);

    /**
     * Records that a pending request is published again unless another application instance has just done so.
     * Returns whether this caller may publish it.
     */
    @Modifying
    @Transactional
    @Query("""
            update CodeListEntity codeList set codeList.updated = :now
            where codeList.key = :key
              and codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.SyncPending
              and codeList.updated <= :requestedBefore
            """)
    int claimSyncRedispatch(@Nonnull @Param("key") String key, @Nonnull @Param("requestedBefore") Instant requestedBefore,
                            @Nonnull @Param("now") Instant now);

    /** Returns synchronizations whose run started before {@code startedBefore} and is considered interrupted. */
    @Nonnull
    @Query("""
            select codeList.key from CodeListEntity codeList
            where codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.Syncing
              and (codeList.syncRunId is null or codeList.syncStarted is null or codeList.syncStarted <= :startedBefore)
            order by codeList.key
            """)
    List<String> findInterruptedSyncKeys(@Nonnull @Param("startedBefore") Instant startedBefore);
}
