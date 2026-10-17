package de.aivot.prosuna.backend.process.models.processContext;

import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.ComputedElementSubState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessNodeConfigurationValidationContextTest {
    @Test
    void shouldExposeDeferredTopLevelAndReplicatingContainerValuesDuringAuthoring() {
        var rowStates = new ComputedElementStates();
        rowStates.put("fileName", new ComputedElementState().setInputValueDeferred(true));

        var containerState = new ComputedElementState().setSubStates(List.of(
                ComputedElementSubState.of("row-1", rowStates)
        ));
        var derivedData = new DerivedRuntimeElementData();
        derivedData.getElementStates().put("subject", new ComputedElementState().setInputValueDeferred(true));
        derivedData.getElementStates().put("attachments", containerState);

        var context = new ProcessNodeConfigurationValidationContext<>(
                new ProcessNodeEntity(),
                new Object(),
                derivedData,
                ProcessNodeConfigurationValidationPhase.Authoring
        );

        assertTrue(context.isDeferred("subject"));
        assertTrue(context.isDeferred("attachments", 0, "fileName"));
        assertFalse(context.isDeferred("attachments", 1, "fileName"));
        assertFalse(context.isDeferred("unknown"));
    }

    @Test
    void shouldNeverReportValuesAsDeferredAfterRuntimeResolution() {
        var derivedData = new DerivedRuntimeElementData();
        derivedData.getElementStates().put("subject", new ComputedElementState().setInputValueDeferred(true));

        var context = new ProcessNodeConfigurationValidationContext<>(
                new ProcessNodeEntity(),
                new Object(),
                derivedData,
                ProcessNodeConfigurationValidationPhase.Runtime
        );

        assertFalse(context.isDeferred("subject"));
    }
}
