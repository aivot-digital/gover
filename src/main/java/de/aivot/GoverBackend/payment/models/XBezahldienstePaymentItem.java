package de.aivot.GoverBackend.payment.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aivot.GoverBackend.utils.MathUtils;
import de.aivot.GoverBackend.utils.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class XBezahldienstePaymentItem implements Serializable {
    // ID der Position vom Online-Dienst vergeben
    @JsonProperty("id")
    private String id = null;

    // Eine fachliche Referenz auf den "Artikel" bzw. den Inhalt der Position.
    @JsonProperty("reference")
    private String reference = null;

    // Beschreibung der Position vom Online-Dienst zur Verwendung z.B. auf der Pay-Page.
    @JsonProperty("description")
    private String description = null;

    // Steuersatz als Prozentbetrag
    @JsonProperty("taxRate")
    private BigDecimal taxRate = null;

    // Menge der Position
    @JsonProperty("quantity")
    private Long quantity = null;

    // Nettobetrag der Position
    @JsonProperty("totalNetAmount")
    private BigDecimal totalNetAmount = null;

    // Steuerbetrag der Position
    @JsonProperty("totalTaxAmount")
    private BigDecimal totalTaxAmount = null;

    // Nettobetrag der Position
    @JsonProperty("singleNetAmount")
    private BigDecimal singleNetAmount = null;

    // Steuerbetrag der Position
    @JsonProperty("singleTaxAmount")
    private BigDecimal singleTaxAmount = null;

    // Container für Key-Value-Paare für zusätzliche Informationen für die Verbuchung im Bezahldienst oder nachgelagerten Systemen
    @JsonProperty("bookingData")
    private Map<String, String> bookingData = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id != null) {
            this.id = StringUtils.cleanAndTruncate(
                    id,
                    "[^\\w\\d-]",
                    36
            );
        } else {
            this.id = null;
        }
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        if (reference != null) {
            this.reference = StringUtils.cleanAndTruncate(
                    reference,
                    "[^\\w\\d-]",
                    36
            );
        } else {
            this.reference = null;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null) {
            this.description = StringUtils.cleanAndTruncate(
                    description,
                    "[^\\w\\d\\s-,\\.\\u00C0-\\u017F]",
                    250
            );
        } else {
            this.description = null;
        }
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        if (taxRate != null) {
            this.taxRate = MathUtils.bigDecimalValueBetween(BigDecimal.ZERO, BigDecimal.valueOf(100), taxRate);
        } else {
            this.taxRate = null;
        }
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        if (quantity != null) {
            this.quantity = MathUtils.longValueBetween(1, 999999, quantity);
        } else {
            this.quantity = null;
        }
    }

    public BigDecimal getTotalNetAmount() {
        return totalNetAmount;
    }

    public void setTotalNetAmount(BigDecimal totalNetAmount) {
        if (totalNetAmount != null) {
            this.totalNetAmount = MathUtils.bigDecimalValueBetween(BigDecimal.ZERO, BigDecimal.valueOf(999999), totalNetAmount);
        } else {
            this.totalNetAmount = null;
        }
    }

    public BigDecimal getTotalTaxAmount() {
        return totalTaxAmount;
    }

    public void setTotalTaxAmount(BigDecimal totalTaxAmount) {
        if (totalTaxAmount != null) {
            this.totalTaxAmount = MathUtils.bigDecimalValueBetween(BigDecimal.ZERO, BigDecimal.valueOf(999999), totalTaxAmount);
        } else {
            this.totalTaxAmount = null;
        }
    }

    public BigDecimal getSingleNetAmount() {
        return singleNetAmount;
    }

    public void setSingleNetAmount(BigDecimal singleNetAmount) {
        if (singleNetAmount != null) {
            this.singleNetAmount = MathUtils.bigDecimalValueBetween(BigDecimal.ZERO, BigDecimal.valueOf(999999), singleNetAmount);
        } else {
            this.singleNetAmount = null;
        }
    }

    public BigDecimal getSingleTaxAmount() {
        return singleTaxAmount;
    }

    public void setSingleTaxAmount(BigDecimal singleTaxAmount) {
        if (singleTaxAmount != null) {
            this.singleTaxAmount = MathUtils.bigDecimalValueBetween(BigDecimal.ZERO, BigDecimal.valueOf(999999), singleTaxAmount);
        } else {
            this.singleTaxAmount = null;
        }
    }

    public Map<String, String> getBookingData() {
        return bookingData;
    }

    public void setBookingData(Map<String, String> bookingData) {
        this.bookingData = bookingData;
    }

    public void addBookingData(String key, String value) {
        if (bookingData == null) {
            bookingData = new HashMap<>();
        }
        bookingData.put(
                key,
                StringUtils.cleanAndTruncate(
                        value,
                        "[^\\w\\d\\s-,\\.\\u00C0-\\u017F]",
                        250
                )
        );
    }

    @JsonIgnore
    public BigDecimal getBruttoAmount() {
        return getTotalNetAmount().add(getTotalTaxAmount());
    }
}
