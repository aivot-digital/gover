package de.aivot.GoverBackend.storage.repositories;

import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StorageIndexItemRepository extends JpaRepository<StorageIndexItemEntity, StorageIndexItemEntityId>, JpaSpecificationExecutor<StorageIndexItemEntity> {
    List<StorageIndexItemEntity> findAllByStorageProviderId(Integer storageProviderId);

    @Query(
            value = "SELECT * FROM storage_index_items WHERE storage_provider_id = :storageProviderId AND path_from_root ~ :path",
            nativeQuery = true
    )
    List<StorageIndexItemEntity> listAllInFolder(@Param("storageProviderId") Integer id,
                                                 @Param("path") String path);
}
