package de.aivot.prosuna.backend.system.dtos;

import de.aivot.prosuna.backend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record SystemBuildDTO(
        @Nullable
        String buildVersion,

        @Nullable
        String buildNumber,

        @Nullable
        String buildTimestamp
) {
    @Nonnull
    public static SystemBuildDTO fromProperties(@Nonnull BuildProperties properties) {
        return new SystemBuildDTO(
                properties.getBuildVersion(),
                properties.getBuildNumber(),
                properties.getBuildTimestamp()
        );
    }
}
