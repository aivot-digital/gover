package de.aivot.gover.backend.secrets.repositories;

import de.aivot.gover.backend.secrets.entities.SecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SecretRepository extends JpaRepository<SecretEntity, UUID>, JpaSpecificationExecutor<SecretEntity> {
}
