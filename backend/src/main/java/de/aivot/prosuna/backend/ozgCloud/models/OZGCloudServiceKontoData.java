package de.aivot.prosuna.backend.ozgCloud.models;

import de.aivot.prosuna.backend.ozgCloud.enums.OZGCloudServiceKontoType;
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
