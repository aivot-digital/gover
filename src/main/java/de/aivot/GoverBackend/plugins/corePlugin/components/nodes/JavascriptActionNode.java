package de.aivot.GoverBackend.plugins.corePlugin.components.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextField;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.corePlugin.Core;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionEntity;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionVersionEntity;
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
public class JavascriptActionNode implements ProcessNodeProvider, PluginComponent {
    private static final String PORT_NAME = "output";

    private static final String CODE_FIELD_KEY = "js_code";
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public JavascriptActionNode(JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
    }

    @Nonnull
    @Override
    public String getKey() {
        return "js";
    }

    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public GroupLayout getConfigurationLayout(@Nonnull UserEntity user,
                                              @Nonnull ProcessDefinitionEntity processDefinition,
                                              @Nonnull ProcessDefinitionVersionEntity processDefinitionVersion,
                                              @Nullable ProcessDefinitionNodeEntity thisNode) {
        var layout = new GroupLayout();
        layout.setId(getKey() + "-config");

        var codeField = new TextField();
        codeField.setId(CODE_FIELD_KEY);
        codeField.setLabel("Javascript-Code");
        codeField.setHint("Geben Sie den benutzerdefinierten Javascript-Code ein, der zur Verarbeitung der Daten verwendet werden soll.");
        codeField.setRequired(true);
        codeField.setIsMultiline(true);
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
                                           @Nonnull ProcessDefinitionNodeEntity thisNode,
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
