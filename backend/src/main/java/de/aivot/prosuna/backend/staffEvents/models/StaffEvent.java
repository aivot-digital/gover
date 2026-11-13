package de.aivot.prosuna.backend.staffEvents.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * An event for the browsers of connected staff users, e.g. to reload an open view of a changed resource.
 *
 * <p>Publish it with Spring's {@code ApplicationEventPublisher}. It is sent once the surrounding transaction commits
 * and dropped if the transaction rolls back. Delivery is best effort: clients treat an event as a hint and load the
 * current state through the regular endpoints, which check the permissions.
 *
 * @param type   The kind of event, e.g. the type of the changed resource.
 * @param scope  The ID of the affected resource, if any.
 * @param userId The only user who receives the event. Without it, the {@link StaffEventAudience} of the type decides
 *               which users receive it.
 */
public record StaffEvent(@Nonnull String type, @Nullable String scope, @Nullable String userId) {
}
