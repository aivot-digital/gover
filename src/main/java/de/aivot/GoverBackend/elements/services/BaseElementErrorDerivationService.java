package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.functions.FunctionCode;
import de.aivot.GoverBackend.models.functions.FunctionNoCode;
import de.aivot.GoverBackend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class BaseElementErrorDerivationService<Ctx extends BaseElementDerivationContext> {
    private final Logger logger = LoggerFactory.getLogger(BaseElementErrorDerivationService.class);

    public void deriveErrorForElement(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull BaseElement originalElement
    ) {
        if (originalElement instanceof BaseInputElement<?>) {
            deriveErrorForElement(context, idPrefix, (BaseInputElement<?>) originalElement);
        } else {
            // For non-input elements, we do not derive errors
            context.getElementDerivationData().setError(originalElement.getResolvedId(idPrefix), null);
        }
    }

    /**
     * Derive the value of the current input element.
     *
     * @param context         The context in which the form is being derived.
     * @param idPrefix        The prefix that should be used for the id of the element.
     * @param originalElement The current element that is being derived.
     */
    private void deriveErrorForElement(
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
