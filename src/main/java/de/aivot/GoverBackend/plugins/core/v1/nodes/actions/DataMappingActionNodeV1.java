package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.form.input.TableInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TableInputElementColumn;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinitionContextConfig;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataMappingActionNodeV1 implements ProcessNodeDefinition {
    public static final String NODE_KEY = "data_mapping";

    private static final String PORT_NAME = "output";

    private static final String MAPPINGS_FIELD_ID = "mappings";
    private static final String FROM_FIELD_COLUMN_KEY = "fromField";
    private static final String TO_FIELD_COLUMN_KEY = "toField";
    private static final String TRANSFORM_FUNCTION_COLUMN_KEY = "transformFunction";

    private static final String TRANSFORM_ARGS_OBJECT_NAME = "__mappingArgs";

    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public DataMappingActionNodeV1(JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
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
        return "Daten abbilden";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Bildet Werte von Quellfeldern auf Zielfelder ab und unterstützt optionale JavaScript-Transformationen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var table = new TableInputElement();
        table.setId(MAPPINGS_FIELD_ID);
        table.setLabel("Abbildungsregeln");
        table.setHint("Definieren Sie pro Zeile einen Quellpfad, einen Zielpfad und optional eine Transformationsfunktion (Javascript), z.B. (wert) => 'Der Wer ist ' + wert");
        table.setRequired(true);
        table.setMinimumRequiredRows(1);
        table.setFields(List.of(
                new TableInputElementColumn()
                        .setKey(FROM_FIELD_COLUMN_KEY)
                        .setLabel("Quellpfad")
                        .setDatatype(TableColumnDataType.String)
                        .setOptional(false),
                new TableInputElementColumn()
                        .setKey(TO_FIELD_COLUMN_KEY)
                        .setLabel("Zielpfad")
                        .setDatatype(TableColumnDataType.String)
                        .setOptional(false),
                new TableInputElementColumn()
                        .setKey(TRANSFORM_FUNCTION_COLUMN_KEY)
                        .setLabel("Transformationsfunktion")
                        .setDatatype(TableColumnDataType.String)
                        .setOptional(true)
        ));

        layout.addChild(table);
        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Weiter",
                        "Der Prozess wird hier fortgesetzt, nachdem die Daten abgebildet wurden."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var sourceRoot = context.getProcessData().get("$");
        if (!(sourceRoot instanceof Map<?, ?> sourceRootMapRaw)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Die Vorgangsdatenwurzel ($) ist kein Objekt und kann nicht für die Datenabbildung verwendet werden."
            );
        }

        @SuppressWarnings("unchecked")
        var sourceRootMap = (Map<String, Object>) sourceRootMapRaw;
        var outputRoot = deepCopyMap(sourceRootMap);

        var rules = parseRules(context);
        var mappedValues = new ArrayList<Map<String, Object>>();

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService.fillJsEngineWithData(context.getProcessData(), engine);

            for (int i = 0; i < rules.size(); i++) {
                var rule = rules.get(i);
                var rowIndex = i + 1;

                var sourcePath = parsePath(rule.fromField(), rowIndex, "From Field");
                var targetPath = parsePath(rule.toField(), rowIndex, "To Field");

                var sourceValue = readPath(outputRoot, sourcePath);
                var transformedValue = applyTransform(
                        engine,
                        rowIndex,
                        rule.transformFunction(),
                        sourceValue,
                        sourceRootMap,
                        outputRoot
                );

                writePath(outputRoot, targetPath, transformedValue, rowIndex);

                var data = Map.of(
                        "originalPath", rule.fromField(),
                        "newPath", rule.toField(),
                        "original", sourceValue != null ? sourceValue.toString() : "null",
                        "mapped", transformedValue
                );

                mappedValues.add(data);
            }
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Initialisieren der JavaScript-Engine für die Datenabbildung: %s",
                    e.getMessage()
            );
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setProcessData(outputRoot)
                .setNodeData(Map.of(
                        "mappedRuleCount", rules.size(),
                        "mappedValues", mappedValues
                ));
    }

    @Nonnull
    private List<MappingRule> parseRules(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var configuration = context.getThisNode().getConfiguration();
        var rawMappings = configuration
                .getOpt(MAPPINGS_FIELD_ID)
                .map(mappingField -> mappingField.getValue())
                .orElse(List.of());

        if (!(rawMappings instanceof Collection<?> rows)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Konfiguration des Feldes %s ist ungültig. Es wird eine Liste von Abbildungsregeln erwartet.",
                    StringUtils.quote(MAPPINGS_FIELD_ID)
            );
        }

        var result = new ArrayList<MappingRule>();
        var rowIndex = 1;

        for (var rowObj : rows) {
            if (!(rowObj instanceof Map<?, ?> rowRaw)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Abbildungsregel in Zeile %d ist ungültig. Es wird ein Objekt erwartet.",
                        rowIndex
                );
            }

            var row = castStringObjectMap(rowRaw);
            var fromField = toNullableTrimmedString(row.get(FROM_FIELD_COLUMN_KEY));
            var toField = toNullableTrimmedString(row.get(TO_FIELD_COLUMN_KEY));
            var transformFunction = toNullableTrimmedString(row.get(TRANSFORM_FUNCTION_COLUMN_KEY));

            if (StringUtils.isNullOrEmpty(fromField)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Abbildungsregel in Zeile %d enthält kein gültiges %s.",
                        rowIndex,
                        StringUtils.quote("From Field")
                );
            }

            if (StringUtils.isNullOrEmpty(toField)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Abbildungsregel in Zeile %d enthält kein gültiges %s.",
                        rowIndex,
                        StringUtils.quote("To Field")
                );
            }

            result.add(new MappingRule(fromField, toField, transformFunction));
            rowIndex++;
        }

        if (result.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Es wurde keine Abbildungsregel angegeben."
            );
        }

        return result;
    }

    @Nonnull
    private Object applyTransform(@Nonnull de.aivot.GoverBackend.javascript.services.JavascriptEngine engine,
                                  int rowIndex,
                                  String transformFunction,
                                  Object sourceValue,
                                  @Nonnull Map<String, Object> sourceRoot,
                                  @Nonnull Map<String, Object> outputRoot) throws ProcessNodeExecutionException {
        if (StringUtils.isNullOrEmpty(transformFunction)) {
            return sourceValue;
        }

        var args = new HashMap<String, Object>();
        args.put("value", sourceValue);
        args.put("source", sourceRoot);
        args.put("processData", outputRoot);

        var wrappedFunctionCall = String.format(
                "(%s)(%s.value, %s.source, %s.processData)",
                transformFunction,
                TRANSFORM_ARGS_OBJECT_NAME,
                TRANSFORM_ARGS_OBJECT_NAME,
                TRANSFORM_ARGS_OBJECT_NAME
        );

        try {
            var result = engine
                    .registerGlobalObject(TRANSFORM_ARGS_OBJECT_NAME, args)
                    .evaluateCode(JavascriptCode.of(wrappedFunctionCall));
            return result.asObject();
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die JavaScript-Transformation in Zeile %d ist ungültig oder konnte nicht ausgeführt werden: %s",
                    rowIndex,
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private static List<PathPart> parsePath(@Nonnull String path,
                                            int rowIndex,
                                            @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var trimmedPath = path.trim();
        if (trimmedPath.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Pfadangabe %s in Zeile %d darf nicht leer sein.",
                    StringUtils.quote(fieldLabel),
                    rowIndex
            );
        }

        var result = new ArrayList<PathPart>();
        var token = new StringBuilder();

        for (int i = 0; i < trimmedPath.length(); i++) {
            char c = trimmedPath.charAt(i);

            if (c == '.') {
                flushObjectToken(token, result, trimmedPath, rowIndex, fieldLabel);
                continue;
            }

            if (c == '[') {
                flushObjectToken(token, result, trimmedPath, rowIndex, fieldLabel);

                int closingBracket = trimmedPath.indexOf(']', i);
                if (closingBracket < 0) {
                    throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Schließende ] fehlt.");
                }

                var indexStr = trimmedPath.substring(i + 1, closingBracket).trim();
                if (indexStr.isEmpty()) {
                    throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Array-Index fehlt.");
                }

                int index;
                try {
                    index = Integer.parseInt(indexStr);
                } catch (NumberFormatException e) {
                    throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Array-Index ist keine Zahl.");
                }

                if (index < 0) {
                    throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Array-Index darf nicht negativ sein.");
                }

                result.add(new ArrayPathPart(index));
                i = closingBracket;
                continue;
            }

            if (c == ']') {
                throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Unerwartete ] gefunden.");
            }

            token.append(c);
        }

        flushObjectToken(token, result, trimmedPath, rowIndex, fieldLabel);

        if (result.isEmpty()) {
            throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Pfad enthält keine Segmente.");
        }

        if (result.getFirst() instanceof ArrayPathPart) {
            throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Pfad muss mit einem Objektsegment beginnen.");
        }

        return result;
    }

    private static void flushObjectToken(@Nonnull StringBuilder token,
                                         @Nonnull List<PathPart> target,
                                         @Nonnull String path,
                                         int rowIndex,
                                         @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (token.isEmpty()) {
            return;
        }

        var key = token.toString().trim();
        token.setLength(0);

        if (key.isEmpty()) {
            throw invalidPathException(path, rowIndex, fieldLabel, "Leeres Objektsegment ist nicht erlaubt.");
        }

        target.add(new ObjectPathPart(key));
    }

    @Nonnull
    private static ProcessNodeExecutionExceptionInvalidConfiguration invalidPathException(@Nonnull String path,
                                                                                          int rowIndex,
                                                                                          @Nonnull String fieldLabel,
                                                                                          @Nonnull String detail) {
        return new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ungültiger Pfad in %s (Zeile %d): %s. Pfad: %s",
                StringUtils.quote(fieldLabel),
                rowIndex,
                detail,
                StringUtils.quote(path)
        );
    }

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
                                  Object value,
                                  int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        Object current = targetRoot;

        for (int i = 0; i < path.size() - 1; i++) {
            var currentPart = path.get(i);
            var nextPart = path.get(i + 1);

            if (currentPart instanceof ObjectPathPart objectPathPart) {
                if (!(current instanceof Map<?, ?> currentMapRaw)) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Konflikt beim Schreiben der Abbildungsregel in Zeile %d: Erwartet wurde ein Objektsegment.",
                            rowIndex
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
                    current = ensureCompatibleContainer(existing, nextPart, rowIndex);
                }
                continue;
            }

            var arrayPathPart = (ArrayPathPart) currentPart;
            if (!(current instanceof List<?> currentListRaw)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Konflikt beim Schreiben der Abbildungsregel in Zeile %d: Erwartet wurde ein Arraysegment.",
                        rowIndex
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
                current = ensureCompatibleContainer(existing, nextPart, rowIndex);
            }
        }

        var lastPart = path.get(path.size() - 1);
        if (lastPart instanceof ObjectPathPart objectPathPart) {
            if (!(current instanceof Map<?, ?> currentMapRaw)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Konflikt beim Schreiben der Abbildungsregel in Zeile %d: Das Ziel ist kein Objekt.",
                        rowIndex
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
                    "Konflikt beim Schreiben der Abbildungsregel in Zeile %d: Das Ziel ist kein Array.",
                    rowIndex
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
                ? new ArrayList<>()
                : new HashMap<String, Object>();
    }

    @Nonnull
    private static Object ensureCompatibleContainer(Object existing,
                                                    @Nonnull PathPart nextPart,
                                                    int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (nextPart instanceof ArrayPathPart && !(existing instanceof List<?>)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Konflikt beim Schreiben der Abbildungsregel in Zeile %d: Erwartet wurde ein Array, aber ein anderes Format ist bereits vorhanden.",
                    rowIndex
            );
        }
        if (nextPart instanceof ObjectPathPart && !(existing instanceof Map<?, ?>)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Konflikt beim Schreiben der Abbildungsregel in Zeile %d: Erwartet wurde ein Objekt, aber ein anderes Format ist bereits vorhanden.",
                    rowIndex
            );
        }
        return existing;
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
        for (var item : source) {
            copy.add(deepCopyValue(item));
        }
        return copy;
    }

    private static Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return deepCopyMap(castStringObjectMap(mapValue));
        }
        if (value instanceof List<?> listValue) {
            return deepCopyList(listValue);
        }
        return value;
    }

    @Nonnull
    private static Map<String, Object> castStringObjectMap(@Nonnull Map<?, ?> map) {
        var result = new HashMap<String, Object>();
        for (var entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    private static String toNullableTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }

    private record MappingRule(@Nonnull String fromField,
                               @Nonnull String toField,
                               String transformFunction) {
    }

    private sealed interface PathPart permits ObjectPathPart, ArrayPathPart {
    }

    private record ObjectPathPart(@Nonnull String key) implements PathPart {
    }

    private record ArrayPathPart(int index) implements PathPart {
    }
}
