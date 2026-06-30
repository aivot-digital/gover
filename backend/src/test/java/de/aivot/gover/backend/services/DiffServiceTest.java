package de.aivot.gover.backend.services;

import de.aivot.gover.backend.models.lib.DiffItem;
import de.aivot.gover.backend.services.DiffService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiffServiceTest {

    @Test
    void createDiff_UsesDotNotationAndBracketIndices() {
        var oldObject = new JSONObject(Map.of(
                "id", "form-1",
                "applicant", Map.of(
                        "addresses", List.of(
                                Map.of("street", "Old Street")
                        )
                )
        ));
        var newObject = new JSONObject(Map.of(
                "id", "form-1",
                "applicant", Map.of(
                        "addresses", List.of(
                                Map.of("street", "New Street")
                        )
                )
        ));

        var diff = DiffService.createDiff(oldObject, newObject);

        assertEquals(1, diff.size());
        assertEquals("applicant.addresses[0].street", diff.getFirst().field());
        assertEquals("Old Street", diff.getFirst().oldValue());
        assertEquals("New Street", diff.getFirst().newValue());
    }

    @Test
    void rollBackDiff_AppliesDotNotationPaths() {
        var targetObject = new JSONObject(Map.of(
                "applicant", Map.of(
                        "addresses", List.of(
                                Map.of("street", "New Street")
                        )
                )
        ));

        var result = DiffService.rollBackDiff(
                targetObject,
                new DiffItem("applicant.addresses[0].street", "Old Street", "New Street")
        );

        assertEquals(
                "Old Street",
                ((Map<?, ?>) ((List<?>) ((Map<?, ?>) result.toMap().get("applicant")).get("addresses")).getFirst()).get("street")
        );
    }

    @Test
    void rollBackDiff_SupportsLegacySlashPaths() {
        var targetObject = new JSONObject(Map.of(
                "applicant", Map.of("name", "Grace")
        ));

        var result = DiffService.rollBackDiff(
                targetObject,
                new DiffItem("/applicant/name", "Ada", "Grace")
        );

        assertEquals("Ada", ((Map<?, ?>) result.toMap().get("applicant")).get("name"));
    }
}
