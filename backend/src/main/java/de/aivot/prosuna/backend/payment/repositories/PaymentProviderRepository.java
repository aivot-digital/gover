package de.aivot.prosuna.backend.payment.repositories;

import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PaymentProviderRepository extends JpaRepository<PaymentProviderEntity, UUID>, JpaSpecificationExecutor<PaymentProviderEntity> {
}
