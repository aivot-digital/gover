package de.aivot.prosuna.backend.process.filters;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.ServletRequestDataBinder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProcessInstanceEventFilterTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @SuppressWarnings("unchecked")
    void queryParametersBindAndBuildPredicates(boolean historyRelevant) {
        var filter = ProcessInstanceEventFilter.create();
        var request = new MockHttpServletRequest();
        request.addParameter("historyRelevant", String.valueOf(historyRelevant));
        request.addParameter("concernedUserId", "user-1");
        request.addParameter("concernedIdentityId", "identity-1");
        request.addParameter("concernedIdentityTitle", "Antrag");
        var binder = new ServletRequestDataBinder(filter);
        binder.bind(request);

        assertFalse(binder.getBindingResult().hasErrors());
        assertEquals(historyRelevant, filter.getHistoryRelevant());
        assertEquals("user-1", filter.getConcernedUserId());
        assertEquals("identity-1", filter.getConcernedIdentityId());
        assertEquals("Antrag", filter.getConcernedIdentityTitle());
        Root<ProcessInstanceEventEntity> root = mock(Root.class, RETURNS_DEEP_STUBS);
        var builder = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        filter.build().toPredicate(root, mock(CriteriaQuery.class), builder);

        verify(builder).equal(root.get("isHistoryRelevant"), historyRelevant);
        verify(builder).equal(root.get("concernedUserId"), "user-1");
        verify(builder).equal(root.get("concernedIdentityId"), "identity-1");
        verify(builder).function("sql", Boolean.class, builder.literal("? ILIKE ?"),
                root.get("concernedIdentityTitle"), builder.literal("%Antrag%"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void absentFiltersDoNotRestrictEventFields() {
        Root<ProcessInstanceEventEntity> root = mock();
        var builder = mock(CriteriaBuilder.class);

        ProcessInstanceEventFilter.create().build().toPredicate(root, mock(CriteriaQuery.class), builder);

        verifyNoInteractions(root);
        verify(builder).and(new jakarta.persistence.criteria.Predicate[0]);
    }

    @Test
    void entityUsesTheSameJsonFieldNamesAsTheFrontend() {
        var entity = new ProcessInstanceEventEntity()
                .setHistoryRelevant(false)
                .setConcernedUserId("user-1")
                .setConcernedIdentityId("identity-1")
                .setConcernedIdentityTitle("Antragstellende Person");
        var mapper = JsonMapperTestUtils.createMapper();
        var json = mapper.readTree(mapper.writeValueAsString(entity));

        assertFalse(json.get("historyRelevant").asBoolean());
        assertFalse(json.has("isHistoryRelevant"));
        assertEquals("user-1", json.get("concernedUserId").asString());
        assertEquals("identity-1", json.get("concernedIdentityId").asString());
        assertEquals("Antragstellende Person", json.get("concernedIdentityTitle").asString());
    }
}
