package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.gover.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.models.PaymentItem;
import de.aivot.gover.backend.payment.models.PaymentPayload;
import de.aivot.gover.backend.payment.models.PaymentProviderDefinition;
import jakarta.annotation.Nonnull;
import kotlin.jvm.internal.BooleanSpreadBuilder;

import java.math.BigDecimal;
import java.util.List;

public record FormTriggerCostCalculationResponseV1(
        @Nonnull BigDecimal totalCost,
        @Nonnull Boolean hasTaxes,
        @Nonnull List<PaymentItem> paymentItems,
        @Nonnull String paymentProviderName
) {
    public static FormTriggerCostCalculationResponseV1 empty() {
        return new FormTriggerCostCalculationResponseV1(
                BigDecimal.ZERO,
                false,
                List.of(),
                ""
        );
    }

    public static FormTriggerCostCalculationResponseV1 of(PaymentPayload request, PaymentProviderDefinition provider) {
        return new FormTriggerCostCalculationResponseV1(
                request.getTotal(),
                request
                        .getPaymentItems()
                        .stream()
                        .map(PaymentItem::getTaxRate)
                        .map(BigDecimal.ZERO::compareTo)
                        .anyMatch(compareResult -> compareResult != 0),
                request.getPaymentItems(),
                provider.getProviderName()
        );
    }
}
