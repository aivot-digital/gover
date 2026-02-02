package de.aivot.GoverBackend.storage.repositories;

import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StorageIndexItemRepository extends JpaRepository<StorageIndexItemEntity, StorageIndexItemEntityId>, JpaSpecificationExecutor<StorageIndexItemEntity> {
    List<StorageIndexItemEntity> findAllByStorageProviderId(Integer storageProviderId);
}
