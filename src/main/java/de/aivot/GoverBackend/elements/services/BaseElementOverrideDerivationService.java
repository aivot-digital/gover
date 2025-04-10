package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.utils.ElementResolver;
import de.aivot.GoverBackend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class BaseElementOverrideDerivationService<Ctx extends BaseElementDerivationContext> {
    private final Logger logger = LoggerFactory.getLogger(BaseElementOverrideDerivationService.class);

    /**
     * Derive the overrides of the current element and all its child elements.
     * The derived overrides are stored in the context.
     * Invisible elements are not overridden.
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
        switch (currentElement) {
            case RootElement rootElement:
                deriveOverridesForRootElement(context, idPrefix, rootElement);
                break;

            case StepElement stepElement:
                deriveOverridesForStepElement(context, idPrefix, stepElement, false);
                break;

            case GroupLayout groupLayout:
                deriveOverridesForGroupLayout(context, idPrefix, groupLayout);
                break;

            case ReplicatingContainerLayout replicatingContainerLayout:
                deriveOverridesForReplicatingContainerLayout(context, idPrefix, replicatingContainerLayout);
                break;

            case BaseElement baseElement:
                deriveOverrideForBaseElement(context, idPrefix, baseElement);
                break;
        }
    }

    /**
     * Derive the overrides for all child elements of the root element.
     * The root element itself cannot be overridden.
     * The root element is also always visible so no visibility check is done.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param rootElement The root element that is being derived.
     */
    protected void deriveOverridesForRootElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull RootElement rootElement
    ) {
        for (var element : rootElement.getChildren()) {
            derive(context, idPrefix, element);
        }
    }

    /**
     * Derive the overrides for all child elements of the step element.
     * This element and its child elements are only overridden if the element is visible.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param stepElement The step element that is being derived.
     */
    protected void deriveOverridesForStepElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull StepElement stepElement,
            @Nonnull Boolean skipChildren
    ) {
        // Invisible elements don't need to be overridden and their children don't need to be derived
        if (context.isInvisible(stepElement.getResolvedId(idPrefix))) {
            return;
        }

        // Derive the override for the step element itself
        deriveOverrideForBaseElement(context, idPrefix, stepElement);

        // Derive the overrides for all children of the step element
        if (!skipChildren) {
            for (var element : stepElement.getChildren()) {
                derive(context, idPrefix, element);
            }
        }
    }

    /**
     * Derive the overrides for all child elements of the group layout.
     * This element and its child elements are only overridden if the element is visible.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param groupLayout The group layout that is being derived.
     */
    protected void deriveOverridesForGroupLayout(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull GroupLayout groupLayout
    ) {
        // Invisible elements don't need to be overridden and their children don't need to be derived
        if (context.isInvisible(groupLayout.getResolvedId(idPrefix))) {
            return;
        }

        // Derive the override for the group layout itself
        deriveOverrideForBaseElement(context, idPrefix, groupLayout);

        // Derive the overrides for all children of the group layout
        for (var element : groupLayout.getChildren()) {
            derive(context, idPrefix, element);
        }
    }

    /**
     * Derive the overrides for all child elements of the replicating container layout.
     * This element and its child elements are only overridden if the element is visible.
     * The overrides are derived for each dataset in the replicating container layout.
     * The children are only derived if the replicating container layout is visible.
     *
     * @param context                    The context in which the element is being derived.
     * @param idPrefix                   The prefix that should be used for the id of the element.
     * @param replicatingContainerLayout The replicating container layout that is being derived.
     */
    protected void deriveOverridesForReplicatingContainerLayout(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull ReplicatingContainerLayout replicatingContainerLayout
    ) {
        var resolvedId = replicatingContainerLayout.getResolvedId(idPrefix);

        // Invisible elements don't need to be overridden and their children don't need to be derived
        if (context.isInvisible(resolvedId)) {
            return;
        }

        // Derive the override for the replicating container layout itself
        deriveOverrideForBaseElement(context, idPrefix, replicatingContainerLayout);

        // Get the value of the replicating container layout
        // This value should be a collection of prefixes for the children
        // Each prefix represents a single dataset in the replicating container
        var replicatingContainerValue = context.getValue(resolvedId, Collection.class);

        // If no child datasets are present, no further derivation is necessary
        if (replicatingContainerValue.isEmpty()) {
            return;
        }

        // Derive the overrides for all children of the replicating container layout
        for (var childId : replicatingContainerValue.get()) {
            for (var element : replicatingContainerLayout.getChildren()) {
                var childPrefix = resolvedId + "_" + childId.toString();
                derive(context, childPrefix, element);
            }
        }
    }

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
    private void deriveOverrideForBaseElement(
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
