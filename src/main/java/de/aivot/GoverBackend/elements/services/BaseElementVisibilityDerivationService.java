package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class BaseElementVisibilityDerivationService<Ctx extends BaseElementDerivationContext> {
    private final Logger logger = LoggerFactory.getLogger(BaseElementVisibilityDerivationService.class);

    /**
     * Derive the visibility of the base element.
     * The visibility is derived from the visibility code, visibility expression, and is visible function.
     * The derived visibility is stored in the context.
     *
     * @param context     The context in which the derivation is being done.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param baseElement The base element for which the visibility should be derived.
     */
    public void deriveVisibilityForElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull BaseElement baseElement,
            @Nonnull Boolean isParentVisible
    ) {
        // If the parent is not visible, the element itself is not visible
        if (!isParentVisible) {
            context.getElementDerivationData().setVisibility(baseElement.getResolvedId(idPrefix), false);
            return;
        }

        try {
            Boolean isElementVisible = true;

            // Determine if visibility calculation should be done with javascript code
            if (baseElement.getVisibilityCode() != null && baseElement.getVisibilityCode().isNotEmpty()) {
                isElementVisible = context
                        .getJavascriptEngine()
                        .registerGlobalContextObject(
                                context.getJavascriptContextObject(baseElement.getResolvedId(idPrefix), baseElement)
                        )
                        .evaluateCode(baseElement.getVisibilityCode())
                        .asBoolean();
            }

            // Determine if visibility calculation should be done with a no code expression
            else if (baseElement.getVisibilityExpression() != null) {
                isElementVisible = context
                        .getNoCodeEvaluationService()
                        .evaluate(baseElement.getVisibilityExpression(), context.getElementDerivationData(), idPrefix)
                        .getValueAsBoolean();
            }

            // Determine if visibility calculation should be done with a function
            else if (baseElement.getIsVisible() != null && (
                    baseElement.getIsVisible() instanceof FunctionCode functionCode && StringUtils.isNotNullOrEmpty(functionCode.getCode()) ||
                    baseElement.getIsVisible() instanceof FunctionNoCode functionNoCode && functionNoCode.getConditionSet() != null
            )) {
                var res = baseElement
                        .getIsVisible()
                        .evaluate(
                                idPrefix,
                                baseElement,
                                context
                        );

                if (res.isNoCodeResult()) {
                    isElementVisible = res.getObjectValue() == null;
                } else {
                    isElementVisible = res.getBooleanValue();
                }
            }

            // Store the derived visibility in the context
            context.getElementDerivationData().setVisibility(baseElement.getResolvedId(idPrefix), isElementVisible);
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("An error occurred while deriving the visibility for element")
                    .addKeyValue("elementId", baseElement.getResolvedId(idPrefix))
                    .setCause(e)
                    .log();
            context.getElementDerivationData().setError(baseElement.getResolvedId(idPrefix), e.getMessage());
        }
    }
}
