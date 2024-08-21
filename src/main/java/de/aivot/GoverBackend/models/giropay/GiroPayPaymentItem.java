package de.aivot.GoverBackend.models.giropay;

import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentItem;

import java.math.BigDecimal;

public class GiroPayPaymentItem {
    private String name;
    private String ean;
    private int quantity;
    private int grossAmount;

    public static GiroPayPaymentItem valueOf(XBezahldienstePaymentItem xItem) {
        GiroPayPaymentItem gItem = new GiroPayPaymentItem();

        gItem.setName(xItem.getDescription());
        gItem.setEan(null);
        gItem.setQuantity(xItem.getQuantity().intValue());

        var itemTotalAmount = xItem.getSingleNetAmount().add(xItem.getSingleTaxAmount());
        var itemTotalAmountInCents = itemTotalAmount.multiply(BigDecimal.valueOf(100)).intValue();
        gItem.setGrossAmount(itemTotalAmountInCents);

        return gItem;
    }

    public void setName(String name) {
        if (name == null || name.length() > 100) {
            throw new IllegalArgumentException("Name cannot be null or longer than 100 characters");
        }
        this.name = name;
    }

    public void setEan(String ean) {
        if (ean != null && ean.length() > 100) {
            throw new IllegalArgumentException("EAN cannot be longer than 100 characters");
        }
        this.ean = ean;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        this.quantity = quantity;
    }

    public void setGrossAmount(int grossAmount) {
        if (grossAmount < 0) {
            throw new IllegalArgumentException("Gross Amount cannot be negative");
        }
        this.grossAmount = grossAmount;
    }

    public String getName() {
        return name;
    }

    public String getEan() {
        return ean;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getGrossAmount() {
        return grossAmount;
    }
}
