package de.aivot.prosuna.backend.plugins.core.v1.payment;

import de.aivot.prosuna.backend.audit.services.AuditLogService;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentTransaction;
import de.aivot.prosuna.backend.plugins.core.v1.payment.models.GiroPayCallbackResponse;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GirocheckoutPaymentProviderDefinitionV1Test {
    private static final String PROJECT_PASSWORD = "project-password";
    private static final String MERCHANT_TX_ID = "request-1";
    private static final String REFERENCE = "giro-reference-1";

    private JsonMapper previousMapper;
    private GirocheckoutPaymentProviderDefinitionV1 definition;
    private DerivedRuntimeElementData config;

    @BeforeEach
    void setUp() throws Exception {
        previousMapper = JsonMapperTestUtils.installMapper();

        var secretKey = UUID.randomUUID();
        var secretEntity = mock(SecretEntity.class);
        var secretService = mock(SecretService.class);
        when(secretService.retrieve(secretKey)).thenReturn(Optional.of(secretEntity));
        when(secretService.decrypt(secretEntity)).thenReturn(PROJECT_PASSWORD);

        definition = new GirocheckoutPaymentProviderDefinitionV1(
                new AuditService(mock(AuditLogService.class)),
                secretService,
                mock(HttpService.class)
        );

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("projectPasswordSecret", secretKey.toString());
        config = new DerivedRuntimeElementData(effectiveValues, new ComputedElementStates());
    }

    @AfterEach
    void tearDown() {
        JsonMapperTestUtils.restoreMapper(previousMapper);
    }

    @Test
    void acceptsTheSignedResultOfTheTransaction() throws Exception {
        var result = definition.onPaymentResultPush(
                new PaymentProviderEntity(),
                config,
                createTransaction(),
                createSignedCallback(callback -> {
                })
        );

        assertEquals(XBezahldienstStatus.PAYED, result.getPaymentInformation().getStatus());
    }

    @Test
    void rejectsTheSignedResultOfAnotherMerchantTransaction() throws Exception {
        var callback = createSignedCallback(response -> response.setGcMerchantTxId("request-2"));

        assertThrows(PaymentException.class, () -> definition.onPaymentResultPush(
                new PaymentProviderEntity(),
                config,
                createTransaction(),
                callback
        ));
    }

    @Test
    void rejectsTheSignedResultOfAnotherGiroCheckoutTransaction() throws Exception {
        var callback = createSignedCallback(response -> response.setGcReference("giro-reference-2"));

        assertThrows(PaymentException.class, () -> definition.onPaymentResultPush(
                new PaymentProviderEntity(),
                config,
                createTransaction(),
                callback
        ));
    }

    @Test
    void rejectsASignedResultWithAnotherAmount() throws Exception {
        var callback = createSignedCallback(response -> response.setGcAmount(100));

        assertThrows(PaymentException.class, () -> definition.onPaymentResultPush(
                new PaymentProviderEntity(),
                config,
                createTransaction(),
                callback
        ));
    }

    @Test
    void rejectsASignedResultWithAnotherCurrency() throws Exception {
        var callback = createSignedCallback(response -> response.setGcCurrency("USD"));

        assertThrows(PaymentException.class, () -> definition.onPaymentResultPush(
                new PaymentProviderEntity(),
                config,
                createTransaction(),
                callback
        ));
    }

    @Test
    void rejectsAResultWithAnInvalidHash() throws Exception {
        var callback = createSignedCallback(response -> {
        });
        callback.put("gcHash", "invalid");

        assertThrows(PaymentException.class, () -> definition.onPaymentResultPush(
                new PaymentProviderEntity(),
                config,
                createTransaction(),
                callback
        ));
    }

    private static XBezahldienstePaymentTransaction createTransaction() {
        var paymentRequest = new XBezahldienstePaymentRequest();
        paymentRequest.setRequestId(MERCHANT_TX_ID);
        paymentRequest.setGrosAmount(new BigDecimal("12.50"));
        paymentRequest.setCurrency("EUR");

        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setTransactionId(REFERENCE);
        paymentInformation.setStatus(XBezahldienstStatus.INITIAL);

        var transaction = new XBezahldienstePaymentTransaction();
        transaction.setPaymentRequest(paymentRequest);
        transaction.setPaymentInformation(paymentInformation);
        return transaction;
    }

    /**
     * Creates a successful payment result for the transaction, applies the change and signs the result, like
     * GiroCheckout signs every result it reports.
     */
    private static Map<String, Object> createSignedCallback(Consumer<GiroPayCallbackResponse> change) throws Exception {
        var response = new GiroPayCallbackResponse();
        response.setGcReference(REFERENCE);
        response.setGcMerchantTxId(MERCHANT_TX_ID);
        response.setGcBackendTxId("backend-1");
        response.setGcAmount(1250);
        response.setGcCurrency("EUR");
        response.setGcResultPayment(GiroPayCallbackResponse.RESULT_PAYMENT_SUCCESS);
        change.accept(response);

        var callback = new HashMap<String, Object>();
        callback.put("gcReference", response.getGcReference());
        callback.put("gcMerchantTxId", response.getGcMerchantTxId());
        callback.put("gcBackendTxId", response.getGcBackendTxId());
        callback.put("gcAmount", response.getGcAmount());
        callback.put("gcCurrency", response.getGcCurrency());
        callback.put("gcResultPayment", response.getGcResultPayment());
        callback.put("gcHash", GiroPayCallbackResponse.generateHash(response, PROJECT_PASSWORD));
        return callback;
    }
}
