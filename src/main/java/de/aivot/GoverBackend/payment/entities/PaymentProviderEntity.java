package de.aivot.GoverBackend.payment.entities;

import de.aivot.GoverBackend.core.converters.AuthoredElementValuesConverter;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payment_providers")
public class PaymentProviderEntity {
    @Id
    @Nonnull
    @NotNull
    private UUID key;

    @Nonnull
    @NotNull
    @Column(length = 32)
    private String paymentProviderDefinitionKey;

    @Nonnull
    @NotNull
    private Integer paymentProviderDefinitionVersion;

    @Nonnull
    @NotNull
    @Column(length = 64)
    private String name;

    @Nonnull
    @NotNull
    @Column(length = 255)
    private String description;

    @Nonnull
    @NotNull
    @ColumnDefault("FALSE")
    private Boolean isTestProvider;

    @Nonnull
    @NotNull
    @ColumnDefault("FALSE")
    private Boolean isEnabled;

    @Nonnull
    @NotNull
    @Column(columnDefinition = "jsonb")
    @Convert(converter = AuthoredElementValuesConverter.class)
    private AuthoredElementValues config;

    // region Constructors

    // Empty constructor for JPA
    public PaymentProviderEntity() {
    }

    // Full constructor

    public PaymentProviderEntity(@Nonnull UUID key,
                                 @Nonnull String paymentProviderDefinitionKey,
                                 @Nonnull Integer paymentProviderDefinitionVersion,
                                 @Nonnull String name,
                                 @Nonnull String description,
                                 @Nonnull Boolean isTestProvider,
                                 @Nonnull Boolean isEnabled,
                                 @Nonnull AuthoredElementValues config) {
        this.key = key;
        this.paymentProviderDefinitionKey = paymentProviderDefinitionKey;
        this.paymentProviderDefinitionVersion = paymentProviderDefinitionVersion;
        this.name = name;
        this.description = description;
        this.isTestProvider = isTestProvider;
        this.isEnabled = isEnabled;
        this.config = config;
    }

    // endregion

    // region HashCode and Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PaymentProviderEntity that = (PaymentProviderEntity) o;
        return Objects.equals(key, that.key) && Objects.equals(paymentProviderDefinitionKey, that.paymentProviderDefinitionKey) && Objects.equals(paymentProviderDefinitionVersion, that.paymentProviderDefinitionVersion) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(isTestProvider, that.isTestProvider) && Objects.equals(isEnabled, that.isEnabled) && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, paymentProviderDefinitionKey, paymentProviderDefinitionVersion, name, description, isTestProvider, isEnabled, config);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public UUID getKey() {
        return key;
    }

    public PaymentProviderEntity setKey(@Nonnull UUID key) {
        this.key = key;
        return this;
    }

    @Nonnull
    public String getPaymentProviderDefinitionKey() {
        return paymentProviderDefinitionKey;
    }

    public PaymentProviderEntity setPaymentProviderDefinitionKey(@Nonnull String paymentProviderDefinitionKey) {
        this.paymentProviderDefinitionKey = paymentProviderDefinitionKey;
        return this;
    }

    @Nonnull
    public Integer getPaymentProviderDefinitionVersion() {
        return paymentProviderDefinitionVersion;
    }

    public PaymentProviderEntity setPaymentProviderDefinitionVersion(@Nonnull Integer paymentProviderDefinitionVersion) {
        this.paymentProviderDefinitionVersion = paymentProviderDefinitionVersion;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public PaymentProviderEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    public PaymentProviderEntity setDescription(@Nonnull String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public Boolean getTestProvider() {
        return isTestProvider;
    }

    public PaymentProviderEntity setTestProvider(@Nonnull Boolean testProvider) {
        isTestProvider = testProvider;
        return this;
    }

    @Nonnull
    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public PaymentProviderEntity setIsEnabled(@Nonnull Boolean enabled) {
        isEnabled = enabled;
        return this;
    }

    @Nonnull
    public AuthoredElementValues getConfig() {
        return config;
    }

    public PaymentProviderEntity setConfig(@Nonnull AuthoredElementValues config) {
        this.config = config;
        return this;
    }

    // endregion
}
