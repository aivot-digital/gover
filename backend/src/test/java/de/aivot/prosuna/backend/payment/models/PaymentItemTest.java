package de.aivot.prosuna.backend.payment.models;

import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentItemTest {
    @ParameterizedTest
    @CsvSource({
            "10.00,    1, 0,        0.0000,   0.00,   0.00,    10.00",
            "10.00,    1, 7,        7.0000,   0.70,   0.70,    10.70",
            "10.00,    1, 19,       19.0000,  1.90,   1.90,    11.90",
            "10.00,    1, 100,      100.0000, 10.00,  10.00,   20.00",
            "10.00,    1, 7.5,      7.5000,   0.75,   0.75,    10.75",
            "10.00,    1, 7.25,     7.2500,   0.73,   0.73,    10.73",
            "1000.00,  1, 7.125,    7.1250,   71.25,  71.25,   1071.25",
            "10000.00, 1, 7.1234,   7.1234,   712.34, 712.34,  10712.34",
            "10000.00, 1, 0.0001,   0.0001,   0.01,   0.01,    10000.01",
            "10000.00, 3, 7.1234,   7.1234,   712.34, 2137.02, 32137.02",
            "0.07,     3, 7.5,      7.5000,   0.01,   0.03,    0.24",
            "0.01,     3, 19,       19.0000,  0.00,   0.00,    0.03",
            "10000.00, 1, 7.12344,  7.1234,   712.34, 712.34,  10712.34",
            "10000.00, 1, 7.12345,  7.1235,   712.35, 712.35,  10712.35"
    })
    void shouldCalculateConsistentTotalsWithFourDecimalTaxRates(
            BigDecimal netPrice,
            long quantity,
            BigDecimal taxRate,
            BigDecimal expectedTaxRate,
            BigDecimal expectedSingleTax,
            BigDecimal expectedTotalTax,
            BigDecimal expectedTotal
    ) throws PaymentException {
        var item = new PaymentItem();
        item.setId("fee");
        item.setReference("FEE");
        item.setDescription("Fee");
        item.setNetPrice(netPrice);
        item.setQuantity(quantity);
        item.setTaxRate(taxRate);

        var providerItem = item.toXBezahldienstePaymentItem().orElseThrow();

        assertAll(
                () -> assertEquals(expectedTotal, item.getTotalPrice()),
                () -> assertEquals(expectedTaxRate, providerItem.getTaxRate()),
                () -> assertEquals(expectedSingleTax, providerItem.getSingleTaxAmount()),
                () -> assertEquals(expectedTotalTax, providerItem.getTotalTaxAmount()),
                () -> assertEquals(expectedTotal, providerItem.getBruttoAmount()),
                () -> assertEquals(item.getTotalPrice(), providerItem.getBruttoAmount())
        );
    }
}
