package de.aivot.GoverBackend.payment.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.aivot.GoverBackend.enums.XBezahldienstPaymentMethod;
import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.utils.StringUtils;

import java.net.URI;

/**
 * Dieses Objekt enthält die Informationen vom Bezahldienst, die 1-zu-1 in den Antrag übernommen werden können. Die Antrags-API wird künftig angeglichen.
 */
public class XBezahldienstePaymentInformation {
    // Die Rest-URL der Payment Transaction für die Statusabfrage.
    // Diese Information könnte aus den Informationen generiert werden.
    // Eine explizite Ablage der URL dient der Robustheit der Schnittstelle.
    @JsonProperty("transactionUrl")
    private URI transactionUrl = null;

    // Die Redirect-URL an die der Online-Dienst den Benutzer weiterleiten soll.
    // Diese URL öffnet die "Paypage" / "Bezahlseite" des Bezahldienstes.
    // Diese URL muss nur im Status "INITIAL" (also bei der Post-Response oder Statusabfrage vor Abschluss / Abbruch der Transaktion) enthalten sein.
    // Nach Abschluss der Transaktion sollte diese URL nicht enthalten sein.
    @JsonProperty("transactionRedirectUrl")
    private URI transactionRedirectUrl = null;

    // Eine vom Bezahldienst vergebene Transaktions-ID beim POST des Requests.
    // Diese ID wird für die späteren Abfragen (GET) verwendet.
    // Dies kann schon das Kassenzeichen sein. Sofern dies später vergeben wird, kann hier eine 'technische' ID des Bezahldienstes vergeben werden.
    // Falls der Bezahldienst keine eigenen IDs hier verwendet, kann die Schnittstelle auch die Request-ID zurück geben.
    @JsonProperty("transactionId")
    private String transactionId = null;

    // Das "fachliche" Kennzeichen für eine Bezahlung.
    // Dieses kann bei dem POST des Requests vom Bezahldienst vergeben werden oder erst nach der Autorisierung durch den Bezahler.
    // Daher ist die relevante ID für diese Schnittstelle die transactionId, die durch den Bezahldienst bei der Anlage des Payment-Requests vergeben wird.
    @JsonProperty("transactionReference")
    private String transactionReference = null;

    // Zeitstempel der erfolgreichen Durchführung der Bezahlung.
    @JsonProperty("transactionTimestamp")
    private String transactionTimestamp = null;

    // Die vom Benutzer ausgewählte Zahlart. Das Feld ist nur bei einer erfolgreichen Zahlung vorhanden / befüllt.
    @JsonProperty("paymentMethod")
    private XBezahldienstPaymentMethod paymentMethod = null;

    // Weitere Erläuterung zur gewählten Zahlart.
    @JsonProperty("paymentMethodDetail")
    private String paymentMethodDetail = null;

    // Der Status der Transaktion soll dem EfA-Onlinedienst erkennbar machen, ob die Bezahlung erfolgreich durchgeführt wurde.
    @JsonProperty("status")
    private XBezahldienstStatus status = null;

    // Optionale ergänzende Erläuterungen zum Status.
    // Diese Informationen werden Teil des Antrages und bieten dem Bezahldienst die Möglichkeit Informationen an das Fachverfahren / Sachbearbeitung zu schicken.
    // Grundsätzlich ist dieses Feld nicht notwendig - Inhalte sind zwischen Bezahldienst und Fachverfahren abzustimmen.
    @JsonProperty("statusDetail")
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
