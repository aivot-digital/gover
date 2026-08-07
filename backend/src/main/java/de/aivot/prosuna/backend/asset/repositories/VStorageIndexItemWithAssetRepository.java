package de.aivot.prosuna.backend.asset.repositories;

import de.aivot.prosuna.backend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.prosuna.backend.asset.entities.VStorageIndexItemWithAssetEntityId;
import de.aivot.prosuna.backend.core.repositories.ReadOnlyRepository;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VStorageIndexItemWithAssetRepository extends ReadOnlyRepository<VStorageIndexItemWithAssetEntity, VStorageIndexItemWithAssetEntityId>, JpaSpecificationExecutor<VStorageIndexItemWithAssetEntity> {
    @Nonnull
    Optional<VStorageIndexItemWithAssetEntity> findByAssetKey(@Nonnull UUID assetKey);

    @Query(
            value = """
                    SELECT * FROM v_storage_index_items_with_assets
                    WHERE storage_provider_id = :storageProviderId
                      AND path_from_root <> CAST(:folderPath AS TEXT)
                      AND left(path_from_root, char_length(CAST(:folderPath AS TEXT))) = CAST(:folderPath AS TEXT)
                      AND position(
                          '/' in trim(trailing '/' from substring(path_from_root from char_length(CAST(:folderPath AS TEXT)) + 1))
                      ) = 0
                      AND (missing = false OR :includeMissing = true)
                      AND (
                          :isPublic IS NULL
                          OR (
                              :isPublic = true
                              AND (
                                  directory = true
                                  OR asset_is_private = false
                              )
                          )
                          OR (
                              :isPublic = false
                              AND directory = false
                              AND coalesce(asset_is_private, true) = true
                          )
                      )
                      AND (
                          directory = true
                          OR CAST(:contentTypePattern AS TEXT) IS NULL
                          OR coalesce(mime_type, '') = ''
                          OR lower(mime_type) ~ :contentTypePattern
                      )
                    ORDER BY directory DESC
                    """,
            nativeQuery = true
    )
    @Nonnull
    List<VStorageIndexItemWithAssetEntity> listAllInFolder(@Param("storageProviderId") Integer storageProviderId,
                                                           @Param("folderPath") String folderPath,
                                                           @Param("includeMissing") boolean includeMissing,
                                                           @Param("contentTypePattern") String contentTypePattern,
                                                           @Param("isPublic") Boolean isPublic);

    List<VStorageIndexItemWithAssetEntity> findAllByMimeType(String mimeType);
}
