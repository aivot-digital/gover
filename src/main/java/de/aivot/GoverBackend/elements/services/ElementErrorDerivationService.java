package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.exceptions.DerivationException;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.javascript.exceptions.JavascriptException;
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
            @Nonnull NoCodeEvaluationService noCodeEvaluationService,
            @Nonnull ElementDerivationLogger derivationLogger
    ) throws DerivationException {
        try {
            return deriveInputElement(rootElement,
                    accumulator,
                    dataObject,
                    currentElement,
                    javascriptEngine,
                    noCodeEvaluationService,
                    derivationLogger);
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
                                      @Nonnull NoCodeEvaluationService noCodeEvaluationService,
                                      @Nonnull ElementDerivationLogger derivationLogger) throws JavascriptException {
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
            var res = javascriptEngine
                    .registerGlobalContextObject(accumulator)
                    .registerElementObject(currentInputElement)
                    .evaluateCode(validation.getJavascriptCode());

            derivationLogger.log(currentInputElement, res);

            return res.asString();
        }

        if (validation.getNoCodeList() != null && !validation.getNoCodeList().isEmpty()) {
            for (var validationExpression : validation.getNoCodeList()) {
                var res = noCodeEvaluationService
                        .evaluate(validationExpression.getNoCode(), accumulator);
                if (!res.getValueAsBoolean()) {
                    return validationExpression.getMessage();
                }
            }
        }

        if (validation.getConditionSet() != null && rootElement instanceof LayoutElement<?> elementWithChildren) {
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
