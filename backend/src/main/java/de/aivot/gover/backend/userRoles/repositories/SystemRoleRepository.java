package de.aivot.gover.backend.userRoles.repositories;

import de.aivot.gover.backend.userRoles.entities.SystemRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemRoleRepository extends JpaRepository<SystemRoleEntity, Integer>, JpaSpecificationExecutor<SystemRoleEntity> {
}
