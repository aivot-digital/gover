package de.aivot.GoverBackend.plugins.core.v1.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.form.input.CodeInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JavascriptActionNode implements ProcessNodeDefinition, PluginComponent {
    private static final String PORT_NAME = "output";

    private static final String CODE_FIELD_KEY = "js_code";
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public JavascriptActionNode(JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
    }

    @Override
    public @Nonnull String getKey() {
        return "js";
    }

    @Nonnull
    @Override
    public Integer getVersion() {
        return 1;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull UserEntity user,
                                                      @Nonnull ProcessEntity processDefinition,
                                                      @Nonnull ProcessVersionEntity processDefinitionVersion,
                                                      @Nullable ProcessNodeEntity thisNode) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var codeField = new CodeInputElement();
        codeField.setId(CODE_FIELD_KEY);
        codeField.setLabel("Javascript-Code");
        codeField.setHint("Geben Sie den benutzerdefinierten Javascript-Code ein, der zur Verarbeitung der Daten verwendet werden soll.");
        codeField.setRequired(true);
        layout.addChild(codeField);

        return layout;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Javascript";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Verarbeitet Daten mithilfe von benutzerdefiniertem Javascript-Code.";
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Datenweitergabe",
                        "Die verarbeiteten Daten werden hier weitergegeben."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessInstanceEntity processInstance,
                                           @Nonnull ProcessNodeEntity thisNode,
                                           @Nonnull Map<String, Object> workingData) throws Exception {
        var configuration = thisNode
                .getConfiguration();
        var code = configuration
                .get(CODE_FIELD_KEY)
                .getOptionalValue()
                .orElse("")
                .toString();

        var jsCode = new JavascriptCode()
                .setCode(code);

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService
                    .fillJsEngineWithData(workingData, engine);

            try {
                var result = engine
                        .registerGlobalObject("$", workingData.get("$"))
                        .evaluateCode(jsCode);

                return new ProcessNodeExecutionResultTaskCompleted()
                        .setViaPort(PORT_NAME)
                        .setProcessData(result.asMap())
                        .setNodeData(Map.of(
                                "stdout", result.getStdOutput(),
                                "stderr", result.getErrOutput()
                        ))
                        .addEvent(ProcessNodeExecutionEvent.of(
                                ProcessHistoryEventType.Complete,
                                "Javascript-Code erfolgreich ausgeführt.",
                                "Der benutzerdefinierte Javascript-Code wurde erfolgreich ausgeführt."
                        ));
            } catch (Exception e) {
                return new ProcessNodeExecutionResultError()
                        .setMessage(e.getLocalizedMessage());
            }
        } catch (Exception e) {
            return new ProcessNodeExecutionResultError()
                    .setMessage(e.getLocalizedMessage());
        }
    }
}
