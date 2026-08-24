package de.aivot.prosuna.backend.payment.dtos;

import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentProviderDefinitionResponseDTOTest {
    @Test
    void from_ShouldMapDocumentationUrl() throws Exception {
        var definition = mock(PaymentProviderDefinition.class);
        when(definition.getKey()).thenReturn("de.aivot.test.payment");
        when(definition.getMajorVersion()).thenReturn(2);
        when(definition.getProviderName()).thenReturn("Test payment");
        when(definition.getProviderDescription()).thenReturn("Test payment description");
        when(definition.getDocumentationUrl()).thenReturn("https://docs.example.com/payment/test");
        when(definition.getPaymentConfigLayout()).thenReturn(null);

        var result = PaymentProviderDefinitionResponseDTO.from(definition);

        assertEquals("de.aivot.test.payment", result.key());
        assertEquals(2, result.version());
        assertEquals("Test payment", result.name());
        assertEquals("Test payment description", result.description());
        assertEquals("https://docs.example.com/payment/test", result.documentationUrl());
        assertNull(result.configLayout());
    }
}
