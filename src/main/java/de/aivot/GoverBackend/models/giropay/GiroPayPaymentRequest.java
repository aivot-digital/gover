package de.aivot.GoverBackend.models.giropay;

import com.beust.jcommander.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldiensteRequestor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GiroPayPaymentRequest {
    private Integer merchantId;
    private Integer projectId;
    private String merchantTxId;
    private Integer amount;
    private String currency;
    private String purpose;
    private String type = "SALE";
    private String shoppingCartType = "AUTHORITIES_PAYMENT";
    private String shippingAddresseFirstName;
    private String shippingAddresseLastName;
    private String shippingCompany;
    private String shippingAdditionalAddressInformation;
    private String shippingStreet;
    private String shippingStreetNumber;
    private String shippingZipCode;
    private String shippingCity;
    private String shippingCountry;
    private String shippingEmail;
    private String merchantOrderReferenceNumber;
    private List<GiroPayPaymentItem> cart;
    private String deliveryType = "STANDARD";
    private String urlRedirect;
    private String urlNotify;
    private String kassenzeichen;
    private String giropayCustomerId;
    private String hash;

    public static GiroPayPaymentRequest valueOf(XBezahldienstePaymentRequest xRequest, String originatorId, String endpointId, String giroPayPassword) {
        GiroPayPaymentRequest gRequest = new GiroPayPaymentRequest();
        XBezahldiensteRequestor xRequester = xRequest.getRequestor();

        var originatorIdInt = Integer.parseInt(originatorId);
        var endpointIdInt = Integer.parseInt(endpointId);

        gRequest.setMerchantId(originatorIdInt);
        gRequest.setProjectId(endpointIdInt);
        gRequest.setMerchantTxId(xRequest.getRequestId());
        gRequest.setAmount(xRequest.getGrosAmount().multiply(BigDecimal.valueOf(100)).intValue()); // Convert to cents
        gRequest.setCurrency(xRequest.getCurrency());
        gRequest.setPurpose(xRequest.getPurpose());

        gRequest.setUrlRedirect(xRequest.getRedirectUrl());
        gRequest.setUrlNotify(xRequest.getRedirectUrl() + "/notify"); // TODO: Create new notify url endpoint

        if (xRequester != null) {
            gRequest.setShippingAddresseFirstName(xRequester.getFirstName());
            gRequest.setShippingAddresseLastName(xRequester.getLastName());
            gRequest.setShippingCompany(xRequester.getOrganizationName());

            var xAddress = xRequester.getAddress();
            if (xAddress != null) {
                gRequest.setShippingAdditionalAddressInformation(Strings.join("\n", xAddress.getAddressLine()));
                gRequest.setShippingStreet(xAddress.getStreet());
                gRequest.setShippingStreetNumber(xAddress.getHouseNumber());
                gRequest.setShippingZipCode(xAddress.getPostalCode());
                gRequest.setShippingCity(xAddress.getCity());
                gRequest.setShippingCountry(xAddress.getCountry());
            }

            // gRequest.setShippingEmail(null); // Vorerst nicht unterstützt
        }
        // gRequest.setMerchantOrderReferenceNumber(null); // Vorerst nicht unterstützt
        gRequest.setCart(xRequest.getItems().stream().map(GiroPayPaymentItem::valueOf).toList());

        // gRequest.setKassenzeichen(null); // Vorerst nicht unterstützt
        // gRequest.setGiropayCustomerId(null); // Vorerst nicht unterstützt

        try {
            gRequest.setHash(generateHash(gRequest, giroPayPassword));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        return gRequest;
    }

    public static String generateHash(GiroPayPaymentRequest gRequest, String projectPassword) throws NoSuchAlgorithmException, InvalidKeyException {
        StringBuilder sb = new StringBuilder();

        Consumer<Object> append = (Object o) -> {
            if (o != null) {
                sb.append(o);
            }
        };

        append.accept(gRequest.merchantId);
        append.accept(gRequest.projectId);
        append.accept(gRequest.merchantTxId);
        append.accept(gRequest.amount);
        append.accept(gRequest.currency);
        append.accept(gRequest.purpose);
        append.accept(gRequest.type);
        append.accept(gRequest.shoppingCartType);
        append.accept(gRequest.shippingAddresseFirstName);
        append.accept(gRequest.shippingAddresseLastName);
        append.accept(gRequest.shippingCompany);
        append.accept(gRequest.shippingAdditionalAddressInformation);
        append.accept(gRequest.shippingStreet);
        append.accept(gRequest.shippingStreetNumber);
        append.accept(gRequest.shippingZipCode);
        append.accept(gRequest.shippingCity);
        append.accept(gRequest.shippingCountry);
        append.accept(gRequest.shippingEmail);
        append.accept(gRequest.merchantOrderReferenceNumber);
        try {
            append.accept(new ObjectMapper().writeValueAsString(gRequest.cart));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        append.accept(gRequest.deliveryType);
        append.accept(gRequest.urlRedirect);
        append.accept(gRequest.urlNotify);
        append.accept(gRequest.kassenzeichen);
        append.accept(gRequest.giropayCustomerId);

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
        append.accept("type", type);
        append.accept("shoppingCartType", shoppingCartType);
        append.accept("shippingAddresseFirstName", shippingAddresseFirstName);
        append.accept("shippingAddresseLastName", shippingAddresseLastName);
        append.accept("shippingCompany", shippingCompany);
        append.accept("shippingAdditionalAddressInformation", shippingAdditionalAddressInformation);
        append.accept("shippingStreet", shippingStreet);
        append.accept("shippingStreetNumber", shippingStreetNumber);
        append.accept("shippingZipCode", shippingZipCode);
        append.accept("shippingCity", shippingCity);
        append.accept("shippingCountry", shippingCountry);
        append.accept("shippingEmail", shippingEmail);
        append.accept("merchantOrderReferenceNumber", merchantOrderReferenceNumber);
        try {
            append.accept("cart", new ObjectMapper().writeValueAsString(cart));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        append.accept("deliveryType", deliveryType);
        append.accept("urlRedirect", urlRedirect);
        append.accept("urlNotify", urlNotify);
        append.accept("kassenzeichen", kassenzeichen);
        append.accept("giropayCustomerId", giropayCustomerId);
        append.accept("hash", hash);

        return sb.toString();
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

    public void setType(String type) {
        if (type == null || type.length() != 4) {
            throw new IllegalArgumentException("Type cannot be null and must be a 4-character string");
        }
        this.type = type;
    }

    public void setShoppingCartType(String shoppingCartType) {
        if (shoppingCartType != null && shoppingCartType.length() > 19) {
            throw new IllegalArgumentException("Shopping Cart Type cannot be longer than 19 characters");
        }
        this.shoppingCartType = shoppingCartType;
    }

    public void setShippingAddresseFirstName(String shippingAddresseFirstName) {
        if (shippingAddresseFirstName != null && shippingAddresseFirstName.length() > 100) {
            throw new IllegalArgumentException("Shipping Address First Name cannot be longer than 100 characters");
        }
        this.shippingAddresseFirstName = shippingAddresseFirstName;
    }

    public void setShippingAddresseLastName(String shippingAddresseLastName) {
        if (shippingAddresseLastName != null && shippingAddresseLastName.length() > 100) {
            throw new IllegalArgumentException("Shipping Address Last Name cannot be longer than 100 characters");
        }
        this.shippingAddresseLastName = shippingAddresseLastName;
    }

    public void setShippingCompany(String shippingCompany) {
        if (shippingCompany != null && shippingCompany.length() > 100) {
            throw new IllegalArgumentException("Shipping Company cannot be longer than 100 characters");
        }
        this.shippingCompany = shippingCompany;
    }

    public void setShippingAdditionalAddressInformation(String shippingAdditionalAddressInformation) {
        if (shippingAdditionalAddressInformation != null && shippingAdditionalAddressInformation.length() > 100) {
            throw new IllegalArgumentException("Shipping Additional Address Information cannot be longer than 100 characters");
        }
        this.shippingAdditionalAddressInformation = shippingAdditionalAddressInformation;
    }

    public void setShippingStreet(String shippingStreet) {
        if (shippingStreet != null && shippingStreet.length() > 100) {
            throw new IllegalArgumentException("Shipping Street cannot be longer than 100 characters");
        }
        this.shippingStreet = shippingStreet;
    }

    public void setShippingStreetNumber(String shippingStreetNumber) {
        if (shippingStreetNumber != null && shippingStreetNumber.length() > 10) {
            throw new IllegalArgumentException("Shipping Street Number cannot be longer than 10 characters");
        }
        this.shippingStreetNumber = shippingStreetNumber;
    }

    public void setShippingZipCode(String shippingZipCode) {
        if (shippingZipCode != null && shippingZipCode.length() > 10) {
            throw new IllegalArgumentException("Shipping Zip Code cannot be longer than 10 characters");
        }
        this.shippingZipCode = shippingZipCode;
    }

    public void setShippingCity(String shippingCity) {
        if (shippingCity != null && shippingCity.length() > 100) {
            throw new IllegalArgumentException("Shipping City cannot be longer than 100 characters");
        }
        this.shippingCity = shippingCity;
    }

    public void setShippingCountry(String shippingCountry) {
        if (shippingCountry != null && shippingCountry.length() != 2) {
            throw new IllegalArgumentException("Shipping Country must be a 2-character string");
        }
        this.shippingCountry = shippingCountry;
    }

    public void setShippingEmail(String shippingEmail) {
        if (shippingEmail != null && shippingEmail.length() > 255) {
            throw new IllegalArgumentException("Shipping Email cannot be longer than 255 characters");
        }
        this.shippingEmail = shippingEmail;
    }

    public void setMerchantOrderReferenceNumber(String merchantOrderReferenceNumber) {
        if (merchantOrderReferenceNumber != null && merchantOrderReferenceNumber.length() > 20) {
            throw new IllegalArgumentException("Merchant Order Reference Number cannot be longer than 20 characters");
        }
        this.merchantOrderReferenceNumber = merchantOrderReferenceNumber;
    }

    public void setCart(List<GiroPayPaymentItem> cart) {
        this.cart = cart;
    }

    public void setDeliveryType(String deliveryType) {
        if (deliveryType != null && deliveryType.length() > 12) {
            throw new IllegalArgumentException("Delivery Type cannot be longer than 12 characters");
        }
        this.deliveryType = deliveryType;
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

    public void setKassenzeichen(String kassenzeichen) {
        if (kassenzeichen != null && kassenzeichen.length() > 255) {
            throw new IllegalArgumentException("Kassenzeichen cannot be longer than 255 characters");
        }
        this.kassenzeichen = kassenzeichen;
    }

    public void setGiropayCustomerId(String giropayCustomerId) {
        if (giropayCustomerId != null && giropayCustomerId.length() > 20) {
            throw new IllegalArgumentException("Giropay Customer ID cannot be longer than 20 characters");
        }
        this.giropayCustomerId = giropayCustomerId;
    }

    public void setHash(String hash) {
        if (hash == null || hash.length() != 32) {
            throw new IllegalArgumentException("Hash cannot be null and must be a 32-character string");
        }
        this.hash = hash;
    }

    public Integer getMerchantId() {
        return merchantId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public String getMerchantTxId() {
        return merchantTxId;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getType() {
        return type;
    }

    public String getShoppingCartType() {
        return shoppingCartType;
    }

    public String getShippingAddresseFirstName() {
        return shippingAddresseFirstName;
    }

    public String getShippingAddresseLastName() {
        return shippingAddresseLastName;
    }

    public String getShippingCompany() {
        return shippingCompany;
    }

    public String getShippingAdditionalAddressInformation() {
        return shippingAdditionalAddressInformation;
    }

    public String getShippingStreet() {
        return shippingStreet;
    }

    public String getShippingStreetNumber() {
        return shippingStreetNumber;
    }

    public String getShippingZipCode() {
        return shippingZipCode;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public String getShippingCountry() {
        return shippingCountry;
    }

    public String getShippingEmail() {
        return shippingEmail;
    }

    public String getMerchantOrderReferenceNumber() {
        return merchantOrderReferenceNumber;
    }

    public List<GiroPayPaymentItem> getCart() {
        return cart;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public String getUrlRedirect() {
        return urlRedirect;
    }

    public String getUrlNotify() {
        return urlNotify;
    }

    public String getKassenzeichen() {
        return kassenzeichen;
    }

    public String getGiropayCustomerId() {
        return giropayCustomerId;
    }

    public String getHash() {
        return hash;
    }
}
