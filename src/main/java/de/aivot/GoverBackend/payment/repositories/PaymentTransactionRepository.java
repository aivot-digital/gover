package de.aivot.GoverBackend.payment.repositories;

import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, String>, JpaSpecificationExecutor<PaymentTransactionEntity> {
}
