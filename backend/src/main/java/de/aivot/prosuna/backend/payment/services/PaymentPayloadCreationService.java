package de.aivot.prosuna.backend.payment.services;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValueItem;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValueRequestorMapping;
import de.aivot.prosuna.backend.enums.XBezahldienstGender;
import de.aivot.prosuna.backend.javascript.exceptions.JavascriptException;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.models.payment.PaymentProduct;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentItem;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.XBezahldiensteAddress;
import de.aivot.prosuna.backend.payment.models.XBezahldiensteRequestor;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.utils.NumberUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PaymentPayloadCreationService {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TemplateRenderService templateRenderService;
    private final NoCodeEvaluationService noCodeEvaluationService;
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public PaymentPayloadCreationService(TemplateRenderService templateRenderService,
                                         NoCodeEvaluationService noCodeEvaluationService,
                                         JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.templateRenderService = templateRenderService;
        this.noCodeEvaluationService = noCodeEvaluationService;
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
    }

    @Nonnull
    public Optional<PaymentPayload> createRequest(
            @Nonnull PaymentConfigElementValue paymentConfigElementValue,
            @Nonnull DerivedRuntimeElementData derivedRuntimeElementData,
            @Nonnull ProcessExecutionData processExecutionData
    ) throws PaymentException {
        var items = createItems(paymentConfigElementValue, derivedRuntimeElementData, processExecutionData);

        var payload = new PaymentPayload()
                .setPurpose(renderRequired(paymentConfigElementValue.purpose(), processExecutionData, "Buchungstext"))
                .setDescription(renderRequired(paymentConfigElementValue.description(), processExecutionData, "Beschreibung"))
                .setRequestor(createRequestor(paymentConfigElementValue, processExecutionData))
                .setPaymentItems(items)
                .setTotal(calculateTotal(items));

        if (payload.getTotal().compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        if (payload.getTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new PaymentException("Die Zahlungsanfrage darf keine negativen Gesamtkosten enthalten.");
        }

        return Optional.of(payload);
    }

    @Nonnull
    private List<PaymentItem> createItems(
            @Nonnull PaymentConfigElementValue paymentConfigElementValue,
            @Nonnull DerivedRuntimeElementData derivedRuntimeElementData,
            @Nonnull ProcessExecutionData processExecutionData
    ) throws PaymentException {
        var configItems = paymentConfigElementValue.items();
        if (configItems == null || configItems.isEmpty()) {
            throw new PaymentException("Die Zahlungsanfrage muss mindestens eine Zahlungsposition enthalten.");
        }

        var items = new LinkedList<PaymentItem>();
        for (var i = 0; i < configItems.size(); i++) {
            var item = createItem(configItems.get(i), derivedRuntimeElementData, processExecutionData, i);
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
    private PaymentItem createItem(
            @Nonnull PaymentConfigElementValueItem itemConfig,
            @Nonnull DerivedRuntimeElementData derivedRuntimeElementData,
            @Nonnull ProcessExecutionData processExecutionData,
            int index
    ) throws PaymentException {
        var quantity = resolveQuantity(itemConfig, derivedRuntimeElementData, processExecutionData, index);
        if (quantity <= 0) {
            return null;
        }

        var singleNetAmount = resolveCosts(itemConfig, derivedRuntimeElementData, processExecutionData, index);
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

        var paymentItem = new PaymentItem();
        paymentItem.setId(resolveItemId(itemConfig, index));
        paymentItem.setReference(renderRequired(itemConfig.reference(), processExecutionData, "Referenz der Zahlungsposition " + (index + 1)));
        paymentItem.setDescription(renderRequired(itemConfig.description(), processExecutionData, "Beschreibung der Zahlungsposition " + (index + 1)));
        paymentItem.setQuantity(quantity);
        paymentItem.setTaxRate(normalizedTaxRate);
        paymentItem.setNetPrice(normalizedSingleNetAmount);
        paymentItem.setBookingData(renderBookingData(itemConfig, processExecutionData));

        return paymentItem;
    }

    @Nonnull
    private BigDecimal calculateTotal(@Nonnull List<PaymentItem> items) {
        return items
                .stream()
                .map(PaymentItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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
            @Nonnull DerivedRuntimeElementData derivedRuntimeElementData,
            @Nonnull ProcessExecutionData processExecutionData,
            int index
    ) throws PaymentException {
        return switch (itemConfig.costType()) {
            case FixedCosts -> requireNumber(itemConfig.fixedCosts(), "Kosten der Zahlungsposition " + (index + 1));
            case VariableCosts -> resolveVariableNumber(
                    itemConfig.variableCostsCalculationType(),
                    itemConfig.variableCostsNoCodeCalculation(),
                    itemConfig.variableCostsLowCodeCalculation(),
                    derivedRuntimeElementData,
                    processExecutionData,
                    "Kosten der Zahlungsposition " + (index + 1)
            );
            case null -> throw new PaymentException("Die Zahlungsposition %d muss einen Kostentyp enthalten.", index + 1);
        };
    }

    private long resolveQuantity(
            @Nonnull PaymentConfigElementValueItem itemConfig,
            @Nonnull DerivedRuntimeElementData derivedRuntimeElementData,
            @Nonnull ProcessExecutionData processExecutionData,
            int index
    ) throws PaymentException {
        var quantity = switch (itemConfig.quantityType()) {
            case FixedQuantity -> requireNumber(itemConfig.fixedQuantity(), "Menge der Zahlungsposition " + (index + 1));
            case VariableQuantity -> resolveVariableNumber(
                    itemConfig.variableQuantityCalculationType(),
                    itemConfig.variableQuantityNoCodeCalculation(),
                    itemConfig.variableQuantityLowCodeCalculation(),
                    derivedRuntimeElementData,
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
            @Nonnull DerivedRuntimeElementData derivedRuntimeElementData,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull String fieldName
    ) throws PaymentException {
        return switch (calculationType) {
            case NoCode -> {
                if (noCodeCalculation == null) {
                    throw new PaymentException("%s muss eine No-Code-Berechnung enthalten.", fieldName);
                }
                var result = noCodeEvaluationService
                        .evaluate(noCodeCalculation, derivedRuntimeElementData, processExecutionData);
                yield requireNumber(result.getValue(), fieldName);
            }
            case LowCode -> {
                if (lowCodeCalculation == null || lowCodeCalculation.isEmpty()) {
                    throw new PaymentException("%s muss eine Low-Code-Berechnung enthalten.", fieldName);
                }
                yield evaluateLowCodeNumber(lowCodeCalculation, derivedRuntimeElementData, processExecutionData, fieldName);
            }
            case null -> throw new PaymentException("%s muss einen Berechnungstyp enthalten.", fieldName);
        };
    }

    @Nonnull
    private BigDecimal evaluateLowCodeNumber(
            @Nonnull JavascriptCode lowCodeCalculation,
            @Nullable DerivedRuntimeElementData derivedRuntimeElementData,
            @Nullable ProcessExecutionData processExecutionData,
            @Nonnull String fieldName
    ) throws PaymentException {
        try (var javascriptEngine = javascriptEngineFactoryService.getEngine()) {
            if (derivedRuntimeElementData != null) {
                javascriptEngine.registerGlobalContextObject(derivedRuntimeElementData);
            }
            if (processExecutionData != null) {
                javascriptEngine.registerProcessExecutionData(processExecutionData);
            }
            var result = javascriptEngine
                    .evaluateCode(lowCodeCalculation);
            return requireNumber(result.asNumber(), fieldName);
        } catch (JavascriptException e) {
            throw new PaymentException(e, "%s konnte nicht per Low-Code berechnet werden.", fieldName);
        } catch (Exception e) {
            throw new PaymentException(e, "Die Low-Code-Engine für %s konnte nicht geschlossen werden.", fieldName);
        }
    }

    @Nonnull
    private List<PaymentProduct.BookingDataItem> renderBookingData(
            @Nonnull PaymentConfigElementValueItem itemConfig,
            @Nonnull ProcessExecutionData processExecutionData
    ) throws PaymentException {
        var bookingData = new LinkedList<PaymentProduct.BookingDataItem>();
        if (itemConfig.additionalBookingData() == null) {
            return bookingData;
        }

        for (var entry : itemConfig.additionalBookingData().entrySet()) {
            if (StringUtils.isNullOrEmpty(entry.getKey())) {
                throw new PaymentException("Buchungsdaten dürfen keine leeren Schlüssel enthalten.");
            }

            var value = render(entry.getValue(), processExecutionData);
            if (value != null) {
                bookingData.add(new PaymentProduct.BookingDataItem(entry.getKey(), value));
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

        return switch (mapping.requestorSourceType()) {
            case FixPerson -> createPersonRequestor(mapping, processExecutionData);
            case FixOrg -> createOrganizationRequestor(mapping, processExecutionData);
            case ProcessDataKey -> createDynamicRequestor(mapping, processExecutionData);
            case null -> throw new PaymentException("Für die Zahlungsanfrage muss eine Quelle für die Antragsteller-Zuweisung konfiguriert sein.");
        };
    }

    @Nonnull
    private XBezahldiensteRequestor createPersonRequestor(
            @Nonnull PaymentConfigElementValueRequestorMapping mapping,
            @Nonnull ProcessExecutionData processExecutionData
    ) throws PaymentException {
        var requestor = new XBezahldiensteRequestor();
        requestor.setIsOrganization(false);
        requestor.setLastName(readString(processExecutionData, mapping.lastNameDestinationKey()));
        requestor.setFirstName(readString(processExecutionData, mapping.firstNameDestinationKey()));
        requestor.setGender(readGender(processExecutionData, mapping.genderDestinationKey()));

        var address = createAddress(mapping, processExecutionData);
        if (address != null) {
            requestor.setAddress(address);
        }

        return requestor;
    }

    @Nonnull
    private XBezahldiensteRequestor createOrganizationRequestor(
            @Nonnull PaymentConfigElementValueRequestorMapping mapping,
            @Nonnull ProcessExecutionData processExecutionData
    ) {
        var requestor = new XBezahldiensteRequestor();
        requestor.setIsOrganization(true);
        requestor.setOrganizationName(readString(processExecutionData, mapping.organizationNameDestinationKey()));

        var address = createAddress(mapping, processExecutionData);
        if (address != null) {
            requestor.setAddress(address);
        }

        return requestor;
    }

    @Nonnull
    private XBezahldiensteRequestor createDynamicRequestor(
            @Nonnull PaymentConfigElementValueRequestorMapping mapping,
            @Nonnull ProcessExecutionData processExecutionData
    ) throws PaymentException {
        var isOrganization = readBoolean(processExecutionData, mapping.isOrganizationDestinationKey());
        if (isOrganization == null) {
            throw new PaymentException("Der Wert für %s muss angeben, ob die Antragsteller-Zuweisung eine Organisation ist.", mapping.isOrganizationDestinationKey());
        }

        return isOrganization
                ? createOrganizationRequestor(mapping, processExecutionData)
                : createPersonRequestor(mapping, processExecutionData);
    }

    @Nullable
    private XBezahldiensteAddress createAddress(
            @Nonnull PaymentConfigElementValueRequestorMapping mapping,
            @Nonnull ProcessExecutionData processExecutionData
    ) {
        var address = new XBezahldiensteAddress();
        address.setStreet(readString(processExecutionData, mapping.streetDestinationKey()));
        address.setHouseNumber(readString(processExecutionData, mapping.houseNumberDestinationKey()));
        address.setAddressLineFromString(readString(processExecutionData, mapping.addressLineDestinationKey()));
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
