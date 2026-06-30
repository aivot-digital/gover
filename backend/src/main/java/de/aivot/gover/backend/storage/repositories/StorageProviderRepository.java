package de.aivot.gover.backend.storage.repositories;

import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.enums.StorageProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StorageProviderRepository extends JpaRepository<StorageProviderEntity, Integer>, JpaSpecificationExecutor<StorageProviderEntity> {
    List<StorageProviderEntity> findAllByStorageProviderDefinitionKey(String storageProviderDefinitionKey);

    List<StorageProviderEntity> findAllByType(StorageProviderType type);
}
