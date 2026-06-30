package de.aivot.gover.backend.elements.annotations;

import de.aivot.gover.backend.enums.ElementType;
import jakarta.annotation.Nonnull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
public @interface LayoutElementPOJOBinding {
    @Nonnull
    String id();

    @Nonnull
    ElementType type();

    @Nonnull
    ElementPOJOBindingProperty[] properties() default {};
}
