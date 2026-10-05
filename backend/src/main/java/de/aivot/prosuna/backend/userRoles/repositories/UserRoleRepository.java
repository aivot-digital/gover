package de.aivot.prosuna.backend.userRoles.repositories;

import de.aivot.prosuna.backend.userRoles.entities.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Integer>, JpaSpecificationExecutor<UserRoleEntity> {
}
