package de.aivot.GoverBackend.payment.repositories;

import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentProviderRepository extends JpaRepository<PaymentProviderEntity, String>, JpaSpecificationExecutor<PaymentProviderEntity> {
}
