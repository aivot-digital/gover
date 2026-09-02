package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.HashMap;
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
    public Map<String, Object> performValidation() {
        var errors = new HashMap<String, Object>();

        requireText(errors, "description", description, "Beschreibung");
        requireText(errors, "reference", reference, "Referenz");

        if (idType == null) {
            errors.put("idType", "Es muss ein ID-Typ ausgewählt werden.");
        }
        if (idType == IdType.Predefined) {
            requireText(errors, "predefinedId", predefinedId, "Vordefinierte ID");
        }

        if (costType == null) {
            errors.put("costType", "Es muss ein Kostentyp ausgewählt werden.");
        } else if (costType == CostType.FixedCosts) {
            requireNotNegative(errors, "fixedCosts", fixedCosts, "Feste Kosten");
        } else if (costType == CostType.VariableCosts) {
            validateVariableCalculation(
                    errors,
                    "variableCostsCalculationType",
                    "variableCostsNoCodeCalculation",
                    "variableCostsLowCodeCalculation",
                    variableCostsCalculationType,
                    variableCostsNoCodeCalculation,
                    variableCostsLowCodeCalculation,
                    "Variable Kosten"
            );
        }

        if (quantityType == null) {
            errors.put("quantityType", "Es muss ein Mengentyp ausgewählt werden.");
        } else if (quantityType == QuantityType.FixedQuantity) {
            requireNotNegative(errors, "fixedQuantity", fixedQuantity, "Feste Menge");
        } else if (quantityType == QuantityType.VariableQuantity) {
            validateVariableCalculation(
                    errors,
                    "variableQuantityCalculationType",
                    "variableQuantityNoCodeCalculation",
                    "variableQuantityLowCodeCalculation",
                    variableQuantityCalculationType,
                    variableQuantityNoCodeCalculation,
                    variableQuantityLowCodeCalculation,
                    "Variable Menge"
            );
        }

        if (fixedTaxRate == null) {
            errors.put("fixedTaxRate", "Es muss ein Steuersatz angegeben werden.");
        } else if (fixedTaxRate.compareTo(BigDecimal.ZERO) < 0 || fixedTaxRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            errors.put("fixedTaxRate", "Der Steuersatz muss zwischen 0 und 100 liegen.");
        }

        if (additionalBookingData != null) {
            for (var entry : additionalBookingData.entrySet()) {
                if (StringUtils.isNullOrEmpty(entry.getKey())) {
                    errors.put("additionalBookingData", "Buchungsdaten dürfen keine leeren Schlüssel enthalten.");
                }
            }
        }

        return errors;
    }

    private void validateVariableCalculation(
            Map<String, Object> errors,
            String calculationTypeKey,
            String noCodeCalculationKey,
            String lowCodeCalculationKey,
            @Nullable VariableValueCalculationType calculationType,
            @Nullable NoCodeOperand noCodeCalculation,
            @Nullable JavascriptCode lowCodeCalculation,
            String fieldName
    ) {
        if (calculationType == null) {
            errors.put(calculationTypeKey, fieldName + " benötigen einen Berechnungstyp.");
            return;
        }

        if (calculationType == VariableValueCalculationType.NoCode) {
            if (noCodeCalculation == null) {
                errors.put(noCodeCalculationKey, fieldName + " benötigen eine No-Code-Berechnung.");
                return;
            }

            var noCodeError = noCodeCalculation.validate();
            if (!noCodeError.isValid()) {
                errors.put(noCodeCalculationKey, fieldName + " enthalten einen ungültigen No-Code-Ausdruck.");
            }
        } else if (lowCodeCalculation == null || lowCodeCalculation.isEmpty()) {
            errors.put(lowCodeCalculationKey, fieldName + " benötigen eine Low-Code-Berechnung.");
        }
    }

    private void requireText(Map<String, Object> errors, String fieldKey, @Nullable String value, String fieldName) {
        if (StringUtils.isNullOrEmpty(value)) {
            errors.put(fieldKey, fieldName + " muss angegeben werden.");
        }
    }

    private void requireNotNegative(Map<String, Object> errors, String fieldKey, @Nullable BigDecimal value, String fieldName) {
        if (value == null) {
            errors.put(fieldKey, fieldName + " müssen angegeben werden.");
        } else if (value.compareTo(BigDecimal.ZERO) < 0) {
            errors.put(fieldKey, fieldName + " dürfen nicht negativ sein.");
        }
    }

    private void requireNotNegative(Map<String, Object> errors, String fieldKey, @Nullable Long value, String fieldName) {
        if (value == null) {
            errors.put(fieldKey, fieldName + " muss angegeben werden.");
        } else if (value < 0) {
            errors.put(fieldKey, fieldName + " darf nicht negativ sein.");
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
