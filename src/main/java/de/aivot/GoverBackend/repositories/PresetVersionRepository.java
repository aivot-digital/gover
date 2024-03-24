package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.PresetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

public interface PresetVersionRepository extends JpaRepository<PresetVersion, String> {
    
    @Query("SELECT v FROM PresetVersion v WHERE v.preset = ?1 AND v.version = ?2 ORDER BY string_to_array(v.version, '.') DESC")
    Optional<PresetVersion> getByPresetAndVersion(String preset, String version);

    @Transactional
    @Query("SELECT v FROM PresetVersion v WHERE v.preset = ?1 ORDER BY string_to_array(v.version, '.') DESC")
    Collection<PresetVersion> findAllByPreset(String preset);

    
    boolean existsByPresetAndVersion(String preset, String version);

    
    boolean existsByPreset(String preset);
}
