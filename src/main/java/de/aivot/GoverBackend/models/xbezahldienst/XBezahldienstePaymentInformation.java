package de.aivot.GoverBackend.models.xbezahldienst;

import com.google.gson.annotations.SerializedName;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import de.aivot.GoverBackend.enums.XBezahldienstPaymentMethod;
import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.utils.StringUtils;
import net.minidev.json.annotate.JsonIgnore;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

/**
 * Dieses Objekt enthält die Informationen vom Bezahldienst, die 1-zu-1 in den Antrag übernommen werden können. Die Antrags-API wird künftig angeglichen.
 */
public class XBezahldienstePaymentInformation {
    // Die Rest-URL der Payment Transaction für die Statusabfrage.
    // Diese Information könnte aus den Informationen generiert werden.
    // Eine explizite Ablage der URL dient der Robustheit der Schnittstelle.
    @SerializedName("transactionUrl")
    private URI transactionUrl = null;

    // Die Redirect-URL an die der Online-Dienst den Benutzer weiterleiten soll.
    // Diese URL öffnet die "Paypage" / "Bezahlseite" des Bezahldienstes.
    // Diese URL muss nur im Status "INITIAL" (also bei der Post-Response oder Statusabfrage vor Abschluss / Abbruch der Transaktion) enthalten sein.
    // Nach Abschluss der Transaktion sollte diese URL nicht enthalten sein.
    @SerializedName("transactionRedirectUrl")
    private URI transactionRedirectUrl = null;

    // Eine vom Bezahldienst vergebene Transaktions-ID beim POST des Requests.
    // Diese ID wird für die späteren Abfragen (GET) verwendet.
    // Dies kann schon das Kassenzeichen sein. Sofern dies später vergeben wird, kann hier eine 'technische' ID des Bezahldienstes vergeben werden.
    // Falls der Bezahldienst keine eigenen IDs hier verwendet, kann die Schnittstelle auch die Request-ID zurück geben.
    @SerializedName("transactionId")
    private String transactionId = null;

    // Das "fachliche" Kennzeichen für eine Bezahlung.
    // Dieses kann bei dem POST des Requests vom Bezahldienst vergeben werden oder erst nach der Autorisierung durch den Bezahler.
    // Daher ist die relevante ID für diese Schnittstelle die transactionId, die durch den Bezahldienst bei der Anlage des Payment-Requests vergeben wird.
    @SerializedName("transactionReference")
    private String transactionReference = null;

    // Zeitstempel der erfolgreichen Durchführung der Bezahlung.
    @SerializedName("transactionTimestamp")
    private String transactionTimestamp = null;

    // Die vom Benutzer ausgewählte Zahlart. Das Feld ist nur bei einer erfolgreichen Zahlung vorhanden / befüllt.
    @SerializedName("paymentMethod")
    private XBezahldienstPaymentMethod paymentMethod = null;

    // Weitere Erläuterung zur gewählten Zahlart.
    @SerializedName("paymentMethodDetail")
    private String paymentMethodDetail = null;

    // Der Status der Transaktion soll dem EfA-Onlinedienst erkennbar machen, ob die Bezahlung erfolgreich durchgeführt wurde.
    @SerializedName("status")
    private XBezahldienstStatus status = null;

    // Optionale ergänzende Erläuterungen zum Status.
    // Diese Informationen werden Teil des Antrags und bieten dem Bezahldienst die Möglichkeit Informationen an das Fachverfahren / Sachbearbeitung zu schicken.
    // Grundsätzlich ist dieses Feld nicht notwendig - Inhalte sind zwischen Bezahldienst und Fachverfahren abzustimmen.
    @SerializedName("statusDetail")
    private String statusDetail = null;

    public URI getTransactionUrl() {
        return transactionUrl;
    }

    public void setTransactionUrl(URI transactionUrl) {
        this.transactionUrl = transactionUrl;
    }

    public URI getTransactionRedirectUrl() {
        return transactionRedirectUrl;
    }

    public void setTransactionRedirectUrl(URI transactionRedirectUrl) {
        this.transactionRedirectUrl = transactionRedirectUrl;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        if (transactionId != null) {
            this.transactionId = StringUtils.cleanAndTruncate(
                    transactionId,
                    "[^\\w\\d-]",
                    44
            );
        } else {
            this.transactionId = null;
        }
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        if (transactionReference != null) {
            this.transactionReference = StringUtils.cleanAndTruncate(
                    transactionReference,
                    "[^\\w\\d-]",
                    36
            );
        } else {
            this.transactionReference = null;
        }
    }

    public String getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(String transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public XBezahldienstPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(XBezahldienstPaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethodDetail() {
        return paymentMethodDetail;
    }

    public void setPaymentMethodDetail(String paymentMethodDetail) {
        this.paymentMethodDetail = paymentMethodDetail;
    }

    public XBezahldienstStatus getStatus() {
        return status;
    }

    public void setStatus(XBezahldienstStatus status) {
        this.status = status;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail(String statusDetail) {
        if (statusDetail != null) {
            this.statusDetail = StringUtils.cleanAndTruncate(
                    statusDetail,
                    "[^\\w\\d-]",
                    99
            );
        } else {
            this.statusDetail = null;
        }
    }
}
