package de.aivot.prosuna.backend.ozgCloud.models;

import de.aivot.prosuna.backend.ozgCloud.enums.OZGCloudPostfachAdresseType;
import jakarta.annotation.Nonnull;

public record OZGCloudPostfachAdresse(
        @Nonnull
        String identifier,
        @Nonnull
        OZGCloudPostfachAdresseType type
) {
}
