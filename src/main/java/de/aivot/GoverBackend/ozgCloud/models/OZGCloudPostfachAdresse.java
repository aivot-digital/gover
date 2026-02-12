package de.aivot.GoverBackend.ozgCloud.models;

import de.aivot.GoverBackend.ozgCloud.enums.OZGCloudPostfachAdresseType;
import jakarta.annotation.Nonnull;

public record OZGCloudPostfachAdresse(
        @Nonnull
        String identifier,
        @Nonnull
        OZGCloudPostfachAdresseType type
) {
}
