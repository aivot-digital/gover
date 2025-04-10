package de.aivot.GoverBackend.preset.repositories;

import de.aivot.GoverBackend.preset.entities.Preset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PresetRepository extends JpaRepository<Preset, String>, JpaSpecificationExecutor<Preset> {

}
