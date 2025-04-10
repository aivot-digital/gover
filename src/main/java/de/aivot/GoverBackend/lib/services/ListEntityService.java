package de.aivot.GoverBackend.lib.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface for services that provide a list of entities.
 *
 * @param <T> The type of the entity.
 */
public interface ListEntityService<T> {
    /**
     * List all entities.
     *
     * @return A page containing all entities.
     * @throws ResponseException If an error occurs.
     */
    @Nonnull
    default Page<T> list() throws ResponseException {
        return list(null, null);
    }

    /**
     * List all entities that match the given filter.
     * The filter is applied to the entities before they are returned.
     *
     * @param filter The filter to apply.
     * @return A page containing all entities that match the filter.
     * @throws ResponseException If an error occurs.
     */
    @Nonnull
    default Page<T> list(
            @Nonnull Filter<T> filter
    ) throws ResponseException {
        return list(null, filter);
    }

    /**
     * List all entities within the given pageable.
     *
     * @param pageable The pageable to use.
     * @return A page containing all entities within the pageable.
     * @throws ResponseException If an error occurs.
     */
    @Nonnull
    default Page<T> list(
            @Nonnull Pageable pageable
    ) throws ResponseException {
        return list(pageable, null);
    }

    /**
     * List all entities that match the given filter within the given pageable.
     * The filter is applied to the entities before they are returned.
     * The pageable is used to limit the number of entities returned.
     * If the filter is null, all entities are returned.
     * If the pageable is null, all entities are returned.
     *
     * @param pageable The pageable to use.
     * @param filter   The filter to apply.
     * @return A page containing all entities that match the filter within the pageable.
     * @throws ResponseException If an error occurs.
     */
    @Nonnull
    default Page<T> list(
            @Nullable Pageable pageable,
            @Nullable Filter<T> filter
    ) throws ResponseException {
        var paging = pageable != null ? pageable : Pageable.unpaged();

        var spec = filter != null ? filter.build() : null;

        var res = performList(paging, spec, filter);

        return res == null ? Page.empty() : res;
    }

    /**
     * Perform the list operation.
     * This method is called by the list methods.
     * It should be implemented by the service to provide the actual list operation.
     * If the service does not support listing, it should return null.
     * If the service does not support filtering, it should ignore the filter.
     * If the service does not support paging, it should ignore the pageable.
     * If the service does not support sorting, it should ignore the sort.
     *
     * @param pageable      The pageable to use.
     * @param specification The specification to apply.
     * @param filter        The filter to apply.
     * @return A page containing all entities that match the filter within the pageable.
     * @throws ResponseException If an error occurs.
     */
    @Nullable
    Page<T> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<T> specification,
            @Nullable Filter<T> filter
    ) throws ResponseException;
}
