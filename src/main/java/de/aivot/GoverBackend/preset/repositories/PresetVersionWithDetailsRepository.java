package de.aivot.GoverBackend.preset.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.preset.entities.PresetVersionEntity;
import de.aivot.GoverBackend.preset.entities.PresetVersionEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PresetVersionWithDetailsRepository extends ReadOnlyRepository<PresetVersionEntity, PresetVersionEntityId>, JpaSpecificationExecutor<PresetVersionEntity> {
}
