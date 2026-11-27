package de.aivot.prosuna.backend.payment.models;

import de.aivot.prosuna.backend.models.payment.PaymentProduct;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class PaymentItem {
    /**
     * Tax rates are percentages with up to four decimal places, e.g. 7.1234 %.
     */
    public static final int TAX_RATE_SCALE = 4;
    private static final int MONEY_SCALE = 2;

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
        var netPricePerItem = calculateSingleNetAmount();

        return multiplyByQuantity(netPricePerItem)
                .add(multiplyByQuantity(calculateSingleTaxAmount(netPricePerItem)))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    // getTotalPrice() and toXBezahldienstePaymentItem() share these helpers so the payload total always
    // matches the gross amount sent to the payment provider.
    @Nonnull
    private BigDecimal calculateNormalizedTaxRate() {
        return this.taxRate
                .setScale(TAX_RATE_SCALE, RoundingMode.HALF_UP);
    }

    @Nonnull
    private BigDecimal calculateSingleNetAmount() {
        return this.netPrice
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    // Taxes are rounded per item before they are multiplied by the quantity.
    @Nonnull
    private BigDecimal calculateSingleTaxAmount(@Nonnull BigDecimal singleNetAmount) {
        return singleNetAmount
                .multiply(calculateNormalizedTaxRate().movePointLeft(2))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    @Nonnull
    private BigDecimal multiplyByQuantity(@Nonnull BigDecimal singleAmount) {
        return singleAmount
                .multiply(BigDecimal.valueOf(this.quantity))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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
            var tax = calculateNormalizedTaxRate();
            var netPricePerItem = calculateSingleNetAmount();
            var taxPerItem = calculateSingleTaxAmount(netPricePerItem);
            var totalNetPrice = multiplyByQuantity(netPricePerItem);
            var totalTax = multiplyByQuantity(taxPerItem);

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
