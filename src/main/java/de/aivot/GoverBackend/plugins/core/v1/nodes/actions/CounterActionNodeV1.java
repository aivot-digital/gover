package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.utils.NumberUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CounterActionNodeV1 implements ProcessNodeDefinition<CounterActionNodeV1.CounterConfiguration> {
    public static final String NODE_KEY = "counter";

    private static final String PORT_NAME = "output";

    private static final String VARIABLE_FIELD_ID = "variable";
    private static final String INCREMENT_FIELD_ID = "increment";

    private static final String OUTPUT_VALUE = "value";
    private static final String OUTPUT_PREVIOUS_VALUE = "previousValue";
    private static final String OUTPUT_INCREMENT = "increment";
    private static final String OUTPUT_STORAGE_TARGET = "storageTarget";
    private static final String OUTPUT_STORAGE_MODE = "storageMode";

    private static final String STORAGE_MODE_PROCESS_DATA = "processData";
    private static final String STORAGE_MODE_RUNTIME_DATA = "runtimeData";

    private static final long DEFAULT_INCREMENT = 1L;
    private static final String RUNTIME_DATA_VALUE_KEY = OUTPUT_VALUE;

    private final ProcessInstanceTaskRepository processInstanceTaskRepository;

    public CounterActionNodeV1(ProcessInstanceTaskRepository processInstanceTaskRepository) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Zähler";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Erhöht einen Zählerstand bei jeder Ausführung um einen konfigurierbaren Wert.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(CounterConfiguration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        layout
                .findChild(INCREMENT_FIELD_ID, NumberInputElement.class)
                .ifPresent(incrementField -> incrementField.setValue(new ElementValueFunctions()
                        .setType(ValueFunctionType.NoCode)
                        .setNoCode(new NoCodeStaticValue(DEFAULT_INCREMENT))));

        return layout;
    }

    @Nonnull
    @Override
    public Class<CounterConfiguration> getNodeConfigurationClass() {
        return CounterConfiguration.class;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Weiter",
                        "Der Prozess wird fortgesetzt, nachdem der Zähler erhöht wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_VALUE,
                        "Zählerstand",
                        "Der neue Zählerstand nach der Erhöhung."
                ),
                new ProcessNodeOutput(
                        OUTPUT_PREVIOUS_VALUE,
                        "Vorheriger Zählerstand",
                        "Der Zählerstand vor der Erhöhung."
                ),
                new ProcessNodeOutput(
                        OUTPUT_INCREMENT,
                        "Inkrement",
                        "Der Wert, um den der Zähler erhöht wurde."
                )
        );
    }

    @Override
    public List<ProcessDataKeyHint> calculateProcessDataKeyHints(@Nonnull ProcessNodeEntity processNodeEntity,
                                                                 @Nonnull CounterConfiguration configuration,
                                                                 @Nonnull List<ProcessDataKeyHint> previousDataKeys) {
        var variablePath = configuration.variable;

        if (StringUtils.isNullOrEmpty(variablePath)) {
            return previousDataKeys;
        }

        var res = new ArrayList<ProcessDataKeyHint>();

        for (var existingDataKey : previousDataKeys) {
            if (Objects.equals(existingDataKey.key(), variablePath)) {
                continue;
            }
            res.add(existingDataKey);
        }

        res.add(new ProcessDataKeyHint(
                variablePath,
                ProcessDataKeyHintType.ProcessData
        ));

        return res;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<CounterConfiguration> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        var variablePath = configuration.variable;
        var incrementObj = configuration.increment;
        var increment = DEFAULT_INCREMENT;
        if (incrementObj != null) {
            increment = incrementObj.longValue();
        }

        long lastIndex;
        String storageType;
        if (StringUtils.isNotNullOrEmpty(variablePath)) {
            lastIndex = getLastIndexByVariablePath(context, variablePath);
            storageType = STORAGE_MODE_PROCESS_DATA;
        } else {
            lastIndex = getLastIndexFromPreviousInstantiation(context);
            storageType = STORAGE_MODE_RUNTIME_DATA;
        }

        var nextIndex = lastIndex + increment;

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_VALUE, nextIndex);
        nodeData.put(OUTPUT_PREVIOUS_VALUE, lastIndex);
        nodeData.put(OUTPUT_INCREMENT, increment);
        nodeData.put(OUTPUT_STORAGE_TARGET, variablePath);
        nodeData.put(OUTPUT_STORAGE_MODE, storageType);

        var execData = context.getCurrentProcessExecutionData();
        if (StringUtils.isNotNullOrEmpty(variablePath)) {
            ProcessExecutionData.writeProcessDataValue(execData, variablePath, nextIndex);
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setProcessData(execData.getProcessData())
                .setNodeData(nodeData);
    }

    private static long getLastIndexByVariablePath(@Nonnull ProcessNodeExecutionInitContext<CounterConfiguration> context, String variablePath) throws ProcessNodeExecutionExceptionInvalidDataType {
        long lastIndex;
        var currentIndexObj = ProcessExecutionData.resolveProcessDataValue(
                context.getCurrentProcessExecutionData(),
                variablePath
        );

        if (currentIndexObj == null) {
            currentIndexObj = 0;
        }

        try {
            lastIndex = NumberUtils
                    .asNumber(currentIndexObj)
                    .orElse(0)
                    .longValue();
        } catch (NumberFormatException e) {
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    "Der aktuelle Wert der Vorgangsdatenvariable %s konnte nicht in eine Zahl umgewandelt werden. Der Wert war %s",
                    StringUtils.quote(variablePath),
                    StringUtils.quote(currentIndexObj.toString())
            );
        }
        return lastIndex;
    }

    private long getLastIndexFromPreviousInstantiation(@Nonnull ProcessNodeExecutionInitContext<CounterConfiguration> context) {
        long lastIndex;
        var lastIterationTask = processInstanceTaskRepository.findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(
                context.getThisProcessInstance().getId(),
                context.getThisNode().getId()
        );

        var lastIterationIndex = lastIterationTask
                .map(ProcessInstanceTaskEntity::getRuntimeData)
                .map(runtimeData -> runtimeData.get(RUNTIME_DATA_VALUE_KEY))
                .orElse(null);

        if (lastIterationIndex == null) {
            lastIterationIndex = 0;
        }

        lastIndex = NumberUtils
                .asNumber(lastIterationIndex)
                .orElse(0)
                .longValue();
        return lastIndex;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class CounterConfiguration {
        public static final String VARIABLE = VARIABLE_FIELD_ID;
        @InputElementPOJOBinding(id = VARIABLE, type = ElementType.ProcessDataKeyInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Vorgangsdatenvariable"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optionaler Pfad innerhalb der Vorgangsdaten, z. B. schleife.zähler. Wenn leer, wird der letzte Zählerstand dieses Prozesselements aus den Elementdaten verwendet."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 9.0)
        })
        public String variable;

        public static final String INCREMENT = INCREMENT_FIELD_ID;
        @InputElementPOJOBinding(id = INCREMENT, type = ElementType.Number, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Inkrement"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Natürliche Zahl, um die der Zähler erhöht wird."),
                @ElementPOJOBindingProperty(key = "decimalPlaces", intValue = 0),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 3.0),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public Number increment;
    }
}
