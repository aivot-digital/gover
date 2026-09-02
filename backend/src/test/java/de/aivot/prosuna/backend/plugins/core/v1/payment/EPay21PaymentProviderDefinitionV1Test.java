package de.aivot.prosuna.backend.plugins.core.v1.payment;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentItem;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.prosuna.backend.payment.models.XBezahldiensteRequestor;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static XBezahldienstePaymentRequest paymentRequestWith(XBezahldiensteRequestor requestor) {
        var paymentItem = new XBezahldienstePaymentItem();
        paymentItem.setBookingData(new HashMap<>());

        var paymentRequest = new XBezahldienstePaymentRequest();
        paymentRequest.setItems(List.of(paymentItem));
        paymentRequest.setRequestor(requestor);
        return paymentRequest;
    }
}
