package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class BaseElementErrorDerivationService<Ctx extends BaseElementDerivationContext> {
    private final Logger logger = LoggerFactory.getLogger(BaseElementErrorDerivationService.class);

    /**
     * Derives the errors for the given element and its children.
     * The errors are derived based on the current values, visibilities and overrides.
     * The derived errors are stored in the context.
     * No error derivation is done for invisible elements.
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
                deriveErrorsForRootElement(context, idPrefix, rootElement);
                break;

            case StepElement stepElement:
                deriveErrorsForStepElement(context, idPrefix, stepElement);
                break;

            case GroupLayout groupLayout:
                deriveErrorsForGroupLayout(context, idPrefix, groupLayout);
                break;

            case ReplicatingContainerLayout replicatingContainerLayout:
                deriveErrorsForReplicatingContainerLayout(context, idPrefix, replicatingContainerLayout);
                break;

            case BaseInputElement<?> baseElement:
                deriveErrorForBaseInputElement(context, idPrefix, baseElement);
                break;

            default:
                // No error derivation for other element types necessary.
                break;
        }
    }

    /**
     * Derives the errors for all child elements of the root element.
     * The root element is always visible and has no errors.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param rootElement The root element that is being derived.
     */
    protected void deriveErrorsForRootElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull RootElement rootElement
    ) {
        for (var element : rootElement.getChildren()) {
            derive(context, idPrefix, element);
        }
    }

    /**
     * Derives the errors for all child elements of the step element.
     * Invisible steps and their children don't need error derivation.
     * Steps have no errors themselves.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param stepElement The step element that is being derived.
     */
    protected void deriveErrorsForStepElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull StepElement stepElement
    ) {
        // Invisible steps and their children don't need error derivation
        if (context.isInvisible(stepElement.getResolvedId(idPrefix))) {
            return;
        }

        for (var element : stepElement.getChildren()) {
            derive(context, idPrefix, element);
        }
    }

    /**
     * Derives the errors for all child elements of the group layout.
     * Invisible group layouts and their children don't need error derivation.
     * Group layouts have no errors themselves.
     *
     * @param context     The context in which the element is being derived.
     * @param idPrefix    The prefix that should be used for the id of the element.
     * @param groupLayout The group layout that is being derived.
     */
    protected void deriveErrorsForGroupLayout(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull GroupLayout groupLayout
    ) {
        // Invisible groups and their children don't need error derivation
        if (context.isInvisible(groupLayout.getResolvedId(idPrefix))) {
            return;
        }

        for (var element : groupLayout.getChildren()) {
            derive(context, idPrefix, element);
        }
    }

    /**
     * Derives the errors for all child elements of the replicating container layout.
     * Invisible replicating container layouts and their children don't need error derivation.
     * Replicating container layouts can have errors themselves.
     *
     * @param context                    The context in which the element is being derived.
     * @param idPrefix                   The prefix that should be used for the id of the element.
     * @param replicatingContainerLayout The replicating container layout that is being derived.
     */
    protected void deriveErrorsForReplicatingContainerLayout(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull ReplicatingContainerLayout replicatingContainerLayout
    ) {
        var resolvedId = replicatingContainerLayout.getResolvedId(idPrefix);

        // Invisible replicating container layouts and their children don't need error derivation
        if (context.isInvisible(resolvedId)) {
            return;
        }

        deriveErrorForBaseInputElement(context, idPrefix, replicatingContainerLayout);

        var replicatingContainerValue = context.getValue(resolvedId, Collection.class);

        if (replicatingContainerValue.isEmpty()) {
            return;
        }

        for (var childId : replicatingContainerValue.get()) {
            for (var element : replicatingContainerLayout.getChildren()) {
                derive(context, resolvedId + "_" + childId.toString(), element);
            }
        }
    }

    /**
     * Derive the value of the current input element.
     *
     * @param context         The context in which the form is being derived.
     * @param idPrefix        The prefix that should be used for the id of the element.
     * @param originalElement The current element that is being derived.
     */
    protected void deriveErrorForBaseInputElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull BaseInputElement<?> originalElement
    ) {
        var resolvedId = originalElement.getResolvedId(idPrefix);

        try {
            // Invisible elements are always valid and need no error derivation
            if (context.isInvisible(resolvedId)) {
                return;
            }

            // If no value is present, the element is valid if it is not required
            if (!context.hasValue(resolvedId) && !Boolean.TRUE.equals(originalElement.getRequired())) {
                return;
            }

            var elemRaw = context
                    .getElementDerivationData()
                    .getOverride(resolvedId, originalElement);

            if (!(elemRaw instanceof BaseInputElement<?>)) {
                context.setError(resolvedId, "Element is not a BaseInputElement");
                return;
            }

            var currentElement = (BaseInputElement<?>) elemRaw;

            // Apply the standard validations for the specific input element type
            try {
                currentElement.validate(idPrefix, context);
            } catch (ValidationException e) {
                context.setError(resolvedId, e.getMessage());
                return;
            }

            // Determine if the element is valid based on the validation code, expressions or function
            String error = null;
            if (currentElement.getValidationCode() != null && currentElement.getValidationCode().isNotEmpty()) {
                error = context
                        .getJavascriptEngine()
                        .registerGlobalContextObject(
                                context.getJavascriptContextObject(resolvedId, currentElement)
                        )
                        .evaluateCode(currentElement.getValidationCode())
                        .asString();
            } else if (currentElement.getValidationExpressions() != null && !currentElement.getValidationExpressions().isEmpty()) {
                for (var validationExpression : currentElement.getValidationExpressions()) {
                    var res = context
                            .getNoCodeEvaluationService()
                            .evaluate(
                                    validationExpression.getExpression(),
                                    context.getElementDerivationData(),
                                    idPrefix
                            );
                    if (!res.getValueAsBoolean()) {
                        error = validationExpression.getMessage();
                        break;
                    }
                }
            } else if (
                    currentElement.getValidate() != null &&
                    (
                            currentElement.getValidate() instanceof FunctionCode functionCode && StringUtils.isNotNullOrEmpty(functionCode.getCode()) ||
                            currentElement.getValidate() instanceof FunctionNoCode functionNoCode && functionNoCode.getConditionSet() != null
                    )
            ) {
                error = currentElement
                        .getValidate()
                        .evaluate(
                                idPrefix,
                                currentElement,
                                context
                        )
                        .getStringValue();
            }

            context.getElementDerivationData().setError(resolvedId, error);
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("An error occurred while deriving error for element")
                    .addKeyValue("elementId", resolvedId)
                    .setCause(e)
                    .log();
            context.getElementDerivationData().setError(resolvedId, e.getMessage());
        }
    }
}
