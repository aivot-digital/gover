package de.aivot.GoverBackend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class IfFlowControlNodeV1 implements ProcessNodeDefinition {
    private static final String PORT_NAME_TRUE = "true";
    private static final String PORT_NAME_FALSE = "false";

    private static final String CONDITION_FIELD_KEY = "condition";
    private final ProcessDataService processDataService;

    public IfFlowControlNodeV1(ProcessDataService processDataService) {
        this.processDataService = processDataService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "if";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.FlowControl;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Konditionelle Verzweigung (Wenn-Dann-Sonst)";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Leitet den Vorgang basierend auf einer Bedingung in unterschiedliche Pfade ein.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var conditionField = new TextInputElement();
        conditionField.setId(CONDITION_FIELD_KEY);
        conditionField.setLabel("Bedingung");
        conditionField.setHint("Geben Sie die Bedingung ein, die ausgewertet werden soll. Verwenden Sie gültige Ausdrücke basierend auf den Prozessdaten.");
        conditionField.setRequired(true);
        layout.addChild(conditionField);

        return layout;
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
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var configuration = context
                .getThisNode()
                .getConfiguration();

        var condition = configuration
                .get(CONDITION_FIELD_KEY)
                .getOptionalValue()
                .orElse("")
                .toString();

        var conditionValueStr = processDataService
                .interpolate(context.getProcessData(), condition);

        var conditionValue = Boolean
                .parseBoolean(conditionValueStr);

        var metadata = new HashMap<String, Object>();
        metadata.put("condition", condition);
        metadata.put("conditionValueStr", conditionValueStr);
        metadata.put("conditionValue", conditionValue);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(conditionValue ? PORT_NAME_TRUE : PORT_NAME_FALSE)
                .setNodeData(metadata);
    }
}
