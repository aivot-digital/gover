package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementState;
import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DataMappingActionNodeV1 implements ProcessNodeDefinition<DataMappingActionNodeV1.DataMappingActionNodeV1Config> {
    public static final String NODE_KEY = "data_mapping";

    private static final String PORT_NAME = "output";

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
        return "Datenfelder kopieren";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Bildet Werte von Quellfeldern auf Zielfelder ab und unterstützt optionale JavaScript-Transformationen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        try {
            return ElementPOJOMapper.createFromPOJO(DataMappingActionNodeV1Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e);
        }
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
    public Map<String, String> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull DataMappingActionNodeV1Config configuration) throws ResponseException {
        var errors = new LinkedHashMap<String, String>();

        var rawRules = configuration.rules;

        if (!(rawRules instanceof Collection<?> rows)) {
            var error = "Die Konfiguration des Feldes \"Abbildungsregeln\" ist ungültig. Es wird eine Liste von Abbildungsregeln erwartet.";
            errors.put(DataMappingActionNodeV1Config.RULES_FIELD_ID, error);
            return errors;
        }

        if (rows.isEmpty()) {
            var error = "Es wurde keine Abbildungsregel angegeben.";
            errors.put(DataMappingActionNodeV1Config.RULES_FIELD_ID, error);
            return errors;
        }

        String topLevelError = null;
        var rowIndex = 1;
        var hasRowErrors = false;

        for (var rowObj : rows) {
            var rowState = new ComputedElementStates();

            String source;
            String target;
            boolean deleteOnly;

            if (rowObj instanceof DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule rule) {
                source = toNullableTrimmedString(rule.source);
                target = toNullableTrimmedString(rule.target);
                deleteOnly = toBoolean(rule.deleteOnly);
            } else if (rowObj instanceof Map<?, ?> rowRaw) {
                var row = castStringObjectMap(rowRaw);
                source = toNullableTrimmedString(row.get(DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.SOURCE_FIELD_ID));
                target = toNullableTrimmedString(row.get(DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.TARGET_FIELD_ID));
                deleteOnly = toBoolean(row.get(DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.DELETE_ONLY_FIELD_ID));
            } else {
                if (topLevelError == null) {
                    topLevelError = String.format(
                            "Die Abbildungsregel in Zeile %d ist ungültig. Es wird ein Objekt erwartet.",
                            rowIndex
                    );
                }
                hasRowErrors = true;
                rowIndex++;
                continue;
            }

            if (source == null) {
                putFieldError(
                        rowState,
                        DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.SOURCE_FIELD_ID,
                        String.format("Die Abbildungsregel in Zeile %d enthält keinen gültigen Ausgangspfad.", rowIndex)
                );
                hasRowErrors = true;
            } else {
                try {
                    parsePath(source, rowIndex, "Ausgangspfad");
                } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
                    putFieldError(
                            rowState,
                            DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.SOURCE_FIELD_ID,
                            e.getMessage()
                    );
                    hasRowErrors = true;
                }
            }

            if (!deleteOnly) {
                if (target == null) {
                    putFieldError(
                            rowState,
                            DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.TARGET_FIELD_ID,
                            String.format("Die Abbildungsregel in Zeile %d enthält keinen gültigen Zielpfad.", rowIndex)
                    );
                    hasRowErrors = true;
                } else {
                    try {
                        parsePath(target, rowIndex, "Zielpfad");
                    } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
                        putFieldError(
                                rowState,
                                DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.TARGET_FIELD_ID,
                                e.getMessage()
                        );
                        hasRowErrors = true;
                    }
                }
            }

            rowIndex++;
        }

        if (hasRowErrors) {
            var error = topLevelError != null
                    ? topLevelError
                    : "Bitte überprüfen Sie die markierten Abbildungsregeln.";
            errors.put(DataMappingActionNodeV1Config.RULES_FIELD_ID, error);
        }

        return errors.isEmpty() ? null : errors;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<DataMappingActionNodeV1Config> context) throws ProcessNodeExecutionException {
        var sourceRoot = context.getCurrentProcessExecutionData().get("$");
        if (!(sourceRoot instanceof Map<?, ?> sourceRootMapRaw)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Die Vorgangsdatenwurzel ($) ist kein Objekt und kann nicht für die Datenabbildung verwendet werden."
            );
        }

        @SuppressWarnings("unchecked")
        var sourceRootMap = (Map<String, Object>) sourceRootMapRaw;
        var outputRoot = deepCopyMap(sourceRootMap);
        var config = context.getConfigurationOfExecutingNode();
        var rules = config.rules;
        var mappedValues = new ArrayList<Map<String, Object>>();

        try (var engine = initializeEngine(context.getCurrentProcessExecutionData())) {
            for (int i = 0; i < rules.size(); i++) {
                var rule = rules.get(i);
                var rowIndex = i + 1;
                var deleteOnly = Boolean.TRUE.equals(rule.deleteOnly);
                var cleanupSource = Boolean.TRUE.equals(rule.cleanupSource);
                var cleanupEmptyContainers = Boolean.TRUE.equals(config.cleanupEmptyContainers);

                var sourcePath = parsePath(rule.source, rowIndex, "Ausgangspfad");
                var targetPath = deleteOnly
                        ? null
                        : parsePath(rule.target, rowIndex, "Zielpfad");

                var sourceValue = readPath(outputRoot, sourcePath);
                Object transformedValue = null;

                if (!deleteOnly) {
                    transformedValue = applyTransform(
                            engine,
                            rowIndex,
                            null,
                            sourceValue,
                            sourceRootMap,
                            outputRoot
                    );

                    writePath(outputRoot, Objects.requireNonNull(targetPath), transformedValue, rowIndex);
                }

                if (deleteOnly || (cleanupSource && !sourcePath.equals(targetPath))) {
                    removePath(outputRoot, sourcePath, cleanupEmptyContainers);
                }

                mappedValues.add(createMappedValueEntry(rule, sourceValue, transformedValue));
            }
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler bei der Datenabbildung: %s",
                    defaultMessage(e)
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
    private ResolvedConfiguration loadConfiguration(@Nonnull Map<String, Object> rawConfiguration)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        DataMappingActionNodeV1Config config;
        try {
            var configuration = new EffectiveElementValues();
            configuration.putAll(rawConfiguration);
            config = ElementPOJOMapper.mapToPOJO(configuration, DataMappingActionNodeV1Config.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des Datenabbildungsprozesses ist ungültig: %s",
                    e.getMessage()
            );
        }

        if (config.rules == null || config.rules.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Es wurde keine Abbildungsregel angegeben."
            );
        }

        var resolvedRules = new ArrayList<ResolvedRule>();
        for (int i = 0; i < config.rules.size(); i++) {
            var rowIndex = i + 1;
            var rule = config.rules.get(i);

            if (rule == null) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Abbildungsregel in Zeile %d ist ungültig.",
                        rowIndex
                );
            }

            var source = requireRulePath(rule.source, rowIndex, "Ausgangspfad");
            var target = toNullableTrimmedString(rule.target);
            var deleteOnly = Boolean.TRUE.equals(rule.deleteOnly);

            if (!deleteOnly && target == null) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Abbildungsregel in Zeile %d enthält keinen gültigen Zielpfad.",
                        rowIndex
                );
            }

            resolvedRules.add(new ResolvedRule(
                    source,
                    target,
                    Boolean.TRUE.equals(rule.cleanupSource) || deleteOnly,
                    deleteOnly
            ));
        }

        return new ResolvedConfiguration(
                resolvedRules,
                Boolean.TRUE.equals(config.cleanupEmptyContainers)
        );
    }

    @Nonnull
    private de.aivot.GoverBackend.javascript.services.JavascriptEngine initializeEngine(@Nonnull Map<String, Object> processExecutionData)
            throws ProcessNodeExecutionExceptionUnknown {
        de.aivot.GoverBackend.javascript.services.JavascriptEngine engine = null;
        try {
            engine = javascriptEngineFactoryService.getEngine();
            ProcessDataService.fillJsEngineWithData(processExecutionData, engine);
            return engine;
        } catch (Exception e) {
            if (engine != null) {
                try {
                    engine.close();
                } catch (Exception ignored) {
                    // Ignore close errors and surface the original initialization problem.
                }
            }
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Initialisieren der JavaScript-Engine für die Datenabbildung: %s",
                    defaultMessage(e)
            );
        }
    }

    @Nonnull
    private static String requireRulePath(@Nullable String path,
                                          int rowIndex,
                                          @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var normalized = toNullableTrimmedString(path);
        if (normalized == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Abbildungsregel in Zeile %d enthält keinen gültigen %s.",
                    rowIndex,
                    fieldLabel
            );
        }
        return normalized;
    }

    @Nonnull
    private Map<String, Object> createMappedValueEntry(@Nonnull DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule rule,
                                                              Object sourceValue,
                                                              Object mappedValue) {
        var deleteOnly = Boolean.TRUE.equals(rule.deleteOnly);
        var cleanupSource = Boolean.TRUE.equals(rule.cleanupSource);
        var data = new LinkedHashMap<String, Object>();
        data.put("originalPath", rule.source);
        data.put("newPath", deleteOnly ? null : rule.target);
        data.put("cleanupSource", cleanupSource);
        data.put("deleteOnly", deleteOnly);
        data.put("original", sourceValue != null ? sourceValue.toString() : "null");
        data.put("mapped", mappedValue);
        return data;
    }

    private static void putFieldError(@Nonnull ComputedElementStates rowState,
                                      @Nonnull String fieldId,
                                      @Nonnull String error) {
        rowState
                .computeIfAbsent(fieldId, ignored -> new ComputedElementState())
                .setError(error);
    }

    private static boolean toBoolean(@Nullable Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s.trim());
        }
        return false;
    }

    @Nonnull
    private static String defaultMessage(@Nonnull Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
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
    private static List<PathPart> parsePath(@Nullable String path,
                                            int rowIndex,
                                            @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (path == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Pfadangabe %s in Zeile %d darf nicht leer sein.",
                    StringUtils.quote(fieldLabel),
                    rowIndex
            );
        }

        var trimmedPath = path.trim();
        if (trimmedPath.startsWith("$.")) {
            trimmedPath = trimmedPath.substring(2).trim();
        }

        if (trimmedPath.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Pfadangabe %s in Zeile %d darf nicht leer sein.",
                    StringUtils.quote(fieldLabel),
                    rowIndex
            );
        }

        var result = new ArrayList<PathPart>();
        var token = new StringBuilder();
        var expectingSegment = true;

        for (int i = 0; i < trimmedPath.length(); i++) {
            char c = trimmedPath.charAt(i);

            if (c == '.') {
                if (expectingSegment) {
                    throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Leeres Objektsegment ist nicht erlaubt.");
                }
                flushObjectToken(token, result, trimmedPath, rowIndex, fieldLabel);
                expectingSegment = true;
                continue;
            }

            if (c == '[') {
                if (expectingSegment) {
                    throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Pfad muss mit einem Objektsegment beginnen.");
                }
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
                expectingSegment = false;
                continue;
            }

            if (c == ']') {
                throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Unerwartete ] gefunden.");
            }

            token.append(c);
            expectingSegment = false;
        }

        if (expectingSegment) {
            throw invalidPathException(trimmedPath, rowIndex, fieldLabel, "Leeres Objektsegment ist nicht erlaubt.");
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

    private static boolean removePath(@Nonnull Map<String, Object> targetRoot,
                                      @Nonnull List<PathPart> path,
                                      boolean cleanupEmptyContainers) {
        return removePathRecursive(targetRoot, path, 0, cleanupEmptyContainers).removed();
    }

    @Nonnull
    private static PathRemovalResult removePathRecursive(Object current,
                                                         @Nonnull List<PathPart> path,
                                                         int pathIndex,
                                                         boolean cleanupEmptyContainers) {
        if (current == null) {
            return PathRemovalResult.NOT_FOUND;
        }

        var currentPart = path.get(pathIndex);
        var isLeaf = pathIndex == path.size() - 1;

        if (currentPart instanceof ObjectPathPart objectPathPart) {
            if (!(current instanceof Map<?, ?> currentMapRaw)) {
                return PathRemovalResult.NOT_FOUND;
            }

            @SuppressWarnings("unchecked")
            var currentMap = (Map<String, Object>) currentMapRaw;

            if (isLeaf) {
                if (!currentMap.containsKey(objectPathPart.key())) {
                    return PathRemovalResult.NOT_FOUND;
                }

                currentMap.remove(objectPathPart.key());
                return new PathRemovalResult(true, currentMap.isEmpty());
            }

            var child = currentMap.get(objectPathPart.key());
            var childResult = removePathRecursive(child, path, pathIndex + 1, cleanupEmptyContainers);
            if (!childResult.removed()) {
                return PathRemovalResult.NOT_FOUND;
            }

            if (cleanupEmptyContainers && childResult.containerEmpty()) {
                currentMap.remove(objectPathPart.key());
            }

            return new PathRemovalResult(true, currentMap.isEmpty());
        }

        var arrayPathPart = (ArrayPathPart) currentPart;
        if (!(current instanceof List<?> currentListRaw)) {
            return PathRemovalResult.NOT_FOUND;
        }

        @SuppressWarnings("unchecked")
        var currentList = (List<Object>) currentListRaw;
        if (arrayPathPart.index() < 0 || arrayPathPart.index() >= currentList.size()) {
            return PathRemovalResult.NOT_FOUND;
        }

        if (isLeaf) {
            currentList.remove(arrayPathPart.index());
            return new PathRemovalResult(true, currentList.isEmpty());
        }

        var child = currentList.get(arrayPathPart.index());
        var childResult = removePathRecursive(child, path, pathIndex + 1, cleanupEmptyContainers);
        if (!childResult.removed()) {
            return PathRemovalResult.NOT_FOUND;
        }

        if (cleanupEmptyContainers && childResult.containerEmpty()) {
            currentList.remove(arrayPathPart.index());
        }

        return new PathRemovalResult(true, currentList.isEmpty());
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
        var trimmed = value.toString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private sealed interface PathPart permits ObjectPathPart, ArrayPathPart {
    }

    private record ObjectPathPart(@Nonnull String key) implements PathPart {
    }

    private record ArrayPathPart(int index) implements PathPart {
    }

    private record PathRemovalResult(boolean removed, boolean containerEmpty) {
        private static final PathRemovalResult NOT_FOUND = new PathRemovalResult(false, false);
    }

    private record ResolvedConfiguration(@Nonnull List<ResolvedRule> rules,
                                         boolean cleanupEmptyContainers) {
    }

    private record ResolvedRule(@Nonnull String source,
                                @Nullable String target,
                                boolean cleanupSource,
                                boolean deleteOnly) {
    }

    @Nonnull
    @Override
    public Class<DataMappingActionNodeV1Config> getNodeConfigurationClass() {
        return DataMappingActionNodeV1Config.class;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class DataMappingActionNodeV1Config {
        private static final String CLEANUP_EMPTY_CONTAINERS_FIELD_ID = "cleanupEmptyContainers";
        private static final String RULES_FIELD_ID = "rules";

        @InputElementPOJOBinding(id = CLEANUP_EMPTY_CONTAINERS_FIELD_ID, type = ElementType.Checkbox, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Leere Objekte und Arrays bereinigen"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Entfernt nach dem Bereinigen eines Ausgangswerts auch leere Objekte und Arrays. Wenn deaktiviert, bleiben leere Container bestehen.")
        })
        public Boolean cleanupEmptyContainers;
        public List<DataMappingActionNodeV1Rule> rules;

        @ReplicatingContainerLayoutElementElementPOJOBinding(id = RULES_FIELD_ID, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Abbildungsregeln"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie alle Abbildungsregeln, die auf die Daten angewendet werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "#. Abbildungsregel"),
                @ElementPOJOBindingProperty(key = "addLabel", strValue = "Neue Abbildungsregel"),
                @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Abbildungsregel entfernen")
        })
        public static class DataMappingActionNodeV1Rule {
            public static final String SOURCE_FIELD_ID = "source";
            @InputElementPOJOBinding(id = SOURCE_FIELD_ID, type = ElementType.Text, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Ausgangspfad"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Der Ausgangspfad für den abzubildenden oder zu löschenden Wert. Wenn der Pfad nicht existiert, wird null verwendet. Pfade können mit Punktnotation und Array-Indizes angegeben werden, z.B. person.name oder items[0].price. Ein führendes $. ist optional."),
                    @ElementPOJOBindingProperty(key = "required", boolValue = true),
                    @ElementPOJOBindingProperty(key = "prefix", strValue = "$.")
            })
            public String source;

            public static final String TARGET_FIELD_ID = "target";
            @InputElementPOJOBinding(id = TARGET_FIELD_ID, type = ElementType.Text, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Zielpfad"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Der Zielpfad, auf den der Wert abgebildet werden soll. Wenn der Pfad nicht existiert, wird er automatisch erstellt. Kann leer bleiben, wenn der Wert nur gelöscht werden soll."),
                    @ElementPOJOBindingProperty(key = "prefix", strValue = "$.")
            })
            public String target;

            public static final String CLEANUP_SOURCE_FIELD_ID = "cleanupSource";
            @InputElementPOJOBinding(id = CLEANUP_SOURCE_FIELD_ID, type = ElementType.Checkbox, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Ausgangswert bereinigen"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Entfernt den Ausgangswert nach dem Schreiben aus den Vorgangsdaten.")
            })
            public Boolean cleanupSource;

            public static final String DELETE_ONLY_FIELD_ID = "deleteOnly";
            @InputElementPOJOBinding(id = DELETE_ONLY_FIELD_ID, type = ElementType.Checkbox, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Wert nur löschen"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Löscht den Ausgangswert ohne ihn in einen Zielpfad zu kopieren.")
            })
            public Boolean deleteOnly;
        }
    }
}
