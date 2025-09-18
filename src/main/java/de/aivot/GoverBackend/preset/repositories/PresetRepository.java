package de.aivot.GoverBackend.preset.repositories;

import de.aivot.GoverBackend.preset.entities.PresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PresetRepository extends JpaRepository<PresetEntity, UUID>, JpaSpecificationExecutor<PresetEntity> {

}
