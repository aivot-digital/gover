package de.aivot.GoverBackend.models.payment;

import de.aivot.GoverBackend.enums.PaymentType;
import de.aivot.GoverBackend.models.functions.FunctionCode;

import java.io.Serializable;
import java.util.List;

public class PaymentProduct implements Serializable {
    private String id;
    private String reference;
    private String description;
    private PaymentType type;
    private Integer upfrontFixedQuantity;
    private FunctionCode upfrontQuantityFunction;
    private Double taxRate;
    private Double netPrice;
    private List<BookingDataItem> bookingData;
    private String taxInformation;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getUpfrontFixedQuantity() {
        return upfrontFixedQuantity;
    }

    public void setUpfrontFixedQuantity(Integer upfrontFixedQuantity) {
        this.upfrontFixedQuantity = upfrontFixedQuantity;
    }

    public FunctionCode getUpfrontQuantityFunction() {
        return upfrontQuantityFunction;
    }

    public void setUpfrontQuantityFunction(FunctionCode upfrontQuantityFunction) {
        this.upfrontQuantityFunction = upfrontQuantityFunction;
    }

    public Double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(Double taxRate) {
        this.taxRate = taxRate;
    }

    public Double getNetPrice() {
        return netPrice;
    }

    public void setNetPrice(Double netPrice) {
        this.netPrice = netPrice;
    }

    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<BookingDataItem> getBookingData() {
        return bookingData;
    }

    public void setBookingData(List<BookingDataItem> bookingData) {
        this.bookingData = bookingData;
    }

    public String getTaxInformation() {
        return taxInformation;
    }

    public void setTaxInformation(String taxInformation) {
        this.taxInformation = taxInformation;
    }

    public record BookingDataItem(String key, String value) implements Serializable {}
}
