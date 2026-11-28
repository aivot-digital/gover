package de.aivot.prosuna.backend.plugins.core.v1.nodes.terminators;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import de.aivot.prosuna.backend.process.enums.ProcessRetentionTimeUnit;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.prosuna.backend.process.models.ProcessRetentionTime;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTerminationNodeV1Test {
    private final DefaultTerminationNodeV1 node = new DefaultTerminationNodeV1();

    @Test
    void configurationExplainsWhenToProvideAnOverrideAndItsUnit() throws Exception {
        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null, new ProcessEntity().setId(7), new ProcessVersionEntity().setProcessVersion(1),
                new ProcessNodeEntity().setId(11)));
        var value = layout.findChild("retention_value", NumberInputElement.class).orElseThrow();
        var unit = layout.findChild("retention_unit", SelectInputElement.class).orElseThrow();

        assertTrue(value.getHint().contains("positive ganze Zahl"));
        assertTrue(value.getHint().contains("beide Felder leer"));
        assertTrue(value.getHint().contains("Aufbewahrungsfrist der Prozessversion"));
        assertTrue(value.getHint().contains("höchstens 100 Jahre"));
        assertEquals("Wählen Sie eine Zeiteinheit, wenn Sie eine abweichende Aufbewahrungsfrist angeben.", unit.getHint());
    }

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
        assertEquals("Wählen Sie für die abweichende Aufbewahrungsfrist eine Zeiteinheit aus.",
                node.validateConfiguration(validationContext(config)).get("retention_unit").getFirst());
        config.retentionUnit = "days";
        config.retentionValue = 1.5;
        assertEquals("Geben Sie für die abweichende Aufbewahrungsfrist eine positive ganze Zahl an.",
                node.validateConfiguration(validationContext(config)).get("retention_value").getFirst());
        config.retentionValue = 1;
        config.retentionUnit = "invalid";
        assertEquals("Wählen Sie für die abweichende Aufbewahrungsfrist eine Zeiteinheit aus.",
                node.validateConfiguration(validationContext(config)).get("retention_unit").getFirst());
        var error = assertThrows(ProcessNodeExecutionExceptionBrokenImplementation.class, () -> node.init(context(config)));
        assertEquals("Die abweichende Aufbewahrungsfrist ist unvollständig oder ungültig. Geben Sie eine positive ganze Zahl und eine Zeiteinheit an.",
                error.getMessage());
    }

    @ParameterizedTest
    @EnumSource(ProcessRetentionTimeUnit.class)
    void validateConfiguration_EnforcesMaximumForEveryOverrideUnit(ProcessRetentionTimeUnit unit) {
        var config = new DefaultTerminationNodeV1.DefaultTerminationNodeV1Config();
        var maximum = ProcessRetentionTime.maximumValue(unit);
        config.retentionUnit = unit.name().toLowerCase();
        config.retentionValue = maximum;

        assertNull(node.validateConfiguration(validationContext(config)));
        config.retentionValue = maximum + 1;
        assertEquals("Die abweichende Aufbewahrungsfrist darf höchstens 100 Jahre betragen.",
                node.validateConfiguration(validationContext(config)).get("retention_value").getFirst());
        var error = assertThrows(ProcessNodeExecutionExceptionBrokenImplementation.class, () -> node.init(context(config)));
        assertEquals("Die abweichende Aufbewahrungsfrist darf höchstens 100 Jahre betragen.", error.getMessage());
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
