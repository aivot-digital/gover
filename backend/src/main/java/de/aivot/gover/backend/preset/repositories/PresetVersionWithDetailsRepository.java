package de.aivot.gover.backend.preset.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.preset.entities.PresetVersionEntity;
import de.aivot.gover.backend.preset.entities.PresetVersionEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PresetVersionWithDetailsRepository extends ReadOnlyRepository<PresetVersionEntity, PresetVersionEntityId>, JpaSpecificationExecutor<PresetVersionEntity> {
}
