package de.aivot.GoverBackend.userRoles.repositories;

import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Integer>, JpaSpecificationExecutor<UserRoleEntity> {
    List<UserRoleEntity> findAllByName(String name);
}
