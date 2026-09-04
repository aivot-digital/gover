package de.aivot.prosuna.backend.payment.models;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentInformationTest {
    @Test
    void shouldExposeOnlyTheVersionIndependentJsonShape() throws Exception {
        var information = new PaymentInformation(
                "provider-123",
                "reference-456",
                PaymentStatus.PAID,
                null,
                Instant.parse("2026-09-04T10:00:00Z"),
                new PaymentMethod("PAYPAL", null),
                "accepted"
        );

        var mapper = JsonMapperTestUtils.createMapper();
        var json = mapper.valueToTree(information);

        assertEquals("provider-123", json.get("providerTransactionId").asText());
        assertEquals("PAID", json.get("status").asText());
        assertEquals("PAYPAL", json.get("paymentMethod").get("code").asText());
        assertTrue(json.has("paymentUrl"));
        assertFalse(json.has("transactionId"));
        assertFalse(json.has("transactionRedirectUrl"));
        assertFalse(json.has("transactionUrl"));
        assertEquals(information, mapper.readValue(mapper.writeValueAsString(information), PaymentInformation.class));
    }

    @Test
    void shouldEnforceStatusDependentInformation() {
        assertThrows(IllegalArgumentException.class, () -> new PaymentInformation(
                "provider-123", null, PaymentStatus.PENDING, null, null, null, null
        ));
        assertThrows(IllegalArgumentException.class, () -> new PaymentInformation(
                "provider-123", null, PaymentStatus.FAILED,
                URI.create("https://payment.example.test/provider-123"), null, null, null
        ));
        assertThrows(IllegalArgumentException.class, () -> new PaymentInformation(
                "provider-123", null, PaymentStatus.CANCELED, null,
                Instant.parse("2026-09-04T10:00:00Z"), null, null
        ));
    }

    @Test
    void shouldPreferPaymentMethodDetailForDisplay() {
        assertEquals("PayPal", new PaymentMethod("PAYPAL", null).displayName());
        assertEquals("SEPA-Lastschrift", new PaymentMethod("OTHER", "SEPA-Lastschrift").displayName());
        assertEquals("NEW_METHOD", new PaymentMethod("NEW_METHOD", null).displayName());
    }
}
