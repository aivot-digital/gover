package de.aivot.prosuna.backend.elements.annotations;

import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import jakarta.annotation.Nonnull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.FIELD)
public @interface InputElementPOJOBinding {
    @Nonnull
    String id();

    @Nonnull
    ElementType type();

    @Nonnull
    ElementPOJOBindingProperty[] properties() default {};

    /** Enables dynamic input modes for this trusted backend-defined field. An empty array keeps the field literal-only. */
    @Nonnull
    InputMode[] allowedInputModes() default {};

    /** The mode initially shown when no authored value exists. It must be part of {@link #allowedInputModes()}. */
    @Nonnull
    InputMode defaultInputMode() default InputMode.Literal;

    /**
     * Sources accepted by Variable wrappers. Omitting the property allows all sources; the mapper discards the
     * default when Variable is not an allowed mode.
     */
    @Nonnull
    InputVariableSource[] allowedVariableSources() default {
            InputVariableSource.ProcessData,
            InputVariableSource.ElementData,
            InputVariableSource.ElementMetadata,
            InputVariableSource.ProtectedProcessData
    };

    /** Enables template expressions inside Literal values of Text and RichTextInput fields. */
    boolean dynamicText() default false;

    /** Variable sources offered by the dynamic-text picker. */
    @Nonnull
    InputVariableSource[] dynamicTextVariableSuggestionSources() default {
            InputVariableSource.ProcessData,
            InputVariableSource.ElementData,
            InputVariableSource.ElementMetadata,
            InputVariableSource.ProtectedProcessData
    };
}
