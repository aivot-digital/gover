package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceAssigned;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class InstanceUnassignmentActionNodeV1Test {
    private final InstanceUnassignmentActionNodeV1 node = new InstanceUnassignmentActionNodeV1();

    @Test
    void definesAutomaticActionWithoutSettingsOrOutputs() throws Exception {
        assertEquals(CorePlugin.PLUGIN_KEY, node.getParentPluginKey());
        assertEquals("unassign_instance", node.getComponentKey());
        assertEquals("1.0.0", node.getComponentVersion());
        assertEquals(ProcessNodeType.Action, node.getType());
        assertArrayEquals(new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic}, node.getExecutionTypes());
        assertEquals(InstanceUnassignmentActionNodeV1.Config.class, node.getNodeConfigurationClass());
        assertTrue(node.getInitialConfiguration().isEmpty());
        assertTrue(node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null, new ProcessEntity().setId(42), new ProcessVersionEntity().setProcessVersion(3),
                new ProcessNodeEntity().setId(12))).getChildren().isEmpty());
        assertTrue(node.getOutputs().isEmpty());
        assertEquals(1, node.getPorts().size());
        assertEquals("success", node.getPorts().getFirst().key());
    }

    @Test
    void clearsAssignmentAndContinuesWithCurrentProcessData() {
        var processData = Map.<String, Object>of("case", "data");
        var context = new ProcessNodeExecutionInitContext<>(
                mock(ProcessNodeExecutionLogger.class), new ProcessNodeEntity().setId(12),
                new ProcessInstanceEntity().setId(99L).setAssignedUserId("previous"),
                new ProcessInstanceTaskEntity().setId(17L), null,
                new ProcessExecutionData().addProcessData(processData), new InstanceUnassignmentActionNodeV1.Config());

        var result = assertInstanceOf(ProcessNodeExecutionResultInstanceAssigned.class, node.init(context));

        assertNull(result.getAssignedUserId());
        assertEquals("success", result.getViaPort());
        assertEquals(processData, result.getProcessData());
        assertEquals(Map.of(), result.getRuntimeData());
        assertEquals(Map.of(), result.getNodeData());
    }
}
