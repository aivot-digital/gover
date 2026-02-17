package de.aivot.GoverBackend.utils;

import jakarta.annotation.Nonnull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

public class PaginationUtils {
    /**
     * Filter the sorting of a Pageable to only allow certain properties.
     * Remove all other sorting properties.
     * @param pageable The original pageable
     * @param allowedSortProperties The allowed sort properties
     * @return A new pageable with only the allowed sort properties
     */
    public static Pageable filterSorting(@Nonnull Pageable pageable, @Nonnull String ...allowedSortProperties) {
        var allowedSortPropertiesSet = Set.of(allowedSortProperties);

        List<Sort.Order> filteredOrders = pageable
                .getSort()
                .stream()
                .filter(order -> allowedSortPropertiesSet.contains(order.getProperty()))
                .toList();

        Sort filteredSort = Sort.by(filteredOrders);

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                filteredSort
        );
    }
}
