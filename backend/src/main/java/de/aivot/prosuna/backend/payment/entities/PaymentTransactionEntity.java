package de.aivot.prosuna.backend.payment.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.payment.models.PaymentInformation;
import de.aivot.prosuna.backend.payment.models.PaymentRequest;
import de.aivot.prosuna.backend.payment.models.PaymentStatus;
import de.aivot.prosuna.backend.utils.StringUtils;
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
    private PaymentRequest paymentRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    private PaymentInformation paymentInformation;

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
    public PaymentStatus getStatus() {
        return paymentInformation != null ? paymentInformation.status() : PaymentStatus.PENDING;
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

    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public PaymentTransactionEntity setPaymentRequest(PaymentRequest paymentRequest) {
        this.paymentRequest = paymentRequest;
        return this;
    }

    public PaymentInformation getPaymentInformation() {
        return paymentInformation;
    }

    public PaymentTransactionEntity setPaymentInformation(PaymentInformation paymentInformation) {
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
