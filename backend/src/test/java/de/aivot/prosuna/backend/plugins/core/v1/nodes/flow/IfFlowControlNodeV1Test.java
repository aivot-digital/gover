package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import de.aivot.prosuna.backend.elements.models.elements.form.input.NoCodeInputElementItem;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IfFlowControlNodeV1Test {
    private final IfFlowControlNodeV1 node = new IfFlowControlNodeV1(
            new JavascriptEngineFactoryService(List.of()),
            new NoCodeEvaluationService(List.of())
    );

    @Test
    void shouldSelectThePortFromTheConfiguredConditionType() throws Exception {
        var noCode = new IfFlowControlNodeV1.IfFlowControlNodeConfig();
        noCode.conditionType = FlowConditionSupport.CONDITION_TYPE_VALUE_NO_CODE;
        noCode.conditionNoCode = new NoCodeInputElementItem(new NoCodeStaticValue(true));
        noCode.condition = "false";
        assertEquals("true", execute(noCode).getViaPort());

        var lowCode = new IfFlowControlNodeV1.IfFlowControlNodeConfig();
        lowCode.conditionType = FlowConditionSupport.CONDITION_TYPE_VALUE_LOW_CODE;
        lowCode.condition = "1 > 2";
        var result = execute(lowCode);
        assertEquals("false", result.getViaPort());
        assertEquals(false, result.getNodeData().get("conditionValue"));
    }

    @Test
    void shouldFallBackToLowCodeForLegacyNodesWithoutConditionType() throws Exception {
        var legacy = new IfFlowControlNodeV1.IfFlowControlNodeConfig();
        legacy.condition = "true";
        assertEquals("true", execute(legacy).getViaPort());
    }

    @Test
    void shouldRejectNonBooleanLowCodeResults() {
        assertTrue(lowCodeError("'ja'").contains("Die Bedingung des If-Knotens konnte nicht als JavaScript ausgeführt werden"));
        assertTrue(lowCodeError("undefined").contains("Die Bedingung des If-Knotens muss einen booleschen JavaScript-Wert ergeben."));
    }

    private String lowCodeError(String condition) {
        var config = new IfFlowControlNodeV1.IfFlowControlNodeConfig();
        config.conditionType = FlowConditionSupport.CONDITION_TYPE_VALUE_LOW_CODE;
        config.condition = condition;
        return assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class, () -> execute(config)).getMessage();
    }

    private ProcessNodeExecutionResultTaskCompleted execute(IfFlowControlNodeV1.IfFlowControlNodeConfig config) throws Exception {
        ProcessNodeExecutionInitContext<IfFlowControlNodeV1.IfFlowControlNodeConfig> context =
                mock(ProcessNodeExecutionInitContext.class);
        when(context.getCurrentProcessExecutionData()).thenReturn(new ProcessExecutionData());
        when(context.getConfigurationOfExecutingNode()).thenReturn(config);
        return assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context));
    }
}
