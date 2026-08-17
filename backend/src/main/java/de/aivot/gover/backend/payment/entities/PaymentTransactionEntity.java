package de.aivot.gover.backend.payment.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.gover.backend.enums.XBezahldienstStatus;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransactionEntity {
    @JsonIgnore
    public static final int KEY_LENGTH = 128;

    @Id
    @Column(length = 128)
    private String key;

    @NotNull
    private UUID paymentProviderKey;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    private XBezahldienstePaymentRequest paymentRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    private XBezahldienstePaymentInformation paymentInformation;

    private String paymentError;

    @NotNull
    private String redirectUrl;

    @NotNull
    private Instant created;

    @NotNull
    private Instant updated;

    @PrePersist
    public void prePersist() {
        created = Instant.now();
        updated = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
    }

    @Nonnull
    public Boolean hasError() {
        return StringUtils.isNotNullOrEmpty(paymentError);
    }

    @Nonnull
    public XBezahldienstStatus getStatus() {
        return paymentInformation != null ? paymentInformation.getStatus() : XBezahldienstStatus.INITIAL;
    }

    public String getKey() {
        return key;
    }

    public PaymentTransactionEntity setKey(String key) {
        this.key = key;
        return this;
    }

    public UUID getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public PaymentTransactionEntity setPaymentProviderKey(UUID paymentProviderKey) {
        this.paymentProviderKey = paymentProviderKey;
        return this;
    }

    public XBezahldienstePaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public PaymentTransactionEntity setPaymentRequest(XBezahldienstePaymentRequest paymentRequest) {
        this.paymentRequest = paymentRequest;
        return this;
    }

    public XBezahldienstePaymentInformation getPaymentInformation() {
        return paymentInformation;
    }

    public PaymentTransactionEntity setPaymentInformation(XBezahldienstePaymentInformation paymentInformation) {
        this.paymentInformation = paymentInformation;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public PaymentTransactionEntity setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getUpdated() {
        return updated;
    }

    public PaymentTransactionEntity setUpdated(Instant updated) {
        this.updated = updated;
        return this;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public PaymentTransactionEntity setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
        return this;
    }

    public String getPaymentError() {
        return paymentError;
    }

    public PaymentTransactionEntity setPaymentError(String paymentError) {
        this.paymentError = paymentError;
        return this;
    }
}
