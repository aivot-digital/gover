package de.aivot.gover.backend.plugins.form.v1.nodes;

import de.aivot.gover.backend.payment.entities.PaymentProviderEntity;
import de.aivot.gover.backend.payment.models.PaymentItem;
import de.aivot.gover.backend.payment.models.PaymentPayload;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.util.List;

public record FormTriggerCostCalculationResponseV1(
        @Nonnull BigDecimal totalCost,
        @Nonnull List<PaymentItem> paymentItems,
        @Nonnull String paymentProviderName
) {
    public static FormTriggerCostCalculationResponseV1 empty() {
        return new FormTriggerCostCalculationResponseV1(
                BigDecimal.ZERO,
                List.of(),
                ""
        );
    }

    public static FormTriggerCostCalculationResponseV1 of(PaymentPayload request, PaymentProviderEntity provider) {
        return new FormTriggerCostCalculationResponseV1(
                request.getTotal(),
                request.getPaymentItems(),
                provider.getName()
        );
    }
}
