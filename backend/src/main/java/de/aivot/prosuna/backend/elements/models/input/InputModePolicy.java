package de.aivot.prosuna.backend.elements.models.input;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Trusted backend policy describing which authored representations an input accepts. The policy is part of the
 * backend-defined element, never of the user-authored value.
 */
public record InputModePolicy(
        @Nonnull List<InputMode> allowedModes,
        @Nullable InputMode defaultMode,
        @Nonnull List<InputVariableSource> allowedVariableSources
) implements Serializable {
    /**
     * Creates a policy using every variable source when Variable is enabled, and no sources otherwise.
     */
    public InputModePolicy(@Nonnull List<InputMode> allowedModes, @Nullable InputMode defaultMode) {
        this(
                allowedModes,
                defaultMode,
                allowedModes.contains(InputMode.Variable)
                        ? List.of(InputVariableSource.values())
                        : List.of()
        );
    }

    public InputModePolicy {
        if (allowedModes == null) {
            throw new IllegalArgumentException("Input mode policy modes must not be null.");
        }
        allowedModes = List.copyOf(allowedModes);
        if (allowedVariableSources == null) {
            // Missing sources are an ergonomic shorthand in trusted backend definitions, while the normalized
            // policy sent to clients always contains an explicit list.
            allowedVariableSources = allowedModes.contains(InputMode.Variable)
                    ? List.of(InputVariableSource.values())
                    : List.of();
        }
        allowedVariableSources = List.copyOf(allowedVariableSources);

        var effectiveDefaultMode = defaultMode == null ? InputMode.Literal : defaultMode;
        if (allowedModes.isEmpty()) {
            throw new IllegalArgumentException("At least one input mode must be allowed.");
        }
        if (new LinkedHashSet<>(allowedModes).size() != allowedModes.size()) {
            throw new IllegalArgumentException("Input modes must not contain duplicates.");
        }
        if (new LinkedHashSet<>(allowedVariableSources).size() != allowedVariableSources.size()) {
            throw new IllegalArgumentException("Variable sources must not contain duplicates.");
        }
        if (!allowedModes.contains(effectiveDefaultMode)) {
            throw new IllegalArgumentException("The default input mode must be included in allowedModes.");
        }
        if (!allowedModes.contains(InputMode.Variable) && !allowedVariableSources.isEmpty()) {
            throw new IllegalArgumentException("Variable sources can only be configured when Variable is an allowed input mode.");
        }
        if (allowedModes.contains(InputMode.Variable) && allowedVariableSources.isEmpty()) {
            throw new IllegalArgumentException("At least one variable source must be configured when Variable is an allowed input mode.");
        }
    }

    @Nonnull
    @JsonIgnore
    public InputMode effectiveDefaultMode() {
        return defaultMode == null ? InputMode.Literal : defaultMode;
    }
}
