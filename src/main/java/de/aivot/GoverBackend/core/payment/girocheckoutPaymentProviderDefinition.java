package de.aivot.GoverBackend.core.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.payment.models.GiroPayCallbackResponse;
import de.aivot.GoverBackend.core.payment.models.GiroPayPaymentRequest;
import de.aivot.GoverBackend.core.payment.models.GiroPaymentStartResponse;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioFieldOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectField;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextField;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.exceptions.PaymentHttpRequestException;
import de.aivot.GoverBackend.payment.exceptions.PaymentMissingDataException;
import de.aivot.GoverBackend.payment.exceptions.PaymentSerializationException;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentTransaction;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class girocheckoutPaymentProviderDefinition implements PaymentProviderDefinition {
    private final static String MERCHANT_ID_FIELD = "sellerId";
    private final static String PROJECT_ID_FIELD = "projectId";
    private final static String PROJECT_PASSWORD_FIELD = "projectPasswordSecret";
    private final static String PAYMENT_URL = "https://payment.girosolution.de/girocheckout/api/v2/transaction/start";

    private final SecretService secretService;
    private final ScopedAuditService auditService;
    private final HttpService httpService;

    @Autowired
    public girocheckoutPaymentProviderDefinition(AuditService auditService, SecretService secretService, HttpService httpService) {
        this.auditService = auditService.createScopedAuditService(girocheckoutPaymentProviderDefinition.class);
        this.secretService = secretService;
        this.httpService = httpService;
    }

    @Nonnull
    @Override
    public String getKey() {
        return "girocheckout";
    }

    @Nonnull
    @Override
    public String getProviderName() {
        return "GiroCheckout";
    }

    @Nonnull
    @Override
    public String getProviderDescription() {
        return "Zahlungsdienstleister GiroCheckout";
    }

    @Nonnull
    @Override
    public GroupLayout getPaymentConfigLayout() throws ResponseException {
        var list = new LinkedList<BaseFormElement>();

        var sellerIdInput = new TextField();
        sellerIdInput.setType(ElementType.Text);
        sellerIdInput.setId(MERCHANT_ID_FIELD);
        sellerIdInput.setRequired(true);
        sellerIdInput.setLabel("Verkäufer-ID");
        sellerIdInput.setPlaceholder("Verkäufer-ID");
        sellerIdInput.setHint("Die Verkäufer-ID finden Sie in Ihrem GiroCockpit.");
        sellerIdInput.setWeight(6.0d);
        list.add(sellerIdInput);

        var projectIdInput = new TextField();
        projectIdInput.setType(ElementType.Text);
        projectIdInput.setId(PROJECT_ID_FIELD);
        projectIdInput.setRequired(true);
        projectIdInput.setLabel("Projekt-ID");
        projectIdInput.setPlaceholder("Projekt-ID");
        projectIdInput.setHint("Die Projekt-ID finden Sie in Ihrem GiroCockpit.");
        projectIdInput.setWeight(6.0d);
        list.add(projectIdInput);

        var projectPasswordInput = new SelectField();
        projectPasswordInput.setType(ElementType.Select);
        projectPasswordInput.setId(PROJECT_PASSWORD_FIELD);
        projectPasswordInput.setRequired(true);
        projectPasswordInput.setLabel("Projekt-Passwort");
        projectPasswordInput.setPlaceholder("Projekt-Passwort");
        projectPasswordInput.setHint("Das Projekt-Passwort finden Sie in Ihrem GiroCockpit. Es muss zuvor unter \"Geheimnisse\" hinterlegt werden, um hier auswählbar zu sein.");
        List<RadioFieldOption> clientSecretInputOptions = secretService
                .list()
                .stream()
                .map(secret -> new RadioFieldOption()
                        .setValue(secret.getKey().toString())
                        .setLabel(secret.getName())
                )
                .toList();
        projectPasswordInput.setOptions(clientSecretInputOptions);
        list.add(projectPasswordInput);

        var group = new GroupLayout();
        group.setType(ElementType.Group);
        group.setId("giroCheckoutConfig");
        group.setChildren(list);

        return group;
    }

    @Nonnull
    @Override
    public XBezahldienstePaymentTransaction initiatePayment(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull ElementData config,
            @Nonnull XBezahldienstePaymentRequest paymentRequest
    ) throws PaymentException {
        var merchantId = (String) config.get(MERCHANT_ID_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(merchantId)) {
            throw new PaymentMissingDataException("Merchant ID", paymentProviderEntity);
        }

        var projectId = (String) config.get(PROJECT_ID_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(projectId)) {
            throw new PaymentMissingDataException("Project ID", paymentProviderEntity);
        }

        var passwordSecret = getPasswordSecret(paymentProviderEntity, config);

        var notifyUrl = paymentRequest
                .getRedirectUrl()
                .replace("redirect", "notify");

        var giroPayPaymentRequest = GiroPayPaymentRequest
                .valueOf(paymentRequest, merchantId, projectId, passwordSecret, notifyUrl);

        var xFormUrlEncoded = giroPayPaymentRequest.toApplicationXWwwFormUrlEncoded();

        auditService.logMessage("Payment Request to GiroCheckout: " + xFormUrlEncoded, Map.of());

        HttpResponse<String> response;
        try {
            response = httpService
                    .post(
                            URI.create(PAYMENT_URL),
                            xFormUrlEncoded,
                            HttpServiceHeaders
                                    .create()
                                    .withContentType(HttpServiceHeaders.APPLICATION_X_WWW_FORM_URLENCODED)
                    );
        } catch (HttpConnectionException e) {
            throw new PaymentHttpRequestException(e, paymentProviderEntity, xFormUrlEncoded);
        }

        if (response.statusCode() != 200) {
            throw new PaymentHttpRequestException(
                    response.statusCode(),
                    paymentProviderEntity,
                    xFormUrlEncoded,
                    response.body()
            );
        }

        var objectMapper = new ObjectMapper();

        GiroPaymentStartResponse transaction;
        try {
            transaction = objectMapper
                    .readValue(response.body(), GiroPaymentStartResponse.class);
        } catch (JsonProcessingException e) {
            throw new PaymentSerializationException(e, "Failed to deserialize response body", response.body(), paymentProviderEntity);
        }

        if (transaction.getRc() != 0) {
            throw new PaymentHttpRequestException(
                    transaction.getRc(),
                    paymentProviderEntity,
                    xFormUrlEncoded,
                    response.body()
            );
        }

        return transaction.toXBezahldienstePaymentTransaction(paymentRequest, PAYMENT_URL);
    }

    @Nonnull
    @Override
    public XBezahldienstePaymentTransaction onPaymentResultPull(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull ElementData config,
            @Nonnull XBezahldienstePaymentTransaction paymentRequest
    ) throws PaymentException {
        // No implementation for checkPaymentStatus because GiroCheckout has no API for this
        return paymentRequest;
    }

    @Nonnull
    @Override
    public XBezahldienstePaymentTransaction onPaymentResultPush(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull ElementData config,
            @Nonnull XBezahldienstePaymentTransaction paymentTransaction,
            @Nonnull Map<String, Object> callbackData
    ) throws PaymentException {
        ObjectMapper objectMapper = new ObjectMapper();
        var callbackResponse = objectMapper
                .convertValue(callbackData, GiroPayCallbackResponse.class);

        var projectPassword = getPasswordSecret(paymentProviderEntity, config);

        String desiredHash;
        try {
            desiredHash = GiroPayCallbackResponse.generateHash(callbackResponse, projectPassword);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new PaymentException(
                    e,
                    "Failed to generate hash for callback response for provider %s (%s)",
                    paymentProviderEntity.getName(),
                    paymentProviderEntity.getKey()
            );
        }

        if (!desiredHash.equals(callbackResponse.getGcHash())) {
            throw new PaymentException(
                    "Hash mismatch for callback response for provider %s (%s)",
                    paymentProviderEntity.getName(),
                    paymentProviderEntity.getKey()
            );
        }

        var updatedPaymentInformation = callbackResponse.toXBezahldienstePaymentInformation(paymentTransaction.getPaymentInformation());

        paymentTransaction.setPaymentInformation(updatedPaymentInformation);

        return paymentTransaction;
    }


    @Nonnull
    private String getPasswordSecret(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull ElementData config
    ) throws PaymentMissingDataException {
        var passwordSecretField = (String) config.get(PROJECT_PASSWORD_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(passwordSecretField)) {
            throw new PaymentMissingDataException("Project password", paymentProviderEntity);
        }

        UUID passwordSecretFieldKey;
        try {
            passwordSecretFieldKey = UUID.fromString(passwordSecretField);
        } catch (IllegalArgumentException e) {
            throw new PaymentMissingDataException("Project password", paymentProviderEntity);
        }

        var passwordSecretEntity = secretService
                .retrieve(passwordSecretFieldKey)
                .orElseThrow(() -> new PaymentMissingDataException("Project password entity", paymentProviderEntity));

        String passwordSecret = null;
        try {
            passwordSecret = secretService
                    .decrypt(passwordSecretEntity);
        } catch (Exception e) {
            throw new PaymentMissingDataException("Project password value", paymentProviderEntity);
        }

        if (StringUtils.isNullOrEmpty(passwordSecret)) {
            throw new PaymentMissingDataException("Project password value", paymentProviderEntity);
        }

        return passwordSecret;
    }
}
