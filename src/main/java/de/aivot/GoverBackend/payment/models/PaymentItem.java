package de.aivot.GoverBackend.payment.models;

import de.aivot.GoverBackend.models.payment.PaymentProduct;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.utils.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class PaymentItem {
    private String id;
    private String reference;
    private String description;
    private Long quantity;
    private BigDecimal taxRate;
    private BigDecimal netPrice;
    private List<PaymentProduct.BookingDataItem> bookingData;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    /**
     * Tax rate in percent.
     * Must be 100 based like 19% not 0.19.
     * @param taxRate
     */
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getNetPrice() {
        return netPrice;
    }

    public void setNetPrice(BigDecimal netPrice) {
        this.netPrice = netPrice;
    }

    public List<PaymentProduct.BookingDataItem> getBookingData() {
        return bookingData;
    }

    public void setBookingData(List<PaymentProduct.BookingDataItem> bookingData) {
        this.bookingData = bookingData;
    }

    public String getTaxInformation() {
        return taxInformation;
    }

    public void setTaxInformation(String taxInformation) {
        this.taxInformation = taxInformation;
    }

    public BigDecimal getTotalPrice() {
        var quant = BigDecimal
                .valueOf(quantity)
                .setScale(2, RoundingMode.HALF_UP);

        var taxRateInDecimal = taxRate
                .setScale(2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP), RoundingMode.HALF_UP);

        var taxModifier = BigDecimal
                .ONE
                .setScale(2, RoundingMode.HALF_UP)
                .add(taxRateInDecimal);

        return netPrice
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(quant)
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(taxModifier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Optional<XBezahldienstePaymentItem> toXBezahldienstePaymentItem() throws PaymentException {
        if (StringUtils.isNullOrEmpty(this.getReference())) {
            throw new PaymentException("Product %s has no reference", this.getId());
        }

        if (StringUtils.isNullOrEmpty(this.getDescription())) {
            throw new PaymentException("Product %s has no description", this.getId());
        }

        if (this.getTaxRate() == null) {
            throw new PaymentException("Product %s has no tax rate", this.getId());
        }

        if (this.getNetPrice() == null) {
            throw new PaymentException("Product %s  has no net price", this.getId());
        }

        if (this.getQuantity() > 0) {
            var tax = this
                    .getTaxRate()
                    .setScale(2, RoundingMode.HALF_UP);

            var taxRate = tax
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

            var netPricePerItem = this
                    .getNetPrice()
                    .setScale(2, RoundingMode.HALF_UP);

            var taxPerItem = netPricePerItem
                    .multiply(taxRate)
                    .setScale(2, RoundingMode.HALF_UP);

            var totalNetPrice = netPricePerItem
                    .multiply(BigDecimal.valueOf(this.getQuantity()).setScale(2, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);

            var totalTax = taxPerItem
                    .multiply(BigDecimal.valueOf(this.getQuantity()).setScale(2, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);

            var item = new XBezahldienstePaymentItem();

            item.setId(this.getId());
            item.setReference(this.getReference());
            item.setDescription(this.getDescription());

            item.setQuantity(this.getQuantity());

            item.setTaxRate(tax);

            item.setSingleNetAmount(netPricePerItem);
            item.setSingleTaxAmount(taxPerItem);

            item.setTotalNetAmount(totalNetPrice);
            item.setTotalTaxAmount(totalTax);

            var bookingData = new HashMap<String, String>();
            if (this.getBookingData() != null) {
                for (var dataItem : this.getBookingData()) {
                    bookingData.put(dataItem.key(), dataItem.value());
                }
            }
            item.setBookingData(bookingData);

            return Optional.of(item);
        }

        return Optional.empty();
    }
}
