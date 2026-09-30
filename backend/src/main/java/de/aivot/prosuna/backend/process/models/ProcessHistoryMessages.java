package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/** Human-readable snapshots for process history; technical identifiers are never display fallbacks. */
public final class ProcessHistoryMessages {
    private ProcessHistoryMessages() {
    }

    @Nonnull
    public static String userName(@Nullable UserEntity user) {
        return user == null || user.getFullName() == null || user.getFullName().isBlank()
                ? "Person ohne verfügbaren Namen" : user.getFullName();
    }

    @Nonnull
    public static String identityTitle(@Nullable IdentityData identity) {
        return identity == null || identity.title() == null || identity.title().isBlank()
                ? "Identität ohne Titel" : identity.title();
    }
}
