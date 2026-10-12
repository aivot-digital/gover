package de.aivot.prosuna.backend.storage.repositories;

import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import jakarta.annotation.Nonnull;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select provider from StorageProviderEntity provider where provider.id = :id")
    Optional<StorageProviderEntity> findByIdForUpdate(@Param("id") Integer id);

    /** Requests a synchronization of all providers, e.g. for the nightly synchronization. */
    @Modifying
    @Transactional
    @Query("""
            update StorageProviderEntity provider
            set provider.status = de.aivot.prosuna.backend.storage.enums.StorageProviderStatus.SyncPending,
                provider.updated = :now
            """)
    int requestSyncOfAll(@Nonnull @Param("now") Instant now);

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
}
