package de.aivot.GoverBackend.asset.repositories;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID>, JpaSpecificationExecutor<AssetEntity> {
    @Query("select distinct a.storageProviderId from AssetEntity a where a.storageProviderId is not null")
    List<Integer> findDistinctStorageProviderIds();

    Optional<AssetEntity> findByStorageProviderIdAndStoragePathFromRoot(Integer storageProviderId, String storagePathFromRoot);

    boolean existsByStorageProviderIdAndStoragePathFromRoot(Integer storageProviderId, String storagePathFromRoot);

    boolean existsByStorageProviderIdAndStoragePathFromRootAndKeyNot(Integer storageProviderId, String storagePathFromRoot, UUID key);
}
