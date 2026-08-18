package de.aivot.gover.backend.elements.uiPresets;

import de.aivot.gover.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.gover.backend.enums.XBezahldienstStatus;
import de.aivot.gover.backend.payment.entities.PaymentProviderEntity;
import de.aivot.gover.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.gover.backend.payment.models.PaymentProviderDefinition;
import de.aivot.gover.backend.payment.models.PaymentPayload;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentInformation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentGroupPresetTest {
    @Test
    void shouldUseConfiguredPaymentResultMessages() throws Exception {
        var paidMessage = renderMessage(XBezahldienstStatus.PAYED, "# Bezahlt\n**Danke.**", "# Nicht bezahlt.");
        assertTrue(paidMessage.contains("# Zahlung erfolgreich\n# Bezahlt\n**Danke.**"));
        assertTrue(paidMessage.contains("Sie können die Zahlungsbestätigung über den folgenden Link herunterladen: [https://example.test/payment-confirmation/](https://example.test/payment-confirmation/)"));

        assertTrue(renderMessage(
                XBezahldienstStatus.FAILED,
                "# Bezahlt\n**Danke.**",
                "# Nicht bezahlt\nBitte **erneut versuchen**."
        ).contains("# Zahlung fehlgeschlagen\n# Nicht bezahlt\nBitte **erneut versuchen**."));
        assertTrue(renderMessage(
                XBezahldienstStatus.CANCELED,
                "# Bezahlt\n**Danke.**",
                "# Nicht bezahlt\nBitte **erneut versuchen**."
        ).contains("# Zahlung abgebrochen\n# Nicht bezahlt\nBitte **erneut versuchen**."));
    }

    @Test
    void shouldKeepDefaultPaymentResultMessagesWhenConfiguredMessagesAreBlank() throws Exception {
        assertTrue(renderMessage(XBezahldienstStatus.PAYED, " ", null).contains("# Zahlung erfolgreich"));
        assertTrue(renderMessage(XBezahldienstStatus.FAILED, null, " ").contains("# Zahlung fehlgeschlagen"));
        assertTrue(renderMessage(XBezahldienstStatus.CANCELED, null, " ").contains("# Zahlung abgebrochen"));
    }

    private static String renderMessage(XBezahldienstStatus status,
                                        String successMessage,
                                        String failureMessage) throws Exception {
        var preset = new PaymentGroupPreset(
                paymentProvider(),
                paymentProviderDefinition(),
                new PaymentPayload(),
                paymentTransaction(status),
                successMessage,
                failureMessage,
                "https://example.test/payment-confirmation/"
        );

        var richText = (RichTextContentElement) preset.getChildren().getFirst();
        return richText.getContent();
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

    private static PaymentTransactionEntity paymentTransaction(XBezahldienstStatus status) {
        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setStatus(status);

        return new PaymentTransactionEntity()
                .setKey("tx-1")
                .setPaymentProviderKey(UUID.randomUUID())
                .setPaymentInformation(paymentInformation);
    }
}
