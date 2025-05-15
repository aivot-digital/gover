package de.aivot.GoverBackend.payment.filters;

import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import java.util.List;

public class PaymentTransactionFilter implements Filter<PaymentTransactionEntity> {
    private String paymentProviderKey;
    private XBezahldienstStatus status;
    private Boolean hasError;

    private PaymentTransactionFilter() {
    }

    public static PaymentTransactionFilter create() {
        return new PaymentTransactionFilter();
    }

    public PaymentTransactionFilter setPaymentProviderKey(String paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public PaymentTransactionFilter setStatus(XBezahldienstStatus status) {
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
                .withJsonEquals("paymentInformation", List.of("status"), status != null ? status.getKey() : null);

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
