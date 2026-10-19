package de.aivot.prosuna.backend.elements.uiPresets;

import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.elements.uiPresets.PaymentGroupPreset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;
import java.net.URI;
import de.aivot.prosuna.backend.payment.models.PaymentItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentGroupPresetTest {
    @Test
    void shouldUseConfiguredPaymentResultMessages() throws Exception {
        var paidPreset = createPreset(XBezahldienstStatus.PAYED, "# Bezahlt\n**Danke.**", "# Nicht bezahlt.");
        var paidMessage = renderMessage(paidPreset);
        assertTrue(paidMessage.contains("# Zahlung erfolgreich\n# Bezahlt\n**Danke.**"));
        assertEquals(
                "https://example.test/payment-confirmation/",
                paidPreset.findChild("download", LinkButtonContentElement.class).orElseThrow().getHref()
        );

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

    @ParameterizedTest
    @CsvSource({"19, 19", "7.5, '7,5'"})
    void shouldDescribeTaxedPaymentItemsConsistentlyWithTheFormDialog(String taxRate, String formattedTaxRate) throws Exception {
        var item = new PaymentItem();
        item.setDescription("Position");
        item.setNetPrice(BigDecimal.TEN);
        item.setQuantity(1L);
        item.setTaxRate(new BigDecimal(taxRate));
        var transaction = paymentTransaction(XBezahldienstStatus.INITIAL);
        transaction.getPaymentInformation().setTransactionRedirectUrl(URI.create("https://example.test/pay"));
        var preset = new PaymentGroupPreset(paymentProvider(), paymentProviderDefinition(),
                new PaymentPayload().setPaymentItems(List.of(item)).setTotal(item.getTotalPrice()),
                transaction, null, null, "https://example.test/confirmation");

        var text = renderMessage(preset);
        assertTrue(text.contains(" Euro inkl. " + formattedTaxRate + "% Steuern"));
        if (taxRate.equals("19")) {
            assertTrue(text.contains("Position: 11,90 Euro inkl. 19% Steuern"));
            assertTrue(text.contains("Insgesamt zu entrichtende Zahlung: 11,90 Euro inkl. Steuern"));
        }
    }

    private static String renderMessage(XBezahldienstStatus status,
                                        String successMessage,
                                        String failureMessage) throws Exception {
        return renderMessage(createPreset(status, successMessage, failureMessage));
    }

    private static String renderMessage(PaymentGroupPreset preset) {
        var richText = preset.findChild("rtx", RichTextContentElement.class).orElseThrow();
        return richText.getContent();
    }

    private static PaymentGroupPreset createPreset(XBezahldienstStatus status,
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

    private static PaymentTransactionEntity paymentTransaction(XBezahldienstStatus status) {
        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setStatus(status);

        return new PaymentTransactionEntity()
                .setKey("tx-1")
                .setPaymentProviderKey(UUID.randomUUID())
                .setPaymentInformation(paymentInformation);
    }
}
