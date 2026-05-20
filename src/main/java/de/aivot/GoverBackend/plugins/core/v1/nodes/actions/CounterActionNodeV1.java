package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
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
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * This node is used to increment (or decrement) a counter when working with loops in a process. A user should be able to specify a process data key where the counter is stored as
 * well as an increment step value. If no process data key is defined, the value is stored in the node data of the node and fetched from the previous iteration of this node.
 */
@Component
public class CounterActionNodeV1 implements ProcessNodeDefinition<CounterActionNodeV1.CounterConfiguration> {
    public static final String NODE_KEY = "counter";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_VALUE = "value";
    private static final String OUTPUT_PREVIOUS_VALUE = "previousValue";
    private static final String OUTPUT_INCREMENT = "increment";
    private static final String OUTPUT_STORAGE_TARGET = "storageTarget";
    private static final String OUTPUT_STORAGE_MODE = "storageMode";
    private static final String STORAGE_MODE_PROCESS_DATA = "processData";
    private static final String STORAGE_MODE_NODE_DATA = "nodeData";

    private static final long DEFAULT_INCREMENT = 1L;

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
    public Class<CounterConfiguration> getNodeConfigurationClass() {
        return CounterConfiguration.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        try {
            return ElementPOJOMapper
                    .createFromPOJO(CounterConfiguration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }
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

        // Check if an increment exists in the configuration. If not, use the default.
        long increment;
        Number incrementObj = configuration.increment;
        if (incrementObj != null) {
            increment = incrementObj.longValue();
        } else {
            increment = DEFAULT_INCREMENT;
        }

        // Create variables to store the relevant counter information for this iteration.
        long lastCounterValue;
        String storageType;

        // Extract the variable process data key from the configuration.
        String variableProcessDataKey = configuration.variable;

        // If a process data key for the variable exists, extract the value from there, otherwise extract the value from the last set of node data.
        if (StringUtils.isNotNullOrEmpty(variableProcessDataKey)) {
            lastCounterValue = getLastCounterValueByVariablePath(context, variableProcessDataKey);
            storageType = STORAGE_MODE_PROCESS_DATA;
        } else {
            lastCounterValue = getLastCounterValueFromPreviousInstantiation(context);
            storageType = STORAGE_MODE_NODE_DATA;
        }

        // Increment the last counter value by the defined increment to get the next counter value.
        var nextCounterValue = lastCounterValue + increment;

        // Build the node data wir all node specific information of this run.
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_VALUE, nextCounterValue);
        nodeData.put(OUTPUT_PREVIOUS_VALUE, lastCounterValue);
        nodeData.put(OUTPUT_INCREMENT, increment);
        nodeData.put(OUTPUT_STORAGE_TARGET, variableProcessDataKey);
        nodeData.put(OUTPUT_STORAGE_MODE, storageType);

        // Update the process data with the next counter value, if a process data key for the variable is defined.
        var execData = context.getCurrentProcessExecutionData();
        if (StringUtils.isNotNullOrEmpty(variableProcessDataKey)) {
            ProcessExecutionData.writeProcessDataValue(execData, variableProcessDataKey, nextCounterValue);
        }

        // Return the result with the updated process data and the node data containing all relevant information about this iteration.
        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setProcessData(execData.getProcessData())
                .setNodeData(nodeData);
    }

    /**
     * Get the last counter value, based on the process data key of the variable. If no value is present in the process data, 0 is returned as the default value.
     *
     * @param context                The current execution context of this operation.
     * @param variableProcessDataKey The process data key pointing to the previous counter value.
     * @return The previous counter value at the variable process data key or 0 if no value is present.
     * @throws ProcessNodeExecutionExceptionInvalidDataType This exception is thrown, when the existing counter value in the process data cannot be converted to a number.
     */
    private static long getLastCounterValueByVariablePath(@Nonnull ProcessNodeExecutionInitContext<CounterConfiguration> context,
                                                          @Nonnull String variableProcessDataKey) throws ProcessNodeExecutionExceptionInvalidDataType {
        long lastIndex;
        var currentIndexObj = ProcessExecutionData.resolveProcessDataValue(
                context.getCurrentProcessExecutionData(),
                variableProcessDataKey
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
                    "Der aktuelle Wert der Vorgangsdatenvariable %s konnte nicht in eine Zahl umgewandelt werden. Der Wert war %s.",
                    StringUtils.quote(variableProcessDataKey),
                    StringUtils.quote(currentIndexObj.toString())
            );
        }
        return lastIndex;
    }

    /**
     * Get the last counter value based on the node data of the last instantiation of this process node. If no previous instantiation exists, 0 is returned.
     *
     * @param context The current execution context of this operation.
     * @return The last counter value or 0 if no previous instantiation exists.
     */
    private long getLastCounterValueFromPreviousInstantiation(@Nonnull ProcessNodeExecutionInitContext<CounterConfiguration> context) {
        Optional<ProcessInstanceTaskEntity> lastIterationTask = processInstanceTaskRepository
                .findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(
                        context.getThisProcessInstance().getId(),
                        context.getThisNode().getId()
                );

        Object lastIterationIndex = lastIterationTask
                .map(ProcessInstanceTaskEntity::getNodeData)
                .map(nodeData -> nodeData.getOrDefault(OUTPUT_VALUE, 0))
                .orElse(0);

        return NumberUtils
                .asNumber(lastIterationIndex)
                .orElse(0)
                .longValue();
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class CounterConfiguration {
        public static final String VARIABLE_FIELD_ID = "variable";
        @InputElementPOJOBinding(id = VARIABLE_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Vorgangsdatenvariable"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optionaler Pfad innerhalb der Vorgangsdaten, z. B. schleife.zähler. Wenn leer, wird der letzte Zählerstand dieses Prozesselements aus den Elementdaten verwendet."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 9.0),
                @ElementPOJOBindingProperty(key = "disableWildCards", boolValue = true),
        })
        @Nullable
        public String variable;

        public static final String INCREMENT_FIELD_ID = "increment";
        @InputElementPOJOBinding(id = INCREMENT_FIELD_ID, type = ElementType.Number, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Inkrement"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optionale Natürliche Zahl, um die der Zähler erhöht wird. Wenn leer, wird standardmäßig um 1 erhöht."),
                @ElementPOJOBindingProperty(key = "decimalPlaces", intValue = 0),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 3.0)
        })
        @Nullable
        public Number increment;
    }
}
