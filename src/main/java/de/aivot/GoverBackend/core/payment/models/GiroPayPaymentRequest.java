package de.aivot.GoverBackend.core.payment.models;

import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GiroPayPaymentRequest {
    private Integer merchantId;
    private Integer projectId;
    private String merchantTxId;
    private Integer amount;
    private String currency;
    private String purpose;
    private String urlRedirect;
    private String urlNotify;
    private String hash;

    public static GiroPayPaymentRequest valueOf(XBezahldienstePaymentRequest xRequest, String merchantId, String projectId, String giroPayPassword, String notifyUrl) {
        var gRequest = new GiroPayPaymentRequest();

        var merchantIdInt = Integer.parseInt(merchantId);
        var projectIdInt = Integer.parseInt(projectId);

        gRequest.setMerchantId(merchantIdInt);
        gRequest.setProjectId(projectIdInt);

        gRequest.setMerchantTxId(xRequest.getRequestId());
        gRequest.setAmount(xRequest.getGrosAmount().multiply(BigDecimal.valueOf(100)).intValue()); // Convert to cents
        gRequest.setCurrency(xRequest.getCurrency());
        gRequest.setPurpose(xRequest.getPurpose());

        gRequest.setUrlRedirect(xRequest.getRedirectUrl());
        gRequest.setUrlNotify(notifyUrl);

        String hash;
        try {
            hash = generateHash(gRequest, giroPayPassword);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        gRequest.setHash(hash);

        return gRequest;
    }

    public static String generateHash(GiroPayPaymentRequest gRequest, String projectPassword) throws NoSuchAlgorithmException, InvalidKeyException {
        var sb = new StringBuilder();

        Consumer<Object> append = (Object o) -> {
            if (o != null) {
                sb.append(o);
            } else {
                throw new IllegalArgumentException("Cannot generate hash with null values");
            }
        };

        append.accept(gRequest.merchantId);
        append.accept(gRequest.projectId);
        append.accept(gRequest.merchantTxId);
        append.accept(gRequest.amount);
        append.accept(gRequest.currency);
        append.accept(gRequest.purpose);
        append.accept(gRequest.urlRedirect);
        append.accept(gRequest.urlNotify);

        var mac = Mac.getInstance("HmacMD5");
        var secretKeySpec = new SecretKeySpec(projectPassword.getBytes(StandardCharsets.UTF_8), "HmacMD5");
        mac.init(secretKeySpec);
        byte[] digest = mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));
        var hash = new StringBuilder();
        for (byte b : digest) {
            hash.append(String.format("%02x", b));
        }

        return hash.toString();
    }

    public String toApplicationXWwwFormUrlEncoded() {
        var sb = new StringBuilder();

        BiConsumer<String, Object> append = (String key, Object value) -> {
            if (value != null) {
                sb.append(key).append("=").append(value).append("&");
            }
        };

        append.accept("merchantId", merchantId);
        append.accept("projectId", projectId);
        append.accept("merchantTxId", merchantTxId);
        append.accept("amount", amount);
        append.accept("currency", currency);
        append.accept("purpose", purpose);
        append.accept("urlRedirect", urlRedirect);
        append.accept("urlNotify", urlNotify);
        append.accept("hash", hash);

        return sb
                .toString()
                .replaceAll("&$", "");
    }

    public void setMerchantId(Integer merchantId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        this.merchantId = merchantId;
    }

    public void setProjectId(Integer projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }
        this.projectId = projectId;
    }

    public void setMerchantTxId(String merchantTxId) {
        if (merchantTxId == null || merchantTxId.length() > 255) {
            throw new IllegalArgumentException("Merchant Transaction ID cannot be null or longer than 255 characters");
        }
        this.merchantTxId = merchantTxId;
    }

    public void setAmount(Integer amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency cannot be null and must be a 3-character string");
        }
        this.currency = currency;
    }

    public void setPurpose(String purpose) {
        if (purpose == null || purpose.length() > 27) {
            throw new IllegalArgumentException("Purpose cannot be null or longer than 27 characters");
        }
        this.purpose = purpose;
    }

    public void setUrlRedirect(String urlRedirect) {
        if (urlRedirect == null || urlRedirect.length() > 2048) {
            throw new IllegalArgumentException("URL Redirect cannot be null or longer than 2048 characters");
        }
        this.urlRedirect = urlRedirect;
    }

    public void setUrlNotify(String urlNotify) {
        if (urlNotify == null || urlNotify.length() > 2048) {
            throw new IllegalArgumentException("URL Notify cannot be null or longer than 2048 characters");
        }
        this.urlNotify = urlNotify;
    }

    public void setHash(String hash) {
        if (hash == null || hash.length() != 32) {
            throw new IllegalArgumentException("Hash cannot be null and must be a 32-character string");
        }
        this.hash = hash;
    }
}
