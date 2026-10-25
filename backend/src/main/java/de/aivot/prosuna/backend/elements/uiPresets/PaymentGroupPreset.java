package de.aivot.prosuna.backend.elements.uiPresets;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import de.aivot.prosuna.backend.elements.models.elements.form.content.ImageContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentItem;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.utils.NumberUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
                        Für diesen Vorgang ist eine Zahlung erforderlich.
                        Die Zahlung wird durch den Dienstleister **%s** abgewickelt.
                        Bitte achten Sie darauf, dass Sie die Zahlungsinformationen korrekt eingeben und den Zahlungsvorgang abschließen.
                        
                        
                        **Wichtig:** Die weitere Verarbeitung wartet auf die Bestätigung der Zahlung.
                        
                        
                        Zahlungsübersicht:
                        %s
                        
                        
                        Gesamtbetrag: %s Euro%s
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
                                                        ? " inkl. %s%% Steuern".formatted(item.getTaxRate().stripTrailingZeros().toPlainString().replace('.', ','))
                                                        : ""
                                        ))
                                        .collect(Collectors.joining()),
                                NumberUtils.formatGermanNumber(paymentPayload.getTotal(), 2),
                                paymentPayload
                                        .getPaymentItems()
                                        .stream()
                                        .map(PaymentItem::getTaxRate)
                                        .map(BigDecimal.ZERO::compareTo)
                                        .anyMatch(i -> i != 0) ? " inkl. Steuern" : ""
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
                                        : "Die Zahlung ist fehlgeschlagen. Bitte wenden Sie sich an die zuständige Stelle."
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
                                        : "Die Zahlung wurde abgebrochen. Bei Fragen wenden Sie sich bitte an die zuständige Stelle."
                        );
            }
            case XBezahldienstStatus.PAYED -> {
                yield """
                        # Zahlung abgeschlossen
                        %s
                        """.formatted(
                        StringUtils.isNotNullOrEmpty(successMessage) ? successMessage : "Die Zahlung wurde erfolgreich abgeschlossen."
                );
            }
        };

        var leftGroup = new GroupLayoutElement();
        leftGroup.setId("left");
        leftGroup.setWeight(8.0);
        addChild(leftGroup);

        var richtext = new RichTextContentElement();
        richtext.setId("rtx");
        richtext.setContent(content);
        richtext.setWeight(12.0);
        leftGroup.addChild(richtext);

        if (transaction.getStatus() == XBezahldienstStatus.INITIAL) {
            LinkButtonContentElement payButton = new LinkButtonContentElement();
            payButton.setId("pay");
            payButton.setLabel("Zahlung durchführen");
            payButton.setHref(transaction.getPaymentInformation().getTransactionRedirectUrl().toString());
            payButton.setWeight(6.0);
            payButton.setOpenInNewTab(false);
            leftGroup.addChild(payButton);

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

        if (transaction.getStatus() == XBezahldienstStatus.PAYED) {
            LinkButtonContentElement downloadButton = new LinkButtonContentElement();
            downloadButton.setId("download");
            downloadButton.setLabel("Zahlungsbestätigung herunterladen");
            downloadButton.setHref(downloadUrl);
            downloadButton.setWeight(6.0);
            downloadButton.setOpenInNewTab(false);
            leftGroup.addChild(downloadButton);
        }
    }
}
