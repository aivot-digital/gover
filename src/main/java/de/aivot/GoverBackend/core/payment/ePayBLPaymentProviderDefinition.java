package de.aivot.GoverBackend.core.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.elements.models.form.BaseFormElement;
import de.aivot.GoverBackend.elements.models.form.input.SelectField;
import de.aivot.GoverBackend.elements.models.form.input.TextField;
import de.aivot.GoverBackend.elements.models.form.input.TextPattern;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentTransaction;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.services.storages.AssetStorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.util.LinkedList;
import java.util.Map;

@Component
public class ePayBLPaymentProviderDefinition implements PaymentProviderDefinition {
    private final static String ORIGINATOR_ID_FIELD = "originatorId";
    private final static String ENDPOINT_ID_FIELD = "endpointId";
    private final static String CERTIFICATE_FIELD = "certificate";
    private final static String CERTIFICATE_PASSWORD_FIELD = "certificatePassword";
    private final static String PAYMENT_TRANSACTION_URL_FIELD = "paymentTransactionUrl";

    // TODO: Replace with special asset service when the service exists
    private final AssetRepository assetRepository;
    private final SecretService secretService;
    private final AssetStorageService assetStorageService;

    @Autowired
    public ePayBLPaymentProviderDefinition(AssetRepository assetRepository, SecretService secretService, AssetStorageService assetStorageService) {
        this.assetRepository = assetRepository;
        this.secretService = secretService;
        this.assetStorageService = assetStorageService;
    }

    @Nonnull
    @Override
    public String getKey() {
        return "epaybl";
    }

    @Nonnull
    @Override
    public String getProviderName() {
        return "ePayBL";
    }

    @Nonnull
    @Override
    public String getProviderDescription() {
        return "Zahlungsdienstleister ePayBL";
    }

    @Nullable
    @Override
    public GroupLayout getPaymentConfigLayout() throws ResponseException {
        var list = new LinkedList<BaseFormElement>();

        var originatorIdInput = new TextField(Map.of());
        originatorIdInput.setType(ElementType.Text);
        originatorIdInput.setId(ORIGINATOR_ID_FIELD);
        originatorIdInput.setIsMultiline(false);
        originatorIdInput.setRequired(true);
        originatorIdInput.setLabel("Originator ID");
        originatorIdInput.setPlaceholder("Originator ID");
        originatorIdInput.setHint("Diese ID wird vom Zahlungsdienstleister festgelegt. Üblicherweise ist dies eine Kennzeichnung für das Formular wie z.B. der Formularname, eine LeiKa-ID etc.");
        originatorIdInput.setWeight(6.0d);
        list.add(originatorIdInput);

        var endpointIdInput = new TextField(Map.of());
        endpointIdInput.setType(ElementType.Text);
        endpointIdInput.setId(ENDPOINT_ID_FIELD);
        endpointIdInput.setIsMultiline(false);
        endpointIdInput.setRequired(true);
        endpointIdInput.setLabel("Endpoint ID");
        endpointIdInput.setPlaceholder("Endpoint ID");
        endpointIdInput.setHint("Diese ID wird vom Zahlungsdienstleister festgelegt. Üblicherweise ist dies eine Kennzeichnung der zuständigen Stelle z.B. der Ortsname, Amtlicher Regionalschlüssel etc.");
        endpointIdInput.setWeight(6.0d);
        list.add(endpointIdInput);


        var clientCertificateInput = new SelectField(Map.of());
        clientCertificateInput.setType(ElementType.Select);
        clientCertificateInput.setId(CERTIFICATE_FIELD);
        clientCertificateInput.setRequired(true);
        clientCertificateInput.setLabel("Zertifikat");
        clientCertificateInput.setPlaceholder("ePayBL Zertifikat");
        clientCertificateInput.setHint("Das .p12-Zertifikat wird vom Zahlungsdienstleister bereitgestellt. Es muss zuvor unter \"Dokumente & Medieninhalte\" hochgeladen werden, um hier auswählbar zu sein.");
        var clientCertificateInputOptions = assetRepository
                .findAll()
                .stream()
                .filter(secret -> "application/x-pkcs12".equals(secret.getContentType()))
                .map(secret -> (Object) Map.of(
                        "value", secret.getKey(),
                        "label", secret.getFilename()
                ))
                .toList();
        clientCertificateInput.setOptions(clientCertificateInputOptions);
        clientCertificateInput.setWeight(6.0d);
        list.add(clientCertificateInput);

        var clientSecretInput = new SelectField(Map.of());
        clientSecretInput.setType(ElementType.Select);
        clientSecretInput.setId(CERTIFICATE_PASSWORD_FIELD);
        clientSecretInput.setRequired(true);
        clientSecretInput.setLabel("Zertifikatpasswort");
        clientSecretInput.setPlaceholder("ePayBL Zertifikat Passwort");
        clientSecretInput.setHint("Das vom Zahlungsdienstleister bereitgestellte Passwort für das .p12-Zertifikat. Es muss zuvor unter \"Geheimnisse\" hinterlegt werden, um hier auswählbar zu sein.");
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

        var paymentTransactionUrlInput = new TextField(Map.of());
        paymentTransactionUrlInput.setType(ElementType.Text);
        paymentTransactionUrlInput.setId(PAYMENT_TRANSACTION_URL_FIELD);
        paymentTransactionUrlInput.setRequired(true);
        paymentTransactionUrlInput.setLabel("Basis-URL");
        paymentTransactionUrlInput.setPlaceholder("https://epayment-stage.dataport.de/konnektor/epayment/");
        paymentTransactionUrlInput.setHint("Die Basis-URL des Zielsystems gemäß XBezahldienste-Standard. Diese wird vom Zahlungsdienstleister bereitgestellt.");
        TextPattern urlPattern = new TextPattern(Map.of(
                "regex", "^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([\\/\\w .-]*)*\\/?$",
                "message", "Bitte geben Sie eine gültige URL ein (z. B. https://example.com)."
        ));
        paymentTransactionUrlInput.setPattern(urlPattern);
        list.add(paymentTransactionUrlInput);

        var group = new GroupLayout(Map.of());
        group.setType(ElementType.Group);
        group.setId("ePayBLConfig");
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
        var originatorID = getOriginatorID(paymentProviderEntity, config);
        var endpointID = getEndpointID(paymentProviderEntity, config);
        var normalizedPaymentTransactionUrl = getNormalizedPaymentTransactionUrl(paymentProviderEntity, config);

        var objectMapper = new ObjectMapper();

        String body;
        try {
            body = objectMapper
                    .writeValueAsString(paymentRequest);
        } catch (JsonProcessingException e) {
            throw new PaymentException(e, "Failed to serialize payment request for payment provider %s (%s)", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }

        var paymentPath = String
                .format("%spaymenttransaction/%s/%s", normalizedPaymentTransactionUrl, originatorID, endpointID);

        var request = HttpRequest
                .newBuilder(URI.create(paymentPath))
                .headers("Content-Type", ContentType.APPLICATION_JSON.getType())
                .headers("Accept", ContentType.APPLICATION_JSON.getType())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var client = HttpClient
                .newBuilder()
                .sslContext(getSslContext(paymentProviderEntity, config))
                .build();

        HttpResponse<String> response;
        try {
            response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new PaymentException(
                    e,
                    "Failed to initiate payment from payment provider %s (%s). Request body was %s",
                    paymentProviderEntity.getName(),
                    paymentProviderEntity.getKey(),
                    body
            );
        }

        if (response.statusCode() != 200) {
            throw new PaymentException(
                    "Failed to initiate payment from payment provider %s (%s). Status code was %d with body %s",
                    paymentProviderEntity.getName(),
                    paymentProviderEntity.getKey(),
                    response.statusCode(),
                    response.body()
            );
        }

        try {
            return objectMapper.readValue(response.body(), XBezahldienstePaymentTransaction.class);
        } catch (JsonProcessingException e) {
            throw new PaymentException(e, "Failed to deserialize payment transaction for payment provider %s (%s)", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
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
        var normalizedPaymentTransactionUrl = getNormalizedPaymentTransactionUrl(paymentProviderEntity, config);

        var client = HttpClient
                .newBuilder()
                .sslContext(getSslContext(paymentProviderEntity, config))
                .build();

        var paymentPath = String
                .format("%spaymenttransaction/%s/%s/%s", normalizedPaymentTransactionUrl, originatorID, endpointID, transaction.getPaymentInformation().getTransactionId());

        var request = HttpRequest
                .newBuilder(URI.create(paymentPath))
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
            @Nonnull XBezahldienstePaymentTransaction paymentRequest,
            @Nonnull Map<String, Object> callbackData
    ) throws PaymentException {
        return onPaymentResultPull(paymentProviderEntity, config, paymentRequest);
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
    private static String getNormalizedPaymentTransactionUrl(@Nonnull PaymentProviderEntity paymentProviderEntity, @Nonnull Map<String, Object> config) throws PaymentException {
        var paymentTransactionUrl = (String) config.get(PAYMENT_TRANSACTION_URL_FIELD);
        if (StringUtils.isNullOrEmpty(paymentTransactionUrl)) {
            throw new PaymentException("Payment transaction URL for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
        return StringUtils.normalizeUrl(paymentTransactionUrl);
    }

    @Nonnull
    private SSLContext getSslContext(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config
    ) throws PaymentException {
        var paymentProviderPasswordSecretKey = (String) config.get(CERTIFICATE_PASSWORD_FIELD);
        if (StringUtils.isNullOrEmpty(paymentProviderPasswordSecretKey)) {
            throw new PaymentException("Certificate password field is missing for payment provider %s (%s)", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }

        var paymentProviderClientCertPassSecret = secretService
                .retrieve(paymentProviderPasswordSecretKey)
                .orElseThrow(() -> new PaymentException("Certificate password secret for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey()));

        String paymentProviderClientCertPass = null;
        try {
            paymentProviderClientCertPass = secretService
                    .decrypt(paymentProviderClientCertPassSecret);
        } catch (Exception e) {
            throw new PaymentException(e, "Failed to decrypt certificate password for payment provider %s (%s)", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }

        if (StringUtils.isNullOrEmpty(paymentProviderClientCertPass)) {
            throw new PaymentException("Certificate password for payment provider %s (%s) is empty", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }

        var paymentProviderClientCertificateAssetKey = (String) config.get(CERTIFICATE_FIELD);
        if (StringUtils.isNullOrEmpty(paymentProviderClientCertificateAssetKey)) {
            throw new PaymentException("Certificate asset key for payment provider %s (%s) is not specified", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }

        var paymentProviderClientCertAsset = assetRepository
                .findById(paymentProviderClientCertificateAssetKey)
                .orElseThrow(() -> new PaymentException("Certificate for payment provider %s (%s) is missing", paymentProviderEntity.getName(), paymentProviderEntity.getKey()));

        byte[] paymentProviderClientCertBytes;
        try {
            paymentProviderClientCertBytes = assetStorageService
                    .getAssetData(paymentProviderClientCertAsset);
        } catch (ResponseException e) {
            throw new PaymentException(e, "Failed to retrieve certificate for payment provider %s (%s)", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }

        try {
            KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
            InputStream certStream = new ByteArrayInputStream(paymentProviderClientCertBytes);
            clientKeyStore.load(certStream, paymentProviderClientCertPass.toCharArray());

            var keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientKeyStore, paymentProviderClientCertPass.toCharArray());

            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    keyManagerFactory.getKeyManagers(),
                    null,
                    null
            );

            return sslContext;
        } catch (Exception e) {
            throw new PaymentException(e, "Failed to create SSL context for payment provider %s (%s)", paymentProviderEntity.getName(), paymentProviderEntity.getKey());
        }
    }
}
