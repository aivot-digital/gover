package de.aivot.prosuna.backend.payment.xbezahldienste;

import de.aivot.prosuna.backend.payment.models.PaymentRequest;
import de.aivot.prosuna.backend.payment.models.PaymentRequestItem;
import de.aivot.prosuna.backend.payment.models.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class XBezahldiensteMapperTest {
    @Test
    void shouldMapNeutralRequestToBothProtocolVersions() {
        var request = request();

        var v110 = XBezahldiensteV110Mapper.toExternal(request);
        var v120 = XBezahldiensteV120Mapper.toExternal(request);

        assertEquals("EUR", v110.getCurrency());
        assertEquals(11.90, v110.getGrossAmount());
        assertEquals("item-1", v110.getItems().getFirst().getId());
        assertEquals("EUR", v120.getCurrency());
        assertEquals(11.90, v120.getGrossAmount());
        assertEquals("item-1", v120.getItems().getFirst().getId());
    }

    @Test
    void shouldMapV110PaymentInformationToTheNeutralStatus() throws Exception {
        var external = new de.aivot.prosuna.backend.xbezahldienste.v1_1_0.PaymentInformation();
        external.setTransactionId("tx-110");
        external.setTransactionReference("ref-110");
        external.setStatus(de.aivot.prosuna.backend.xbezahldienste.v1_1_0.PaymentInformation.StatusEnum.PAYED);
        external.setTransactionTimestamp(OffsetDateTime.parse("2026-09-04T10:00:00Z"));
        external.setTransactionRedirectUrl(URI.create("https://obsolete.example.test"));
        external.setPaymentMethod(de.aivot.prosuna.backend.xbezahldienste.v1_1_0.PaymentInformation.PaymentMethodEnum.PAYPAL);

        var information = XBezahldiensteV110Mapper.toDomain(external);

        assertEquals(PaymentStatus.PAID, information.status());
        assertEquals("tx-110", information.providerTransactionId());
        assertEquals(Instant.parse("2026-09-04T10:00:00Z"), information.paidAt());
        assertEquals("PAYPAL", information.paymentMethod().code());
        assertNull(information.paymentUrl());
    }

    @Test
    void shouldMapV120PendingPaymentInformation() throws Exception {
        var external = new de.aivot.prosuna.backend.xbezahldienste.v1_2_0.PaymentInformation();
        external.setTransactionId("tx-120");
        external.setStatus(de.aivot.prosuna.backend.xbezahldienste.v1_2_0.PaymentInformation.StatusEnum.INITIAL);
        external.setTransactionRedirectUrl(URI.create("https://payment.example.test/tx-120"));

        var information = XBezahldiensteV120Mapper.toDomain(external);

        assertEquals(PaymentStatus.PENDING, information.status());
        assertEquals(URI.create("https://payment.example.test/tx-120"), information.paymentUrl());
        assertNull(information.paidAt());
        assertNull(information.paymentMethod());
    }

    private static PaymentRequest request() {
        var item = new PaymentRequestItem(
                "item-1",
                "reference-1",
                "Gebühr",
                new BigDecimal("19.0000"),
                1,
                new BigDecimal("10.00"),
                new BigDecimal("1.90"),
                new BigDecimal("10.00"),
                new BigDecimal("1.90"),
                Map.of("case", "AZ-1")
        );
        return new PaymentRequest(
                "request-1",
                Instant.parse("2026-09-04T09:00:00Z"),
                "EUR",
                new BigDecimal("11.90"),
                "AZ-1",
                "Gebührenbescheid",
                URI.create("https://prosuna.example.test/redirect"),
                List.of(item),
                null
        );
    }
}
