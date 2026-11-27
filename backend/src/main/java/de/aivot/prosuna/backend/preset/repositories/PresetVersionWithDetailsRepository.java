package de.aivot.prosuna.backend.preset.repositories;

import de.aivot.prosuna.backend.core.repositories.ReadOnlyRepository;
import de.aivot.prosuna.backend.preset.entities.PresetVersionEntity;
import de.aivot.prosuna.backend.preset.entities.PresetVersionEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PresetVersionWithDetailsRepository extends ReadOnlyRepository<PresetVersionEntity, PresetVersionEntityId>, JpaSpecificationExecutor<PresetVersionEntity> {
}
