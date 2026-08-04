package de.aivot.gover.backend.payment.services;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.PaymentConfigElementValueItem;
import de.aivot.gover.backend.elements.models.elements.form.input.PaymentConfigElementValueRequestorMapping;
import de.aivot.gover.backend.enums.XBezahldienstGender;
import de.aivot.gover.backend.javascript.exceptions.JavascriptException;
import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.gover.backend.nocode.models.NoCodeOperand;
import de.aivot.gover.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.gover.backend.payment.exceptions.PaymentException;
import de.aivot.gover.backend.payment.models.XBezahldiensteAddress;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentItem;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.gover.backend.payment.models.XBezahldiensteRequestor;
import de.aivot.gover.backend.process.models.ProcessDataValueUtils;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import de.aivot.gover.backend.utils.NumberUtils;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentRequestCreationService {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TemplateRenderService templateRenderService;
    private final NoCodeEvaluationService noCodeEvaluationService;
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public PaymentRequestCreationService(TemplateRenderService templateRenderService,
                                         NoCodeEvaluationService noCodeEvaluationService,
                                         JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.templateRenderService = templateRenderService;
        this.noCodeEvaluationService = noCodeEvaluationService;
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
    }

    @Nonnull
    public XBezahldienstePaymentRequest createRequest(
            @Nonnull PaymentConfigElementValue paymentConfigElementValue,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull String redirectUrl
    ) throws PaymentException {
        if (StringUtils.isNullOrEmpty(redirectUrl)) {
            throw new PaymentException("Die Redirect-URL für die Zahlungsanfrage darf nicht leer sein.");
        }

        var items = createItems(paymentConfigElementValue, processExecutionData);

        var request = new XBezahldienstePaymentRequest();
        request.setRandomRequestId();
        request.setRequestTimestampNow();
        request.setPurpose(renderRequired(paymentConfigElementValue.purpose(), processExecutionData, "Verwendungszweck"));
        request.setDescription(renderRequired(paymentConfigElementValue.description(), processExecutionData, "Beschreibung"));
        request.setRedirectUrl(redirectUrl);
        request.setRequestor(createRequestor(paymentConfigElementValue, processExecutionData));
        request.setItemsAndCalculateGrosAmount(items);

        if (request.getGrosAmount() == null || request.getGrosAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new PaymentException("Der Gesamtbetrag der Zahlungsanfrage darf nicht 0 sein.");
        }

        return request;
    }

    @Nonnull
    private List<XBezahldienstePaymentItem> createItems(
            @Nonnull PaymentConfigElementValue paymentConfigElementValue,
            @Nonnull ProcessExecutionData processExecutionData
    ) throws PaymentException {
        var configItems = paymentConfigElementValue.items();
        if (configItems == null || configItems.isEmpty()) {
            throw new PaymentException("Die Zahlungsanfrage muss mindestens eine Zahlungsposition enthalten.");
        }

        var items = new LinkedList<XBezahldienstePaymentItem>();
        for (var i = 0; i < configItems.size(); i++) {
            var item = createItem(configItems.get(i), processExecutionData, i);
            if (item != null) {
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            throw new PaymentException("Die Zahlungsanfrage enthält keine zahlbaren Positionen.");
        }

        return items;
    }

    @Nullable
    private XBezahldienstePaymentItem createItem(
            @Nonnull PaymentConfigElementValueItem itemConfig,
            @Nonnull ProcessExecutionData processExecutionData,
            int index
    ) throws PaymentException {
        var quantity = resolveQuantity(itemConfig, processExecutionData, index);
        if (quantity <= 0) {
            return null;
        }

        var singleNetAmount = resolveCosts(itemConfig, processExecutionData, index);
        if (singleNetAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new PaymentException("Die Zahlungsposition %d darf keine negativen Kosten enthalten.", index + 1);
        }

        var taxRate = itemConfig.fixedTaxRate();
        if (taxRate == null) {
            throw new PaymentException("Die Zahlungsposition %d muss einen Steuersatz enthalten.", index + 1);
        }
        if (taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(ONE_HUNDRED) > 0) {
            throw new PaymentException("Der Steuersatz der Zahlungsposition %d muss zwischen 0 und 100 liegen.", index + 1);
        }

        var normalizedSingleNetAmount = singleNetAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        var normalizedTaxRate = taxRate.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        var singleTaxAmount = normalizedSingleNetAmount
                .multiply(normalizedTaxRate)
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
        var quantityAsDecimal = BigDecimal.valueOf(quantity);

        var paymentItem = new XBezahldienstePaymentItem();
        paymentItem.setId(resolveItemId(itemConfig, index));
        paymentItem.setReference(renderRequired(itemConfig.reference(), processExecutionData, "Referenz der Zahlungsposition " + (index + 1)));
        paymentItem.setDescription(renderRequired(itemConfig.description(), processExecutionData, "Beschreibung der Zahlungsposition " + (index + 1)));
        paymentItem.setQuantity(quantity);
        paymentItem.setTaxRate(normalizedTaxRate);
        paymentItem.setSingleNetAmount(normalizedSingleNetAmount);
        paymentItem.setSingleTaxAmount(singleTaxAmount);
        paymentItem.setTotalNetAmount(normalizedSingleNetAmount.multiply(quantityAsDecimal).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        paymentItem.setTotalTaxAmount(singleTaxAmount.multiply(quantityAsDecimal).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        paymentItem.setBookingData(renderBookingData(itemConfig, processExecutionData));

        return paymentItem;
    }

    @Nonnull
    private String resolveItemId(@Nonnull PaymentConfigElementValueItem itemConfig, int index) throws PaymentException {
        return switch (itemConfig.idType()) {
            case AutoGeneratedUUID -> UUID.randomUUID().toString();
            case Predefined -> {
                if (StringUtils.isNullOrEmpty(itemConfig.predefinedId())) {
                    throw new PaymentException("Die Zahlungsposition %d muss eine vordefinierte ID enthalten.", index + 1);
                }
                yield itemConfig.predefinedId();
            }
            case null -> throw new PaymentException("Die Zahlungsposition %d muss einen ID-Typ enthalten.", index + 1);
        };
    }

    @Nonnull
    private BigDecimal resolveCosts(
            @Nonnull PaymentConfigElementValueItem itemConfig,
            @Nonnull ProcessExecutionData processExecutionData,
            int index
    ) throws PaymentException {
        return switch (itemConfig.costType()) {
            case FixedCosts -> requireNumber(itemConfig.fixedCosts(), "Kosten der Zahlungsposition " + (index + 1));
            case VariableCosts -> resolveVariableNumber(
                    itemConfig.variableCostsCalculationType(),
                    itemConfig.variableCostsNoCodeCalculation(),
                    itemConfig.variableCostsLowCodeCalculation(),
                    processExecutionData,
                    "Kosten der Zahlungsposition " + (index + 1)
            );
            case null -> throw new PaymentException("Die Zahlungsposition %d muss einen Kostentyp enthalten.", index + 1);
        };
    }

    private long resolveQuantity(
            @Nonnull PaymentConfigElementValueItem itemConfig,
            @Nonnull ProcessExecutionData processExecutionData,
            int index
    ) throws PaymentException {
        var quantity = switch (itemConfig.quantityType()) {
            case FixedQuantity -> requireNumber(itemConfig.fixedQuantity(), "Menge der Zahlungsposition " + (index + 1));
            case VariableQuantity -> resolveVariableNumber(
                    itemConfig.variableQuantityCalculationType(),
                    itemConfig.variableQuantityNoCodeCalculation(),
                    itemConfig.variableQuantityLowCodeCalculation(),
                    processExecutionData,
                    "Menge der Zahlungsposition " + (index + 1)
            );
            case null -> throw new PaymentException("Die Zahlungsposition %d muss einen Mengentyp enthalten.", index + 1);
        };

        return quantity.setScale(0, RoundingMode.DOWN).longValue();
    }

    @Nonnull
    private BigDecimal resolveVariableNumber(
            @Nullable PaymentConfigElementValueItem.VariableValueCalculationType calculationType,
            @Nullable NoCodeOperand noCodeCalculation,
            @Nullable JavascriptCode lowCodeCalculation,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull String fieldName
    ) throws PaymentException {
        return switch (calculationType) {
            case NoCode -> {
                if (noCodeCalculation == null) {
                    throw new PaymentException("%s muss eine No-Code-Berechnung enthalten.", fieldName);
                }
                var result = noCodeEvaluationService
                        .evaluate(noCodeCalculation, new DerivedRuntimeElementData(), processExecutionData);
                yield requireNumber(result.getValue(), fieldName);
            }
            case LowCode -> {
                if (lowCodeCalculation == null || lowCodeCalculation.isEmpty()) {
                    throw new PaymentException("%s muss eine Low-Code-Berechnung enthalten.", fieldName);
                }
                yield evaluateLowCodeNumber(lowCodeCalculation, processExecutionData, fieldName);
            }
            case null -> throw new PaymentException("%s muss einen Berechnungstyp enthalten.", fieldName);
        };
    }

    @Nonnull
    private BigDecimal evaluateLowCodeNumber(
            @Nonnull JavascriptCode lowCodeCalculation,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull String fieldName
    ) throws PaymentException {
        try (var javascriptEngine = javascriptEngineFactoryService.getEngine()) {
            var result = javascriptEngine
                    .registerProcessExecutionData(processExecutionData)
                    .evaluateCode(lowCodeCalculation);
            return requireNumber(result.asNumber(), fieldName);
        } catch (JavascriptException e) {
            throw new PaymentException(e, "%s konnte nicht per Low-Code berechnet werden.", fieldName);
        } catch (Exception e) {
            throw new PaymentException(e, "Die Low-Code-Engine für %s konnte nicht geschlossen werden.", fieldName);
        }
    }

    @Nonnull
    private LinkedHashMap<String, String> renderBookingData(
            @Nonnull PaymentConfigElementValueItem itemConfig,
            @Nonnull ProcessExecutionData processExecutionData
    ) throws PaymentException {
        var bookingData = new LinkedHashMap<String, String>();
        if (itemConfig.additionalBookingData() == null) {
            return bookingData;
        }

        for (var entry : itemConfig.additionalBookingData().entrySet()) {
            if (StringUtils.isNullOrEmpty(entry.getKey())) {
                throw new PaymentException("Buchungsdaten dürfen keine leeren Schlüssel enthalten.");
            }

            var value = render(entry.getValue(), processExecutionData);
            if (value != null) {
                bookingData.put(entry.getKey(), value);
            }
        }

        return bookingData;
    }

    @Nullable
    private XBezahldiensteRequestor createRequestor(
            @Nonnull PaymentConfigElementValue paymentConfigElementValue,
            @Nonnull ProcessExecutionData processExecutionData
    ) throws PaymentException {
        if (!Boolean.TRUE.equals(paymentConfigElementValue.mapRequestor())) {
            return null;
        }

        var mapping = paymentConfigElementValue.requestorMapping();
        if (mapping == null) {
            throw new PaymentException("Für die Zahlungsanfrage muss eine Antragsteller-Zuweisung konfiguriert sein.");
        }

        var requestor = new XBezahldiensteRequestor();
        requestor.setLastName(readString(processExecutionData, mapping.lastNameDestinationKey()));
        requestor.setFirstName(readString(processExecutionData, mapping.firstNameDestinationKey()));
        requestor.setGender(readGender(processExecutionData, mapping.genderDestinationKey()));
        requestor.setOrganization(readBoolean(processExecutionData, mapping.isOrganizationDestinationKey()));
        requestor.setOrganizationName(readString(processExecutionData, mapping.organizationNameDestinationKey()));

        var address = createAddress(mapping, processExecutionData);
        if (address != null) {
            requestor.setAddress(address);
        }

        if (requestor.getLastName() == null &&
                requestor.getFirstName() == null &&
                requestor.getGender() == null &&
                requestor.getOrganization() == null &&
                requestor.getOrganizationName() == null &&
                requestor.getAddress() == null) {
            return null;
        }

        return requestor;
    }

    @Nullable
    private XBezahldiensteAddress createAddress(
            @Nonnull PaymentConfigElementValueRequestorMapping mapping,
            @Nonnull ProcessExecutionData processExecutionData
    ) {
        var address = new XBezahldiensteAddress();
        address.setStreet(readString(processExecutionData, mapping.streetDestinationKey()));
        address.setHouseNumber(readString(processExecutionData, mapping.houseNumberDestinationKey()));
        address.setAddressLine(readString(processExecutionData, mapping.addressLineDestinationKey()));
        address.setPostalCode(readString(processExecutionData, mapping.postalCodeDestinationKey()));
        address.setCity(readString(processExecutionData, mapping.cityDestinationKey()));

        var country = readString(processExecutionData, mapping.countryDestinationKey());
        address.setCountry(country == null ? null : country.toUpperCase(Locale.ROOT));

        if (address.getStreet() == null &&
                address.getHouseNumber() == null &&
                address.getAddressLine() == null &&
                address.getPostalCode() == null &&
                address.getCity() == null &&
                address.getCountry() == null) {
            return null;
        }

        return address;
    }

    @Nullable
    private String readString(@Nonnull ProcessExecutionData processExecutionData,
                              @Nullable String destinationKey) {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            return null;
        }

        var value = ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, destinationKey);
        return StringUtils.toNullableTrimmedString(value);
    }

    @Nullable
    private Boolean readBoolean(@Nonnull ProcessExecutionData processExecutionData,
                                @Nullable String destinationKey) throws PaymentException {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            return null;
        }

        var value = ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, destinationKey);
        if (value == null) {
            return null;
        }

        return switch (value) {
            case Boolean bool -> bool;
            case Number number -> number.doubleValue() != 0;
            case String str -> parseBoolean(str);
            default -> throw new PaymentException("Der Wert für %s ist kein gültiger boolescher Wert.", destinationKey);
        };
    }

    @Nullable
    private XBezahldienstGender readGender(@Nonnull ProcessExecutionData processExecutionData,
                                           @Nullable String destinationKey) throws PaymentException {
        if (StringUtils.isNullOrEmpty(destinationKey)) {
            return null;
        }

        var value = ProcessDataValueUtils.resolveProcessDataValue(processExecutionData, destinationKey);
        if (value == null) {
            return null;
        }
        if (value instanceof XBezahldienstGender gender) {
            return gender;
        }

        var valueAsString = value.toString().trim();
        for (var gender : XBezahldienstGender.values()) {
            if (gender.matches(valueAsString) || gender.name().equalsIgnoreCase(valueAsString)) {
                return gender;
            }
        }

        throw new PaymentException("Der Wert für %s ist kein gültiges XBezahldienste-Geschlecht.", destinationKey);
    }

    private boolean parseBoolean(@Nonnull String value) throws PaymentException {
        var normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedValue) {
            case "true", "wahr", "ja", "yes", "1" -> true;
            case "false", "falsch", "nein", "no", "0" -> false;
            default -> throw new PaymentException("Der Wert %s ist kein gültiger boolescher Wert.", value);
        };
    }

    @Nonnull
    private String renderRequired(@Nullable String template,
                                  @Nonnull ProcessExecutionData processExecutionData,
                                  @Nonnull String fieldName) throws PaymentException {
        var value = render(template, processExecutionData);
        if (StringUtils.isNullOrEmpty(value)) {
            throw new PaymentException("%s darf nicht leer sein.", fieldName);
        }
        return value;
    }

    @Nullable
    private String render(@Nullable String template,
                          @Nonnull ProcessExecutionData processExecutionData) throws PaymentException {
        try {
            return templateRenderService.interpolate(processExecutionData, template);
        } catch (RuntimeException e) {
            throw new PaymentException(e, "Die Vorlage %s konnte nicht gerendert werden.", StringUtils.quote(template));
        }
    }

    @Nonnull
    private BigDecimal requireNumber(@Nullable Object value, @Nonnull String fieldName) throws PaymentException {
        var number = toBigDecimal(value);
        if (number == null) {
            throw new PaymentException("%s muss eine Zahl ergeben.", fieldName);
        }
        return number;
    }

    @Nullable
    private BigDecimal toBigDecimal(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case BigDecimal bigDecimal -> bigDecimal;
            case Number number -> BigDecimal.valueOf(number.doubleValue());
            case String string -> {
                var trimmedString = string.trim();
                if (trimmedString.isEmpty()) {
                    yield null;
                }
                try {
                    yield new BigDecimal(trimmedString);
                } catch (NumberFormatException ignored) {
                    yield NumberUtils.parseGermanNumber(trimmedString, MONEY_SCALE);
                }
            }
            default -> null;
        };
    }
}
