package de.aivot.gover.backend.lib.services;

public interface WriteEntityService<T, I> extends CreateEntityService<T>, UpdateEntityService<T, I>, DeleteEntityService<T, I> {
}
