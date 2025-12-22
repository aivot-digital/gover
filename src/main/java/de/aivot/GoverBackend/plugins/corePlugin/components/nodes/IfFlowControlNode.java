package de.aivot.GoverBackend.plugins.corePlugin.components.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.form.input.FunctionInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class IfFlowControlNode implements ProcessNodeProvider, PluginComponent {
    private static final String PORT_NAME_TRUE = "true";
    private static final String PORT_NAME_FALSE = "false";

    private static final String CONDITION_FIELD_KEY = "condition";
    private final ProcessDataService processDataService;

    public IfFlowControlNode(ProcessDataService processDataService) {
        this.processDataService = processDataService;
    }

    @Nonnull
    @Override
    public String getKey() {
        return "if";
    }

    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull UserEntity user,
                                                      @Nonnull ProcessDefinitionEntity processDefinition,
                                                      @Nonnull ProcessDefinitionVersionEntity processDefinitionVersion,
                                                      @Nullable ProcessDefinitionNodeEntity thisNode) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var conditionField = new FunctionInputElement();
        conditionField.setId(CONDITION_FIELD_KEY);
        conditionField.setLabel("Bedingung");
        conditionField.setHint("Geben Sie die Bedingung ein, die ausgewertet werden soll. Verwenden Sie gültige Ausdrücke basierend auf den Prozessdaten.");
        conditionField.setRequired(true);
        layout.addChild(conditionField);

        return layout;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.FlowControl;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Wenn-Dann";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Der Wenn-Dann-Knoten ermöglicht bedingte Verzweigungen im Prozessfluss basierend auf definierten Bedingungen.";
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME_TRUE,
                        "Wahr",
                        "Der Prozessfluss wird hier fortgesetzt, wenn die Bedingung erfüllt ist."
                ),
                new ProcessNodePort(
                        PORT_NAME_FALSE,
                        "Falsch",
                        "Der Prozessfluss wird hier fortgesetzt, wenn die Bedingung nicht erfüllt ist."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessInstanceEntity processInstance,
                                           @Nonnull ProcessDefinitionNodeEntity thisNode,
                                           @Nonnull Map<String, Object> workingData) throws Exception {
        var configuration = thisNode.getConfiguration();
        var condition = configuration
                .get(CONDITION_FIELD_KEY)
                .getOptionalValue()
                .orElse("")
                .toString();

        var conditionValueStr = processDataService
                .interpolate(workingData, condition);

        var conditionValue = Boolean
                .parseBoolean(conditionValueStr);

        var metadata = new HashMap<String, Object>();
        metadata.put("condition", condition);
        metadata.put("conditionValue", conditionValue);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(conditionValue ? PORT_NAME_TRUE : PORT_NAME_FALSE)
                .setNodeData(metadata)
                .addEvent(ProcessNodeExecutionEvent.of(
                        ProcessHistoryEventType.Complete,
                        "Bedingung ausgewertet",
                        "Die Bedingung '%s' wurde mit dem Wert '%s' ausgewertet.".formatted(condition, conditionValue)
                ));
    }
}
