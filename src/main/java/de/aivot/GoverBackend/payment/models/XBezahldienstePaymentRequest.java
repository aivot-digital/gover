package de.aivot.GoverBackend.payment.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.aivot.GoverBackend.utils.MathUtils;
import de.aivot.GoverBackend.utils.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;


public class XBezahldienstePaymentRequest implements Serializable {
    // Die Request-ID wird vom Online-Dienst vergeben und vom Bezahldienst abgelegt.
    @JsonProperty("requestId")
    private String requestId = null;

    // Zeitstempel des Online-Antrags / Payment-Requests.
    @JsonProperty("requestTimestamp")
    private String requestTimestamp = null;

    // Im ersten Schritt ist hier nur 'EUR' vorgesehen. Perspektivisch sollte hier die entsprechende ISO-Norm referenziert werden.
    @JsonProperty("currency")
    private String currency = "EUR";

    // Der Gesamtbetrag dient dem Bezahldienst zur Validierung der einzelnen Positionen.
    @JsonProperty("grosAmount")
    private BigDecimal grosAmount = null;

    // Verwendungszweck / Buchungstext der Payment Transaction.
    @JsonProperty("purpose")
    private String purpose = null;

    // Die Beschreibung des Payment Requests. Kann auf der Paypage / PSP Seite angezeigt werden.
    @JsonProperty("description")
    private String description = null;

    // Rücksprung-URL vom Bezahldienst zum Online-Dienst. Der Status (Erfolg / Abbruch) muss über die Statusabfrage (GET) abgefragt werden.
    @JsonProperty("redirectUrl")
    private String redirectUrl = null;

    // Die Positionen des Payment Requests.
    @JsonProperty("items")
    private List<XBezahldienstePaymentItem> items = null;

    // Der Bezahler des Payment Requests.
    @JsonProperty("requestor")
    private XBezahldiensteRequestor requestor = null;

    public String getRequestId() {
        return requestId;
    }

    public void setRandomRequestId() {
        this.requestId = UUID.randomUUID().toString();
    }

    public void setRequestId(String requestId) {
        if (requestId != null) {
            this.requestId = StringUtils.cleanAndTruncate(
                    requestId,
                    "[^\\w\\d-]",
                    36
            );
        } else {
            this.requestId = null;
        }
    }

    public String getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(String requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public void setRequestTimestampNow() {
        this.requestTimestamp = ZonedDateTime
                .now()
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        if (currency != null) {
            this.currency = StringUtils.cleanAndTruncate(
                    currency,
                    "[\\W]",
                    3
            );
        } else {
            this.currency = null;
        }
    }

    public BigDecimal getGrosAmount() {
        return grosAmount;
    }

    public void setGrosAmount(BigDecimal grosAmount) {
        if (grosAmount != null) {
            this.grosAmount = MathUtils.bigDecimalValueBetween(BigDecimal.ZERO, BigDecimal.valueOf(999999), grosAmount);
        } else {
            this.grosAmount = null;
        }
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        if (purpose != null) {
            this.purpose = StringUtils.cleanAndTruncate(
                    purpose,
                    "[^\\w\\d\\ \\-]",
                    27
            );
        } else {
            this.purpose = null;
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

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public List<XBezahldienstePaymentItem> getItems() {
        return items;
    }

    public void setItems(List<XBezahldienstePaymentItem> items) {
        this.items = items;
    }

    public void setItemsAndCalculateGrosAmount(List<XBezahldienstePaymentItem> items) {
        this.items = items;
        this.grosAmount = items
                .stream()
                .map(i -> i.getTotalNetAmount().add(i.getTotalTaxAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public XBezahldiensteRequestor getRequestor() {
        return requestor;
    }

    public void setRequestor(XBezahldiensteRequestor requestor) {
        this.requestor = requestor;
    }
}
