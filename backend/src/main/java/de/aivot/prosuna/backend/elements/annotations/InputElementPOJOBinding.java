package de.aivot.prosuna.backend.elements.annotations;

import de.aivot.prosuna.backend.enums.ElementType;
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
}
