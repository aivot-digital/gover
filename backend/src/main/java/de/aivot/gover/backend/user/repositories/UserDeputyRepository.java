package de.aivot.gover.backend.user.repositories;

import de.aivot.gover.backend.user.entities.UserDeputyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserDeputyRepository extends JpaRepository<UserDeputyEntity, Integer>, JpaSpecificationExecutor<UserDeputyEntity> {
}
