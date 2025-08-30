package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.enums.PaymentType;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.javascript.exceptions.JavascriptException;
import de.aivot.GoverBackend.javascript.models.JavascriptResult;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
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
import java.util.Optional;

@Service
public class FormPaymentService {
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderService paymentProviderService;
    private final ElementDerivationService elementDerivationService;
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    @Autowired
    public FormPaymentService(PaymentTransactionService paymentTransactionService,
                              PaymentProviderService paymentProviderService,
                              ElementDerivationService elementDerivationService,
                              JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderService = paymentProviderService;
        this.elementDerivationService = elementDerivationService;
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
    }

    public Optional<PaymentTransactionEntity> createTransaction(
            @Nonnull FormVersionWithDetailsEntity form,
            @Nonnull String submissionId,
            @Nonnull ElementData elementData
    ) throws PaymentException, ResponseException {
        var paymentItems = createPaymentItems(form, elementData);

        if (paymentItems.isEmpty()) {
            return Optional.empty();
        }

        var paymentProviderEntity = paymentProviderService
                .retrieve(form.getPaymentProviderKey())
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
            @Nonnull FormVersionWithDetailsEntity form,
            @Nonnull ElementData elementData
    ) throws PaymentException {
        var derivationRequest = new ElementDerivationRequest()
                .setElement(form.getRootElement())
                .setElementData(elementData)
                .setOptions(
                        new ElementDerivationOptions()
                                .setSkipErrorsForElementIds(List.of(ElementDerivationOptions.ALL_ELEMENTS))
                                .setSkipOverridesForElementIds(List.of())
                                .setSkipValuesForElementIds(List.of())
                                .setSkipVisibilitiesForElementIds(List.of())
                );

        var derivedElementData = elementDerivationService
                .derive(derivationRequest);

        var javascriptEngine = javascriptEngineFactoryService
                .getEngine();

        List<PaymentItem> items = new LinkedList<>();

        for (var product : form.getPaymentProducts()) {
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
                case PaymentType.UpfrontCalculated -> calculateProductQuantity(javascriptEngine, form, derivedElementData, product);
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

        try {
            javascriptEngine
                    .close();
        } catch (Exception e) {
            throw new PaymentException(e, "Error closing form derivation context for form %s", form.getTitle());
        }

        return items;
    }

    @Nonnull
    private Long calculateProductQuantity(
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull FormVersionWithDetailsEntity form,
            @Nonnull ElementData context,
            @Nonnull PaymentProduct product
    ) throws PaymentException {
        if (product.getUpfrontQuantityJavascript() != null && product.getUpfrontQuantityJavascript().isNotEmpty()) {
            JavascriptResult res;
            try {
                res = javascriptEngine
                        .registerGlobalContextObject(context)
                        .registerElementObject(form.getRootElement())
                        .evaluateCode(product.getUpfrontQuantityJavascript());
            } catch (JavascriptException e) {
                throw new PaymentException("Upfront quantity calculation JavaScript failed with message " + e.getMessage(), product.getId(), form.getTitle());
            }

            if (res == null) {
                return 0L;
            }

            var value = res.asNumber();
            if (value == null) {
                throw new PaymentException("Upfront quantity calculation JavaScript for product %s of form %s produced no value", product.getId(), form.getTitle());
            }

            return value.longValue();
        }

        throw new PaymentException("Product %s of form %s has no upfront quantity code", product.getId(), form.getTitle());
    }
}
