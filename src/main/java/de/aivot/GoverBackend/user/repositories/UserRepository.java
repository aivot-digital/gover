package de.aivot.GoverBackend.user.repositories;

import de.aivot.GoverBackend.user.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface UserRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {
    Collection<UserEntity> findAllByDeletedInIdpIsFalse();
}
