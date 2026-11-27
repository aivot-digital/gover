package de.aivot.prosuna.backend.seeding.models;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;

/**
 * Provides initial data that a fresh installation needs to be usable.
 * Seeds run on every startup, so {@link #shouldSeed()} must detect existing data and keep
 * data that administrators created, changed or deleted.
 */
public interface SeedProvider {
    boolean shouldSeed();

    void seed() throws ResponseException;
}
