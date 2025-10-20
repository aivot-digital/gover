package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.exceptions.DerivationException;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.ElementWithChildren;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class ElementVisibilityDerivationService {
    private final Logger logger = LoggerFactory
            .getLogger(ElementVisibilityDerivationService.class);

    public boolean derive(
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData elementData,
            @Nonnull BaseElement baseElement,
            @Nonnull Boolean isParentVisible,
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull NoCodeEvaluationService noCodeEvaluationService,
            @Nonnull ElementDerivationLogger derivationLogger
    ) throws DerivationException {
        // If the parent is not visible, the element itself is not visible
        if (!isParentVisible) {
            return false;
        }

        try {
            return performDerivation(
                    rootElement,
                    elementData,
                    baseElement,
                    javascriptEngine,
                    noCodeEvaluationService,
                    derivationLogger
            );
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("An error occurred while deriving the visibility for element")
                    .addKeyValue("elementId", baseElement.getId())
                    .setCause(e)
                    .log();
            throw new DerivationException(baseElement, "Bei der Erzeugung der dynamischen Struktur ist ein unbekannter Fehler aufgetreten. Die Fehlermeldung lautet: " + e.getMessage(), e);
        }
    }

    private boolean performDerivation(
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData elementData,
            @Nonnull BaseElement baseElement,
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull NoCodeEvaluationService noCodeEvaluationService,
            @Nonnull ElementDerivationLogger derivationLogger
    ) throws Exception {
        var vis = baseElement.getVisibility();

        if (vis == null) {
            return true;
        }

        // Determine if visibility calculation should be done with javascript code
        if (vis.getJavascriptCode() != null && vis.getJavascriptCode().isNotEmpty()) {
            var res = javascriptEngine
                    .registerGlobalContextObject(elementData)
                    .registerElementObject(baseElement)
                    .evaluateCode(vis.getJavascriptCode());

            derivationLogger.log(baseElement, res);

            var bl = res.asBoolean();

            return bl == null || bl;
        }

        // Determine if visibility calculation should be done with a no code expression
        if (vis.getNoCode() != null) {
            return noCodeEvaluationService
                    .evaluate(vis.getNoCode(), elementData)
                    .getValueAsBoolean();
        }

        // Determine if visibility calculation should be done with a function
        if (vis.getConditionSet() != null && rootElement instanceof ElementWithChildren<?> elementWithChildren) {
            var res = vis
                    .getConditionSet()
                    .evaluate(
                            elementWithChildren,
                            elementData,
                            baseElement
                    );

            return res == null;
        }

        return true;
    }
}
