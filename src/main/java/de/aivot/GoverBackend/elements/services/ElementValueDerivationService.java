package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.exceptions.DerivationException;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service
public class ElementValueDerivationService {
    private final Logger logger = LoggerFactory.getLogger(ElementValueDerivationService.class);

    public Object derive(
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData accumulator,
            @Nonnull ElementDataObject dataObject,
            @Nonnull BaseInputElement<?> currentElement,
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull NoCodeEvaluationService noCodeEvaluationService
    ) throws DerivationException {
        var val = currentElement.getValue();

        if (val == null) {
            return null; // No value to derive if the element has no value setter
        }

        try {
            // Determine if the value computation should be done with javascript code
            if (val.getJavascriptCode() != null && val.getJavascriptCode().isNotEmpty()) {
                return javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerElementObject(currentElement)
                        .evaluateCode(val.getJavascriptCode())
                        .asObject();
            }

            // Determine if the value computation should be done with a value expression
            if (val.getExpression() != null && val.getExpression().isNotEmpty()) {
                return noCodeEvaluationService
                        .evaluate(val.getExpression(), accumulator)
                        .getValue();
            }
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("An error occurred while deriving the value for element")
                    .addKeyValue("elementId", currentElement.getId())
                    .setCause(e)
                    .log();
            throw new DerivationException(currentElement, "Bei der Erzeugung der dynamischen Struktur ist ein unbekannter Fehler aufgetreten. Die Fehlermeldung lautet: " + e.getMessage(), e);
        }

        return null;
    }
}