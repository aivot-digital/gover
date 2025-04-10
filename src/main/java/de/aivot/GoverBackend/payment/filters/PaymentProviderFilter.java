package de.aivot.GoverBackend.payment.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class PaymentProviderFilter implements Filter<PaymentProviderEntity> {
    private String name;
    private String providerKey;
    private Boolean isTestProvider;

    public static PaymentProviderFilter create() {
        return new PaymentProviderFilter();
    }

    @Nonnull
    @Override
    public Specification<PaymentProviderEntity> build() {
        return SpecificationBuilder
                .create(PaymentProviderEntity.class)
                .withContains("name", name)
                .withEquals("providerKey", providerKey)
                .withEquals("isTestProvider", isTestProvider)
                .build();
    }

    public String getName() {
        return name;
    }

    public PaymentProviderFilter setName(String name) {
        this.name = name;
        return this;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public PaymentProviderFilter setProviderKey(String providerKey) {
        this.providerKey = providerKey;
        return this;
    }

    public Boolean getTestProvider() {
        return isTestProvider;
    }

    public PaymentProviderFilter setTestProvider(Boolean testProvider) {
        isTestProvider = testProvider;
        return this;
    }
}
