package de.aivot.GoverBackend.asset.repositories;

import de.aivot.GoverBackend.asset.entities.AssetWithMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssetWithMetadataRepository extends JpaRepository<AssetWithMetadataEntity, UUID>, JpaSpecificationExecutor<AssetWithMetadataEntity> {
    @Query("select distinct a.storageProviderId from AssetWithMetadataEntity a where a.storageProviderId is not null")
    List<Integer> findDistinctStorageProviderIds();

    @Query(
            value = """
                    SELECT * FROM assets_with_metadata
                    WHERE storage_provider_id = :storageProviderId
                      AND storage_path_from_root IS NOT NULL
                      AND COALESCE(NULLIF(regexp_replace(storage_path_from_root, '[^/]+$', ''), ''), '/') = :folderPath
                    ORDER BY filename ASC
                    """,
            nativeQuery = true
    )
    List<AssetWithMetadataEntity> listAllInFolder(@Param("storageProviderId") Integer storageProviderId,
                                                  @Param("folderPath") String folderPath);
}
