package de.aivot.prosuna.backend.communication.repositories;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunicationProviderRepository extends JpaRepository<CommunicationProviderEntity, Integer>, JpaSpecificationExecutor<CommunicationProviderEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select provider from CommunicationProviderEntity provider where provider.id = :id")
    Optional<CommunicationProviderEntity> findByIdForUpdate(@Param("id") Integer id);
}
