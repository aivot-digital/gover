package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProcessTestClaimRepository extends JpaRepository<ProcessTestClaimEntity, Integer>, JpaSpecificationExecutor<ProcessTestClaimEntity> {
    Optional<ProcessTestClaimEntity> findByAccessKey(@Nonnull String accessKey);
}