package de.aivot.prosuna.backend.plugins.core.v1.payment;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.xbezahldienste.v1_1_0.PaymentItem;
import de.aivot.prosuna.backend.xbezahldienste.v1_1_0.PaymentRequest;
import de.aivot.prosuna.backend.xbezahldienste.v1_1_0.Requestor;
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
        var requestor = new Requestor();
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

        var includesNullRequestorBeforeSerialization = sharedMapper.valueToTree(paymentRequest).has("requestor");

        epay21PaymentProviderDefinitionV1.serializePaymentRequest(paymentRequest, sharedMapper);

        assertEquals(
                includesNullRequestorBeforeSerialization,
                sharedMapper.valueToTree(paymentRequest).has("requestor")
        );
    }

    private static PaymentRequest paymentRequestWith(Requestor requestor) {
        var paymentItem = new PaymentItem();
        paymentItem.setBookingData(new HashMap<>());

        var paymentRequest = new PaymentRequest();
        paymentRequest.setItems(List.of(paymentItem));
        paymentRequest.setRequestor(requestor);
        return paymentRequest;
    }
}
