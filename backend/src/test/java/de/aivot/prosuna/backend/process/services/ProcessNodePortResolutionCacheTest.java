package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.input.LiteralAuthoredInputValue;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProcessNodePortResolutionCacheTest {
    private static final ProcessNodePortResolution RESOLVED = new ProcessNodePortResolution(
            List.of(new ProcessNodePort("success", "Weiter", "Fortsetzen")), List.of());

    private final ProcessNodePortResolutionCache cache = new ProcessNodePortResolutionCache(JsonMapperTestUtils.createMapper());
    private final AtomicInteger resolutions = new AtomicInteger();

    @Test
    void reusesTheResolutionForUnchangedInputs() {
        var first = cache.resolve(node(), this::resolve);
        var second = cache.resolve(node(), this::resolve);

        assertSame(first, second);
        assertEquals(1, resolutions.get());
    }

    @Test
    void resolvesAgainWhenAnInputChanges() {
        cache.resolve(node(), this::resolve);
        List<Consumer<ProcessNodeEntity>> changes = List.of(
                node -> node.setConfiguration(configuration("other")),
                node -> node.setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, true))),
                node -> node.setProcessNodeDefinitionVersion(2),
                node -> node.setProcessNodeDefinitionKey("other_provider"),
                node -> node.setProcessVersion(4),
                node -> node.setProcessId(11)
        );

        for (var change : changes) {
            var node = node();
            change.accept(node);
            cache.resolve(node, this::resolve);
        }

        assertEquals(1 + changes.size(), resolutions.get());
    }

    @Test
    void doesNotKeepResolutionsWithProblems() {
        var failed = ProcessNodePortResolver.failed();

        cache.resolve(node(), () -> count(failed));
        var retried = cache.resolve(node(), this::resolve);

        assertSame(RESOLVED, retried);
        assertEquals(2, resolutions.get());
    }

    @Test
    void evictsTheLeastRecentlyUsedEntryBeyondItsBound() {
        var bounded = new ProcessNodePortResolutionCache(JsonMapperTestUtils.createMapper(), 1);

        bounded.resolve(node(), this::resolve);
        bounded.resolve(node().setConfiguration(configuration("other")), this::resolve);
        bounded.resolve(node(), this::resolve);

        assertEquals(3, resolutions.get());
    }

    private ProcessNodePortResolution resolve() {
        return count(RESOLVED);
    }

    private ProcessNodePortResolution count(ProcessNodePortResolution resolution) {
        resolutions.incrementAndGet();
        return resolution;
    }

    private static ProcessNodeEntity node() {
        return new ProcessNodeEntity().setId(1).setProcessId(10).setProcessVersion(3)
                .setProcessNodeDefinitionKey("provider").setProcessNodeDefinitionVersion(1)
                .setConfiguration(configuration("success"))
                .setDeadlines(Map.of("processing", new ProcessNodeDeadlineConfiguration(3, false)));
    }

    private static AuthoredElementValues configuration(String choice) {
        var values = new AuthoredElementValues();
        values.put("choices", new LiteralAuthoredInputValue(List.of(Map.of("key", choice, "label", "Weiter"))));
        return values;
    }
}
