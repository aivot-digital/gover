package de.aivot.GoverBackend.models.xbezahldienst;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class XBezahldienstePaymentTransaction implements Serializable {
    @SerializedName("paymentInformation")
    private XBezahldienstePaymentInformation paymentInformation = null;

    @SerializedName("paymentRequest")
    private XBezahldienstePaymentRequest paymentRequest = null;

    public XBezahldienstePaymentInformation getPaymentInformation() {
        return paymentInformation;
    }

    public void setPaymentInformation(XBezahldienstePaymentInformation paymentInformation) {
        this.paymentInformation = paymentInformation;
    }

    public XBezahldienstePaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public void setPaymentRequest(XBezahldienstePaymentRequest paymentRequest) {
        this.paymentRequest = paymentRequest;
    }
}
