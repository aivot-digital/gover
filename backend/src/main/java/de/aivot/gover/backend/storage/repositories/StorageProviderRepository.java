package de.aivot.gover.backend.storage.repositories;

import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.enums.StorageProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

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
}
