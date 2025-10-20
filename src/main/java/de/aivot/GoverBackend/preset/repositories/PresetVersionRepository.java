package de.aivot.GoverBackend.preset.repositories;

import de.aivot.GoverBackend.preset.entities.PresetVersionEntity;
import de.aivot.GoverBackend.preset.entities.PresetVersionEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PresetVersionRepository extends JpaRepository<PresetVersionEntity, PresetVersionEntityId>, JpaSpecificationExecutor<PresetVersionEntity> {
    @Query(value = """
            SELECT max(version) from preset_versions where preset_key = :presetKey;
            """, nativeQuery = true)
    Optional<Integer> maxVersionForPresetKey(@Param("presetKey") UUID presetKey);
}
