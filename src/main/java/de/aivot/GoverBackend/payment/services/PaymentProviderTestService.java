package de.aivot.GoverBackend.payment.services;

import de.aivot.GoverBackend.exceptions.BadRequestException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.models.PaymentItem;
import de.aivot.GoverBackend.payment.models.PaymentProviderTestResult;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentProviderTestService {
    public static final String TestRedirectID = "test";

    private final GoverConfig goverConfig;

    private final PaymentProviderService paymentProviderService;

    @Autowired
    public PaymentProviderTestService(
            GoverConfig goverConfig,
            PaymentProviderService paymentProviderService
    ) {
        this.goverConfig = goverConfig;
        this.paymentProviderService = paymentProviderService;
    }

    public PaymentProviderTestResult test(
            @Nonnull String paymentProviderKey,
            @Nonnull String purpose,
            @Nonnull String description,
            @Nonnull BigDecimal amount
    ) {
        var paymentProviderEntity = paymentProviderService
                .retrieve(paymentProviderKey)
                .orElseThrow(() -> new BadRequestException("Invalid provider key"));

        var paymentProviderDefinition = paymentProviderService
                .getProviderDefinition(paymentProviderEntity.getProviderKey())
                .orElseThrow(() -> new BadRequestException("Invalid provider key"));

        var paymentItem = new PaymentItem();
        paymentItem.setId(UUID.randomUUID().toString());
        paymentItem.setReference(UUID.randomUUID().toString());
        paymentItem.setDescription(description);
        paymentItem.setQuantity(1L);
        paymentItem.setTaxRate(BigDecimal.valueOf(19));
        paymentItem.setNetPrice(amount);
        paymentItem.setBookingData(List.of());
        paymentItem.setTaxInformation("19% MwSt.");

        XBezahldienstePaymentRequest paymentRequest;
        try {
            paymentRequest = paymentProviderDefinition
                    .createPaymentRequest(
                            paymentProviderEntity,
                            paymentProviderEntity.getConfig(),
                            purpose,
                            description,
                            List.of(paymentItem),
                            goverConfig.createUrl("/api/public/payment-transaction-callback/", TestRedirectID) + "/redirect/"
                    );
        } catch (PaymentException e) {
            return PaymentProviderTestResult.fromException(null, e);
        }

        XBezahldienstePaymentTransaction transaction;
        try {
            transaction = paymentProviderDefinition
                    .initiatePayment(
                            paymentProviderEntity,
                            paymentProviderEntity.getConfig(),
                            paymentRequest
                    );
        } catch (PaymentException e) {
            return PaymentProviderTestResult.fromException(paymentRequest, e);
        }

        return PaymentProviderTestResult.fromTransaction(paymentRequest, transaction);
    }
}
