package de.aivot.prosuna.backend.communication.repositories;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import jakarta.persistence.LockModeType;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationProviderBindingRepository extends JpaRepository<CommunicationProviderBindingEntity, Integer> {
    List<CommunicationProviderBindingEntity> findAllByIdentityProviderKeyOrderByPositionAscNameAscIdAsc(UUID identityProviderKey);

    List<CommunicationProviderBindingEntity> findAllByCommunicationProviderId(Integer communicationProviderId);

    // Read only parent IDs before locking, so the entity is first loaded after the parent lock.
    @Query("select binding.communicationProviderId as communicationProviderId, binding.identityProviderKey as identityProviderKey from CommunicationProviderBindingEntity binding where binding.id = :id")
    @Nonnull
    Optional<BindingReference> findReferenceById(@Nonnull @Param("id") Integer id);

    interface BindingReference {
        @Nonnull
        Integer getCommunicationProviderId();
        @Nonnull
        UUID getIdentityProviderKey();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select binding from CommunicationProviderBindingEntity binding where binding.id = :id")
    Optional<CommunicationProviderBindingEntity> findByIdForUpdate(@Param("id") Integer id);
}
