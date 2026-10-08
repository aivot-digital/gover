package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessNodePortResolverTest {
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final ProcessNodePortResolver resolver = new ProcessNodePortResolver(VALIDATORS.getValidator());

    @AfterAll
    static void closeValidators() {
        VALIDATORS.close();
    }

    @Test
    void resolvesZeroOneAndManyPortsFromConfigurationInProviderOrder() throws Exception {
        var provider = provider();
        when(provider.getPorts(any())).thenAnswer(invocation -> ((Configuration) invocation.getArgument(0)).keys()
                .stream().map(ProcessNodePortResolverTest::port).toList());
        var node = new ProcessNodeEntity();

        for (var keys : List.of(List.<String>of(), List.of("a"), List.of("b", "a"))) {
            var result = resolver.resolve(node, provider, new Configuration(keys));
            assertEquals(keys, result.ports().stream().map(ProcessNodePort::key).toList());
            assertTrue(result.problems().isEmpty());
        }
    }

    @Test
    void alwaysAppendsExactlyOneDeadlinePortAndDoesNotMutateProviderPorts() throws Exception {
        var provider = provider();
        var ports = List.of(port("a"));
        when(provider.getPorts(any())).thenReturn(ports);
        var node = new ProcessNodeEntity().setTimeLimitDays(3);
        var result = resolver.resolve(node, provider, new Configuration(List.of()));

        assertEquals(List.of("a", ProcessNodePortResolver.DEADLINE_PORT_KEY), result.ports().stream().map(ProcessNodePort::key).toList());
        assertEquals("Fristüberschreitung", result.ports().getLast().label());
        assertEquals(1, ports.size());
        node.setTimeLimitDays(null);
        assertEquals(ports, resolver.resolve(node, provider, new Configuration(List.of())).ports());
        when(provider.getPorts(any())).thenReturn(List.of());
        node.setTimeLimitDays(1);
        assertEquals(List.of(ProcessNodePortResolver.DEADLINE_PORT), resolver.resolve(node, provider, new Configuration(List.of())).ports());
    }

    @Test
    void invalidProviderResultsRemainProblemsAndKeepDeadlineAvailable() throws Exception {
        var provider = provider();
        var node = new ProcessNodeEntity().setTimeLimitDays(1);
        for (var ports : List.of(
                List.of(port("duplicate"), port("duplicate")),
                List.of(port(ProcessNodePortResolver.DEADLINE_PORT_KEY)),
                List.of(port("x".repeat(33))),
                List.of(new ProcessNodePort("a", "", "Description")),
                List.of(new ProcessNodePort("a", "Label", "")),
                Arrays.asList((ProcessNodePort) null)
        )) {
            when(provider.getPorts(any())).thenReturn(ports);
            var result = resolver.resolve(node, provider, new Configuration(List.of()));
            assertFalse(result.problems().isEmpty());
            assertEquals(List.of(ProcessNodePortResolver.DEADLINE_PORT), result.ports());
        }
        when(provider.getPorts(any())).thenReturn(null);
        assertFalse(resolver.resolve(node, provider, new Configuration(List.of())).problems().isEmpty());
        when(provider.getPorts(any())).thenThrow(new IllegalArgumentException("Internal details"));
        var failed = resolver.resolve(node, provider, new Configuration(List.of()));
        assertEquals(List.of(ProcessNodePortResolver.DEADLINE_PORT), failed.ports());
        assertFalse(String.join(" ", failed.problems()).contains("Internal details"));
    }

    @SuppressWarnings("unchecked")
    private static ProcessNodeDefinition<Configuration> provider() {
        return mock(ProcessNodeDefinition.class);
    }

    private static ProcessNodePort port(String key) {
        return new ProcessNodePort(key, "Ausgang " + key, "Fortsetzung");
    }

    private record Configuration(List<String> keys) {
    }
}
