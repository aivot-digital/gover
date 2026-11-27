package de.aivot.prosuna.backend.payment.models;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class PaymentProviderDefinitionTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid", "2026-10-08T11:42:14.181Z", "2026-10-08T11:42:14.181+02:00"})
    void preservesOriginalTimestampByDefault(String timestamp) {
        var definition = mock(PaymentProviderDefinition.class, CALLS_REAL_METHODS);

        assertSame(timestamp, definition.fixTransactionTimestamp(timestamp));
    }
}
