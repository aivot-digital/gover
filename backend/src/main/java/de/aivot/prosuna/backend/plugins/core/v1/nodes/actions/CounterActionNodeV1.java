package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.utils.NumberUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;

/**
 * This node is used to increment (or decrement) a counter when working with loops in a process. A user should be able to specify a process data key where the counter is stored as
 * well as an increment step value. If no process data key is defined, the value is stored in the node data of the node and fetched from the previous iteration of this node.
 */
@Component
public class CounterActionNodeV1 implements ProcessNodeDefinition<CounterActionNodeV1.CounterActionNodeV1Configuration> {

    // The unique node key.
    public static final String NODE_KEY = "counter";

    // Output ports of this node.
    private static final String PORT_NAME = "output";

    // Constants for the node data output information of this node.
    private static final String OUTPUT_VALUE = "value";
    private static final String OUTPUT_PREVIOUS_VALUE = "previousValue";
    private static final String OUTPUT_INCREMENT = "increment";
    private static final String OUTPUT_STORAGE_TARGET = "storageTarget";
    private static final String OUTPUT_STORAGE_MODE = "storageMode";
    private static final String VALUE_OUTPUT_STORAGE_MODE_PROCESS_DATA = "processData";
    private static final String VALUE_OUTPUT_STORAGE_MODE_NODE_DATA = "nodeData";

    // More constants for this node.
    private static final long DEFAULT_INCREMENT = 1L;

    // Injected dependencies.
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;

    public CounterActionNodeV1(ProcessInstanceTaskRepository processInstanceTaskRepository) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<CounterActionNodeV1.CounterActionNodeV1Configuration> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var increment = summary.data(OUTPUT_INCREMENT);
        var amount = increment instanceof Number number ? number.doubleValue() : 0;
        var storageTarget = summary.data(OUTPUT_STORAGE_TARGET);
        var variable = storageTarget instanceof String path && StringUtils.isNotNullOrEmpty(path)
                ? path : context.thisNode().resolveName(this);
        var change = amount > 0 ? " um " + text(increment) + " erhöht"
                : amount < 0 ? " um " + text(new java.math.BigDecimal(increment.toString()).abs().stripTrailingZeros().toPlainString()) + " verringert"
                : " nicht verändert";
        return "Der Zähler „" + text(variable) + "“ wurde" + change + "."
                + detail("Vorheriger Zählerstand", summary.data(OUTPUT_PREVIOUS_VALUE)) + detail("Neuer Zählerstand", summary.data(OUTPUT_VALUE));
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
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
    }

    @Nonnull
    @Override
    public String getName() {
        return "Zähler";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Erhöht oder verringert einen Zählerstand bei jeder Ausführung, zum Beispiel um Wiederholungen zu zählen.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Zählen Sie, wie oft ein Schritt innerhalb eines Vorgangs ausgeführt wird. Eine nachfolgende Bedingung kann den Zählerstand verwenden, um beispielsweise nach drei Wiederholungen einen anderen Prozesspfad zu wählen.

                # Was richten Sie ein?

                Legen Sie unter **„Änderung pro Ausführung“** fest, um welchen Wert sich der Zählerstand verändert. Sie können die Zahl fest eingeben oder über eine Variable, einen No-Code-Ausdruck oder ein Low-Code-Skript bestimmen. Ohne Angabe wird der Zählerstand um `1` erhöht. Negative Werte verringern den Stand; bei `0` bleibt er gleich.

                Optional geben Sie unter **„Speicherort in den Vorgangsdaten“** einen Pfad an, zum Beispiel `schleife.anzahl`. Dann wird der bisherige Stand dort gelesen und der neue dort gespeichert. Ohne Angabe führt das Element seinen letzten erfolgreich abgeschlossenen Zählerstand im selben Vorgang fort.

                # Was passiert bei der Ausführung?

                Das Element liest den bisherigen Zählerstand und addiert die festgelegte Änderung. Fehlt ein bisheriger Stand, beginnt die Berechnung bei `0`. Anschließend wird der Vorgang über den Ausgang „Zähler aktualisiert“ fortgesetzt.

                Der Zähler löst selbst keine Wiederholung aus. Dafür verbinden Sie die erforderlichen Schritte und ergänzen eine Bedingung für den weiteren Ablauf.

                # Welches Ergebnis steht danach bereit?

                Der neue und der vorherige Zählerstand sowie die verwendete Änderung stehen in den Elementdaten bereit. Haben Sie einen Speicherort angegeben, wird der neue Stand zusätzlich an diesem Pfad in den Vorgangsdaten gespeichert.

                Die Zusammenfassung im Vorgangsverlauf zeigt, wie sich der Stand verändert hat.

                # Wie sieht ein Beispiel aus?

                Geben Sie als Speicherort `schleife.anzahl` und als Änderung `1` an. Ist dort noch kein Stand vorhanden, ergibt die erste Ausführung `1`, die zweite `2` und die dritte `3`. Eine anschließende Bedingung kann ab dem Stand `3` einen anderen Prozesspfad wählen.

                # Was ist zu beachten?

                Verwenden Sie ganze Zahlen für Zählerstand und Änderung. Die Änderung wird im Zahlenfeld auf ganze Zahlen gerundet. Bei einem vorhandenen Zählerstand werden Nachkommastellen abgeschnitten.

                Vorhandene Zahlen können auch als Text gespeichert sein. Ja/Nein-Werte werden als `1` beziehungsweise `0` gelesen, Objekte und Listen als `0`. Kann ein vorhandener Text nicht als Zahl gelesen werden, schlägt die Aufgabe fehl.

                Zusätzliche Datenzuweisungen können den neuen Stand überschreiben, wenn sie denselben Pfad in den Vorgangsdaten verwenden. Bei sehr großen Zahlen wird eine Überschreitung des unterstützten Zahlenbereichs derzeit nicht erkannt; das Ergebnis kann dadurch falsch sein.
                """;
    }

    @Nonnull
    @Override
    public Class<CounterActionNodeV1Configuration> getNodeConfigurationClass() {
        return CounterActionNodeV1Configuration.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        try {
            return ElementPOJOMapper
                    .createFromPOJO(CounterActionNodeV1Configuration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Konfiguration des Prozesselements konnte nicht geladen werden.");
        }
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull CounterActionNodeV1Configuration configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Zähler aktualisiert",
                        "Der Vorgang wird nach der Berechnung des neuen Zählerstands fortgesetzt."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_VALUE,
                        "Neuer Zählerstand",
                        "Der berechnete Zählerstand nach dieser Ausführung.",
                        "number"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PREVIOUS_VALUE,
                        "Vorheriger Zählerstand",
                        "Der als ganze Zahl verwendete Zählerstand vor der Änderung.",
                        "number"
                ),
                new ProcessNodeOutput(
                        OUTPUT_INCREMENT,
                        "Änderung pro Ausführung",
                        "Die bei dieser Ausführung addierte Zahl. Negative Zahlen verringern den Zählerstand.",
                        "number"
                )
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull CounterActionNodeV1.CounterActionNodeV1Configuration configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {

        // Check if a process data key for the variable is set.
        // If not, return all previously calculated process data key hints.
        var variableProcessDestinationKey = configuration.variable;
        if (StringUtils.isNullOrEmpty(variableProcessDestinationKey)) {
            return previousMetadata;
        }

        return ProcessNodeDefinitionMetadata
                .reuse(previousMetadata)
                .addForwardedProcessDataKey(
                        variableProcessDestinationKey,
                        "Zähler",
                        null,
                        processNodeEntity,
                        null
                );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<CounterActionNodeV1Configuration> context) throws ProcessNodeExecutionException {
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
            storageType = VALUE_OUTPUT_STORAGE_MODE_PROCESS_DATA;
        } else {
            lastCounterValue = getLastCounterValueFromPreviousInstantiation(context);
            storageType = VALUE_OUTPUT_STORAGE_MODE_NODE_DATA;
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
            ProcessDataValueUtils.writeProcessDataValue(execData, variableProcessDataKey, nextCounterValue);
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
    private static long getLastCounterValueByVariablePath(@Nonnull ProcessNodeExecutionInitContext<CounterActionNodeV1Configuration> context,
                                                          @Nonnull String variableProcessDataKey) throws ProcessNodeExecutionExceptionInvalidDataType {
        var currentCounterObj = ProcessDataValueUtils
                .resolveProcessDataValue(
                        context.getCurrentProcessExecutionData(),
                        variableProcessDataKey
                );

        // If no counter value could be extracted, return 0.
        if (currentCounterObj == null) {
            return 0L;
        }

        // If a counter value was extracted, try to convert it to a number.
        try {
            return NumberUtils
                    .asNumber(currentCounterObj)
                    .orElse(0)
                    .longValue();
        } catch (NumberFormatException e) {
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    "Der Zähler konnte nicht aktualisiert werden. Der Wert %s am Speicherort %s ist keine gültige Zahl. Prüfen Sie den gespeicherten Wert.",
                    StringUtils.quote(currentCounterObj.toString()),
                    StringUtils.quote(variableProcessDataKey)
            );
        }
    }

    /**
     * Get the last counter value based on the node data of the last instantiation of this process node. If no previous instantiation exists, 0 is returned.
     *
     * @param context The current execution context of this operation.
     * @return The last counter value or 0 if no previous instantiation exists.
     */
    private long getLastCounterValueFromPreviousInstantiation(@Nonnull ProcessNodeExecutionInitContext<CounterActionNodeV1Configuration> context) {
        // Find the last completed instantiation of this process node. The current task is already saved as running,
        // and failed or restarted attempts carry no counter value, so only completed tasks hold the previous value.
        Optional<ProcessInstanceTaskEntity> lastIterationTask = processInstanceTaskRepository
                .findFirstByProcessInstanceIdAndProcessNodeIdAndStatusOrderByStartedDescIdDesc(
                        context.getThisProcessInstance().getId(),
                        context.getThisNode().getId(),
                        ProcessTaskStatus.Completed
                );

        // Extract the last iteration counter value or 0 if none exists.
        Object lastIterationCounter = lastIterationTask
                .map(ProcessInstanceTaskEntity::getNodeData)
                .map(nodeData -> nodeData.getOrDefault(OUTPUT_VALUE, 0))
                .orElse(0);

        return NumberUtils
                .asNumber(lastIterationCounter)
                .orElse(0)
                .longValue();
    }

    /**
     * The configuration for the counter node.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class CounterActionNodeV1Configuration {
        // Constants for the configuration field ids.
        public static final String VARIABLE_FIELD_ID = "variable";
        public static final String INCREMENT_FIELD_ID = "increment";

        /**
         * This field contains the process data key variable where the counter value is stored id. It is nullable which will be handled by the node separately.
         */
        @InputElementPOJOBinding(id = VARIABLE_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Speicherort in den Vorgangsdaten"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optional. Geben Sie einen Pfad ein, zum Beispiel schleife.anzahl. Ohne Angabe wird der letzte Zählerstand dieses Prozesselements im selben Vorgang fortgeführt."),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
                @ElementPOJOBindingProperty(key = "disableWildCards", boolValue = true),
        })
        @Nullable
        public String variable;

        /**
         * This field contains the increment value by which the counter is incremented on each instantiation. It is nullable which will be handled by the node separately.
         */
        @InputElementPOJOBinding(
                id = INCREMENT_FIELD_ID,
                type = ElementType.Number,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode},
                allowedVariableSources = {
                        InputVariableSource.ProcessData,
                        InputVariableSource.ElementData,
                        InputVariableSource.ElementMetadata,
                        InputVariableSource.ProtectedProcessData
                },
                properties = {
                        @ElementPOJOBindingProperty(key = "label", strValue = "Änderung pro Ausführung"),
                        @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie eine ganze Zahl ein (Inkrement): 1 erhöht den Zählerstand um eins, -1 verringert ihn um eins. Bei 0 bleibt er gleich. Ohne Angabe wird 1 verwendet."),
                        @ElementPOJOBindingProperty(key = "decimalPlaces", intValue = 0),
                        @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
                }
        )
        @Nullable
        public Number increment;
    }
}
