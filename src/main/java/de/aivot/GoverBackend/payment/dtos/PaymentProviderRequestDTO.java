package de.aivot.GoverBackend.payment.dtos;

import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import javax.annotation.Nonnull;
import java.util.Map;

public record PaymentProviderRequestDTO(
        @Nonnull
        @NotNull(message = "Name is required")
        @NotBlank(message = "Name cannot be blank")
        @Length(max = 64, message = "Name cannot be longer than 64 characters")
        String name,

        @Nonnull
        @NotNull(message = "Description is required")
        @NotBlank(message = "Description cannot be blank")
        @Length(max = 255, message = "Description cannot be longer than 255 characters")
        String description,

        @Nonnull
        @NotNull(message = "Provider key is required")
        @NotBlank(message = "Provider key cannot be blank")
        String providerKey,

        @Nonnull
        @NotNull(message = "Is test provider is required")
        Boolean isTestProvider,

        @Nonnull
        @NotNull(message = "Is enabled is required")
        Boolean isEnabled,

        @Nonnull
        @NotNull(message = "Config is required")
        Map<String, Object> config
) {
    public PaymentProviderEntity toEntity() {
        var entity = new PaymentProviderEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setProviderKey(providerKey);
        entity.setTestProvider(isTestProvider);
        entity.setIsEnabled(isEnabled);
        entity.setConfig(config);
        return entity;
    }
}
