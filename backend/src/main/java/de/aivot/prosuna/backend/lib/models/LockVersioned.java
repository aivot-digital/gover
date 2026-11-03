package de.aivot.prosuna.backend.lib.models;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * An entity protected by optimistic locking through a JPA {@code @Version} attribute.
 * The attribute is named {@code lockVersion} because "version" already denotes process,
 * preset and definition versions in the domain.
 */
public interface LockVersioned {
    String CONFLICT_MESSAGE = "Der Datensatz wurde zwischenzeitlich geändert. Laden Sie ihn bitte neu, bevor Sie Ihre Änderungen erneut speichern.";

    @Nullable
    Long getLockVersion();

    /**
     * Rejects an update that is based on an outdated state of the stored entity.
     * Hibernate checks only the version it loaded itself, so the version sent by the client is compared here.
     * Requests without a lock version keep last-write-wins until every client sends one.
     */
    static void requireCurrent(@Nonnull Object requested, @Nonnull Object existing) throws ResponseException {
        if (requested instanceof LockVersioned requestedEntity
                && existing instanceof LockVersioned existingEntity
                && requestedEntity.getLockVersion() != null
                && !Objects.equals(requestedEntity.getLockVersion(), existingEntity.getLockVersion())) {
            throw ResponseException.conflict(CONFLICT_MESSAGE);
        }
    }
}
