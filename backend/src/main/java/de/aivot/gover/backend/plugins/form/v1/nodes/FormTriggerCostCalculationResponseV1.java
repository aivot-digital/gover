package de.aivot.gover.backend.plugins.form.v1.nodes;

import de.aivot.gover.backend.payment.entities.PaymentProviderEntity;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentItem;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentRequest;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.util.List;

public record FormTriggerCostCalculationResponseV1(
        @Nonnull BigDecimal totalCost,
        @Nonnull List<XBezahldienstePaymentItem> paymentItems,
        @Nonnull String paymentProviderName
) {
    public static FormTriggerCostCalculationResponseV1 empty() {
        return new FormTriggerCostCalculationResponseV1(
                BigDecimal.ZERO,
                List.of(),
                ""
        );
    }

    public static FormTriggerCostCalculationResponseV1 of(XBezahldienstePaymentRequest request, PaymentProviderEntity provider) {
        return new FormTriggerCostCalculationResponseV1(
                request.getGrosAmount(),
                request.getItems(),
                provider.getName()
        );
    }
}
