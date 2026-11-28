package de.aivot.prosuna.backend.plugins.core.v1.payment.models;

import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;

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

    /**
     * Converts an amount to the cents that GiroCheckout expects and reports back.
     */
    public static int toCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).intValue();
    }

    public static GiroPayPaymentRequest valueOf(XBezahldienstePaymentRequest xRequest, String merchantId, String projectId, String giroPayPassword, String notifyUrl) {
        var gRequest = new GiroPayPaymentRequest();

        var merchantIdInt = Integer.parseInt(merchantId);
        var projectIdInt = Integer.parseInt(projectId);

        gRequest.setMerchantId(merchantIdInt);
        gRequest.setProjectId(projectIdInt);

        gRequest.setMerchantTxId(xRequest.getRequestId());
        gRequest.setAmount(toCents(xRequest.getGrosAmount()));
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
                throw new IllegalArgumentException("Die Prüfsumme der Zahlungsanforderung kann nicht berechnet werden, weil Pflichtangaben fehlen.");
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
            throw new IllegalArgumentException("Die Verkäufer-ID muss angegeben werden.");
        }
        this.merchantId = merchantId;
    }

    public void setProjectId(Integer projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Die Projekt-ID muss angegeben werden.");
        }
        this.projectId = projectId;
    }

    public void setMerchantTxId(String merchantTxId) {
        if (merchantTxId == null || merchantTxId.length() > 255) {
            throw new IllegalArgumentException("Die Transaktionskennung muss angegeben werden und darf höchstens 255 Zeichen enthalten.");
        }
        this.merchantTxId = merchantTxId;
    }

    public void setAmount(Integer amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Der Zahlungsbetrag muss angegeben werden.");
        }
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Die Währung muss als Code mit genau drei Zeichen angegeben werden.");
        }
        this.currency = currency;
    }

    public void setPurpose(String purpose) {
        if (purpose == null || purpose.length() > 27) {
            throw new IllegalArgumentException("Der Verwendungszweck muss angegeben werden und darf höchstens 27 Zeichen enthalten.");
        }
        this.purpose = purpose;
    }

    public void setUrlRedirect(String urlRedirect) {
        if (urlRedirect == null || urlRedirect.length() > 2048) {
            throw new IllegalArgumentException("Die Weiterleitungs-URL muss angegeben werden und darf höchstens 2048 Zeichen enthalten.");
        }
        this.urlRedirect = urlRedirect;
    }

    public void setUrlNotify(String urlNotify) {
        if (urlNotify == null || urlNotify.length() > 2048) {
            throw new IllegalArgumentException("Die Benachrichtigungs-URL muss angegeben werden und darf höchstens 2048 Zeichen enthalten.");
        }
        this.urlNotify = urlNotify;
    }

    public void setHash(String hash) {
        if (hash == null || hash.length() != 32) {
            throw new IllegalArgumentException("Die Prüfsumme muss angegeben werden und genau 32 Zeichen enthalten.");
        }
        this.hash = hash;
    }
}
