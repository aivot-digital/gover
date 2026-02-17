package de.aivot.GoverBackend.plugins.core.v1.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElementPattern;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.exceptions.PaymentHttpRequestException;
import de.aivot.GoverBackend.payment.exceptions.PaymentMissingDataException;
import de.aivot.GoverBackend.payment.exceptions.PaymentSerializationException;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentTransaction;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

@Component
public class pmPaymentPaymentProviderDefinition implements PaymentProviderDefinition, PluginComponent {
    private final static String ORIGINATOR_ID_FIELD = "originatorId";
    private final static String ENDPOINT_ID_FIELD = "endpointId";
    private final static String CLIENT_ID_FIELD = "clientId";
    private final static String CLIENT_SECRET_FIELD = "clientSecret";
    private final static String OAUTH_URL_FIELD = "oauthUrl";
    private final static String PAYMENT_TRANSACTION_URL_FIELD = "paymentTransactionUrl";

    private final SecretService secretService;
    private final HttpService httpService;

    @Autowired
    public pmPaymentPaymentProviderDefinition(SecretService secretService, HttpService httpService) {
        this.secretService = secretService;
        this.httpService = httpService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "pmpayment";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "pmPayment Zahlungsdienstleister";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Zahlungsdienstleister pmPayment zur Abwicklung von Zahlungen gemäß XBezahldienste-Standard.";
    }

    @Nonnull
    @Override
    public String getProviderName() {
        return "pmPayment";
    }

    @Nonnull
    @Override
    public String getProviderDescription() {
        return "Zahlungsdienstleister pmPayment";
    }

    @Nonnull
    @Override
    public GroupLayoutElement getPaymentConfigLayout() throws ResponseException {
        var list = new LinkedList<BaseFormElement>();

        var originatorIdInput = new TextInputElement()
                .setPlaceholder("Originator ID")
                .setRequired(true)
                .setLabel("Originator ID")
                .setHint("Diese ID wird vom Zahlungsdienstleister festgelegt. Üblicherweise ist dies eine Kennzeichnung für das Formular wie z.B. der Formularname, eine LeiKa-ID etc.")
                .setWeight(6.0d)
                .setId(ORIGINATOR_ID_FIELD);
        list.add((BaseFormElement) originatorIdInput);

        var endpointIdInput = new TextInputElement();
        endpointIdInput.setId(ENDPOINT_ID_FIELD);
        endpointIdInput.setRequired(true);
        endpointIdInput.setLabel("Endpoint ID");
        endpointIdInput.setPlaceholder("Endpoint ID");
        endpointIdInput.setHint("Diese ID wird vom Zahlungsdienstleister festgelegt. Üblicherweise ist dies eine Kennzeichnung der zuständigen Stelle z.B. der Ortsname, Amtlicher Regionalschlüssel etc.");
        endpointIdInput.setWeight(6.0d);
        list.add(endpointIdInput);

        var clientIdInput = new TextInputElement();
        clientIdInput.setId(CLIENT_ID_FIELD);
        clientIdInput.setRequired(true);
        clientIdInput.setLabel("OAuth Client-ID");
        clientIdInput.setPlaceholder("Client ID");
        clientIdInput.setHint("Die OAuth Client ID aus dem pmPayment-System.");
        clientIdInput.setWeight(6.0d);
        list.add(clientIdInput);

        var clientSecretInput = new SelectInputElement();
        clientSecretInput.setId(CLIENT_SECRET_FIELD);
        clientSecretInput.setRequired(true);
        clientSecretInput.setLabel("OAuth Client Secret");
        clientSecretInput.setPlaceholder("Client Secret");
        clientSecretInput.setHint("Das OAuth Client Secret aus dem pmPayment-System. Es muss zuvor unter \"Geheimnisse\" hinterlegt werden, um hier auswählbar zu sein.");
        var clientSecretInputOptions = secretService
                .list()
                .stream()
                .map(secret -> new RadioInputElementOption()
                        .setValue(secret.getKey().toString())
                        .setLabel(secret.getName())
                )
                .toList();
        clientSecretInput.setOptions(clientSecretInputOptions);
        clientSecretInput.setWeight(6.0d);
        list.add(clientSecretInput);

        TextInputElementPattern urlPattern = new TextInputElementPattern()
                .setRegex("^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([\\/\\w .-]*)*\\/?$")
                .setMessage("Bitte geben Sie eine gültige URL ein (z. B. https://example.com).");

        var oauthUrlInput = new TextInputElement();
        oauthUrlInput.setId(OAUTH_URL_FIELD);
        oauthUrlInput.setRequired(true);
        oauthUrlInput.setLabel("OAuth URL");
        oauthUrlInput.setPlaceholder("https://payment-test.govconnect.de/oauth/token/");
        oauthUrlInput.setHint("Die OAuth-URL des Zielsystems. Diese wird vom Zahlungsdienstleister bereitgestellt.");
        oauthUrlInput.setPattern(urlPattern);
        list.add(oauthUrlInput);

        var paymentTransactionUrlInput = new TextInputElement();
        paymentTransactionUrlInput.setId(PAYMENT_TRANSACTION_URL_FIELD);
        paymentTransactionUrlInput.setRequired(true);
        paymentTransactionUrlInput.setLabel("Basis-URL");
        paymentTransactionUrlInput.setPlaceholder("https://payment-test.govconnect.de/");
        paymentTransactionUrlInput.setHint("Die Basis-URL des Zielsystems gemäß XBezahldienste-Standard. Diese wird vom Zahlungsdienstleister bereitgestellt.");
        paymentTransactionUrlInput.setPattern(urlPattern);
        list.add(paymentTransactionUrlInput);

        var group = new GroupLayoutElement();
        group.setId("pmPaymentConfig");
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
        var accessToken = getAccessToken(config, paymentProviderEntity);

        var originatorID = (String) config.get(ORIGINATOR_ID_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(originatorID)) {
            throw new PaymentException("Originator ID for payment provider %s is missing", getProviderName());
        }

        var endpointID = (String) config.get(ENDPOINT_ID_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(endpointID)) {
            throw new PaymentException("Endpoint ID for payment provider %s is missing", getProviderName());
        }

        var paymentTransactionUrl = (String) config.get(PAYMENT_TRANSACTION_URL_FIELD).getValue();
        var normalizedPaymentTransactionUrl = StringUtils.normalizeUrl(paymentTransactionUrl);

        var objectMapper = ObjectMapperFactory
                .getInstance();

        String body;
        try {
            body = objectMapper
                    .writeValueAsString(paymentRequest);
        } catch (JsonProcessingException e) {
            throw new PaymentException(e, "Failed to serialize payment request");
        }

        var paymentPath = String
                .format("%spaymenttransaction/%s/%s", normalizedPaymentTransactionUrl, originatorID, endpointID);

        HttpResponse<String> response;
        try {
            response = httpService
                    .post(
                            URI.create(paymentPath),
                            body,
                            HttpServiceHeaders
                                    .create()
                                    .withContentType(HttpServiceHeaders.APPLICATION_JSON)
                                    .withAccept(HttpServiceHeaders.APPLICATION_JSON)
                                    .withAuthorizationBearer(accessToken)
                    );
        } catch (HttpConnectionException e) {
            throw new PaymentException(e, "Failed to poll payment from payment provider %s", getProviderName());
        }

        if (response.statusCode() != 200) {
            throw new PaymentException(
                    "Failed to poll payment from payment provider %s. Status code was %d with body %s",
                    getProviderName(),
                    response.statusCode(),
                    response.body()
            );
        }

        try {
            return objectMapper.readValue(response.body(), XBezahldienstePaymentTransaction.class);
        } catch (JsonProcessingException e) {
            throw new PaymentException(e, "Failed to deserialize payment transaction");
        }
    }

    @Nonnull
    @Override
    public XBezahldienstePaymentTransaction onPaymentResultPull(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull ElementData config,
            @Nonnull XBezahldienstePaymentTransaction transaction
    ) throws PaymentException {
        var accessToken = getAccessToken(config, paymentProviderEntity);

        var originatorID = getOriginatorID(paymentProviderEntity, config);
        var endpointID = getEndpointID(paymentProviderEntity, config);
        var normalizedPaymentTransactionUrl = getNormalizedPaymentTransactionUrl(paymentProviderEntity, config);

        var paymentPath = String
                .format("%spaymenttransaction/%s/%s/%s", normalizedPaymentTransactionUrl, originatorID, endpointID, transaction.getPaymentInformation().getTransactionId());

        HttpResponse<String> response;
        try {
            response = httpService
                    .get(
                            URI.create(paymentPath),
                            HttpServiceHeaders
                                    .create()
                                    .withContentType(HttpServiceHeaders.APPLICATION_JSON)
                                    .withAccept(HttpServiceHeaders.APPLICATION_JSON)
                                    .withAuthorizationBearer(accessToken)
                    );
        } catch (HttpConnectionException e) {
            throw new PaymentException(e, "Failed to check payment status for payment provider %s (%s)", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }

        if (response.statusCode() != 200) {
            throw new PaymentException(
                    "Failed to check payment status for payment provider %s (%s). Status code was %d with body %s",
                    paymentProviderEntity.getName(),
                    paymentProviderEntity.getKey(),
                    response.statusCode(),
                    response.body()
            );
        }

        XBezahldienstePaymentTransaction updatedTransaction = null;
        try {
            updatedTransaction = ObjectMapperFactory
                    .getInstance()
                    .readValue(response.body(), XBezahldienstePaymentTransaction.class);
        } catch (JsonProcessingException e) {
            throw new PaymentException(e, "Failed to deserialize payment transaction for payment provider %s (%s)", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }

        return updatedTransaction;
    }

    @Nonnull
    @Override
    public XBezahldienstePaymentTransaction onPaymentResultPush(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull ElementData config,
            @Nonnull XBezahldienstePaymentTransaction paymentTransaction,
            @Nonnull Map<String, Object> callbackData
    ) throws PaymentException {
        return onPaymentResultPull(paymentProviderEntity, config, paymentTransaction);
    }

    private String getAccessToken(ElementData config, PaymentProviderEntity paymentProviderEntity) throws PaymentException {
        var paymentProviderOAuthUrl = (String) config.get(OAUTH_URL_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(paymentProviderOAuthUrl)) {
            throw new PaymentMissingDataException("OAuth URL", paymentProviderEntity);
        }
        var normalizedPaymentProviderOAuthUrl = StringUtils
                .normalizeUrl(paymentProviderOAuthUrl)
                .replaceAll("/$", "");

        var paymentProviderClientId = (String) config.get(CLIENT_ID_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(paymentProviderClientId)) {
            throw new PaymentException("Client ID for payment provider %s is missing", getProviderName());
        }

        var paymentProviderClientSecretKey = (String) config.get(CLIENT_SECRET_FIELD).getValue();
        var paymentProviderClientSecret = getDecryptedClientSecret(paymentProviderClientSecretKey);
        if (StringUtils.isNullOrEmpty(paymentProviderClientSecret)) {
            throw new PaymentException("Client secret %s is empty", paymentProviderClientSecretKey);
        }

        var formData = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s", paymentProviderClientId, paymentProviderClientSecret);

        HttpResponse<String> response;
        try {
            response = httpService
                    .post(
                            URI.create(normalizedPaymentProviderOAuthUrl),
                            formData,
                            HttpServiceHeaders
                                    .create()
                                    .withContentType(HttpServiceHeaders.APPLICATION_X_WWW_FORM_URLENCODED)
                                    .withAccept(HttpServiceHeaders.APPLICATION_JSON)
                    );
        } catch (HttpConnectionException e) {
            throw new PaymentHttpRequestException(e, paymentProviderEntity, formData.replaceAll("client_secret=.*", "client_secret=***"));
        }

        if (response.statusCode() != 200) {
            throw new PaymentHttpRequestException(response.statusCode(), paymentProviderEntity, formData.replaceAll("client_secret=.*", "client_secret=***"), response.body());
        }

        var objectMapper = ObjectMapperFactory
                .getInstance();

        String token;
        try {
            token = objectMapper
                    .readTree(response.body())
                    .get("access_token")
                    .asText();
        } catch (JsonProcessingException e) {
            throw new PaymentSerializationException(e, "Failed to deserialize response body", response.body(), paymentProviderEntity);
        }

        return token;
    }

    @Nonnull
    private String getOriginatorID(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull ElementData config
    ) throws PaymentException {
        var originatorID = (String) config.get(ORIGINATOR_ID_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(originatorID)) {
            throw new PaymentException("Originator ID for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
        return originatorID;
    }

    @Nonnull
    private String getEndpointID(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull ElementData config
    ) throws PaymentException {
        var endpointID = (String) config.get(ENDPOINT_ID_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(endpointID)) {
            throw new PaymentException("Endpoint ID for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
        return endpointID;
    }

    private String getDecryptedClientSecret(@Nonnull String clientSecretKey) throws PaymentException {
        try {
            var uuid = UUID.fromString(clientSecretKey);
            return getDecryptedClientSecret(uuid);
        } catch (IllegalArgumentException e) {
            throw new PaymentException(e, "Client secret key %s is not a valid UUID", clientSecretKey);
        }
    }

    @Nonnull
    private String getDecryptedClientSecret(@Nonnull UUID clientSecretKey) throws PaymentException {
        var paymentProviderClientSecretEntity = secretService
                .retrieve(clientSecretKey)
                .orElseThrow(() -> new PaymentException("Client secret %s not found", clientSecretKey));
        try {
            return secretService
                    .decrypt(paymentProviderClientSecretEntity);
        } catch (Exception e) {
            throw new PaymentException(e, "Failed to decrypt client secret %s", clientSecretKey);
        }
    }

    @Nonnull
    private static String getNormalizedPaymentTransactionUrl(@Nonnull PaymentProviderEntity paymentProviderEntity, @Nonnull ElementData config) throws PaymentException {
        var paymentTransactionUrl = (String) config.get(PAYMENT_TRANSACTION_URL_FIELD).getValue();
        if (StringUtils.isNullOrEmpty(paymentTransactionUrl)) {
            throw new PaymentException("Payment transaction URL for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
        return StringUtils.normalizeUrl(paymentTransactionUrl);
    }
}
