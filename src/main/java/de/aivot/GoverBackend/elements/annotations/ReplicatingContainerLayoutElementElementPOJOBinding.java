package de.aivot.GoverBackend.elements.annotations;

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
}
