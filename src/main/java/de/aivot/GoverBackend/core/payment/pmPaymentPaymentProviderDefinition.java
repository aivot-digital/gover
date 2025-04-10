package de.aivot.GoverBackend.core.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.elements.models.form.BaseFormElement;
import de.aivot.GoverBackend.elements.models.form.input.SelectField;
import de.aivot.GoverBackend.elements.models.form.input.TextField;
import de.aivot.GoverBackend.elements.models.form.input.TextPattern;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.exceptions.PaymentHttpRequestException;
import de.aivot.GoverBackend.payment.exceptions.PaymentMissingDataException;
import de.aivot.GoverBackend.payment.exceptions.PaymentSerializationException;
import de.aivot.GoverBackend.payment.models.*;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Component
public class pmPaymentPaymentProviderDefinition implements PaymentProviderDefinition {
    private final static String ORIGINATOR_ID_FIELD = "originatorId";
    private final static String ENDPOINT_ID_FIELD = "endpointId";
    private final static String CLIENT_ID_FIELD = "clientId";
    private final static String CLIENT_SECRET_FIELD = "clientSecret";
    private final static String OAUTH_URL_FIELD = "oauthUrl";
    private final static String PAYMENT_TRANSACTION_URL_FIELD = "paymentTransactionUrl";

    private final SecretService secretService;

    @Autowired
    public pmPaymentPaymentProviderDefinition(SecretService secretService) {
        this.secretService = secretService;
    }

    @Nonnull
    @Override
    public String getKey() {
        return "pmpayment";
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
    public GroupLayout getPaymentConfigLayout() throws ResponseException {
        var list = new LinkedList<BaseFormElement>();

        var originatorIdInput = new TextField(Map.of());
        originatorIdInput.setType(ElementType.Text);
        originatorIdInput.setId(ORIGINATOR_ID_FIELD);
        originatorIdInput.setRequired(true);
        originatorIdInput.setLabel("Originator ID");
        originatorIdInput.setPlaceholder("Originator ID");
        originatorIdInput.setHint("Diese ID wird vom Zahlungsdienstleister festgelegt. Üblicherweise ist dies eine Kennzeichnung für das Formular wie z.B. der Formularname, eine LeiKa-ID etc.");
        originatorIdInput.setWeight(6.0d);
        list.add(originatorIdInput);

        var endpointIdInput = new TextField(Map.of());
        endpointIdInput.setType(ElementType.Text);
        endpointIdInput.setId(ENDPOINT_ID_FIELD);
        endpointIdInput.setRequired(true);
        endpointIdInput.setLabel("Endpoint ID");
        endpointIdInput.setPlaceholder("Endpoint ID");
        endpointIdInput.setHint("Diese ID wird vom Zahlungsdienstleister festgelegt. Üblicherweise ist dies eine Kennzeichnung der zuständigen Stelle z.B. der Ortsname, Amtlicher Regionalschlüssel etc.");
        endpointIdInput.setWeight(6.0d);
        list.add(endpointIdInput);

        var clientIdInput = new TextField(Map.of());
        clientIdInput.setType(ElementType.Text);
        clientIdInput.setId(CLIENT_ID_FIELD);
        clientIdInput.setRequired(true);
        clientIdInput.setLabel("OAuth Client-ID");
        clientIdInput.setPlaceholder("Client ID");
        clientIdInput.setHint("Die OAuth Client ID aus dem pmPayment-System.");
        clientIdInput.setWeight(6.0d);
        list.add(clientIdInput);

        var clientSecretInput = new SelectField(Map.of());
        clientSecretInput.setType(ElementType.Select);
        clientSecretInput.setId(CLIENT_SECRET_FIELD);
        clientSecretInput.setRequired(true);
        clientSecretInput.setLabel("OAuth Client Secret");
        clientSecretInput.setPlaceholder("Client Secret");
        clientSecretInput.setHint("Das OAuth Client Secret aus dem pmPayment-System. Es muss zuvor unter \"Geheimnisse\" hinterlegt werden, um hier auswählbar zu sein.");
        var clientSecretInputOptions = secretService
                .list()
                .stream()
                .map(secret -> (Object) Map.of(
                        "value", secret.getKey(),
                        "label", secret.getName()
                ))
                .toList();
        clientSecretInput.setOptions(clientSecretInputOptions);
        clientSecretInput.setWeight(6.0d);
        list.add(clientSecretInput);

        TextPattern urlPattern = new TextPattern(Map.of(
                "regex", "^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([\\/\\w .-]*)*\\/?$",
                "message", "Bitte geben Sie eine gültige URL ein (z. B. https://example.com)."
        ));

        var oauthUrlInput = new TextField(Map.of());
        oauthUrlInput.setType(ElementType.Text);
        oauthUrlInput.setId(OAUTH_URL_FIELD);
        oauthUrlInput.setRequired(true);
        oauthUrlInput.setLabel("OAuth URL");
        oauthUrlInput.setPlaceholder("https://payment-test.govconnect.de/oauth/token/");
        oauthUrlInput.setHint("Die OAuth-URL des Zielsystems. Diese wird vom Zahlungsdienstleister bereitgestellt.");
        oauthUrlInput.setPattern(urlPattern);
        list.add(oauthUrlInput);

        var paymentTransactionUrlInput = new TextField(Map.of());
        paymentTransactionUrlInput.setType(ElementType.Text);
        paymentTransactionUrlInput.setId(PAYMENT_TRANSACTION_URL_FIELD);
        paymentTransactionUrlInput.setRequired(true);
        paymentTransactionUrlInput.setLabel("Basis-URL");
        paymentTransactionUrlInput.setPlaceholder("https://payment-test.govconnect.de/");
        paymentTransactionUrlInput.setHint("Die Basis-URL des Zielsystems gemäß XBezahldienste-Standard. Diese wird vom Zahlungsdienstleister bereitgestellt.");
        paymentTransactionUrlInput.setPattern(urlPattern);
        list.add(paymentTransactionUrlInput);

        var group = new GroupLayout(Map.of());
        group.setType(ElementType.Group);
        group.setId("pmPaymentConfig");
        group.setChildren(list);

        return group;
    }

    @Nonnull
    @Override
    public XBezahldienstePaymentTransaction initiatePayment(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config,
            @Nonnull XBezahldienstePaymentRequest paymentRequest
    ) throws PaymentException {
        var accessToken = getAccessToken(config, paymentProviderEntity);

        var originatorID = (String) config.get(ORIGINATOR_ID_FIELD);
        if (StringUtils.isNullOrEmpty(originatorID)) {
            throw new PaymentException("Originator ID for payment provider %s is missing", getProviderName());
        }

        var endpointID = (String) config.get(ENDPOINT_ID_FIELD);
        if (StringUtils.isNullOrEmpty(endpointID)) {
            throw new PaymentException("Endpoint ID for payment provider %s is missing", getProviderName());
        }

        var paymentTransactionUrl = (String) config.get("paymentTransactionUrl");
        var normalizedPaymentTransactionUrl = StringUtils.normalizeUrl(paymentTransactionUrl);

        var objectMapper = new ObjectMapper();

        String body;
        try {
            body = objectMapper
                    .writeValueAsString(paymentRequest);
        } catch (JsonProcessingException e) {
            throw new PaymentException(e, "Failed to serialize payment request");
        }

        var paymentPath = String
                .format("%spaymenttransaction/%s/%s", normalizedPaymentTransactionUrl, originatorID, endpointID);

        var request = HttpRequest
                .newBuilder(URI.create(paymentPath))
                .headers("Content-Type", ContentType.APPLICATION_JSON.getType())
                .headers("Accept", ContentType.APPLICATION_JSON.getType())
                .headers("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var client = HttpClient.newHttpClient();

        HttpResponse<String> response;
        try {
            response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
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
        } finally {
            client.close();
        }
    }

    @Nonnull
    @Override
    public XBezahldienstePaymentTransaction onPaymentResultPull(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config,
            @Nonnull XBezahldienstePaymentTransaction transaction
    ) throws PaymentException {
        var accessToken = getAccessToken(config, paymentProviderEntity);

        var originatorID = getOriginatorID(paymentProviderEntity, config);
        var endpointID = getEndpointID(paymentProviderEntity, config);
        var normalizedPaymentTransactionUrl = getNormalizedPaymentTransactionUrl(paymentProviderEntity, config);

        var client = HttpClient
                .newBuilder()
                .build();

        var paymentPath = String
                .format("%spaymenttransaction/%s/%s/%s", normalizedPaymentTransactionUrl, originatorID, endpointID, transaction.getPaymentInformation().getTransactionId());

        var request = HttpRequest
                .newBuilder(URI.create(paymentPath))
                .headers("Content-Type", ContentType.APPLICATION_JSON.getType())
                .headers("Accept", ContentType.APPLICATION_JSON.getType())
                .headers("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
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
            updatedTransaction = new ObjectMapper()
                    .readValue(response.body(), XBezahldienstePaymentTransaction.class);
        } catch (JsonProcessingException e) {
            throw new PaymentException(e, "Failed to deserialize payment transaction for payment provider %s (%s)", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }

        client.close();

        return updatedTransaction;
    }

    @Nonnull
    @Override
    public XBezahldienstePaymentTransaction onPaymentResultPush(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config,
            @Nonnull XBezahldienstePaymentTransaction paymentTransaction,
            @Nonnull Map<String, Object> callbackData
    ) throws PaymentException {
        return onPaymentResultPull(paymentProviderEntity, config, paymentTransaction);
    }

    private String getAccessToken(Map<String, Object> config, PaymentProviderEntity paymentProviderEntity) throws PaymentException {
        var paymentProviderOAuthUrl = (String) config.get(OAUTH_URL_FIELD);
        if (StringUtils.isNullOrEmpty(paymentProviderOAuthUrl)) {
            throw new PaymentMissingDataException("OAuth URL", paymentProviderEntity);
        }
        var normalizedPaymentProviderOAuthUrl = StringUtils
                .normalizeUrl(paymentProviderOAuthUrl)
                .replaceAll("/$", "");

        var paymentProviderClientId = (String) config.get(CLIENT_ID_FIELD);
        if (StringUtils.isNullOrEmpty(paymentProviderClientId)) {
            throw new PaymentException("Client ID for payment provider %s is missing", getProviderName());
        }

        var paymentProviderClientSecretKey = (String) config.get(CLIENT_SECRET_FIELD);
        var paymentProviderClientSecret = getDecryptedClientSecret(paymentProviderClientSecretKey);
        if (StringUtils.isNullOrEmpty(paymentProviderClientSecret)) {
            throw new PaymentException("Client secret %s is empty", paymentProviderClientSecretKey);
        }

        var formData = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s", paymentProviderClientId, paymentProviderClientSecret);

        var request = HttpRequest
                .newBuilder(URI.create(normalizedPaymentProviderOAuthUrl))
                .headers("Content-Type", ContentType.APPLICATION_URLENCODED.getType())
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        var client = HttpClient.newHttpClient();

        HttpResponse<String> response = null;
        try {
            response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new PaymentHttpRequestException(e, paymentProviderEntity, formData.replaceAll("client_secret=.*", "client_secret=***"));
        }

        if (response.statusCode() != 200) {
            throw new PaymentHttpRequestException(response.statusCode(), paymentProviderEntity, formData.replaceAll("client_secret=.*", "client_secret=***"), response.body());
        }

        var objectMapper = new ObjectMapper();
        String token;
        try {
            token = objectMapper
                    .readTree(response.body())
                    .get("access_token")
                    .asText();
        } catch (JsonProcessingException e) {
            throw new PaymentSerializationException(e, "Failed to deserialize response body", response.body(), paymentProviderEntity);
        }

        client.close();

        return token;
    }

    @Nonnull
    private String getOriginatorID(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config
    ) throws PaymentException {
        var originatorID = (String) config.get(ORIGINATOR_ID_FIELD);
        if (StringUtils.isNullOrEmpty(originatorID)) {
            throw new PaymentException("Originator ID for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
        return originatorID;
    }

    @Nonnull
    private String getEndpointID(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config
    ) throws PaymentException {
        var endpointID = (String) config.get(ENDPOINT_ID_FIELD);
        if (StringUtils.isNullOrEmpty(endpointID)) {
            throw new PaymentException("Endpoint ID for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
        return endpointID;
    }

    @Nonnull
    private String getDecryptedClientSecret(@Nonnull String clientSecretKey) throws PaymentException {
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
    private static String getNormalizedPaymentTransactionUrl(@Nonnull PaymentProviderEntity paymentProviderEntity, @Nonnull Map<String, Object> config) throws PaymentException {
        var paymentTransactionUrl = (String) config.get(PAYMENT_TRANSACTION_URL_FIELD);
        if (StringUtils.isNullOrEmpty(paymentTransactionUrl)) {
            throw new PaymentException("Payment transaction URL for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
        return StringUtils.normalizeUrl(paymentTransactionUrl);
    }
}
