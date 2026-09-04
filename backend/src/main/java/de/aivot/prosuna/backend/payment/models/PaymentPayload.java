package de.aivot.prosuna.backend.payment.models;

import java.math.BigDecimal;
import java.util.List;

public class PaymentPayload {
    private String purpose;
    private String description;
    private BigDecimal total;
    private List<PaymentItem> paymentItems;
    private PaymentRequestor requestor;

    public String getPurpose() {
        return purpose;
    }

    public PaymentPayload setPurpose(String purpose) {
        this.purpose = purpose;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PaymentPayload setDescription(String description) {
        this.description = description;
        return this;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public PaymentPayload setTotal(BigDecimal total) {
        this.total = total;
        return this;
    }

    public List<PaymentItem> getPaymentItems() {
        return paymentItems;
    }

    public PaymentPayload setPaymentItems(List<PaymentItem> paymentItems) {
        this.paymentItems = paymentItems;
        return this;
    }

    public PaymentRequestor getRequestor() {
        return requestor;
    }

    public PaymentPayload setRequestor(PaymentRequestor requestor) {
        this.requestor = requestor;
        return this;
    }
}
