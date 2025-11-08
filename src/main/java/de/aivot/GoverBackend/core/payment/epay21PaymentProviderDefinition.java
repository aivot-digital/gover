package de.aivot.GoverBackend.core.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import de.aivot.GoverBackend.elements.models.form.BaseFormElement;
import de.aivot.GoverBackend.elements.models.form.input.SelectField;
import de.aivot.GoverBackend.elements.models.form.input.TextField;
import de.aivot.GoverBackend.elements.models.form.input.TextPattern;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.exceptions.PaymentMissingDataException;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentTransaction;
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
public class epay21PaymentProviderDefinition implements PaymentProviderDefinition {
    private final static String ORIGINATOR_ID_FIELD = "originatorId";
    private final static String ENDPOINT_ID_FIELD = "endpointId";
    private final static String PASSWORD_SECRET_KEY_FIELD = "passwordSecretKey";
    private final static String USERNAME_FIELD = "username";
    private final static String PAYMENT_TRANSACTION_URL_FIELD = "paymentTransactionUrl";

    private final SecretService secretService;

    @Autowired
    public epay21PaymentProviderDefinition(SecretService secretService) {
        this.secretService = secretService;
    }

    @Nonnull
    @Override
    public String getKey() {
        return "epay21";
    }

    @Nonnull
    @Override
    public String getProviderName() {
        return "ePay21";
    }

    @Nonnull
    @Override
    public String getProviderDescription() {
        return "Zahlungsdienstleister ePay21";
    }

    @Nonnull
    @Override
    public GroupLayout getPaymentConfigLayout() throws ResponseException {
        var list = new LinkedList<BaseFormElement>();

        var originatorIdInput = new TextField(Map.of());
        originatorIdInput.setPlaceholder("Originator ID");
        originatorIdInput.setType(ElementType.Text);
        originatorIdInput.setRequired(true);
        originatorIdInput.setLabel("Originator ID");
        originatorIdInput.setHint("Diese ID wird vom Zahlungsdienstleister festgelegt. Üblicherweise ist dies eine Kennzeichnung für das Formular wie z.B. der Formularname, eine LeiKa-ID etc.");
        originatorIdInput.setWeight(6.0d);
        originatorIdInput.setId(ORIGINATOR_ID_FIELD);
        list.add(originatorIdInput);

        var endpointIdInput = new TextField(Map.of());
        endpointIdInput.setId(ENDPOINT_ID_FIELD);
        endpointIdInput.setType(ElementType.Text);
        endpointIdInput.setRequired(true);
        endpointIdInput.setLabel("Endpoint ID");
        endpointIdInput.setPlaceholder("Endpoint ID");
        endpointIdInput.setHint("Diese ID wird vom Zahlungsdienstleister festgelegt. Üblicherweise ist dies eine Kennzeichnung der zuständigen Stelle z.B. der Ortsname, Amtlicher Regionalschlüssel etc.");
        endpointIdInput.setWeight(6.0d);
        list.add(endpointIdInput);

        var usernameInput = new TextField(Map.of());
        usernameInput.setType(ElementType.Text);
        usernameInput.setId(USERNAME_FIELD);
        usernameInput.setRequired(true);
        usernameInput.setLabel("Benutzername");
        usernameInput.setPlaceholder("Benutzername");
        usernameInput.setHint("Der Benutzername für die Authentifizierung am Zahlungsdienstleister.");
        usernameInput.setWeight(6.0d);
        list.add(usernameInput);

        var passwordInput = new SelectField(Map.of());
        passwordInput.setType(ElementType.Select);
        passwordInput.setId(PASSWORD_SECRET_KEY_FIELD);
        passwordInput.setRequired(true);
        passwordInput.setLabel("Passwort");
        passwordInput.setPlaceholder("Passwort");
        passwordInput.setHint("Das Passwort für die Authentifizierung am Zahlungsdienstleister.");
        List<Object> clientSecretInputOptions = secretService
                .list()
                .stream()
                .map(secret -> (Object) Map.of(
                        "value", secret.getKey(),
                        "label", secret.getName())
                )
                .toList();
        passwordInput.setOptions(clientSecretInputOptions);
        passwordInput.setWeight(6.0d);
        list.add(passwordInput);

        TextPattern urlPattern = new TextPattern(Map.of());
        urlPattern.setRegex("^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([\\/\\w .-]*)*\\/?$");
        urlPattern.setMessage("Bitte geben Sie eine gültige URL ein (z. B. https://example.com).");

        var paymentTransactionUrlInput = new TextField(Map.of());
        paymentTransactionUrlInput.setType(ElementType.Text);
        paymentTransactionUrlInput.setId(PAYMENT_TRANSACTION_URL_FIELD);
        paymentTransactionUrlInput.setRequired(true);
        paymentTransactionUrlInput.setLabel("Basis-URL");
        paymentTransactionUrlInput.setPlaceholder("https://epay-qs.ekom21.de/xbezahldienste/api/v1.0.0/");
        paymentTransactionUrlInput.setHint("Die Basis-URL des Zielsystems gemäß XBezahldienste-Standard. Diese wird vom Zahlungsdienstleister bereitgestellt.");
        paymentTransactionUrlInput.setPattern(urlPattern);
        list.add(paymentTransactionUrlInput);

        var group = new GroupLayout(Map.of());
        group.setType(ElementType.Group);
        group.setId("epay21Config");
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
        var originatorID = (String) config.get(ORIGINATOR_ID_FIELD);
        if (StringUtils.isNullOrEmpty(originatorID)) {
            throw new PaymentException("Originator ID for payment provider %s is missing", getProviderName());
        }

        var endpointID = (String) config.get(ENDPOINT_ID_FIELD);
        if (StringUtils.isNullOrEmpty(endpointID)) {
            throw new PaymentException("Endpoint ID for payment provider %s is missing", getProviderName());
        }

        var username = getUsername(paymentProviderEntity, config);
        var password = getPasswordSecret(paymentProviderEntity, config);
        var auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        var paymentTransactionUrl = (String) config.get("paymentTransactionUrl");
        var normalizedPaymentTransactionUrl = StringUtils.normalizeUrl(paymentTransactionUrl);

        var objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        paymentRequest.setRequestor(null);
        for (var item : paymentRequest.getItems()) {
            if (item.getBookingData().isEmpty()) {
                item.setBookingData(null);
            }
        }

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
                .headers("Authorization", "Basic " + auth)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var client = HttpClient
                .newHttpClient();

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
        var originatorID = getOriginatorID(paymentProviderEntity, config);
        var endpointID = getEndpointID(paymentProviderEntity, config);
        var username = getUsername(paymentProviderEntity, config);
        var password = getPasswordSecret(paymentProviderEntity, config);
        var auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
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
                .headers("Authorization", "Basic " + auth)
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
    private String getUsername(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config
    ) throws PaymentException {
        var username = (String) config.get(USERNAME_FIELD);
        if (StringUtils.isNullOrEmpty(username)) {
            throw new PaymentException("Username for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
        return username;
    }

    @Nonnull
    private static String getNormalizedPaymentTransactionUrl(@Nonnull PaymentProviderEntity paymentProviderEntity, @Nonnull Map<String, Object> config) throws PaymentException {
        var paymentTransactionUrl = (String) config.get(PAYMENT_TRANSACTION_URL_FIELD);
        if (StringUtils.isNullOrEmpty(paymentTransactionUrl)) {
            throw new PaymentException("Payment transaction URL for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
        return StringUtils.normalizeUrl(paymentTransactionUrl);
    }

    @Nonnull
    private String getPasswordSecret(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config
    ) throws PaymentMissingDataException {
        var passwordSecretField = (String) config.get(PASSWORD_SECRET_KEY_FIELD);
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
                .retrieve(passwordSecretFieldKey.toString())
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
