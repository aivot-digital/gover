package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.*;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessNodePortResolverTest {
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ProcessNodePortResolver resolver = new ProcessNodePortResolver(VALIDATORS.getValidator());
    @SuppressWarnings("unchecked")
    private final ProcessNodeDefinition<List<String>> provider = mock(ProcessNodeDefinition.class);
    private final ProcessNodeEntity node = new ProcessNodeEntity();

    @AfterAll
    static void closeValidators() { VALIDATORS.close(); }

    @Test
    void resolvesZeroOneAndManyPortsFromConfigurationInProviderOrder() throws Exception {
        when(provider.getPorts(any())).thenAnswer(invocation -> ((List<String>) invocation.getArgument(0))
                .stream().map(ProcessNodePortResolverTest::port).toList());
        for (var keys : List.of(List.<String>of(), List.of("a"), List.of("b", "a"))) {
            var result = resolver.resolve(node, provider, keys);
            assertEquals(keys, result.ports().stream().map(ProcessNodePort::key).toList());
            assertTrue(result.problems().isEmpty());
        }
    }

    @Test
    void appendsEachHardDeadlineAndIgnoresHiddenSettingsWithoutMutatingProviderPorts() throws Exception {
        var ports = List.of(port("success"));
        when(provider.getPorts(any())).thenReturn(ports);
        when(provider.getDeadlines(any())).thenReturn(List.of(deadline("message"), deadline("form")));
        node.setDeadlines(Map.of("message", new ProcessNodeDeadlineConfiguration(1, true),
                "form", new ProcessNodeDeadlineConfiguration(5, true),
                "hidden", new ProcessNodeDeadlineConfiguration(2, true)));
        var result = resolver.resolve(node, provider, List.of());
        assertTrue(result.problems().isEmpty());
        assertEquals(List.of("success", "message_overdue", "form_overdue"), result.ports().stream().map(ProcessNodePort::key).toList());
        assertNull(result.ports().getFirst().deadlineKey());
        assertEquals("message", result.ports().get(1).deadlineKey());
        assertEquals("form", result.ports().getLast().deadlineKey());
        assertEquals(1, ports.size());
        assertEquals(2, result.deadlineDefinitions().size());
    }

    @ParameterizedTest
    @CsvSource({", false, false", ", true, false", "1, false, false", "1, true, true"})
    void exposesDeadlineOnlyWhenConfiguredAndStrict(Integer days, boolean strict, boolean expected) {
        when(provider.getDeadlines(any())).thenReturn(List.of(deadline("phase")));
        node.setDeadlines(Map.of("phase", new ProcessNodeDeadlineConfiguration(days, strict)));
        var result = resolver.resolve(node, provider, List.of());
        assertEquals(expected ? 1 : 0, result.ports().size());
        assertTrue(result.problems().isEmpty());
        assertEquals(List.of(deadline("phase")), result.deadlineDefinitions());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 3653})
    void rejectsInvalidDurationsButKeepsDefinitionsEditable(int days) {
        when(provider.getDeadlines(any())).thenReturn(List.of(deadline("phase")));
        node.setDeadlines(Map.of("phase", new ProcessNodeDeadlineConfiguration(days, true)));
        var result = resolver.resolve(node, provider, List.of());
        assertFalse(result.problems().isEmpty());
        assertTrue(result.ports().isEmpty());
        assertEquals(List.of(deadline("phase")), result.deadlineDefinitions());
    }

    @Test
    void rejectsDuplicateDeadlineKeysAndExpiryPortCollisionsEvenWithoutConfiguredDeadlines() throws Exception {
        when(provider.getDeadlines(any())).thenReturn(List.of(deadline("phase"), deadline("phase")));
        assertFalse(resolver.resolve(node, provider, List.of()).problems().isEmpty());
        when(provider.getDeadlines(any())).thenReturn(List.of(deadline("phase")));
        when(provider.getPorts(any())).thenReturn(List.of(port("phase_overdue")));
        assertFalse(resolver.resolve(node, provider, List.of()).problems().isEmpty());
        when(provider.getPorts(any())).thenReturn(List.of());
        when(provider.getDeadlines(any())).thenReturn(List.of(deadline("phase"),
                new ProcessNodeDeadlineDefinition("other", "Andere", "Andere Frist", port("phase_overdue"))));
        assertFalse(resolver.resolve(node, provider, List.of()).problems().isEmpty());
    }

    @Test
    void invalidProviderResultsRemainRecoverableWithoutExposingInternalErrors() throws Exception {
        for (var ports : List.of(
                List.of(port("duplicate"), port("duplicate")),
                List.of(new ProcessNodePort("a", "Ausgang", "Beschreibung", "providerControlled")),
                List.of(port("x".repeat(33))),
                List.of(new ProcessNodePort("a", "", "Description")),
                List.of(new ProcessNodePort("a", "Label", "")),
                Arrays.asList((ProcessNodePort) null)
        )) {
            when(provider.getPorts(any())).thenReturn(ports);
            var result = resolver.resolve(node, provider, List.of());
            assertFalse(result.problems().isEmpty());
            assertTrue(result.ports().isEmpty());
        }
        when(provider.getPorts(any())).thenReturn(null);
        assertFalse(resolver.resolve(node, provider, List.of()).problems().isEmpty());
        when(provider.getPorts(any())).thenThrow(new IllegalArgumentException("Internal details"));
        var failed = resolver.resolve(node, provider, List.of());
        assertTrue(failed.ports().isEmpty());
        assertFalse(String.join(" ", failed.problems()).contains("Internal details"));
    }

    private static ProcessNodeDeadlineDefinition deadline(String key) {
        return new ProcessNodeDeadlineDefinition(key, "Frist " + key, "Zeit für diese Phase", port(key + "_overdue"));
    }

    private static ProcessNodePort port(String key) {
        return new ProcessNodePort(key, "Ausgang " + key, "Fortsetzung");
    }
}
