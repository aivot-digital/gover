package de.aivot.GoverBackend.elements.services;

import de.aivot.GoverBackend.elements.models.BaseElementDerivationContext;
import de.aivot.GoverBackend.elements.models.BaseElement;

import javax.annotation.Nonnull;
import java.util.Map;

public abstract class BaseElementDerivationService<Ctx extends BaseElementDerivationContext> {
    public Ctx derive(
            @Nonnull BaseElement baseElement,
            @Nonnull Map<String, Object> inputValues
    ) {
        return derive(baseElement, inputValues, false);
    }

    public Ctx derive(
            @Nonnull BaseElement baseElement,
            @Nonnull Map<String, Object> inputValues,
            @Nonnull Boolean skipValidation
    ) {
        try (Ctx context = prepareContext(inputValues)) {
            deriveVisibilities(context, baseElement);
            deriveOverrides(context, baseElement);
            deriveValues(context, baseElement);

            if (!skipValidation) {
                deriveErrors(context, baseElement);
            }

            return context;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void deriveVisibilities(Ctx context, BaseElement currentElement) {
        new BaseElementVisibilityDerivationService<Ctx>().derive(context, null, currentElement);
    }

    protected void deriveOverrides(Ctx context, BaseElement currentElement) {
        new BaseElementOverrideDerivationService<Ctx>().derive(context, null, currentElement);
    }

    protected void deriveValues(Ctx context, BaseElement currentElement) {
        new BaseElementValueDerivationService<Ctx>().derive(context, null, currentElement);
    }

    protected void deriveErrors(Ctx context, BaseElement currentElement) {
        new BaseElementErrorDerivationService<Ctx>().derive(context, null, currentElement);
    }

    protected abstract Ctx prepareContext(@Nonnull Map<String, Object> inputValues);
}
