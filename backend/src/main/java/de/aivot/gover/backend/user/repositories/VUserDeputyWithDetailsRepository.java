package de.aivot.gover.backend.user.repositories;

import de.aivot.gover.backend.user.entities.VUserDeputyWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VUserDeputyWithDetailsRepository extends JpaRepository<VUserDeputyWithDetailsEntity, Integer>, JpaSpecificationExecutor<VUserDeputyWithDetailsEntity> {
}
