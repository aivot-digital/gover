package de.aivot.GoverBackend.core.payment.models;

import de.aivot.GoverBackend.enums.XBezahldienstPaymentMethod;
import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentTransaction;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GiroPaymentStartResponse {
    private int rc;
    private String msg;
    private String reference;
    private String redirect;

    public XBezahldienstePaymentTransaction toXBezahldienstePaymentTransaction(XBezahldienstePaymentRequest xRequest, String paymentProviderUrl) {
        var xTransaction = new XBezahldienstePaymentTransaction();

        var xInfo = new XBezahldienstePaymentInformation();
        xInfo.setStatus(XBezahldienstStatus.INITIAL);
        xInfo.setStatusDetail(null);
        xInfo.setTransactionId(reference);
        xInfo.setTransactionReference(reference);
        xInfo.setTransactionRedirectUrl(URI.create(redirect));
        xInfo.setPaymentMethod(XBezahldienstPaymentMethod.GIROPAY);
        xInfo.setPaymentMethodDetail(null);
        xInfo.setTransactionTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        xInfo.setTransactionUrl(URI.create(paymentProviderUrl));

        xTransaction.setPaymentRequest(xRequest);
        xTransaction.setPaymentInformation(xInfo);

        return xTransaction;
    }

    public int getRc() {
        return rc;
    }

    public void setRc(int rc) {
        this.rc = rc;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }
}
