package de.aivot.gover.backend.form.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.gover.backend.form.entities.VFormVersionWithDetailsEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface VFormVersionWithDetailsRepository extends ReadOnlyRepository<VFormVersionWithDetailsEntity, VFormVersionWithDetailsEntityId>, JpaSpecificationExecutor<VFormVersionWithDetailsEntity> {
    Optional<VFormVersionWithDetailsEntity> findBySlugAndVersion(String slug, Integer version);
}
