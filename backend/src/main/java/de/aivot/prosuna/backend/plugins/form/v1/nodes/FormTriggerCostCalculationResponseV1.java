package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.payment.models.PaymentItem;

import java.math.BigDecimal;
import java.util.List;

public record FormTriggerCostCalculationResponseV1(BigDecimal totalCost, List<PaymentItem> paymentItems, String paymentProviderName) {
}
