package de.aivot.gover.backend.elements.uiPresets;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import de.aivot.gover.backend.elements.models.elements.form.content.ImageContentElement;
import de.aivot.gover.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.enums.XBezahldienstStatus;
import de.aivot.gover.backend.payment.entities.PaymentProviderEntity;
import de.aivot.gover.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.gover.backend.payment.models.PaymentItem;
import de.aivot.gover.backend.payment.models.PaymentPayload;
import de.aivot.gover.backend.payment.models.PaymentProviderDefinition;
import de.aivot.gover.backend.utils.NumberUtils;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.stream.Collectors;


public class PaymentGroupPreset extends GroupLayoutElement {
    public PaymentGroupPreset(@Nonnull PaymentProviderEntity paymentProvider,
                              @Nonnull PaymentProviderDefinition paymentProviderDefinition,
                              @Nonnull PaymentPayload paymentPayload,
                              @Nonnull PaymentTransactionEntity transaction,
                              @Nullable String successMessage,
                              @Nullable String failureMessage,
                              @Nonnull String downloadUrl) throws IOException, WriterException {
        super();

        setId("payment-group");

        String content = switch (transaction.getStatus()) {
            case XBezahldienstStatus.INITIAL -> {
                yield """
                        # Zahlung ausstehend
                        Um Ihre Einreichung bearbeiten zu können, ist eine Zahlung erforderlich.
                        Die Zahlung wird durch den Dienstleister **%s** abgewickelt.
                        Bitte achten Sie darauf, dass Sie die Zahlungs­informationen korrekt eingeben und den Vorgang abschließen.
                        
                        
                        **Wichtig:** Ihre Einreichung wird erst nach erfolgter Zahlung weiterbearbeitet.
                        
                        
                        Für Ihre Einreichung sind folgende Positionen zu begleichen:
                        %s
                        
                        
                        Insgesamt zu entrichtende Gebühr: %s Euro %s
                        
                        
                        Sie können den Betrag über den folgenden Link zahlen: [%s](%s)
                        """
                        .formatted(
                                StringUtils.quote(paymentProviderDefinition.getProviderName()),
                                paymentPayload
                                        .getPaymentItems()
                                        .stream()
                                        .map(item -> "- %s: %s Euro%s\n".formatted(
                                                item.getDescription(),
                                                NumberUtils.formatGermanNumber(item.getTotalPrice(), 2),
                                                item.getTaxRate().compareTo(BigDecimal.ZERO) > 0
                                                        ? " inkl. %s Steuern".formatted(NumberUtils.formatGermanNumber(item.getTaxRate(), 2))
                                                        : ""
                                        ))
                                        .collect(Collectors.joining()),
                                NumberUtils.formatGermanNumber(paymentPayload.getTotal(), 2),
                                paymentPayload
                                        .getPaymentItems()
                                        .stream()
                                        .map(PaymentItem::getTaxRate)
                                        .map(BigDecimal.ZERO::compareTo)
                                        .anyMatch(i -> i != 0) ? "inkl. Steuern." : "",
                                transaction.getPaymentInformation().getTransactionRedirectUrl(),
                                transaction.getPaymentInformation().getTransactionRedirectUrl()
                        );
            }
            case XBezahldienstStatus.FAILED -> {
                yield """
                        # Zahlung fehlgeschlagen
                        %s
                        """
                        .formatted(
                                StringUtils.isNotNullOrEmpty(failureMessage)
                                        ? failureMessage
                                        : "Die Zahlung wurde abgebrochen. Bitte wenden Sie sich an den Support, um weitere Informationen zu erhalten und die Zahlung erneut zu versuchen."
                        );
            }
            case XBezahldienstStatus.CANCELED -> {
                yield """
                        # Zahlung abgebrochen
                        %s
                        """
                        .formatted(
                                StringUtils.isNotNullOrEmpty(failureMessage)
                                        ? failureMessage
                                        : "Die Zahlung wurde abgebrochen. Bitte wenden Sie sich an den Support, um weitere Informationen zu erhalten und die Zahlung erneut zu versuchen."
                        );
            }
            case XBezahldienstStatus.PAYED -> {
                yield """
                        # Zahlung erfolgreich
                        %s
                        
                        Sie können die Zahlungsbestätigung über den folgenden Link herunterladen: [%s](%s)
                        """.formatted(
                        StringUtils.isNotNullOrEmpty(successMessage) ? successMessage : "Die Zahlung wurde erfolgreich abgeschlossen. Vielen Dank für Ihre Einreichung.",
                        downloadUrl,
                        downloadUrl
                );
            }
        };

        var richtext = new RichTextContentElement();
        richtext.setId("rtx");
        richtext.setContent(content);
        richtext.setWeight(8.0);
        addChild(richtext);

        if (transaction.getStatus() == XBezahldienstStatus.INITIAL) {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bmx = qrCodeWriter.encode(
                    transaction.getPaymentInformation().getTransactionRedirectUrl().toString(),
                    BarcodeFormat.QR_CODE,
                    256, 256
            );
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bmx, "PNG", pngOutputStream);

            String base64Image = Base64.encodeBase64String(pngOutputStream.toByteArray());

            ImageContentElement image = new ImageContentElement();
            image.setId("qr");
            image.setAlt("QR-Code für die Zahlung");
            image.setCaption("Sie können den QR-Code scannen, um die Zahlung über ein mobiles Gerät abzuschließen.");
            image.setSrc("data:image/png;base64," + base64Image);
            image.setWeight(2.0);
            addChild(image);
        }
    }
}
