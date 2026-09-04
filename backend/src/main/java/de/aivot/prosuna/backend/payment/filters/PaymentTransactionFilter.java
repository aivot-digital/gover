package de.aivot.prosuna.backend.payment.filters;

import de.aivot.prosuna.backend.payment.models.PaymentStatus;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public class PaymentTransactionFilter implements Filter<PaymentTransactionEntity> {
    private UUID paymentProviderKey;
    private PaymentStatus status;
    private Boolean hasError;

    private PaymentTransactionFilter() {
    }

    public static PaymentTransactionFilter create() {
        return new PaymentTransactionFilter();
    }

    public PaymentTransactionFilter setPaymentProviderKey(UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public PaymentTransactionFilter setStatus(PaymentStatus status) {
        this.status = status;
        return this;
    }

    public PaymentTransactionFilter setHasError(Boolean hasError) {
        this.hasError = hasError;
        return this;
    }

    @Nonnull
    @Override
    public Specification<PaymentTransactionEntity> build() {
        var spec = SpecificationBuilder
                .create(PaymentTransactionEntity.class)
                .withEquals("paymentProviderKey", paymentProviderKey)
                .withJsonEquals("paymentInformation", List.of("status"), status != null ? status.name() : null);

        if (hasError != null) {
            if (Boolean.TRUE.equals(hasError)) {
                spec = spec.withNotNull("paymentError");
            } else {
                spec = spec.withNull("paymentError");
            }
        }

        return spec
                .build();
    }
}
