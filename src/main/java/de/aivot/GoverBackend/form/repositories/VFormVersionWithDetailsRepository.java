package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntityId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface VFormVersionWithDetailsRepository extends ReadOnlyRepository<VFormVersionWithDetailsEntity, VFormVersionWithDetailsEntityId>, JpaSpecificationExecutor<VFormVersionWithDetailsEntity> {
    Optional<VFormVersionWithDetailsEntity> findBySlugAndVersion(String slug, Integer version);
}
