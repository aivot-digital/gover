package de.aivot.GoverBackend.payment.entities;

import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransactionEntity {
    @Id
    @Column(length = 36)
    private String key;

    @NotNull
    @Column(length = 36)
    private String paymentProviderKey;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    private XBezahldienstePaymentRequest paymentRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    private XBezahldienstePaymentInformation paymentInformation;

    private String paymentError;

    @NotNull
    private String redirectUrl;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private LocalDateTime updated;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
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

    public String getPaymentProviderKey() {
        return paymentProviderKey;
    }

    public PaymentTransactionEntity setPaymentProviderKey(String paymentProviderKey) {
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

    public LocalDateTime getCreated() {
        return created;
    }

    public PaymentTransactionEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public PaymentTransactionEntity setUpdated(LocalDateTime updated) {
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
