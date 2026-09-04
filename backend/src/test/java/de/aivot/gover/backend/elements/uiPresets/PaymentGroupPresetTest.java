package de.aivot.prosuna.backend.elements.uiPresets;

import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.payment.models.PaymentInformation;
import de.aivot.prosuna.backend.payment.models.PaymentStatus;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentGroupPresetTest {
    @Test
    void shouldUseConfiguredPaymentResultMessages() throws Exception {
        var paidPreset = createPreset(PaymentStatus.PAID, "# Bezahlt\n**Danke.**", "# Nicht bezahlt.");
        var paidMessage = renderMessage(paidPreset);
        assertTrue(paidMessage.contains("# Zahlung erfolgreich\n# Bezahlt\n**Danke.**"));
        assertEquals(
                "https://example.test/payment-confirmation/",
                paidPreset.findChild("download", LinkButtonContentElement.class).orElseThrow().getHref()
        );

        assertTrue(renderMessage(
                PaymentStatus.FAILED,
                "# Bezahlt\n**Danke.**",
                "# Nicht bezahlt\nBitte **erneut versuchen**."
        ).contains("# Zahlung fehlgeschlagen\n# Nicht bezahlt\nBitte **erneut versuchen**."));
        assertTrue(renderMessage(
                PaymentStatus.CANCELED,
                "# Bezahlt\n**Danke.**",
                "# Nicht bezahlt\nBitte **erneut versuchen**."
        ).contains("# Zahlung abgebrochen\n# Nicht bezahlt\nBitte **erneut versuchen**."));
    }

    @Test
    void shouldKeepDefaultPaymentResultMessagesWhenConfiguredMessagesAreBlank() throws Exception {
        assertTrue(renderMessage(PaymentStatus.PAID, " ", null).contains("# Zahlung erfolgreich"));
        assertTrue(renderMessage(PaymentStatus.FAILED, null, " ").contains("# Zahlung fehlgeschlagen"));
        assertTrue(renderMessage(PaymentStatus.CANCELED, null, " ").contains("# Zahlung abgebrochen"));
    }

    private static String renderMessage(PaymentStatus status,
                                        String successMessage,
                                        String failureMessage) throws Exception {
        return renderMessage(createPreset(status, successMessage, failureMessage));
    }

    private static String renderMessage(PaymentGroupPreset preset) {
        var richText = preset.findChild("rtx", RichTextContentElement.class).orElseThrow();
        return richText.getContent();
    }

    private static PaymentGroupPreset createPreset(PaymentStatus status,
                                                   String successMessage,
                                                   String failureMessage) throws Exception {
        return new PaymentGroupPreset(
                paymentProvider(),
                paymentProviderDefinition(),
                new PaymentPayload(),
                paymentTransaction(status),
                successMessage,
                failureMessage,
                "https://example.test/payment-confirmation/"
        );
    }

    private static PaymentProviderEntity paymentProvider() {
        return new PaymentProviderEntity()
                .setKey(UUID.randomUUID())
                .setName("Stadtkasse");
    }

    private static PaymentProviderDefinition paymentProviderDefinition() {
        var definition = mock(PaymentProviderDefinition.class);
        when(definition.getProviderName()).thenReturn("Stadtkasse");
        return definition;
    }

    private static PaymentTransactionEntity paymentTransaction(PaymentStatus status) {
        var paymentInformation = new PaymentInformation(
                "tx-1", null, status, null, null, null, null
        );

        return new PaymentTransactionEntity()
                .setKey("tx-1")
                .setPaymentProviderKey(UUID.randomUUID())
                .setPaymentInformation(paymentInformation);
    }
}
