package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class BaseElementDerivationService<Ctx extends BaseElementDerivationContext> {
    public Ctx derive(
            @Nonnull BaseElement currentElement,
            @Nonnull Map<String, Object> inputValues
    ) {
        return derive(
                currentElement,
                inputValues,
                true,
                true,
                true,
                false
        );
    }

    private Ctx derive(
            @Nonnull BaseElement currentElement,
            @Nonnull Map<String, Object> inputValues,
            @Nonnull Boolean deriveVisibilities,
            @Nonnull Boolean deriveOverrides,
            @Nonnull Boolean deriveValues,
            @Nonnull Boolean deriveErrors
    ) {
        var context = prepareContext(inputValues);
        startElementDerivation(context, currentElement, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors);
        return context;
    }

    protected void startElementDerivation(
            @Nonnull Ctx context,
            @Nonnull BaseElement currentElement,
            @Nonnull Boolean deriveVisibilities,
            @Nonnull Boolean deriveOverrides,
            @Nonnull Boolean deriveValues,
            @Nonnull Boolean deriveErrors
    ) {
        switch (currentElement) {
            case RootElement rootElement:
                deriveRootElement(context, rootElement, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors);
                break;

            case StepElement stepElement:
                deriveStepElement(context, stepElement, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors);
                break;

            case GroupLayout groupLayout:
                deriveGroupLayoutRecursive(context, groupLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, true);
                break;

            case ReplicatingContainerLayout replicatingContainerLayout:
                deriveReplicatingLayoutRecursive(context, replicatingContainerLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, true);
                break;

            case BaseElement baseElement:
                deriveElement(context, baseElement, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, true);
                break;
        }
    }

    protected void deriveRootElement(
            @Nonnull Ctx context,
            @Nonnull RootElement rootElement,
            @Nonnull Boolean deriveVisibilities,
            @Nonnull Boolean deriveOverrides,
            @Nonnull Boolean deriveValues,
            @Nonnull Boolean deriveErrors
    ) {
        deriveElement(context, rootElement, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, true);

        deriveElement(context, rootElement.getIntroductionStep(), deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, true);
        deriveElement(context, rootElement.getSummaryStep(), deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, true);
        deriveElement(context, rootElement.getSubmitStep(), deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, true);

        for (var step : rootElement.getChildren()) {
            deriveStepElement(context, step, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors);
        }
    }

    protected void deriveStepElement(
            @Nonnull Ctx context,
            @Nonnull StepElement baseStepElement,
            @Nonnull Boolean deriveVisibilities,
            @Nonnull Boolean deriveOverrides,
            @Nonnull Boolean deriveValues,
            @Nonnull Boolean deriveErrors
    ) {
        deriveElement(context, baseStepElement, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, true);

        var resolvedId = baseStepElement
                .getResolvedId(null);

        var isVisible = context
                .getElementDerivationData()
                .isVisible(resolvedId);

        var stepElement = (StepElement) context
                .getElementDerivationData()
                .getOverride(resolvedId, baseStepElement);

        for (var child : stepElement.getChildren()) {
            switch (child) {
                case GroupLayout groupLayout:
                    deriveGroupLayoutRecursive(context, groupLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, isVisible);
                    break;
                case ReplicatingContainerLayout replicatingContainerLayout:
                    deriveReplicatingLayoutRecursive(context, replicatingContainerLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, isVisible);
                    break;
                default:
                    deriveElement(context, child, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, null, isVisible);
                    break;
            }
        }
    }

    protected void deriveGroupLayoutRecursive(
            @Nonnull Ctx context,
            @Nonnull GroupLayout baseGroupLayout,
            @Nonnull Boolean deriveVisibilities,
            @Nonnull Boolean deriveOverrides,
            @Nonnull Boolean deriveValues,
            @Nonnull Boolean deriveErrors,
            @Nullable String idPrefix,
            @Nonnull Boolean isParentVisible
    ) {
        deriveElement(context, baseGroupLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, idPrefix, isParentVisible);

        var resolvedId = baseGroupLayout
                .getResolvedId(idPrefix);

        var isVisible = context
                .getElementDerivationData()
                .isVisible(resolvedId);

        var groupLayout = (GroupLayout) context
                .getElementDerivationData()
                .getOverride(resolvedId, baseGroupLayout);

        for (var child : groupLayout.getChildren()) {
            switch (child) {
                case GroupLayout childGroupLayout:
                    deriveGroupLayoutRecursive(context, childGroupLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, idPrefix, isParentVisible && isVisible);
                    break;
                case ReplicatingContainerLayout replicatingContainerLayout:
                    deriveReplicatingLayoutRecursive(context, replicatingContainerLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, idPrefix, isParentVisible && isVisible);
                    break;
                default:
                    deriveElement(context, child, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, idPrefix, isParentVisible && isVisible);
                    break;
            }
        }
    }

    protected void deriveReplicatingLayoutRecursive(
            @Nonnull Ctx context,
            @Nonnull ReplicatingContainerLayout baseReplicatingContainerLayout,
            @Nonnull Boolean deriveVisibilities,
            @Nonnull Boolean deriveOverrides,
            @Nonnull Boolean deriveValues,
            @Nonnull Boolean deriveErrors,
            @Nullable String idPrefix,
            @Nonnull Boolean isParentVisible
    ) {
        deriveElement(context, baseReplicatingContainerLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, idPrefix, isParentVisible);

        var resolvedId = baseReplicatingContainerLayout
                .getResolvedId(idPrefix);

        var isVisible = context
                .getElementDerivationData()
                .isVisible(resolvedId);

        var value = context
                .getElementDerivationData()
                .getValue(resolvedId)
                .orElse(List.of());

        var replicatingContainerLayout = (ReplicatingContainerLayout) context
                .getElementDerivationData()
                .getOverride(resolvedId, baseReplicatingContainerLayout);

        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof String childId) {
                    var childPrefix = resolvedId + "_" + childId;

                    for (var child : replicatingContainerLayout.getChildren()) {
                        switch (child) {
                            case GroupLayout childGroupLayout:
                                deriveGroupLayoutRecursive(context, childGroupLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, childPrefix, isParentVisible && isVisible);
                                break;
                            case ReplicatingContainerLayout childReplicatingContainerLayout:
                                deriveReplicatingLayoutRecursive(context, childReplicatingContainerLayout, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, childPrefix, isParentVisible && isVisible);
                                break;
                            default:
                                deriveElement(context, child, deriveVisibilities, deriveOverrides, deriveValues, deriveErrors, childPrefix, isParentVisible && isVisible);
                                break;
                        }
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Value for replicating container layout must be an iterable.");
        }
    }

    protected void deriveElement(
            @Nonnull Ctx context,
            @Nonnull BaseElement _baseElement,
            @Nonnull Boolean deriveVisibilities,
            @Nonnull Boolean deriveOverrides,
            @Nonnull Boolean deriveValues,
            @Nonnull Boolean deriveErrors,
            @Nullable String idPrefix,
            @Nonnull Boolean isParentVisible
    ) {
        var resolvedId = _baseElement
                .getResolvedId(idPrefix);

        // Check if cleaning can be done before deriving overrides and values.
        cleanInputValue(context, _baseElement, resolvedId);

        if (deriveOverrides) {
            deriveOverride(context, idPrefix, _baseElement);
        }

        var baseElement = context
                .getElementDerivationData()
                .getOverride(resolvedId, _baseElement);

        // Clean after overrides to ensure that the input value is not overwritten by an override.
        cleanInputValue(context, baseElement, resolvedId);

        if (deriveVisibilities) {
            deriveVisibility(context, idPrefix, baseElement, isParentVisible);
        }

        var isVisible = context
                .getElementDerivationData()
                .isVisible(resolvedId);

        if (isVisible && baseElement instanceof BaseInputElement<?> baseInputElement) {
            if (deriveValues) {
                deriveValue(context, idPrefix, baseInputElement);
            }
        }

        if (deriveErrors) {
            deriveError(context, idPrefix, baseElement);
        }
    }

    private static <Ctx extends BaseElementDerivationContext> void cleanInputValue(@NotNull Ctx context, BaseElement baseElement, String resolvedId) {
        // Check if the element is disabled or technical, and clean the input value if necessary.
        // This is important to prevent overwriting values which are not supposed to be set by users.
        if (baseElement instanceof BaseInputElement<?> inputElement) {
            if (Boolean.TRUE.equals(inputElement.getDisabled()) || Boolean.TRUE.equals(inputElement.getTechnical())) {
                context.getElementDerivationData().cleanInputValue(resolvedId);
            }
        }
    }

    protected void deriveVisibility(
            @Nonnull Ctx context,
            @Nullable String idPrefix,
            @Nonnull BaseElement currentElement,
            @Nonnull Boolean isParentVisible
    ) {
        getVisibilityDerivationService()
                .deriveVisibilityForElement(
                        context,
                        idPrefix,
                        currentElement,
                        isParentVisible
                );
    }

    protected BaseElementVisibilityDerivationService<Ctx> getVisibilityDerivationService() {
        return new BaseElementVisibilityDerivationService<Ctx>();
    }

    protected void deriveOverride(Ctx context, String idPrefix, BaseElement currentElement) {
        getOverrideDerivationService()
                .deriveOverrideForElement(
                        context,
                        idPrefix,
                        currentElement
                );
    }

    protected BaseElementOverrideDerivationService<Ctx> getOverrideDerivationService() {
        return new BaseElementOverrideDerivationService<Ctx>();
    }

    protected void deriveValue(Ctx context, String idPrefix, BaseInputElement<?> currentElement) {
        getValueDerivationService()
                .deriveValueForElement(
                        context,
                        idPrefix,
                        currentElement
                );
    }

    protected BaseElementValueDerivationService<Ctx> getValueDerivationService() {
        return new BaseElementValueDerivationService<Ctx>();
    }

    protected void deriveError(Ctx context, String idPrefix, BaseElement currentElement) {
        getErrorDerivationService()
                .deriveErrorForElement(
                        context,
                        idPrefix,
                        currentElement
                );
    }

    protected BaseElementErrorDerivationService<Ctx> getErrorDerivationService() {
        return new BaseElementErrorDerivationService<Ctx>();
    }

    protected abstract Ctx prepareContext(@Nonnull Map<String, Object> inputValues);
}
