package de.aivot.prosuna.backend.elements.models.elements;

import de.aivot.prosuna.backend.elements.models.input.DynamicTextPolicy;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/** Marks inputs whose literal text may be interpolated when enabled by trusted backend configuration. */
public interface DynamicTextElement {
    @Nullable
    DynamicTextPolicy getDynamicTextPolicy();

    @Nonnull
    DynamicTextElement setDynamicTextPolicy(@Nullable DynamicTextPolicy policy);
}
