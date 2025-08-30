package de.aivot.GoverBackend.payment.entities;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment_providers")
public class PaymentProviderEntity {
    @Id
    private UUID key;

    @NotNull
    @Column(length = 32)
    private String providerKey;

    @NotNull
    @Column(length = 64)
    private String name;

    @NotNull
    @Column(length = 255)
    private String description;

    @NotNull
    @ColumnDefault("FALSE")
    private Boolean isTestProvider;

    @NotNull
    @ColumnDefault("FALSE")
    private Boolean isEnabled;

    @NotNull
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> config;

    public UUID getKey() {
        return key;
    }

    public void setKey(UUID key) {
        this.key = key;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Boolean getTestProvider() {
        return isTestProvider;
    }

    public void setTestProvider(Boolean testProvider) {
        isTestProvider = testProvider;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
