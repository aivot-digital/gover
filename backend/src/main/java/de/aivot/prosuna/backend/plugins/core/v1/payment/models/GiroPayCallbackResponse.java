package de.aivot.prosuna.backend.plugins.core.v1.payment.models;

import de.aivot.prosuna.backend.payment.models.PaymentInformation;
import de.aivot.prosuna.backend.payment.models.PaymentMethod;
import de.aivot.prosuna.backend.payment.models.PaymentStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.function.Consumer;

public class GiroPayCallbackResponse {
    public static final String RESULT_PAYMENT_SUCCESS = "4000";

    private String gcReference;
    private String gcMerchantTxId;
    private String gcBackendTxId;
    private int gcAmount;
    private String gcCurrency;
    private String gcResultPayment;
    private String gcHash;

    public PaymentInformation toPaymentInformation(PaymentInformation existingInformation) {
        var paid = RESULT_PAYMENT_SUCCESS.equals(gcResultPayment);
        return new PaymentInformation(
                existingInformation.providerTransactionId(),
                gcReference,
                paid ? PaymentStatus.PAID : PaymentStatus.FAILED,
                null,
                paid ? Instant.now() : null,
                paid ? new PaymentMethod("GIROPAY", null) : null,
                gcResultPayment
        );
    }

    public static String generateHash(GiroPayCallbackResponse gReponse, String projectPassword) throws NoSuchAlgorithmException, InvalidKeyException {
        StringBuilder sb = new StringBuilder();

        Consumer<Object> append = (Object o) -> {
            if (o != null) {
                sb.append(o);
            }
        };

        append.accept(gReponse.getGcReference());
        append.accept(gReponse.getGcMerchantTxId());
        append.accept(gReponse.getGcBackendTxId());
        append.accept(gReponse.getGcAmount());
        append.accept(gReponse.getGcCurrency());
        append.accept(gReponse.getGcResultPayment());

        Mac mac = Mac.getInstance("HmacMD5");
        SecretKeySpec secretKeySpec = new SecretKeySpec(projectPassword.getBytes(StandardCharsets.UTF_8), "HmacMD5");
        mac.init(secretKeySpec);
        byte[] digest = mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : digest) {
            hash.append(String.format("%02x", b));
        }

        return hash.toString();
    }

    public String getGcReference() {
        return gcReference;
    }

    public void setGcReference(String gcReference) {
        this.gcReference = gcReference;
    }

    public String getGcMerchantTxId() {
        return gcMerchantTxId;
    }

    public void setGcMerchantTxId(String gcMerchantTxId) {
        this.gcMerchantTxId = gcMerchantTxId;
    }

    public String getGcBackendTxId() {
        return gcBackendTxId;
    }

    public void setGcBackendTxId(String gcBackendTxId) {
        this.gcBackendTxId = gcBackendTxId;
    }

    public int getGcAmount() {
        return gcAmount;
    }

    public void setGcAmount(int gcAmount) {
        this.gcAmount = gcAmount;
    }

    public String getGcCurrency() {
        return gcCurrency;
    }

    public void setGcCurrency(String gcCurrency) {
        this.gcCurrency = gcCurrency;
    }

    public String getGcResultPayment() {
        return gcResultPayment;
    }

    public void setGcResultPayment(String gcResultPayment) {
        this.gcResultPayment = gcResultPayment;
    }

    public String getGcHash() {
        return gcHash;
    }

    public void setGcHash(String gcHash) {
        this.gcHash = gcHash;
    }
}
