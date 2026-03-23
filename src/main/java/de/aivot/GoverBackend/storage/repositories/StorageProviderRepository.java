package de.aivot.GoverBackend.storage.repositories;

import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StorageProviderRepository extends JpaRepository<StorageProviderEntity, Integer>, JpaSpecificationExecutor<StorageProviderEntity> {
}
