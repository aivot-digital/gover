package de.aivot.gover.backend.form.dtos;

import de.aivot.gover.backend.payment.models.PaymentItem;

import java.math.BigDecimal;
import java.util.List;

public record FormCostCalculationResponseDTO(BigDecimal totalCost, List<PaymentItem> paymentItems, String paymentProviderName) {
}
