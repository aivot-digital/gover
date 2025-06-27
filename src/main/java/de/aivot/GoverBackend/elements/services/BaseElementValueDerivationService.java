package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BaseElementValueDerivationService<Ctx extends BaseElementDerivationContext> {
    private final Logger logger = LoggerFactory.getLogger(BaseElementValueDerivationService.class);

    /**
     * Derives the value for the given base element.
     * The value is derived based on the value code, value expression or compute value of the base element.
     * The derived value is stored in the context.
     * No value is derived for invisible elements.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param baseElement The base element that is being derived.
     */
    public void deriveValueForElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull BaseInputElement<?> baseElement
    ) {
        var resolvedId = baseElement.getResolvedId(idPrefix);

        try {
            // Invisible elements don't need a value derivation
            if (context.isInvisible(resolvedId)) {
                return;
            }

            // Determine if the value computation should be done with javascript code
            if (baseElement.getValueCode() != null && baseElement.getValueCode().isNotEmpty()) {
                var value = context
                        .getJavascriptEngine()
                        .registerGlobalContextObject(
                                context.getJavascriptContextObject(resolvedId, baseElement)
                        )
                        .evaluateCode(baseElement.getValueCode())
                        .asObject();

                // Set the value here to avoid settings values to null, when no computation was present
                context.setValue(resolvedId, value);
            }

            // Determine if the value computation should be done with a value expression
            else if (baseElement.getValueExpression() != null) {
                var value = context
                        .getNoCodeEvaluationService()
                        .evaluate(
                                baseElement.getValueExpression(),
                                context.getElementDerivationData(),
                                idPrefix
                        )
                        .getValue();

                // Set the value here to avoid settings values to null, when no computation was present
                context.setValue(resolvedId, value);
            }

            // Determine if the value computation should be done with a compute value function
            else if (
                    baseElement.getComputeValue() != null &&
                    StringUtils.isNotNullOrEmpty(baseElement.getComputeValue().getCode())
            ) {
                var value = baseElement
                        .getComputeValue()
                        .evaluate(
                                idPrefix,
                                baseElement,
                                context
                        )
                        .getObjectValue();

                // Set the value here to avoid settings values to null, when no computation was present
                context.setValue(resolvedId, value);
            }
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("An error occurred while deriving the value for element")
                    .addKeyValue("elementId", resolvedId)
                    .setCause(e)
                    .log();
            context.getElementDerivationData().setError(resolvedId, e.getMessage());
        }
    }
}