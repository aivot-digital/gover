package de.aivot.GoverBackend.audit.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditLogPayloadTest {
    @Test
    void withDiffShouldReturnNullForTwoNullStates() {
        var payload = AuditLogPayload.create(null).withDiff(null, null);

        assertNull(payload.getDiff());
    }

    @Test
    void withDiffShouldWrapWholeStateWhenOneSideIsNull() {
        var newState = Map.<String, Object>of("name", "Alpha", "enabled", true);

        var payload = AuditLogPayload.create(null).withDiff(null, newState);

        var valueDiff = new HashMap<String, Object>();
        valueDiff.put("old", null);
        valueDiff.put("new", newState);

        assertEquals(Map.of("value", valueDiff), payload.getDiff());
    }

    @Test
    void withDiffShouldReturnNullForEqualStates() {
        var timestamp = LocalDateTime.of(2026, 3, 24, 8, 30);
        var state = Map.<String, Object>of(
                "name", "Alpha",
                "count", 3,
                "timestamp", timestamp,
                "items", List.of(Map.of("id", "x", "enabled", true))
        );

        var payload = AuditLogPayload.create(null).withDiff(state, state);

        assertNull(payload.getDiff());
    }

    @Test
    void withDiffShouldTrackAddedAndRemovedKeys() {
        var oldState = Map.<String, Object>of(
                "removed", "legacy",
                "unchanged", true
        );
        var newState = Map.<String, Object>of(
                "added", "modern",
                "unchanged", true
        );

        var payload = AuditLogPayload.create(null).withDiff(oldState, newState);

        var removedDiff = new HashMap<String, Object>();
        removedDiff.put("old", "legacy");
        removedDiff.put("new", null);

        var addedDiff = new HashMap<String, Object>();
        addedDiff.put("old", null);
        addedDiff.put("new", "modern");

        assertEquals(Map.of(
                "removed", removedDiff,
                "added", addedDiff
        ), payload.getDiff());
    }

    @Test
    void withDiffShouldExpandNestedListOfMapsChanges() {
        var oldState = Map.<String, Object>of(
                "nodes", List.of(
                        Map.of("id", "node-1", "label", "Alpha", "config", Map.of("enabled", true)),
                        Map.of("id", "node-2", "label", "Beta")
                )
        );
        var newState = Map.<String, Object>of(
                "nodes", List.of(
                        Map.of("id", "node-1", "label", "Alpha updated", "config", Map.of("enabled", false)),
                        Map.of("id", "node-2", "label", "Beta")
                )
        );

        var payload = AuditLogPayload.create(null).withDiff(oldState, newState);
        var diff = payload.getDiff();

        assertNotNull(diff);
        assertEquals(Map.of(
                "0", Map.of(
                        "label", Map.of("old", "Alpha", "new", "Alpha updated"),
                        "config", Map.of(
                                "enabled", Map.of("old", true, "new", false)
                        )
                )
        ), diff.get("nodes"));
    }

    @Test
    void withDiffShouldNormalizePojoAndMixedListValues() {
        var oldState = Map.<String, Object>of(
                "settings", new NestedSettings(
                        "legacy",
                        List.of(
                                Map.of("key", "channel", "value", "mail"),
                                "plain"
                        )
                )
        );
        var newState = Map.<String, Object>of(
                "settings", new NestedSettings(
                        "modern",
                        List.of(
                                Map.of("key", "channel", "value", "portal"),
                                5
                        )
                )
        );

        var payload = AuditLogPayload.create(null).withDiff(oldState, newState);
        var diff = payload.getDiff();

        assertNotNull(diff);
        assertEquals(Map.of(
                "mode", Map.of("old", "legacy", "new", "modern"),
                "items", Map.of(
                        "0", Map.of(
                                "value", Map.of("old", "mail", "new", "portal")
                        ),
                        "1", Map.of("old", "plain", "new", 5)
                )
        ), diff.get("settings"));
    }

    @Test
    void withDiffShouldTrackListInsertionsAndRemovalsByIndex() {
        var oldState = Map.<String, Object>of(
                "nodes", List.of(
                        Map.of("id", "node-1"),
                        Map.of("id", "node-2")
                )
        );
        var newState = Map.<String, Object>of(
                "nodes", List.of(
                        Map.of("id", "node-1"),
                        Map.of("id", "node-2"),
                        Map.of("id", "node-3")
                )
        );

        var payload = AuditLogPayload.create(null).withDiff(oldState, newState);

        var indexDiff = new HashMap<String, Object>();
        indexDiff.put("old", null);
        indexDiff.put("new", Map.of("id", "node-3"));

        assertEquals(Map.of(
                "nodes", Map.of(
                        "2", indexDiff
                )
        ), payload.getDiff());
    }

    @Test
    void withDiffShouldFallbackToLeafDiffForTypeChanges() {
        var oldState = Map.<String, Object>of(
                "settings", List.of("mail", "portal")
        );
        var newState = Map.<String, Object>of(
                "settings", Map.of("primary", "mail")
        );

        var payload = AuditLogPayload.create(null).withDiff(oldState, newState);

        assertEquals(Map.of(
                "settings", Map.of(
                        "old", List.of("mail", "portal"),
                        "new", Map.of("primary", "mail")
                )
        ), payload.getDiff());
    }

    @Test
    void withDiffShouldNormalizeArraysAndIterables() {
        var oldState = Map.<String, Object>of(
                "settings", new ArrayAndIterableSettings(
                        new String[]{"mail", "portal"},
                        new LinkedHashSet<>(List.of(
                                Map.of("key", "alpha", "enabled", true),
                                Map.of("key", "beta", "enabled", false)
                        ))
                )
        );
        var newState = Map.<String, Object>of(
                "settings", new ArrayAndIterableSettings(
                        new String[]{"mail", "api"},
                        new LinkedHashSet<>(List.of(
                                Map.of("key", "alpha", "enabled", false),
                                Map.of("key", "beta", "enabled", false)
                        ))
                )
        );

        var payload = AuditLogPayload.create(null).withDiff(oldState, newState);

        assertEquals(Map.of(
                "settings", Map.of(
                        "channels", Map.of(
                                "1", Map.of("old", "portal", "new", "api")
                        ),
                        "entries", Map.of(
                                "0", Map.of(
                                        "enabled", Map.of("old", true, "new", false)
                                )
                        )
                )
        ), payload.getDiff());
    }

    private record NestedSettings(String mode, List<Object> items) {
    }

    private record ArrayAndIterableSettings(String[] channels, Iterable<Map<String, Object>> entries) {
    }
}
