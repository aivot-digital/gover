package de.aivot.prosuna.backend.elements.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link String} field of a {@link ReplicatingContainerLayoutElementElementPOJOBinding} class that receives the
 * stable ID of the mapped row. Use it when configuration must reference a row independently of its position or values.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.FIELD)
public @interface ReplicatingContainerRowIdPOJOBinding {
}
