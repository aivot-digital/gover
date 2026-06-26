package de.aivot.gover.backend.ozgCloud.models;

import de.aivot.gover.backend.ozgCloud.enums.OZGCloudPostfachAdresseType;
import jakarta.annotation.Nonnull;

public record OZGCloudPostfachAdresse(
        @Nonnull
        String identifier,
        @Nonnull
        OZGCloudPostfachAdresseType type
) {
}
