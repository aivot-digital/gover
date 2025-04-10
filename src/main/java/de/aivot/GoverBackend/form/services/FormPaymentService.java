package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.enums.PaymentType;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.models.PaymentItem;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.payment.services.PaymentTransactionService;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FormPaymentService {
    private final FormDerivationServiceFactory formDerivationServiceFactory;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderService paymentProviderService;

    @Autowired
    public FormPaymentService(
            FormDerivationServiceFactory formDerivationServiceFactory,
            PaymentTransactionService paymentTransactionService,
            PaymentProviderService paymentProviderService
    ) {
        this.formDerivationServiceFactory = formDerivationServiceFactory;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderService = paymentProviderService;
    }

    public Optional<PaymentTransactionEntity> createTransaction(
            @Nonnull Form form,
            @Nonnull String submissionId,
            @Nonnull Map<String, Object> customerInput
    ) throws PaymentException, ResponseException {
        var paymentItems = createPaymentItems(form, customerInput);

        if (paymentItems.isEmpty()) {
            return Optional.empty();
        }

        var paymentProviderEntity = paymentProviderService
                .retrieve(form.getPaymentProvider())
                .orElseThrow(ResponseException::notFound);

        var redirectUrl = String
                .format("/%s/%s?submissionId=%s", form.getSlug(), form.getVersion(), submissionId);

        var createdTransaction = paymentTransactionService
                .create(
                        paymentProviderEntity,
                        form.getPaymentPurpose(),
                        form.getPaymentDescription(),
                        redirectUrl,
                        paymentItems
                );

        return Optional.of(createdTransaction);
    }

    public List<PaymentItem> createPaymentItems(
            @Nonnull Form form,
            @Nonnull Map<String, Object> customerInput
    ) throws PaymentException {
        List<PaymentItem> items = new LinkedList<>();

        for (var product : form.getProducts()) {
            if (StringUtils.isNullOrEmpty(product.getReference())) {
                throw new PaymentException("Product %s of form %s has no reference", product.getId(), form.getTitle());
            }

            if (StringUtils.isNullOrEmpty(product.getDescription())) {
                throw new PaymentException("Product %s of form %s has no description", product.getId(), form.getTitle());
            }

            if (product.getTaxRate() == null) {
                throw new PaymentException("Product %s of form %s has no tax rate", product.getId(), form.getTitle());
            }

            if (product.getNetPrice() == null) {
                throw new PaymentException("Product %s of form %s has no net price", product.getId(), form.getTitle());
            }

            long quantity = switch (product.getType()) {
                case PaymentType.UpfrontFixed -> {
                    if (product.getUpfrontFixedQuantity() == null) {
                        throw new PaymentException("Product %s of form %s has no fixed upfront quantity", product.getId(), form.getTitle());
                    }
                    yield product.getUpfrontFixedQuantity();
                }
                case PaymentType.UpfrontCalculated -> {
                    if (product.getUpfrontQuantityFunction() == null) {
                        throw new PaymentException("Product %s of form %s has no upfront quantity function", product.getId(), form.getTitle());
                    }

                    var formDerivationContext = formDerivationServiceFactory
                            .create(form, List.of(), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER))
                            .derive(form.getRoot(), customerInput);

                    var result = product
                            .getUpfrontQuantityFunction()
                            .evaluate(null, form.getRoot(), formDerivationContext);

                    if (result == null) {
                        yield 0;
                    }

                    var value = result.getIntegerValue();
                    if (value == null) {
                        throw new PaymentException("Upfront quantity calculation function for product %s of form %s produced no value", product.getId(), form.getTitle());
                    }
                    yield value;
                }
                default -> 0;
            };

            if (quantity > 0) {
                var item = new PaymentItem();

                item.setId(product.getId());
                item.setTaxInformation(product.getTaxInformation());
                item.setNetPrice(BigDecimal.valueOf(product.getNetPrice()));
                item.setTaxRate(BigDecimal.valueOf(product.getTaxRate()));
                item.setReference(product.getReference());
                item.setDescription(product.getDescription());
                item.setQuantity(quantity);
                item.setBookingData(product.getBookingData());

                items.add(item);
            }
        }

        return items;
    }
}
