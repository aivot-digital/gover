package de.aivot.prosuna.backend.payment.dtos;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import jakarta.annotation.Nonnull;

public record PaymentProviderRequestDTO(
        @Nonnull
        @NotNull(message = "Geben Sie einen Namen ein.")
        @NotBlank(message = "Geben Sie einen Namen ein.")
        @Length(max = 64, message = "Der Name darf höchstens 64 Zeichen lang sein.")
        String name,

        @Nonnull
        @NotNull(message = "Geben Sie eine interne Beschreibung ein.")
        @NotBlank(message = "Geben Sie eine interne Beschreibung ein.")
        @Length(max = 255, message = "Die interne Beschreibung darf höchstens 255 Zeichen lang sein.")
        String description,

        @Nonnull
        @NotNull(message = "Wählen Sie einen Zahlungsanbieter aus.")
        @NotBlank(message = "Wählen Sie einen Zahlungsanbieter aus.")
        String providerKey,

        @Nonnull
        @NotNull(message = "Wählen Sie eine Version des Zahlungsanbieters aus.")
        @Min(value = 1, message = "Die Version des Zahlungsanbieters muss mindestens 1 sein.")
        Integer providerVersion,

        @Nonnull
        @NotNull(message = "Geben Sie an, ob der Zahlungsanbieter im Testbetrieb verwendet wird.")
        Boolean isTestProvider,

        @Nonnull
        @NotNull(message = "Geben Sie an, ob der Zahlungsanbieter aktiviert ist.")
        Boolean isEnabled,

        @Nonnull
        @NotNull(message = "Die Konfiguration des Zahlungsanbieters fehlt.")
        AuthoredElementValues config
) {
    public PaymentProviderEntity toEntity() {
        var entity = new PaymentProviderEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setPaymentProviderDefinitionKey(providerKey);
        entity.setPaymentProviderDefinitionVersion(providerVersion);
        entity.setTestProvider(isTestProvider);
        entity.setIsEnabled(isEnabled);
        entity.setConfig(config);
        return entity;
    }
}
