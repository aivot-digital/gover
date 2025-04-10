package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class BaseElementValueDerivationService<Ctx extends BaseElementDerivationContext> {
    private final Logger logger = LoggerFactory.getLogger(BaseElementValueDerivationService.class);

    /**
     * Derives the values for the given element and its children.
     * The derived values are stored in the context.
     * No values are derived for invisible elements.
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
                deriveValuesForRootElement(context, idPrefix, rootElement);
                break;

            case StepElement stepElement:
                deriveValuesForStepElement(context, idPrefix, stepElement);
                break;

            case GroupLayout groupLayout:
                deriveValuesForGroupLayout(context, idPrefix, groupLayout);
                break;

            case ReplicatingContainerLayout replicatingContainerLayout:
                deriveValuesForReplicatingContainerLayout(context, idPrefix, replicatingContainerLayout);
                break;

            case BaseInputElement<?> baseElement:
                deriveValueForBaseElement(context, idPrefix, baseElement);
                break;

            default:
                // No value derivation for other element types necessary.
                break;
        }
    }

    /**
     * Derives the values for all child elements of the root element.
     * The root element has no value and is always visible.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param rootElement The root element that is being derived.
     */
    protected void deriveValuesForRootElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull RootElement rootElement
    ) {
        for (var element : rootElement.getChildren()) {
            derive(context, idPrefix, element);
        }
    }

    /**
     * Derives the values for all child elements of the step element.
     * The step element has no value.
     * If the step element is invisible, no value derivation for its children is necessary.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param stepElement The step element that is being derived.
     */
    protected void deriveValuesForStepElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull StepElement stepElement
    ) {
        // Invisible steps and their children don't need a value derivation.
        if (context.isInvisible(stepElement.getResolvedId(idPrefix))) {
            return;
        }

        // Derive the values for all children of the step element.
        for (var element : stepElement.getChildren()) {
            derive(context, idPrefix, element);
        }
    }

    /**
     * Derives the values for all child elements of the group layout.
     * The group layout has no value.
     * If the group layout is invisible, no value derivation for its children is necessary.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param groupLayout The group layout that is being derived.
     */
    protected void deriveValuesForGroupLayout(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull GroupLayout groupLayout
    ) {
        // Invisible group layouts and their children don't need a value derivation.
        if (context.isInvisible(groupLayout.getResolvedId(idPrefix))) {
            return;
        }

        for (var element : groupLayout.getChildren()) {
            derive(context, idPrefix, element);
        }
    }

    /**
     * Derives the values for all child elements of the replicating container layout.
     * The replicating container layout has a value which can be derived.
     * The value is a collection of prefixes for the children.
     * Each prefix represents a single dataset in the replicating container.
     * If the replicating container layout is invisible, no value derivation for its children is necessary.
     * If no child datasets are present, no further derivation is necessary.
     *
     * @param context                    The context in which the element is being derived.
     * @param idPrefix                   The prefix that should be used for the id of the element.
     * @param replicatingContainerLayout The replicating container layout that is being derived.
     */
    protected void deriveValuesForReplicatingContainerLayout(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull ReplicatingContainerLayout replicatingContainerLayout
    ) {
        var resolvedId = replicatingContainerLayout.getResolvedId(idPrefix);

        // Invisible replicating container layouts and their children don't need a value derivation.
        if (context.isInvisible(resolvedId)) {
            return;
        }

        // Derive the value for the base element of the replicating container layout
        deriveValueForBaseElement(context, idPrefix, replicatingContainerLayout);

        // Get the value of the replicating container layout
        // This value should be a collection of prefixes for the children
        // Each prefix represents a single dataset in the replicating container
        var replicatingContainerValue = context.getValue(resolvedId, Collection.class);

        // If no child datasets are present, no further derivation is necessary
        if (replicatingContainerValue.isEmpty()) {
            return;
        }

        // Derive the values for all children of the replicating container layout
        for (var childId : replicatingContainerValue.get()) {
            for (var element : replicatingContainerLayout.getChildren()) {
                var childPrefix = resolvedId + "_" + childId.toString();
                derive(context, childPrefix, element);
            }
        }
    }

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
    private void deriveValueForBaseElement(
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