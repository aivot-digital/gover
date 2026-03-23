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

import jakarta.annotation.Nonnull;

@Service
public class ElementValueDerivationService {
    private final Logger logger = LoggerFactory.getLogger(ElementValueDerivationService.class);

    public Object derive(
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData accumulator,
            @Nonnull ElementDataObject dataObject,
            @Nonnull BaseInputElement<?> currentElement,
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull NoCodeEvaluationService noCodeEvaluationService,
            @Nonnull ElementDerivationLogger derivationLogger
    ) throws DerivationException {
        var val = currentElement.getValue();

        if (val == null) {
            return null; // No value to derive if the element has no value setter
        }

        try {
            // Determine if the value computation should be done with javascript code
            if (val.getJavascriptCode() != null && val.getJavascriptCode().isNotEmpty()) {
                var res = javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerElementObject(currentElement)
                        .evaluateCode(val.getJavascriptCode());

                derivationLogger.log(currentElement, res);

                return res.asObject();
            }

            // Determine if the value computation should be done with a value expression
            if (val.getNoCode() != null) {
                return noCodeEvaluationService
                        .evaluate(val.getNoCode(), accumulator)
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