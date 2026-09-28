package de.aivot.prosuna.backend.plugins.core.v1.nodes.terminators;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTerminationNodeV1Test {
    private final DefaultTerminationNodeV1 node = new DefaultTerminationNodeV1();

    @Test
    void init_UsesVersionRetentionWhenNoOverrideIsConfigured() throws Exception {
        var config = new DefaultTerminationNodeV1.DefaultTerminationNodeV1Config();

        var result = assertInstanceOf(ProcessNodeExecutionResultInstanceCompleted.class, node.init(context(config)));

        assertNull(result.getRetentionDate());
        assertNull(node.validateConfiguration(validationContext(config)));
    }

    @Test
    void init_SetsOverrideDateWhenBothFieldsAreConfigured() throws Exception {
        var config = new DefaultTerminationNodeV1.DefaultTerminationNodeV1Config();
        config.retentionValue = 2;
        config.retentionUnit = "weeks";
        var before = Instant.now();

        var result = assertInstanceOf(ProcessNodeExecutionResultInstanceCompleted.class, node.init(context(config)));

        assertNotNull(result.getRetentionDate());
        assertTrue(result.getRetentionDate().isAfter(before.plusSeconds(13 * 24 * 60 * 60)));
        assertNull(node.validateConfiguration(validationContext(config)));
    }

    @Test
    void validateConfiguration_RejectsIncompleteFractionalAndUnknownOverrides() {
        var config = new DefaultTerminationNodeV1.DefaultTerminationNodeV1Config();
        config.retentionValue = 1;
        assertNotNull(node.validateConfiguration(validationContext(config)));
        config.retentionUnit = "days";
        config.retentionValue = 1.5;
        assertNotNull(node.validateConfiguration(validationContext(config)));
        config.retentionValue = 1;
        config.retentionUnit = "invalid";
        assertNotNull(node.validateConfiguration(validationContext(config)));
        assertThrows(ProcessNodeExecutionExceptionBrokenImplementation.class, () -> node.init(context(config)));
    }

    @SuppressWarnings("unchecked")
    private static ProcessNodeExecutionInitContext<DefaultTerminationNodeV1.DefaultTerminationNodeV1Config> context(
            DefaultTerminationNodeV1.DefaultTerminationNodeV1Config configuration) {
        var context = mock(ProcessNodeExecutionInitContext.class);
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        return context;
    }

    private static ProcessNodeConfigurationValidationContext<DefaultTerminationNodeV1.DefaultTerminationNodeV1Config> validationContext(
            DefaultTerminationNodeV1.DefaultTerminationNodeV1Config configuration) {
        return new ProcessNodeConfigurationValidationContext<>(mock(ProcessNodeEntity.class), configuration,
                mock(DerivedRuntimeElementData.class), ProcessNodeConfigurationValidationPhase.Authoring);
    }
}
