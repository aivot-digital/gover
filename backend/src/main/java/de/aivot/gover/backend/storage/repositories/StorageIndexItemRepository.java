package de.aivot.gover.backend.storage.repositories;

import de.aivot.gover.backend.storage.entities.StorageIndexItemEntity;
import de.aivot.gover.backend.storage.entities.StorageIndexItemEntityId;
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

    List<StorageIndexItemEntity> findAllByStorageProviderIdAndDirectoryIsTrue(Integer storageProviderId);

    @Query(
            value = """
                        SELECT * FROM storage_index_items
                        WHERE storage_provider_id = :storageProviderId AND
                              path_from_root <> CAST(:folderPath AS TEXT) AND
                              left(path_from_root, char_length(CAST(:folderPath AS TEXT))) = CAST(:folderPath AS TEXT) AND
                              position(
                                  '/' in trim(trailing '/' from substring(path_from_root from char_length(CAST(:folderPath AS TEXT)) + 1))
                              ) = 0 AND
                              (missing = false OR :includeMissing = true)
                        ORDER BY directory DESC
            """,
            nativeQuery = true
    )
    List<StorageIndexItemEntity> listAllInFolder(@Param("storageProviderId") Integer id,
                                                 @Param("folderPath") String folderPath,
                                                 @Param("includeMissing") boolean includeMissing);

    Optional<StorageIndexItemEntity> findByStorageProviderIdAndPathFromRootAndDirectoryIsFalse(
            @Nonnull Integer storageProviderId,
            @Nonnull String pathFromRoot
    );

    @Modifying
    @Transactional
    @Query(
            value = """
                    UPDATE storage_index_items
                    SET path_from_root = CAST(:targetPath AS text),
                        updated = now()
                    WHERE storage_provider_id = :storageProviderId
                      AND path_from_root = CAST(:sourcePath AS text)
                      AND directory = false
                    """,
            nativeQuery = true
    )
    int moveDocumentPath(@Param("storageProviderId") Integer storageProviderId,
                         @Param("sourcePath") String sourcePath,
                         @Param("targetPath") String targetPath);

    @Modifying
    @Transactional
    @Query(
            value = """
                    DELETE FROM storage_index_items
                    WHERE storage_provider_id = :storageProviderId
                      AND (
                          path_from_root = CAST(:folderPath AS text)
                          OR path_from_root LIKE (CAST(:folderPath AS text) || '%')
                      )
                    """,
            nativeQuery = true
    )
    int deleteFolderTree(@Param("storageProviderId") Integer storageProviderId,
                         @Param("folderPath") String folderPath);
}
