package de.aivot.prosuna.backend.codeLists.repositories;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * The synchronization status updates are conditional, so concurrent requests, duplicate messages and multiple
 * application instances cannot synchronize a list twice or overwrite a newer request.
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

    /** Starts a requested synchronization. Returns whether this caller may synchronize the list. */
    @Modifying
    @Transactional
    @Query("""
            update CodeListEntity codeList
            set codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.Syncing,
                codeList.statusMessage = null, codeList.updated = :now
            where codeList.key = :key
              and codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.SyncPending
            """)
    int claimSync(@Nonnull @Param("key") String key, @Nonnull @Param("now") Instant now);

    /** Completes a synchronization unless the list was requested to synchronize again in the meantime. */
    @Modifying
    @Transactional
    @Query("""
            update CodeListEntity codeList
            set codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.Synced,
                codeList.statusMessage = null, codeList.lastSync = :now, codeList.updated = :now
            where codeList.key = :key
              and codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.Syncing
            """)
    int completeSync(@Nonnull @Param("key") String key, @Nonnull @Param("now") Instant now);

    /** Records a failed synchronization unless the list was requested to synchronize again in the meantime. */
    @Modifying
    @Transactional
    @Query("""
            update CodeListEntity codeList
            set codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.SyncFailed,
                codeList.statusMessage = :message, codeList.updated = :now
            where codeList.key = :key
              and codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.Syncing
            """)
    int failSync(@Nonnull @Param("key") String key, @Nullable @Param("message") String message,
                 @Nonnull @Param("now") Instant now);

    /**
     * Requests synchronizations again that an application restart has interrupted. Their last update stays unchanged,
     * so they are immediately due to be published again.
     */
    @Modifying
    @Transactional
    @Query("""
            update CodeListEntity codeList
            set codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.SyncPending
            where codeList.status = de.aivot.prosuna.backend.codeLists.enums.CodeListStatus.Syncing
              and codeList.updated <= :startedBefore
            """)
    int releaseStaleSyncs(@Nonnull @Param("startedBefore") Instant startedBefore);

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
}
