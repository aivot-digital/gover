package de.aivot.GoverBackend.secrets.dtos;

import de.aivot.GoverBackend.secrets.entities.SecretEntity;

import javax.annotation.Nonnull;

/**
 * This class represents the response data for a secret entity.
 * The value of the secret is not included in the response for security reasons.
 */
public record SecretEntityResponseDTO(
        @Nonnull String key,
        @Nonnull String name,
        @Nonnull String description
) {
    /**
     * Get the value of the secret.
     * This returns a placeholder value for security reasons.
     *
     * @return The placeholder value of the secret.
     */
    public String getValue() {
        return "********";
    }

    @Nonnull
    public static SecretEntityResponseDTO fromEntity(@Nonnull SecretEntity entity) {
        return new SecretEntityResponseDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDescription()
        );
    }
}
