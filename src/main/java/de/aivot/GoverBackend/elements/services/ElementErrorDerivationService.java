package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.exceptions.DerivationException;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.ElementWithChildren;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ElementErrorDerivationService {
    private final Logger logger = LoggerFactory.getLogger(ElementErrorDerivationService.class);

    public String derive(
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData accumulator,
            @Nonnull ElementDataObject dataObject,
            @Nonnull BaseInputElement<?> currentElement,
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull NoCodeEvaluationService noCodeEvaluationService
    ) throws DerivationException {
        try {
            return deriveInputElement(rootElement,
                    accumulator,
                    dataObject,
                    currentElement,
                    javascriptEngine,
                    noCodeEvaluationService);
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("An error occurred while deriving error for element")
                    .addKeyValue("elementId", currentElement.getId())
                    .setCause(e)
                    .log();
            throw new DerivationException(currentElement, "Bei der Erzeugung der dynamischen Struktur ist ein unbekannter Fehler aufgetreten. Die Fehlermeldung lautet: " + e.getMessage(), e);
        }
    }

    private String deriveInputElement(@Nonnull BaseElement rootElement,
                                      @Nonnull ElementData accumulator,
                                      @Nonnull ElementDataObject dataObject,
                                      @Nonnull BaseInputElement<?> currentInputElement,
                                      @Nonnull JavascriptEngine javascriptEngine,
                                      @Nonnull NoCodeEvaluationService noCodeEvaluationService) {
        var value = dataObject
                .getValue();

        if (value == null && !Boolean.TRUE.equals(currentInputElement.getRequired())) {
            return null; // No error if the input is not required and no value is provided
        }

        try {
            currentInputElement.validate(value);
        } catch (ValidationException e) {
            return e.getMessage();
        }

        if (currentInputElement.getValidation() == null) {
            return null;
        }

        var validation = currentInputElement
                .getValidation();

        if (validation.getJavascriptCode() != null && validation.getJavascriptCode().isNotEmpty()) {
            return javascriptEngine
                    .registerGlobalContextObject(accumulator)
                    .evaluateCode(validation.getJavascriptCode())
                    .asString();
        }

        if (validation.getValidationExpressions() != null && !validation.getValidationExpressions().isEmpty()) {
            for (var validationExpression : validation.getValidationExpressions()) {
                var res = noCodeEvaluationService
                        .evaluate(validationExpression.getExpression(), accumulator);
                if (!res.getValueAsBoolean()) {
                    return validationExpression.getMessage();
                }
            }
        }

        if (validation.getConditionSet() != null && rootElement instanceof ElementWithChildren<?> elementWithChildren) {
            return validation
                    .getConditionSet()
                    .evaluate(
                            elementWithChildren,
                            accumulator,
                            currentInputElement
                    );
        }

        return null;
    }
}
