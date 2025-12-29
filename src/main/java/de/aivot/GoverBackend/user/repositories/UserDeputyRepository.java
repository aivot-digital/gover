package de.aivot.GoverBackend.user.repositories;

import de.aivot.GoverBackend.user.entities.UserDeputyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserDeputyRepository extends JpaRepository<UserDeputyEntity, Integer>, JpaSpecificationExecutor<UserDeputyEntity> {
}
