package de.aivot.prosuna.backend.payment.models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentAddressTest {
    @Test
    void shouldPreserveDomainValuesWithoutProtocolSanitizing() {
        var address = new PaymentAddress(
                "Teststraße #123",
                "12 a",
                List.of("Gebäude A", "2. OG"),
                "D-12345",
                "München-Süd",
                "de"
        );

        assertEquals("Teststraße #123", address.street());
        assertEquals("12 a", address.houseNumber());
        assertEquals(List.of("Gebäude A", "2. OG"), address.addressLines());
        assertEquals("D-12345", address.postalCode());
        assertEquals("München-Süd", address.city());
        assertEquals("de", address.country());
    }
}
