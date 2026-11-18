package de.aivot.prosuna.backend.staffEvents.models;

import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Decides which connected users receive the events of one type that are published without a user ID. Events of a type
 * without an audience are only delivered to the user they name.
 */
public interface StaffEventAudience {
    @Nonnull
    String type();

    /** Returns the users among the given ones who may receive the event, e.g. those who may read its resource. */
    @Nonnull
    Collection<String> filter(@Nonnull StaffEvent event, @Nonnull Collection<String> userIds);
}
