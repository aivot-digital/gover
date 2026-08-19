package de.aivot.prosuna.backend.user.repositories;

import de.aivot.prosuna.backend.user.entities.UserDeputyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserDeputyRepository extends JpaRepository<UserDeputyEntity, Integer>, JpaSpecificationExecutor<UserDeputyEntity> {
}
