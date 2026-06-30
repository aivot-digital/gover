package de.aivot.gover.backend.ozgCloud.models;

import de.aivot.gover.backend.ozgCloud.enums.OZGCloudServiceKontoType;
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
