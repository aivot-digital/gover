package de.aivot.GoverBackend.payment.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class XBezahldienstePaymentTransaction implements Serializable {
    @JsonProperty("paymentInformation")
    private XBezahldienstePaymentInformation paymentInformation = null;

    @JsonProperty("paymentRequest")
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
