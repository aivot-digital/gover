package de.aivot.prosuna.backend.nocode.models;

import jakarta.annotation.Nullable;

/** Authoring constraints for static numeric operands; evaluation still validates resolved values. */
public record NoCodeNumberConstraints(
        boolean integerOnly,
        @Nullable Double minimum,
        @Nullable Double maximum
) {
}
