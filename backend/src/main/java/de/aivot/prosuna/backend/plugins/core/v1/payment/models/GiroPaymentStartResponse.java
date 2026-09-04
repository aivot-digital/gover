package de.aivot.prosuna.backend.plugins.core.v1.payment.models;

import de.aivot.prosuna.backend.payment.models.PaymentInformation;
import de.aivot.prosuna.backend.payment.models.PaymentStatus;

import java.net.URI;

public class GiroPaymentStartResponse {
    private int rc;
    private String msg;
    private String reference;
    private String redirect;

    public PaymentInformation toPaymentInformation() {
        return new PaymentInformation(
                reference,
                reference,
                PaymentStatus.PENDING,
                URI.create(redirect),
                null,
                null,
                msg
        );
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
