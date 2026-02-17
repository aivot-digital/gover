package de.aivot.GoverBackend.ozgCloud.models;

import de.aivot.GoverBackend.ozgCloud.enums.OZGCloudServiceKontoType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record OZGCloudServiceKontoData(
        @Nonnull
        OZGCloudServiceKontoType type,
        @Nonnull
        String trustLevel, // STORK-QAA-LEVEL-X
        @Nullable
        OZGCloudPostfachAdresse postfachAddress
) {
}
