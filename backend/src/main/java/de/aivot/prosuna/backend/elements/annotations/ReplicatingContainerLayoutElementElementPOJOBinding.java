package de.aivot.prosuna.backend.elements.annotations;

import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import jakarta.annotation.Nonnull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
public @interface ReplicatingContainerLayoutElementElementPOJOBinding {
    @Nonnull
    String id();

    @Nonnull
    ElementPOJOBindingProperty[] properties() default {};

    @Nonnull
    InputMode[] allowedInputModes() default {};

    @Nonnull
    InputMode defaultInputMode() default InputMode.Literal;

    /** Omitting the property allows every variable source when Variable is enabled. */
    @Nonnull
    InputVariableSource[] allowedVariableSources() default {
            InputVariableSource.ProcessData,
            InputVariableSource.ElementData,
            InputVariableSource.ElementMetadata,
            InputVariableSource.ProtectedProcessData
    };
}
