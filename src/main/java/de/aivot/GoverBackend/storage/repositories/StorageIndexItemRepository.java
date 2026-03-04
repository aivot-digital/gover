package de.aivot.GoverBackend.storage.repositories;

import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntityId;
import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorageIndexItemRepository extends JpaRepository<StorageIndexItemEntity, StorageIndexItemEntityId>, JpaSpecificationExecutor<StorageIndexItemEntity> {
    List<StorageIndexItemEntity> findAllByStorageProviderId(Integer storageProviderId);

    @Query(
            value = """
                        SELECT * FROM storage_index_items
                        WHERE storage_provider_id = :storageProviderId AND
                              path_from_root ~ :path AND
                              (missing = false OR :includeMissing = true)
                        ORDER BY directory DESC
            """,
            nativeQuery = true
    )
    List<StorageIndexItemEntity> listAllInFolder(@Param("storageProviderId") Integer id,
                                                 @Param("path") String storagePathFromRoot,
                                                 @Param("includeMissing") boolean includeMissing);

    Optional<StorageIndexItemEntity> findByStorageProviderIdAndPathFromRootAndDirectoryIsFalse(
            @Nonnull Integer storageProviderId,
            @Nonnull String pathFromRoot
    );

    @Modifying
    @Transactional
    @Query(
            value = """
                    DELETE FROM storage_index_items
                    WHERE storage_provider_id = :storageProviderId
                      AND (path_from_root = :folderPath OR path_from_root LIKE CONCAT(:folderPath, '%'))
                    """,
            nativeQuery = true
    )
    int deleteFolderTree(@Param("storageProviderId") Integer storageProviderId,
                         @Param("folderPath") String folderPath);
}
