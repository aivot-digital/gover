package de.aivot.gover.backend.elements.annotations;

import jakarta.annotation.Nonnull;

public @interface ElementPOJOBindingProperty {
    @Nonnull
    String key();

    String strValue() default "";

    int intValue() default Integer.MIN_VALUE;

    boolean boolValue() default false;
    boolean falseValue() default false;

    double doubleValue() default Double.MIN_VALUE;
}
