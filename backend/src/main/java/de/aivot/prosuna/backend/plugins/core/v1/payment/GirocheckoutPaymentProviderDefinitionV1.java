package de.aivot.prosuna.backend.plugins.core.v1.payment;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.core.exceptions.HttpConnectionException;
import de.aivot.prosuna.backend.core.models.HttpServiceHeaders;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SecretSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.exceptions.PaymentHttpRequestException;
import de.aivot.prosuna.backend.payment.exceptions.PaymentMissingDataException;
import de.aivot.prosuna.backend.payment.exceptions.PaymentSerializationException;
import de.aivot.prosuna.backend.payment.models.PaymentInformation;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.models.PaymentRequest;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.payment.models.GiroPayCallbackResponse;
import de.aivot.prosuna.backend.plugins.core.v1.payment.models.GiroPayPaymentRequest;
import de.aivot.prosuna.backend.plugins.core.v1.payment.models.GiroPaymentStartResponse;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;

import java.net.URI;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

@Component
public class GirocheckoutPaymentProviderDefinitionV1 implements PaymentProviderDefinition {
    private final static String MERCHANT_ID_FIELD = "sellerId";
    private final static String PROJECT_ID_FIELD = "projectId";
    private final static String PROJECT_PASSWORD_FIELD = "projectPasswordSecret";
    private final static String PAYMENT_URL = "https://payment.girosolution.de/girocheckout/api/v2/transaction/start";

    private final SecretService secretService;
    private final ScopedAuditService auditService;
    private final HttpService httpService;

    @Autowired
    public GirocheckoutPaymentProviderDefinitionV1(AuditService auditService, SecretService secretService, HttpService httpService) {
        this.auditService = auditService.createScopedAuditService(GirocheckoutPaymentProviderDefinitionV1.class, "Zahlungen");
        this.secretService = secretService;
        this.httpService = httpService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "girocheckout";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "GiroCheckout Zahlungsanbieter";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Zahlungsanbieter GiroCheckout für Zahlungen über GiroPay, Sofortüberweisung, PayPal und Kreditkarte.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Integriert den Zahlungsdienstleister GiroCheckout in die Zahlungsabwicklung von Prosuna.

                Die Komponente erstellt Zahlungsanfragen für die von GiroCheckout unterstützten Zahlarten GiroPay, Sofortüberweisung, PayPal und Kreditkarte. Rückmeldungen des Dienstleisters werden in den Prosuna-Zahlungsstatus übernommen und können im weiteren Prozess verarbeitet werden.
                """;
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
    public GroupLayoutElement getPaymentConfigLayout() throws ResponseException {
        var list = new LinkedList<BaseFormElement>();

        var sellerIdInput = new TextInputElement();
        sellerIdInput.setType(ElementType.Text);
        sellerIdInput.setId(MERCHANT_ID_FIELD);
        sellerIdInput.setRequired(true);
        sellerIdInput.setLabel("Verkäufer-ID");
        sellerIdInput.setPlaceholder("Verkäufer-ID");
        sellerIdInput.setHint("Die Verkäufer-ID finden Sie in Ihrem GiroCockpit.");
        sellerIdInput.setWeight(6.0d);
        list.add(sellerIdInput);

        var projectIdInput = new TextInputElement();
        projectIdInput.setType(ElementType.Text);
        projectIdInput.setId(PROJECT_ID_FIELD);
        projectIdInput.setRequired(true);
        projectIdInput.setLabel("Projekt-ID");
        projectIdInput.setPlaceholder("Projekt-ID");
        projectIdInput.setHint("Die Projekt-ID finden Sie in Ihrem GiroCockpit.");
        projectIdInput.setWeight(6.0d);
        list.add(projectIdInput);

        var projectPasswordInput = new SecretSelectInputElement();
        projectPasswordInput.setId(PROJECT_PASSWORD_FIELD);
        projectPasswordInput.setRequired(true);
        projectPasswordInput.setLabel("Projekt-Passwort");
        projectPasswordInput.setPlaceholder("Projekt-Passwort");
        projectPasswordInput.setHint("Das Projekt-Passwort finden Sie in Ihrem GiroCockpit. Es muss zuvor unter \"Geheimnisse\" hinterlegt werden, um hier auswählbar zu sein.");
        list.add(projectPasswordInput);

        var group = new GroupLayoutElement();
        group.setType(ElementType.GroupLayout);
        group.setId("giroCheckoutConfig");
        group.setChildren(list);

        return group;
    }

    @Nonnull
    @Override
    public PaymentInformation initiatePayment(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull PaymentRequest paymentRequest
    ) throws PaymentException {
        var effectiveValues = config.getEffectiveValues();

        var merchantId = (String) effectiveValues.get(MERCHANT_ID_FIELD);
        if (StringUtils.isNullOrEmpty(merchantId)) {
            throw new PaymentMissingDataException("Merchant ID", paymentProviderEntity);
        }

        var projectId = (String) effectiveValues.get(PROJECT_ID_FIELD);
        if (StringUtils.isNullOrEmpty(projectId)) {
            throw new PaymentMissingDataException("Project ID", paymentProviderEntity);
        }

        var passwordSecret = getPasswordSecret(paymentProviderEntity, config);

        var notifyUrl = paymentRequest
                .redirectUrl()
                .toString()
                .replace("redirect", "notify");

        var giroPayPaymentRequest = GiroPayPaymentRequest
                .valueOf(paymentRequest, merchantId, projectId, passwordSecret, notifyUrl);

        var xFormUrlEncoded = giroPayPaymentRequest.toApplicationXWwwFormUrlEncoded();

        auditService.create()
                .withSystem()
                .setMessage("Eine Zahlungsanfrage wurde an GiroCheckout übermittelt.")
                .setMetadata(Map.of(
                        "paymentProviderKey", String.valueOf(paymentProviderEntity.getKey()),
                        "paymentProviderName", paymentProviderEntity.getName(),
                        "requestBody", xFormUrlEncoded
                )).log();

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

        var objectMapper = JsonMapperFactory
                .getInstance();

        GiroPaymentStartResponse transaction;
        try {
            transaction = objectMapper
                    .readValue(response.body(), GiroPaymentStartResponse.class);
        } catch (JacksonException e) {
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

        return transaction.toPaymentInformation();
    }

    @Nonnull
    @Override
    public PaymentInformation onPaymentResultPull(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull PaymentInformation paymentInformation
    ) throws PaymentException {
        // No implementation for checkPaymentStatus because GiroCheckout has no API for this
        return paymentInformation;
    }

    @Nonnull
    @Override
    public PaymentInformation onPaymentResultPush(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull PaymentInformation paymentInformation,
            @Nonnull Map<String, Object> callbackData
    ) throws PaymentException {
        var objectMapper = JsonMapperFactory
                .getInstance();
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

        return callbackResponse.toPaymentInformation(paymentInformation);
    }


    @Nonnull
    private String getPasswordSecret(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config
    ) throws PaymentMissingDataException {
        var passwordSecretField = (String) config.getEffectiveValues().get(PROJECT_PASSWORD_FIELD);
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
