package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.models.elements.form.content.AlertContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.CodeInputElement;
import de.aivot.prosuna.backend.elements.models.input.LiteralAuthoredInputValue;
import de.aivot.prosuna.backend.enums.AlertType;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LowCodeActionNodeV1Test {
    @Test
    void getInitialConfiguration_ShouldPreserveExistingProcessData() throws Exception {
        var node = createNode();

        var initialConfiguration = node.getInitialConfiguration();

        var codeValue = assertInstanceOf(
                LiteralAuthoredInputValue.class,
                initialConfiguration.get(LowCodeActionNodeV1.LowCodeActionNodeConfig.CODE)
        );
        var code = assertInstanceOf(String.class, codeValue.value());

        try (var engine = new JavascriptEngine(List.of())) {
            var result = engine
                    .registerGlobalObject("$", Map.of("existing", 42))
                    .evaluateCode(new JavascriptCode().setCode(code));

            assertEquals(Map.of("existing", 42), result.asMap());
        }
    }

    @Test
    void getConfigurationLayout_ShouldExplainProcessDataReplacement() throws Exception {
        var node = createNode();

        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                new ProcessEntity(),
                new ProcessVersionEntity(),
                new ProcessNodeEntity()
        ));

        var codeInput = assertInstanceOf(CodeInputElement.class, layout.getChildren().getFirst());
        assertEquals("JavaScript-Code", codeInput.getLabel());
        assertTrue(codeInput.getHint().contains("...$"));

        var notice = assertInstanceOf(AlertContentElement.class, layout.getChildren().get(1));
        assertEquals(AlertType.Info, notice.getAlertType());
        assertTrue(notice.getText().contains("ersetzt die bisherigen Vorgangsdaten vollständig"));
    }

    private LowCodeActionNodeV1 createNode() {
        return new LowCodeActionNodeV1(mock(JavascriptEngineFactoryService.class));
    }
}
