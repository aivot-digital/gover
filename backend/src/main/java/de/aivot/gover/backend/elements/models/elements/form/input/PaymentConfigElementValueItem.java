package de.aivot.gover.backend.elements.models.elements.form.input;

import de.aivot.gover.backend.exceptions.ValidationException;
import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.nocode.models.NoCodeOperand;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentConfigElementValueItem(
        @Nullable
        IdType idType,
        @Nullable
        String predefinedId,

        @Nullable
        String description, // Supports Template Tag Rendering

        @Nullable
        String reference, // Supports Template Tag Rendering

        // Costs of this payment item (single item, netto)
        @Nullable
        CostType costType,
        @Nullable
        BigDecimal fixedCosts,
        @Nullable
        VariableValueCalculationType variableCostsCalculationType,
        @Nullable
        NoCodeOperand variableCostsNoCodeCalculation,
        @Nullable
        JavascriptCode variableCostsLowCodeCalculation,

        // Quantity of this payment item
        @Nullable
        QuantityType quantityType,
        @Nullable
        Long fixedQuantity,
        @Nullable
        VariableValueCalculationType variableQuantityCalculationType,
        @Nullable
        NoCodeOperand variableQuantityNoCodeCalculation,
        @Nullable
        JavascriptCode variableQuantityLowCodeCalculation,

        // More information for this payment item
        @Nullable
        BigDecimal fixedTaxRate,
        @Nullable
        Map<String, String> additionalBookingData // Value Field Supports Template Tag Rendering
) {
    public void performValidation() throws ValidationException {
        requireText(description, "Beschreibung");
        requireText(reference, "Referenz");

        if (idType == null) {
            throw new ValidationException(null, "Es muss ein ID-Typ ausgewählt werden.");
        }
        if (idType == IdType.Predefined) {
            requireText(predefinedId, "Vordefinierte ID");
        }

        if (costType == null) {
            throw new ValidationException(null, "Es muss ein Kostentyp ausgewählt werden.");
        }
        if (costType == CostType.FixedCosts) {
            requireNotNegative(fixedCosts, "Feste Kosten");
        } else {
            validateVariableCalculation(
                    variableCostsCalculationType,
                    variableCostsNoCodeCalculation,
                    variableCostsLowCodeCalculation,
                    "Variable Kosten"
            );
        }

        if (quantityType == null) {
            throw new ValidationException(null, "Es muss ein Mengentyp ausgewählt werden.");
        }
        if (quantityType == QuantityType.FixedQuantity) {
            requireNotNegative(fixedQuantity, "Feste Menge");
        } else {
            validateVariableCalculation(
                    variableQuantityCalculationType,
                    variableQuantityNoCodeCalculation,
                    variableQuantityLowCodeCalculation,
                    "Variable Menge"
            );
        }

        if (fixedTaxRate == null) {
            throw new ValidationException(null, "Es muss ein Steuersatz angegeben werden.");
        }
        if (fixedTaxRate.compareTo(BigDecimal.ZERO) < 0 || fixedTaxRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidationException(null, "Der Steuersatz muss zwischen 0 und 100 liegen.");
        }

        if (additionalBookingData != null) {
            for (var entry : additionalBookingData.entrySet()) {
                if (StringUtils.isNullOrEmpty(entry.getKey())) {
                    throw new ValidationException(null, "Buchungsdaten dürfen keine leeren Schlüssel enthalten.");
                }
            }
        }
    }

    private void validateVariableCalculation(
            @Nullable VariableValueCalculationType calculationType,
            @Nullable NoCodeOperand noCodeCalculation,
            @Nullable JavascriptCode lowCodeCalculation,
            String fieldName
    ) throws ValidationException {
        if (calculationType == null) {
            throw new ValidationException(null, fieldName + " benötigen einen Berechnungstyp.");
        }

        if (calculationType == VariableValueCalculationType.NoCode) {
            if (noCodeCalculation == null) {
                throw new ValidationException(null, fieldName + " benötigen eine No-Code-Berechnung.");
            }

            var noCodeError = noCodeCalculation.validate();
            if (!noCodeError.isValid()) {
                throw new ValidationException(null, fieldName + " enthalten einen ungültigen No-Code-Ausdruck.", noCodeError);
            }
        } else if (lowCodeCalculation == null || lowCodeCalculation.isEmpty()) {
            throw new ValidationException(null, fieldName + " benötigen eine Low-Code-Berechnung.");
        }
    }

    private void requireText(@Nullable String value, String fieldName) throws ValidationException {
        if (StringUtils.isNullOrEmpty(value)) {
            throw new ValidationException(null, fieldName + " muss angegeben werden.");
        }
    }

    private void requireNotNegative(@Nullable BigDecimal value, String fieldName) throws ValidationException {
        if (value == null) {
            throw new ValidationException(null, fieldName + " müssen angegeben werden.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(null, fieldName + " dürfen nicht negativ sein.");
        }
    }

    private void requireNotNegative(@Nullable Long value, String fieldName) throws ValidationException {
        if (value == null) {
            throw new ValidationException(null, fieldName + " muss angegeben werden.");
        }
        if (value < 0) {
            throw new ValidationException(null, fieldName + " darf nicht negativ sein.");
        }
    }

    public enum IdType {
        AutoGeneratedUUID,
        Predefined
    }

    public enum CostType {
        FixedCosts,
        VariableCosts,
    }

    public enum QuantityType {
        FixedQuantity,
        VariableQuantity
    }

    public enum VariableValueCalculationType {
        NoCode,
        LowCode,
    }
}
