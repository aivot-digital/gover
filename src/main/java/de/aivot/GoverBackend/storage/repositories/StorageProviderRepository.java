package de.aivot.GoverBackend.storage.repositories;

import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StorageProviderRepository extends JpaRepository<StorageProviderEntity, Integer>, JpaSpecificationExecutor<StorageProviderEntity> {
    List<StorageProviderEntity> findAllByStorageProviderDefinitionKey(String storageProviderDefinitionKey);
}
