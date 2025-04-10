package de.aivot.GoverBackend.secrets.repositories;

import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecretRepository extends JpaRepository<SecretEntity, String>, JpaSpecificationExecutor<SecretEntity> {
}
