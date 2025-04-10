package de.aivot.GoverBackend.config.repositories;

import de.aivot.GoverBackend.config.entities.UserConfigEntity;
import de.aivot.GoverBackend.config.entities.UserConfigEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface UserConfigRepository extends JpaRepository<UserConfigEntity, UserConfigEntityId>, JpaSpecificationExecutor<UserConfigEntity> {
    Collection<UserConfigEntity> findAllByKey(String key);

    Collection<UserConfigEntity> findAllByUserId(String userId);

    Collection<UserConfigEntity> findAllByUserIdAndPublicConfigIsTrue(String userId);

    Optional<UserConfigEntity> findByUserIdAndKeyAndPublicConfigIsTrue(String userId, String key);
}
