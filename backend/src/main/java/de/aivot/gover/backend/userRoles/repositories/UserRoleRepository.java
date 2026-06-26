package de.aivot.gover.backend.userRoles.repositories;

import de.aivot.gover.backend.userRoles.entities.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Integer>, JpaSpecificationExecutor<UserRoleEntity> {
}
