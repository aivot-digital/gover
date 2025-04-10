package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class BaseElementVisibilityDerivationService<Ctx extends BaseElementDerivationContext> {
    private final Logger logger = LoggerFactory.getLogger(BaseElementVisibilityDerivationService.class);

    /**
     * Derive the visibilities of the current element and all its child elements.
     * The derived visibilities are stored in the context.
     *
     * @param context        The context in which the element is being derived.
     * @param idPrefix       The prefix that should be used for the id of the element.
     * @param currentElement The current element that is being derived.
     */
    public void derive(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull BaseElement currentElement
    ) {
        derive(context, idPrefix, currentElement, true);
    }

    /**
     * Derive the visibilities of the current element and all its child elements.
     * The derived visibilities are stored in the context.
     *
     * @param context         The context in which the element is being derived.
     * @param idPrefix        The prefix that should be used for the id of the element.
     * @param currentElement  The current element that is being derived.
     * @param isParentVisible The visibility of the parent element.
     */
    private void derive(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull BaseElement currentElement,
            @Nonnull Boolean isParentVisible
    ) {
        switch (currentElement) {
            case RootElement rootElement:
                deriveVisibilitiesForRootElement(context, idPrefix, rootElement, isParentVisible);
                break;

            case StepElement stepElement:
                deriveVisibilitiesForStepElement(context, idPrefix, stepElement, isParentVisible, false);
                break;

            case GroupLayout groupLayout:
                deriveVisibilitiesForGroupLayout(context, idPrefix, groupLayout, isParentVisible);
                break;

            case ReplicatingContainerLayout replicatingContainerLayout:
                deriveVisibilitiesForReplicatingContainerLayout(context, idPrefix, replicatingContainerLayout, isParentVisible);
                break;

            case BaseElement baseElement:
                deriveVisibilityForBaseElement(context, idPrefix, baseElement, isParentVisible);
                break;
        }
    }

    /**
     * Derive the visibilities of the root element and all its child elements.
     * The root itself is always visible.
     * Only the children are checked for visibility.
     *
     * @param context         The context in which the derivation is being done.
     * @param idPrefix        The prefix that should be used for the id of the element.
     * @param rootElement     The root element that is being derived.
     * @param isParentVisible The visibility of the parent element.
     */
    protected void deriveVisibilitiesForRootElement(
            @NotNull Ctx context,
            @Nullable String idPrefix,
            @Nonnull RootElement rootElement,
            Boolean isParentVisible
    ) {
        // Iterate over all children of the root element and derive their visibilities
        for (var element : rootElement.getChildren()) {
            derive(context, idPrefix, element, isParentVisible);
        }
    }

    /**
     * Derive the visibilities of the step element and all its child elements.
     * The children are only derived if the step element is visible.
     *
     * @param context         The context in which the derivation is being done.
     * @param idPrefix        The prefix that should be used for the id of the element.
     * @param stepElement     The step element that is being derived.
     * @param isParentVisible The visibility of the parent element.
     */
    protected void deriveVisibilitiesForStepElement(
            @NotNull Ctx context,
            @Nullable String idPrefix,
            @NotNull StepElement stepElement,
            @Nonnull Boolean isParentVisible,
            @Nonnull Boolean skipChildren
    ) {
        // Derive the visibilities of the step element itself
        var isVisible = deriveVisibilityForBaseElement(context, idPrefix, stepElement, isParentVisible);

        if (!skipChildren) {
            for (var element : stepElement.getChildren()) {
                derive(context, idPrefix, element, isParentVisible && isVisible);
            }
        }
    }

    /**
     * Derive the visibilities of the group layout and all its child elements.
     * The children are only derived if the group layout is visible.
     *
     * @param context         The context in which the derivation is being done.
     * @param idPrefix        The prefix that should be used for the id of the element.
     * @param groupLayout     The group layout that is being derived.
     * @param isParentVisible The visibility of the parent element.
     */
    protected void deriveVisibilitiesForGroupLayout(
            @NotNull Ctx context,
            @Nullable String idPrefix,
            GroupLayout groupLayout,
            Boolean isParentVisible
    ) {
        // Derive the visibilities of the group layout itself
        var isVisible = deriveVisibilityForBaseElement(context, idPrefix, groupLayout, isParentVisible);

        for (var element : groupLayout.getChildren()) {
            derive(context, idPrefix, element, isParentVisible && isVisible);
        }
    }

    /**
     * Derive the visibilities of the replicating container layout and all its child elements.
     * The visibilities are derived for each dataset in the replicating container layout.
     * The children are only derived if the replicating container layout is visible.
     *
     * @param context                    The context in which the derivation is being done.
     * @param idPrefix                   The prefix that should be used for the id of the element.
     * @param replicatingContainerLayout The replicating container layout that is being derived.
     * @param isParentVisible            The visibility of the parent element.
     */
    protected void deriveVisibilitiesForReplicatingContainerLayout(
            @NotNull Ctx context,
            @Nullable String idPrefix,
            @NotNull ReplicatingContainerLayout replicatingContainerLayout,
            Boolean isParentVisible
    ) {
        // Derive the visibilities of the replicating container layout itself
        var isVisible = deriveVisibilityForBaseElement(context, idPrefix, replicatingContainerLayout, isParentVisible);

        var resolvedId = replicatingContainerLayout.getResolvedId(idPrefix);

        // Get the value of the replicating container layout
        // This value should be a collection of prefixes for the children
        // Each prefix represents a single dataset in the replicating container
        var replicatingContainerValue = context.getValue(resolvedId, Collection.class);

        // If no child datasets are present, no further derivation is necessary
        if (replicatingContainerValue.isEmpty()) {
            return;
        }

        // Derive the visibilities of the children for each dataset
        for (var childId : replicatingContainerValue.get()) {
            for (var element : replicatingContainerLayout.getChildren()) {
                derive(context, resolvedId + "_" + childId.toString(), element, isParentVisible && isVisible);
            }
        }
    }

    /**
     * Derive the visibility of the base element.
     * The visibility is derived from the visibility code, visibility expression, and is visible function.
     * The derived visibility is stored in the context.
     *
     * @param context     The context in which the derivation is being done.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param baseElement The base element for which the visibility should be derived.
     * @return The derived visibility of the base element.
     */
    protected boolean deriveVisibilityForBaseElement(
            @NotNull Ctx context,
            @Nullable String idPrefix,
            @NotNull BaseElement baseElement,
            @Nonnull Boolean isParentVisible
    ) {
        // If the parent is not visible, the element itself is not visible
        if (!isParentVisible) {
            context.getElementDerivationData().setVisibility(baseElement.getResolvedId(idPrefix), false);
            return false;
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

            // Return the derived visibility
            return isElementVisible != null && isElementVisible;
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("An error occurred while deriving the visibility for element")
                    .addKeyValue("elementId", baseElement.getResolvedId(idPrefix))
                    .setCause(e)
                    .log();
            context.getElementDerivationData().setError(baseElement.getResolvedId(idPrefix), e.getMessage());
            return true;
        }
    }
}
