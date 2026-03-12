package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinitionContextConfig;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.ProcessNodeOutput;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class CounterActionNodeV1 implements ProcessNodeDefinition {
    public static final String NODE_KEY = "counter";

    private static final String PORT_NAME = "output";

    private static final String VARIABLE_FIELD_ID = "variable";
    private static final String INCREMENT_FIELD_ID = "increment";

    private static final String OUTPUT_VALUE = "value";
    private static final String OUTPUT_PREVIOUS_VALUE = "previousValue";
    private static final String OUTPUT_INCREMENT = "increment";
    private static final String OUTPUT_STORAGE_TARGET = "storageTarget";

    private static final String STORAGE_MODE_PROCESS_DATA = "processData";
    private static final String STORAGE_MODE_NODE_DATA = "nodeData";

    private static final long DEFAULT_INCREMENT = 1L;
    private static final String NODE_DATA_VALUE_KEY = OUTPUT_VALUE;

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
        return Core.PLUGIN_KEY;
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
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var variableField = new TextInputElement();
        variableField.setId(VARIABLE_FIELD_ID);
        variableField.setLabel("Vorgangsdatenvariable");
        variableField.setHint("Optionaler Pfad innerhalb der Vorgangsdatenwurzel, z. B. schleife.zähler. Wenn leer, wird der letzte Zählerstand dieses Prozesselements aus den Elementdaten verwendet.");
        variableField.setRequired(false);
        variableField.setWeight(9.0);
        layout.addChild(variableField);

        var incrementField = new NumberInputElement();
        incrementField.setId(INCREMENT_FIELD_ID);
        incrementField.setLabel("Inkrement");
        incrementField.setHint("Natürliche Zahl, um die der Zähler erhöht wird.");
        incrementField.setRequired(false);
        incrementField.setDecimalPlaces(0);
        incrementField.setWeight(3.0);
        incrementField.setValue(new ElementValueFunctions()
                .setType(ValueFunctionType.NoCode)
                .setNoCode(new NoCodeStaticValue(DEFAULT_INCREMENT)));
        layout.addChild(incrementField);

        return layout;
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
                ),
                new ProcessNodeOutput(
                        OUTPUT_STORAGE_TARGET,
                        "Speicherziel",
                        "Der verwendete Speicherpfad des Zählerstands."
                )
        );
    }

    @Override
    public void validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                      @Nonnull ElementData configuration) throws ResponseException {
        boolean hasErrors = false;

        var variableField = configuration.getOrDefault(
                VARIABLE_FIELD_ID,
                new ElementDataObject(ElementType.Text)
        );
        var incrementField = configuration.getOrDefault(
                INCREMENT_FIELD_ID,
                new ElementDataObject(ElementType.Number)
        );

        var variablePath = toNullableTrimmedString(variableField.getValue());
        if (variablePath != null) {
            try {
                parsePath(variablePath, "Vorgangsdatenvariable");
            } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
                variableField.addComputedError(e.getMessage());
                configuration.put(VARIABLE_FIELD_ID, variableField);
                hasErrors = true;
            }
        }

        try {
            resolveIncrement(incrementField.getValue());
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            incrementField.addComputedError(e.getMessage());
            configuration.put(INCREMENT_FIELD_ID, incrementField);
            hasErrors = true;
        }

        if (hasErrors) {
            throw ResponseException.badRequest(configuration);
        }
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var configuration = context.getThisNode().getConfiguration();
        var variablePath = toNullableTrimmedString(configuration.getOpt(VARIABLE_FIELD_ID)
                .map(ElementDataObject::getValue)
                .orElse(null));
        var increment = resolveIncrement(configuration.getOpt(INCREMENT_FIELD_ID)
                .map(ElementDataObject::getValue)
                .orElse(null));

        var nodeData = new LinkedHashMap<String, Object>();

        if (variablePath != null) {
            var path = parsePath(variablePath, "Vorgangsdatenvariable");
            var outputRoot = resolveProcessDataRoot(context.getProcessData().get("$"));
            var previousValue = readCounterValue(readPath(outputRoot, path), "Vorgangsdatenvariable");
            var nextValue = previousValue + increment;

            writePath(outputRoot, path, nextValue);

            nodeData.put(OUTPUT_VALUE, nextValue);
            nodeData.put(OUTPUT_PREVIOUS_VALUE, previousValue);
            nodeData.put(OUTPUT_INCREMENT, increment);
            nodeData.put(OUTPUT_STORAGE_TARGET, variablePath);
            nodeData.put("storageMode", STORAGE_MODE_PROCESS_DATA);

            return new ProcessNodeExecutionResultTaskCompleted()
                    .setViaPort(PORT_NAME)
                    .setProcessData(outputRoot)
                    .setNodeData(nodeData);
        }

        var previousValue = readFallbackCounterValue(context);
        var nextValue = previousValue + increment;
        var fallbackStorageTarget = "_" + context.getThisNode().getDataKey() + "." + NODE_DATA_VALUE_KEY;

        nodeData.put(OUTPUT_VALUE, nextValue);
        nodeData.put(OUTPUT_PREVIOUS_VALUE, previousValue);
        nodeData.put(OUTPUT_INCREMENT, increment);
        nodeData.put(OUTPUT_STORAGE_TARGET, fallbackStorageTarget);
        nodeData.put("storageMode", STORAGE_MODE_NODE_DATA);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(nodeData);
    }

    @Nonnull
    private static Map<String, Object> resolveProcessDataRoot(@Nullable Object rawRoot) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (rawRoot == null) {
            return new HashMap<>();
        }

        if (!(rawRoot instanceof Map<?, ?> rawMap)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Vorgangsdatenwurzel ($) ist kein Objekt und kann nicht für den Zähler verwendet werden."
            );
        }

        return deepCopyMap(castStringObjectMap(rawMap));
    }

    private long readFallbackCounterValue(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var dataKey = context.getThisNode().getDataKey();
        var directNodeData = context.getProcessData().get("_" + dataKey);

        var contextValue = extractStoredCounterValue(directNodeData);
        if (contextValue != null) {
            return contextValue;
        }

        var groupedNodeData = context.getProcessData().get("_");
        if (groupedNodeData instanceof Map<?, ?> allNodeDataRaw) {
            var previousNodeData = allNodeDataRaw.get(dataKey);
            contextValue = extractStoredCounterValue(previousNodeData);
            if (contextValue != null) {
                return contextValue;
            }
        }

        var previousIterationTask = context.getThisTask().getId() != null
                ? processInstanceTaskRepository.findFirstByProcessInstanceIdAndProcessNodeIdAndIdNotOrderByStartedDesc(
                        context.getThisProcessInstance().getId(),
                        context.getThisNode().getId(),
                        context.getThisTask().getId()
                )
                : processInstanceTaskRepository.findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(
                        context.getThisProcessInstance().getId(),
                        context.getThisNode().getId()
                );

        var previousIterationValue = previousIterationTask == null
                ? null
                : extractStoredCounterValue(previousIterationTask.getNodeData());
        if (previousIterationValue != null) {
            return previousIterationValue;
        }

        return 0L;
    }

    @Nullable
    private static Long extractStoredCounterValue(@Nullable Object rawNodeData) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (!(rawNodeData instanceof Map<?, ?> rawMap)) {
            return null;
        }

        var nodeData = castStringObjectMap(rawMap);
        if (!nodeData.containsKey(NODE_DATA_VALUE_KEY)) {
            return null;
        }

        return readCounterValue(nodeData.get(NODE_DATA_VALUE_KEY), "Elementdaten");
    }

    private static long readCounterValue(@Nullable Object rawValue,
                                         @Nonnull String sourceLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (rawValue == null) {
            return 0L;
        }

        var normalized = toBigDecimal(rawValue, sourceLabel);
        try {
            var normalizedInteger = normalized.stripTrailingZeros();
            if (normalizedInteger.scale() > 0) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der vorhandene Zählwert in %s muss eine ganze, nicht negative Zahl sein.",
                        StringUtils.quote(sourceLabel)
                );
            }

            var value = normalizedInteger.longValueExact();
            if (value < 0) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der vorhandene Zählwert in %s muss eine ganze, nicht negative Zahl sein.",
                        StringUtils.quote(sourceLabel)
                );
            }

            return value;
        } catch (ArithmeticException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Der vorhandene Zählwert in %s ist zu groß.",
                    StringUtils.quote(sourceLabel)
            );
        }
    }

    private static long resolveIncrement(@Nullable Object rawValue) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (rawValue == null) {
            return DEFAULT_INCREMENT;
        }

        if (rawValue instanceof String s && s.trim().isEmpty()) {
            return DEFAULT_INCREMENT;
        }

        var normalized = toBigDecimal(rawValue, "Inkrement");
        try {
            var normalizedInteger = normalized.stripTrailingZeros();
            if (normalizedInteger.scale() > 0) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Das Inkrement muss eine natürliche Zahl sein."
                );
            }

            var increment = normalizedInteger.longValueExact();
            if (increment < 1) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Das Inkrement muss mindestens 1 betragen."
                );
            }

            return increment;
        } catch (ArithmeticException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Das Inkrement ist zu groß."
            );
        }
    }

    @Nonnull
    private static BigDecimal toBigDecimal(@Nullable Object rawValue,
                                           @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (rawValue == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Das Feld %s enthält keinen gültigen Zahlenwert.",
                    StringUtils.quote(fieldLabel)
            );
        }

        try {
            return switch (rawValue) {
                case BigDecimal b -> b;
                case BigInteger b -> new BigDecimal(b);
                case Byte b -> BigDecimal.valueOf(b.longValue());
                case Short s -> BigDecimal.valueOf(s.longValue());
                case Integer i -> BigDecimal.valueOf(i.longValue());
                case Long l -> BigDecimal.valueOf(l);
                case Float f -> BigDecimal.valueOf(f.doubleValue());
                case Double d -> BigDecimal.valueOf(d);
                case Number n -> BigDecimal.valueOf(n.doubleValue());
                case String s -> parseDecimalString(s, fieldLabel);
                default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Das Feld %s enthält keinen gültigen Zahlenwert.",
                        StringUtils.quote(fieldLabel)
                );
            };
        } catch (NumberFormatException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Das Feld %s enthält keinen gültigen Zahlenwert.",
                    StringUtils.quote(fieldLabel)
            );
        }
    }

    @Nonnull
    private static BigDecimal parseDecimalString(@Nonnull String value,
                                                 @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Das Feld %s enthält keinen gültigen Zahlenwert.",
                    StringUtils.quote(fieldLabel)
            );
        }

        try {
            return new BigDecimal(trimmedValue);
        } catch (NumberFormatException ignored) {
            var germanDecimal = NumberInputElement.parseGermanNumber(trimmedValue, 8);
            if (germanDecimal != null) {
                return germanDecimal;
            }
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Das Feld %s enthält keinen gültigen Zahlenwert.",
                    StringUtils.quote(fieldLabel)
            );
        }
    }

    @Nullable
    private static String toNullableTrimmedString(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.toString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nonnull
    private static List<PathPart> parsePath(@Nonnull String path,
                                            @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var trimmedPath = path.trim();
        if (trimmedPath.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Pfadangabe für %s darf nicht leer sein.",
                    StringUtils.quote(fieldLabel)
            );
        }

        var result = new ArrayList<PathPart>();
        var token = new StringBuilder();
        var expectingSegment = true;

        for (int i = 0; i < trimmedPath.length(); i++) {
            char c = trimmedPath.charAt(i);

            if (c == '.') {
                if (expectingSegment) {
                    throw invalidPathException(trimmedPath, fieldLabel, "Leeres Objektsegment ist nicht erlaubt.");
                }
                flushObjectToken(token, result, trimmedPath, fieldLabel);
                expectingSegment = true;
                continue;
            }

            if (c == '[') {
                if (expectingSegment) {
                    throw invalidPathException(trimmedPath, fieldLabel, "Pfad muss mit einem Objektsegment beginnen.");
                }
                flushObjectToken(token, result, trimmedPath, fieldLabel);

                int closingBracket = trimmedPath.indexOf(']', i);
                if (closingBracket < 0) {
                    throw invalidPathException(trimmedPath, fieldLabel, "Schließende ] fehlt.");
                }

                var indexStr = trimmedPath.substring(i + 1, closingBracket).trim();
                if (indexStr.isEmpty()) {
                    throw invalidPathException(trimmedPath, fieldLabel, "Array-Index fehlt.");
                }

                int index;
                try {
                    index = Integer.parseInt(indexStr);
                } catch (NumberFormatException e) {
                    throw invalidPathException(trimmedPath, fieldLabel, "Array-Index ist keine Zahl.");
                }

                if (index < 0) {
                    throw invalidPathException(trimmedPath, fieldLabel, "Array-Index darf nicht negativ sein.");
                }

                result.add(new ArrayPathPart(index));
                i = closingBracket;
                expectingSegment = false;
                continue;
            }

            if (c == ']') {
                throw invalidPathException(trimmedPath, fieldLabel, "Unerwartete ] gefunden.");
            }

            token.append(c);
            expectingSegment = false;
        }

        if (expectingSegment) {
            throw invalidPathException(trimmedPath, fieldLabel, "Leeres Objektsegment ist nicht erlaubt.");
        }

        flushObjectToken(token, result, trimmedPath, fieldLabel);

        if (result.isEmpty()) {
            throw invalidPathException(trimmedPath, fieldLabel, "Pfad enthält keine Segmente.");
        }

        if (result.getFirst() instanceof ArrayPathPart) {
            throw invalidPathException(trimmedPath, fieldLabel, "Pfad muss mit einem Objektsegment beginnen.");
        }

        return result;
    }

    private static void flushObjectToken(@Nonnull StringBuilder token,
                                         @Nonnull List<PathPart> target,
                                         @Nonnull String path,
                                         @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (token.isEmpty()) {
            return;
        }

        var key = token.toString().trim();
        token.setLength(0);

        if (key.isEmpty()) {
            throw invalidPathException(path, fieldLabel, "Leeres Objektsegment ist nicht erlaubt.");
        }

        target.add(new ObjectPathPart(key));
    }

    @Nonnull
    private static ProcessNodeExecutionExceptionInvalidConfiguration invalidPathException(@Nonnull String path,
                                                                                          @Nonnull String fieldLabel,
                                                                                          @Nonnull String detail) {
        return new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ungültiger Pfad in %s: %s Pfad: %s",
                StringUtils.quote(fieldLabel),
                detail,
                StringUtils.quote(path)
        );
    }

    @Nullable
    private static Object readPath(@Nonnull Map<String, Object> sourceRoot,
                                   @Nonnull List<PathPart> path) {
        Object current = sourceRoot;

        for (var pathPart : path) {
            if (current == null) {
                return null;
            }

            if (pathPart instanceof ObjectPathPart objectPathPart) {
                if (!(current instanceof Map<?, ?> currentMap)) {
                    return null;
                }
                current = currentMap.get(objectPathPart.key());
                continue;
            }

            var arrayPathPart = (ArrayPathPart) pathPart;
            if (!(current instanceof List<?> currentList)) {
                return null;
            }
            if (arrayPathPart.index() < 0 || arrayPathPart.index() >= currentList.size()) {
                return null;
            }
            current = currentList.get(arrayPathPart.index());
        }

        return current;
    }

    private static void writePath(@Nonnull Map<String, Object> targetRoot,
                                  @Nonnull List<PathPart> path,
                                  long value) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        Object current = targetRoot;

        for (int i = 0; i < path.size() - 1; i++) {
            var currentPart = path.get(i);
            var nextPart = path.get(i + 1);

            if (currentPart instanceof ObjectPathPart objectPathPart) {
                if (!(current instanceof Map<?, ?> currentMapRaw)) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Konflikt beim Schreiben des Zählers: Erwartet wurde ein Objektsegment."
                    );
                }

                @SuppressWarnings("unchecked")
                var currentMap = (Map<String, Object>) currentMapRaw;
                var existing = currentMap.get(objectPathPart.key());

                if (existing == null) {
                    var created = createContainerFor(nextPart);
                    currentMap.put(objectPathPart.key(), created);
                    current = created;
                } else {
                    current = ensureCompatibleContainer(existing, nextPart);
                }
                continue;
            }

            var arrayPathPart = (ArrayPathPart) currentPart;
            if (!(current instanceof List<?> currentListRaw)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Konflikt beim Schreiben des Zählers: Erwartet wurde ein Arraysegment."
                );
            }

            @SuppressWarnings("unchecked")
            var currentList = (List<Object>) currentListRaw;
            ensureListSize(currentList, arrayPathPart.index());

            var existing = currentList.get(arrayPathPart.index());
            if (existing == null) {
                var created = createContainerFor(nextPart);
                currentList.set(arrayPathPart.index(), created);
                current = created;
            } else {
                current = ensureCompatibleContainer(existing, nextPart);
            }
        }

        var lastPart = path.get(path.size() - 1);
        if (lastPart instanceof ObjectPathPart objectPathPart) {
            if (!(current instanceof Map<?, ?> currentMapRaw)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Konflikt beim Schreiben des Zählers: Das Ziel ist kein Objekt."
                );
            }

            @SuppressWarnings("unchecked")
            var currentMap = (Map<String, Object>) currentMapRaw;
            currentMap.put(objectPathPart.key(), value);
            return;
        }

        var arrayPathPart = (ArrayPathPart) lastPart;
        if (!(current instanceof List<?> currentListRaw)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Konflikt beim Schreiben des Zählers: Das Ziel ist kein Array."
            );
        }

        @SuppressWarnings("unchecked")
        var currentList = (List<Object>) currentListRaw;
        ensureListSize(currentList, arrayPathPart.index());
        currentList.set(arrayPathPart.index(), value);
    }

    @Nonnull
    private static Object createContainerFor(@Nonnull PathPart nextPart) {
        return nextPart instanceof ArrayPathPart
                ? new LinkedList<>()
                : new HashMap<String, Object>();
    }

    @Nonnull
    private static Object ensureCompatibleContainer(@Nonnull Object existing,
                                                    @Nonnull PathPart nextPart) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (nextPart instanceof ArrayPathPart) {
            if (existing instanceof List<?>) {
                return existing;
            }
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Konflikt beim Schreiben des Zählers: Ein bestehender Wert verhindert die Erzeugung eines Arrays."
            );
        }

        if (existing instanceof Map<?, ?>) {
            return existing;
        }
        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Konflikt beim Schreiben des Zählers: Ein bestehender Wert verhindert die Erzeugung eines Objekts."
        );
    }

    private static void ensureListSize(@Nonnull List<Object> list, int index) {
        while (list.size() <= index) {
            list.add(null);
        }
    }

    @Nonnull
    private static Map<String, Object> deepCopyMap(@Nonnull Map<String, Object> source) {
        var copy = new HashMap<String, Object>();
        for (var entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    @Nonnull
    private static List<Object> deepCopyList(@Nonnull List<?> source) {
        var copy = new ArrayList<Object>(source.size());
        for (var value : source) {
            copy.add(deepCopyValue(value));
        }
        return copy;
    }

    @Nullable
    private static Object deepCopyValue(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Map<?, ?> map -> deepCopyMap(castStringObjectMap(map));
            case List<?> list -> deepCopyList(list);
            default -> value;
        };
    }

    @Nonnull
    private static Map<String, Object> castStringObjectMap(@Nonnull Map<?, ?> rawMap) {
        var result = new HashMap<String, Object>();
        for (var entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    private sealed interface PathPart permits ObjectPathPart, ArrayPathPart {
    }

    private record ObjectPathPart(@Nonnull String key) implements PathPart {
    }

    private record ArrayPathPart(int index) implements PathPart {
    }
}
