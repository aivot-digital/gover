package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.exceptions.DerivationException;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

@Service
public class ElementOverrideDerivationService {
    private final Logger logger = LoggerFactory.getLogger(ElementOverrideDerivationService.class);

    public BaseElement derive(
            @Nonnull BaseElement rootElement,
            @Nonnull ElementData accumulator,
            @Nonnull ElementDataObject dataObject,
            @Nonnull BaseElement currentElement,
            @Nonnull JavascriptEngine javascriptEngine,
            @Nonnull NoCodeEvaluationService noCodeEvaluationService,
            @Nonnull ElementDerivationLogger derivationLogger
    ) throws DerivationException {
        var override = currentElement.getOverride();

        if (override == null) {
            return null; // No override to derive if the element has no override
        }

        try {
            // Determine if override generation should be done with javascript code
            if (override.getJavascriptCode() != null && override.getJavascriptCode().isNotEmpty()) {
                var res = javascriptEngine
                        .registerGlobalContextObject(accumulator)
                        .registerElementObject(currentElement)
                        .evaluateCode(override.getJavascriptCode());

                // Check if the result is null, which indicates no override was generated
                if (res.isNull()) {
                    return null;
                }

                // Log result output
                derivationLogger.log(currentElement, res);

                // Check if the result is a map, which indicates a valid override
                var resObject = res.asMap();

                // If the result is not a map, no valid override was generated
                if (resObject != null) {
                    // Resolve the element from the map and check if a valid element was generated
                    var resolvedElement = ElementResolver
                            .resolve(resObject);
                    if (resolvedElement == null) {
                        throw new DerivationException(currentElement, "Der erzeugte Datensatz entspricht keinem bekannten Elementtyp");
                    }

                    // Overriding ids is not allowed, so we check if the ids match
                    if (!Objects.equals(currentElement.getId(), resolvedElement.getId())) {
                        throw new DerivationException(currentElement, "Das abgeleitete Element hat eine andere ID als das ursprüngliche Element");
                    }

                    // Overriding types is not allowed, so we check if the types match
                    if (!Objects.equals(currentElement.getType(), resolvedElement.getType())) {
                        throw new DerivationException(currentElement, "Das abgeleitete Element hat einen anderen Typ als das ursprüngliche Element");
                    }

                    // Return the resolved element as the override
                    return resolvedElement;
                } else {
                    return null;
                }
            }

            // Determine if override generation should be done with a no code expression
            if (override.getExpression() != null) {
                // TODO: Implement overriding by expressions
                throw new DerivationException(currentElement, "No-Code-Ausdrücke für dynamische Strukturen sind noch nicht erlaubt");
            }
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("An error occurred while deriving override for element")
                    .addKeyValue("elementId", currentElement.getId())
                    .setCause(e)
                    .log();
            throw new DerivationException(currentElement, "Bei der Erzeugung der dynamischen Struktur ist ein unbekannter Fehler aufgetreten. Die Fehlermeldung lautet: " + e.getMessage(), e);
        }

        return null;
    }
}
