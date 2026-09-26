package de.aivot.prosuna.backend.payment.repositories;

import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, String>, JpaSpecificationExecutor<PaymentTransactionEntity> {
    Optional<PaymentTransactionEntity> findFirstByRedirectUrlOrderByCreatedDesc(String redirectUrl);
}
