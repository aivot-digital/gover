package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.payment.models.PaymentItem;

import java.math.BigDecimal;
import java.util.List;

public record FormCostCalculationResponseDTO(BigDecimal totalCost, List<PaymentItem> paymentItems, String paymentProviderName) {
}
