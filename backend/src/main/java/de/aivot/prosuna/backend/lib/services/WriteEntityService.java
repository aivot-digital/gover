package de.aivot.prosuna.backend.lib.services;

public interface WriteEntityService<T, I> extends CreateEntityService<T>, UpdateEntityService<T, I>, DeleteEntityService<T, I> {
}
