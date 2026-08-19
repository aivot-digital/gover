package de.aivot.gover.backend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.util.List;
import java.util.UUID;

public record PaymentConfigElementValue(
        @Nullable
        UUID paymentProviderKey,
        @Nullable
        String purpose, // Supports Template Tag Rendering
        @Nullable
        String description, // Supports Template Tag Rendering
        @Nullable
        Boolean mapRequestor,
        @Nullable
        PaymentConfigElementValueRequestorMapping requestorMapping,
        @Nullable
        List<PaymentConfigElementValueItem> items,
        @Nullable
        String successMessage, // Supports Template Tag Rendering
        @Nullable
        String failureMessage // Supports Template Tag Rendering
) {
}
