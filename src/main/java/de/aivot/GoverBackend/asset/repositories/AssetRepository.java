package de.aivot.GoverBackend.asset.repositories;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID>, JpaSpecificationExecutor<AssetEntity> {
    Optional<AssetEntity> findByStorageProviderIdAndStoragePathFromRoot(Integer storageProviderId, String storagePathFromRoot);

    void deleteAllByStoragePathFromRootStartingWith(String path);
}
