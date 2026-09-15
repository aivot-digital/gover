package de.aivot.prosuna.backend.search.filters;

import de.aivot.prosuna.backend.permissions.models.PermissionProvider.SearchPermission;
import de.aivot.prosuna.backend.search.entities.SearchItemEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SearchFilterTest {
    @Test
    void buildFailsClosedWithoutUserId() {
        var filter = new SearchFilter(
                null,
                "process",
                null,
                new String[]{"form:v1:form-trigger"},
                List.of(new SearchPermission("processes", "process_definition.read"))
        );
        Root<SearchItemEntity> root = mock();
        CriteriaQuery<?> query = mock();
        CriteriaBuilder criteriaBuilder = mock();
        Predicate disjunction = mock();

        when(criteriaBuilder.disjunction()).thenReturn(disjunction);

        var result = filter.build().toPredicate(root, query, criteriaBuilder);

        assertSame(disjunction, result);
        verify(criteriaBuilder).disjunction();
        verifyNoInteractions(root, query);
    }

    @Test
    void buildFailsClosedWithoutSearchPermissions() {
        var filter = new SearchFilter(
                "user-1",
                "process",
                null,
                new String[]{"form:v1:form-trigger"},
                List.of()
        );
        Root<SearchItemEntity> root = mock();
        CriteriaQuery<?> query = mock();
        CriteriaBuilder criteriaBuilder = mock();
        Predicate disjunction = mock();

        when(criteriaBuilder.disjunction()).thenReturn(disjunction);

        var result = filter.build().toPredicate(root, query, criteriaBuilder);

        assertSame(disjunction, result);
        verify(criteriaBuilder).disjunction();
        verifyNoInteractions(root, query);
    }
}
