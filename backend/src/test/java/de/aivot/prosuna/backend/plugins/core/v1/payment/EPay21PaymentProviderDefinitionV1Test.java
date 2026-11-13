package de.aivot.prosuna.backend.plugins.core.v1.payment;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.properties.HttpServiceProperties;
import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentItem;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentTransaction;
import de.aivot.prosuna.backend.payment.models.XBezahldiensteRequestor;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EPay21PaymentProviderDefinitionV1Test {
    private final JsonMapper sharedMapper = JsonMapperTestUtils.createMapper();

    @Test
    void shouldOmitNullRequestorAndEmptyBookingData() throws Exception {
        var paymentRequest = paymentRequestWith(null);

        var json = sharedMapper.readTree(
                epay21PaymentProviderDefinitionV1.serializePaymentRequest(paymentRequest, sharedMapper)
        );

        assertFalse(json.has("requestor"));
        assertFalse(json.get("items").get(0).has("bookingData"));
    }

    @Test
    void shouldIncludeNonNullRequestor() throws Exception {
        var requestor = new XBezahldiensteRequestor();
        requestor.setName("Mustermann");
        var paymentRequest = paymentRequestWith(requestor);

        var json = sharedMapper.readTree(
                epay21PaymentProviderDefinitionV1.serializePaymentRequest(paymentRequest, sharedMapper)
        );

        assertEquals("Mustermann", json.get("requestor").get("name").asText());
    }

    @Test
    void shouldNotChangeSharedMapperNullInclusion() throws Exception {
        var paymentRequest = paymentRequestWith(null);

        assertTrue(sharedMapper.valueToTree(paymentRequest).has("requestor"));

        epay21PaymentProviderDefinitionV1.serializePaymentRequest(paymentRequest, sharedMapper);

        assertTrue(sharedMapper.valueToTree(paymentRequest).has("requestor"));
    }

    @Test
    void statusQueryGivesUpWhenTheProviderDoesNotAnswer() throws Exception {
        // The scheduled payment poll runs this query; without a timeout it would wait forever.
        // The socket is never accepted, so the connection opens but the provider never answers.
        try (var silentProvider = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            var secretService = mock(SecretService.class);
            var secret = new SecretEntity();
            when(secretService.retrieve(any(UUID.class))).thenReturn(Optional.of(secret));
            when(secretService.decrypt(secret)).thenReturn("password");
            var properties = new HttpServiceProperties();
            properties.setConnectionTimeoutSeconds(1);
            properties.setReadTimeoutSeconds(1);
            var definition = new epay21PaymentProviderDefinitionV1(secretService, properties);

            var values = new EffectiveElementValues();
            values.put("originatorId", "originator");
            values.put("endpointId", "endpoint");
            values.put("username", "user");
            values.put("passwordSecretKey", UUID.randomUUID().toString());
            values.put("paymentTransactionUrl", "https://127.0.0.1:" + silentProvider.getLocalPort() + "/");
            var config = new DerivedRuntimeElementData(values, new ComputedElementStates());
            var paymentInformation = new XBezahldienstePaymentInformation();
            paymentInformation.setTransactionId("transaction");
            var transaction = new XBezahldienstePaymentTransaction();
            transaction.setPaymentInformation(paymentInformation);

            var exception = assertTimeoutPreemptively(Duration.ofSeconds(10), () -> assertThrows(PaymentException.class,
                    () -> definition.onPaymentResultPull(new PaymentProviderEntity(), config, transaction)));
            assertInstanceOf(HttpTimeoutException.class, exception.getCause());
        }
    }

    private static XBezahldienstePaymentRequest paymentRequestWith(XBezahldiensteRequestor requestor) {
        var paymentItem = new XBezahldienstePaymentItem();
        paymentItem.setBookingData(new HashMap<>());

        var paymentRequest = new XBezahldienstePaymentRequest();
        paymentRequest.setItems(List.of(paymentItem));
        paymentRequest.setRequestor(requestor);
        return paymentRequest;
    }
}
