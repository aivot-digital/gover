package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class BaseElementOverrideDerivationService<Ctx extends BaseElementDerivationContext> {
    private final Logger logger = LoggerFactory.getLogger(BaseElementOverrideDerivationService.class);

    /**
     * Derive the override for the current base element.
     * The override is derived based on the override code, override expression or patch element of the element.
     * The derived override is stored in the context.
     * If the element is invisible, no override is derived.
     *
     * @param context        The context in which the element is being derived.
     * @param idPrefix       The prefix that should be used for the id of the element.
     * @param currentElement The current element that is being derived.
     */
    public void deriveOverrideForElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull BaseElement currentElement
    ) {
        var resolvedId = currentElement.getResolvedId(idPrefix);

        try {
            // Invisible elements don't need to be overridden
            if (context.isInvisible(resolvedId)) {
                return;
            }

            BaseElement override = null;

            // Determine if override generation should be done with javascript code
            if (currentElement.getOverrideCode() != null && currentElement.getOverrideCode().isNotEmpty()) {
                var res = context
                        .getJavascriptEngine()
                        .registerGlobalContextObject(
                                context.getJavascriptContextObject(resolvedId, currentElement)
                        )
                        .evaluateCode(currentElement.getOverrideCode());
                if (res.isNull()) {
                    return;
                }
                var resObject = res.asMap();
                if (resObject != null) {
                    var resolvedElement = ElementResolver.resolve(resObject);
                    if (resolvedElement == null) {
                        throw new RuntimeException("The patch code did not return a valid element");
                    }
                    override = resolvedElement;
                }
            }

            // Determine if override generation should be done with a no code expression
            else if (currentElement.getOverrideExpression() != null) {
                // TODO: Implement overriding by expressions
                throw new RuntimeException("Expressions are not yet supported for patching");
            }

            // Determine if visibility calculation should be done with a function
            else if (
                    currentElement.getPatchElement() != null &&
                    StringUtils.isNotNullOrEmpty(currentElement.getPatchElement().getCode())
            ) {
                var res = currentElement.getPatchElement().evaluate(
                        idPrefix,
                        currentElement,
                        context
                );
                if (res.isNull()) {
                    return;
                }
                var resObject = res.getJsonValue();
                var resolvedElement = ElementResolver.resolve(resObject);
                if (resolvedElement == null) {
                    throw new RuntimeException("The patch element did not return a valid element");
                }
                override = resolvedElement;
            }

            // If an override was derived, store it in the context
            if (override != null) {
                context.getElementDerivationData().setOverride(resolvedId, override);
            }
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("An error occurred while deriving override for element")
                    .addKeyValue("elementId", resolvedId)
                    .setCause(e)
                    .log();
            context.getElementDerivationData().setError(resolvedId, e.getMessage());
        }
    }
}
