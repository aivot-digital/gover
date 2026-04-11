package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class NoCodeActionNodeV1 implements ProcessNodeDefinition {
    public static final String NODE_KEY = "no-code";

    private static final String PORT_NAME = "output";

    private static final String VARIABLES_FIELD_ID = "variables";
    private static final String VARIABLE_NAME_FIELD_ID = "name";
    private static final String VARIABLE_TARGET_TYPE_FIELD_ID = "targetType";
    private static final String VARIABLE_EXPRESSION_FIELD_ID = "expression";

    private static final String OUTPUT_NAME_VARIABLES = "variables";
    private static final String OUTPUT_NAME_VARIABLE_COUNT = "variableCount";

    private static final String TARGET_TYPE_ANY = "any";
    private static final String TARGET_TYPE_STRING = "string";
    private static final String TARGET_TYPE_NUMBER = "number";
    private static final String TARGET_TYPE_BOOLEAN = "boolean";
    private static final String TARGET_TYPE_DATE = "date";
    private static final String TARGET_TYPE_DATETIME = "datetime";

    private final NoCodeEvaluationService noCodeEvaluationService;

    public NoCodeActionNodeV1(NoCodeEvaluationService noCodeEvaluationService) {
        this.noCodeEvaluationService = noCodeEvaluationService;
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
        return "No-Code ausführen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Führt benutzerdefinierte No-Code-Ausdrücke aus.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var variablesInput = new ReplicatingContainerLayoutElement();
        variablesInput.setId(VARIABLES_FIELD_ID);
        variablesInput.setLabel("No-Code-Aktion");
        variablesInput.setHint("Pro Eintrag wird ein Variablenname und der zu berechnende No-Code-Ausdruck definiert.");
        variablesInput.setRequired(true);
        // variablesInput.setMinimumRequiredSets(1);
        variablesInput.setHeadlineTemplate("Eintrag #");
        variablesInput.setAddLabel("Eintrag hinzufügen");
        variablesInput.setRemoveLabel("Eintrag entfernen");

        var variableNameInput = new TextInputElement();
        variableNameInput.setId(VARIABLE_NAME_FIELD_ID);
        variableNameInput.setLabel("Variablenname");
        variableNameInput.setHint("Dieser Name wird als Schlüssel in den Vorgangsdaten gespeichert.");
        variableNameInput.setRequired(true);
        variableNameInput.setWeight(8.0);

        var variableTargetTypeInput = new SelectInputElement();
        variableTargetTypeInput.setId(VARIABLE_TARGET_TYPE_FIELD_ID);
        variableTargetTypeInput.setLabel("Zieltyp");
        variableTargetTypeInput.setHint("Gibt an, in welchen Typ das Ergebnis umgewandelt wird.");
        variableTargetTypeInput.setRequired(true);
        variableTargetTypeInput.setWeight(4.0);
        variableTargetTypeInput.setValue(new ElementValueFunctions()
                .setType(ValueFunctionType.NoCode)
                .setNoCode(new NoCodeStaticValue(TARGET_TYPE_ANY))
        );
        variableTargetTypeInput.setOptions(List.of(
                SelectInputElementOption.of(TARGET_TYPE_ANY, "Beliebig"),
                SelectInputElementOption.of(TARGET_TYPE_STRING, "Text"),
                SelectInputElementOption.of(TARGET_TYPE_NUMBER, "Zahl"),
                SelectInputElementOption.of(TARGET_TYPE_BOOLEAN, "Ja/Nein"),
                SelectInputElementOption.of(TARGET_TYPE_DATE, "Datum"),
                SelectInputElementOption.of(TARGET_TYPE_DATETIME, "Datum und Uhrzeit")
        ));

        var variableExpressionInput = new NoCodeInputElement();
        variableExpressionInput.setId(VARIABLE_EXPRESSION_FIELD_ID);
        variableExpressionInput.setLabel("No-Code-Ausdruck");
        variableExpressionInput.setHint("Der Ausdruck wird beim Ausführen des Knotens ausgewertet.");
        variableExpressionInput.setRequired(true);
        variableExpressionInput.setReturnType(NoCodeInputElement.NoCodeInputReturnType.RUNTIME);
        variableExpressionInput.setWeight(12.0);

        variablesInput.setChildren(List.of(
                variableNameInput,
                variableTargetTypeInput,
                variableExpressionInput
        ));

        layout.addChild(variablesInput);
        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Weiter",
                        "Der Prozess wird hier fortgesetzt, nachdem alle Einträge berechnet wurden."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_VARIABLES,
                        "Variablen",
                        "Die berechneten Variablen als Objekt mit Variablennamen als Schlüssel."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_VARIABLE_COUNT,
                        "Anzahl Variablen",
                        "Die Anzahl der erfolgreich berechneten Variablen."
                )
        );
    }

    @Override
    public Map<String, String> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity, @Nonnull AuthoredElementValues configuration, @Nonnull DerivedRuntimeElementData derivedRuntimeElementData) throws ResponseException {
        // TODO: Check validity of this node configuration.
        //       - All variables need to be unique.
        //       - No-Code expressions should be checked for syntax errors (if possible).
        //       - All types are correct
        return null;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var sourceRoot = context.getProcessData().get("$");
        if (!(sourceRoot instanceof Map<?, ?> sourceRootRawMap)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Vorgangsdatenwurzel ($) ist kein Objekt."
            );
        }

        var outputRoot = deepCopyMap(castStringObjectMap(sourceRootRawMap));
        var variableDefinitions = parseVariableDefinitions(context);
        var variableValues = new LinkedHashMap<String, Object>();

        for (int i = 0; i < variableDefinitions.size(); i++) {
            var definition = variableDefinitions.get(i);
            var rowIndex = i + 1;

            var targetPath = parsePath(definition.name(), rowIndex, "Variablenname");
            var processDataContext = new HashMap<>(context.getProcessData());
            processDataContext.put("$", outputRoot);

            final Object evaluatedValue;
            try {
                evaluatedValue = noCodeEvaluationService
                        .evaluate(
                                definition.noCode(),
                                context.getConfiguration(),
                                processDataContext
                        )
                        .getValue();
            } catch (RuntimeException e) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        e,
                        "Der No-Code-Ausdruck in Zeile %d konnte nicht ausgewertet werden: %s",
                        rowIndex,
                        e.getMessage()
                );
            }

            var castedValue = castToTargetType(
                    evaluatedValue,
                    definition.targetType(),
                    rowIndex
            );

            variableValues.put(definition.name(), castedValue);
            writePath(outputRoot, targetPath, castedValue, rowIndex);
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setProcessData(outputRoot)
                .setNodeData(Map.of(
                        OUTPUT_NAME_VARIABLES, variableValues,
                        OUTPUT_NAME_VARIABLE_COUNT, variableValues.size()
                ));
    }

    @Nonnull
    private List<VariableDefinition> parseVariableDefinitions(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        EffectiveElementValues configuration = context
                .getConfiguration()
                .getEffectiveValues();

        ObjectMapper om = ObjectMapperFactory
                .getInstance();

        return ObjectMapperFactory
                .Utils
                .convertToList(
                        configuration.getOrDefault(VARIABLES_FIELD_ID, List.of()),
                        EffectiveElementValues.class
                )
                .stream()
                .map(row -> {
                    var variableName = StringUtils
                            .toNullableTrimmedString(row.get(VARIABLE_NAME_FIELD_ID));

                    var targetType = StringUtils
                            .toNullableTrimmedString(row.getOrDefault(VARIABLE_TARGET_TYPE_FIELD_ID, TARGET_TYPE_ANY));

                    NoCodeInputElementItem noCodeInputElementItem = om
                            .convertValue(row.get(VARIABLE_EXPRESSION_FIELD_ID), NoCodeInputElementItem.class);

                    NoCodeOperand expressionOperand = noCodeInputElementItem != null
                            ? noCodeInputElementItem.getNoCode()
                            : null;

                    if (expressionOperand == null) {
                        throw new RuntimeException("In der Zeile ist kein gültiger No-Code-Ausdruck angegeben.");
                    }

                    return new VariableDefinition(
                            variableName,
                            targetType,
                            expressionOperand
                    );
                })
                .toList();
    }

    private static void validateTargetType(@Nonnull String targetType,
                                           int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (
                TARGET_TYPE_ANY.equals(targetType) ||
                        TARGET_TYPE_STRING.equals(targetType) ||
                        TARGET_TYPE_NUMBER.equals(targetType) ||
                        TARGET_TYPE_BOOLEAN.equals(targetType) ||
                        TARGET_TYPE_DATE.equals(targetType) ||
                        TARGET_TYPE_DATETIME.equals(targetType)
        ) {
            return;
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "In Zeile %d ist der Zieltyp %s ungültig.",
                rowIndex,
                StringUtils.quote(targetType)
        );
    }

    private static Object castToTargetType(Object value,
                                           @Nonnull String targetType,
                                           int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (value == null || TARGET_TYPE_ANY.equals(targetType)) {
            return value;
        }

        try {
            return switch (targetType) {
                case TARGET_TYPE_STRING -> String.valueOf(value);
                case TARGET_TYPE_NUMBER -> castToNumber(value);
                case TARGET_TYPE_BOOLEAN -> castToBoolean(value);
                case TARGET_TYPE_DATE -> castToDate(value);
                case TARGET_TYPE_DATETIME -> castToDateTime(value);
                default -> value;
            };
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Der Wert in Zeile %d konnte nicht in den Zieltyp %s umgewandelt werden.",
                    rowIndex,
                    StringUtils.quote(targetType)
            );
        }
    }

    @Nonnull
    private static Number castToNumber(Object value) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (value instanceof Number n) {
            return n;
        }

        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }

        if (value instanceof String s) {
            var trimmed = s.trim();
            if (trimmed.isEmpty()) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Leere Zeichenkette kann nicht als Zahl umgewandelt werden."
                );
            }
            return new BigDecimal(trimmed);
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Der Typ %s kann nicht als Zahl umgewandelt werden.",
                StringUtils.quote(value.getClass().getSimpleName())
        );
    }

    private static Boolean castToBoolean(Object value) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (value instanceof Boolean b) {
            return b;
        }

        if (value instanceof Number n) {
            return n.doubleValue() != 0.0d;
        }

        if (value instanceof String s) {
            var normalized = s.trim().toLowerCase();
            return switch (normalized) {
                case "true", "1", "ja", "yes", "on" -> true;
                case "false", "0", "nein", "no", "off", "" -> false;
                default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Zeichenkette %s kann nicht als Ja/Nein-Wert umgewandelt werden.",
                        StringUtils.quote(s)
                );
            };
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Der Typ %s kann nicht als Ja/Nein-Wert umgewandelt werden.",
                StringUtils.quote(value.getClass().getSimpleName())
        );
    }

    @Nonnull
    private static String castToDate(Object value) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate().toString();
        }
        if (value instanceof String s) {
            var trimmed = s.trim();
            if (trimmed.isEmpty()) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Leere Zeichenkette kann nicht als Datum umgewandelt werden."
                );
            }

            try {
                return LocalDate.parse(trimmed).toString();
            } catch (Exception ignored) {
                try {
                    return LocalDateTime.parse(trimmed).toLocalDate().toString();
                } catch (Exception ignored2) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Die Zeichenkette %s kann nicht als Datum umgewandelt werden.",
                            StringUtils.quote(s)
                    );
                }
            }
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Der Typ %s kann nicht als Datum umgewandelt werden.",
                StringUtils.quote(value.getClass().getSimpleName())
        );
    }

    @Nonnull
    private static String castToDateTime(Object value) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toString();
        }
        if (value instanceof LocalDate date) {
            return date.atStartOfDay().toString();
        }
        if (value instanceof String s) {
            var trimmed = s.trim();
            if (trimmed.isEmpty()) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Leere Zeichenkette kann nicht als Datum und Uhrzeit umgewandelt werden."
                );
            }

            try {
                return LocalDateTime.parse(trimmed).toString();
            } catch (Exception ignored) {
                try {
                    return LocalDate.parse(trimmed).atStartOfDay().toString();
                } catch (Exception ignored2) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Die Zeichenkette %s kann nicht als Datum und Uhrzeit umgewandelt werden.",
                            StringUtils.quote(s)
                    );
                }
            }
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Der Typ %s kann nicht als Datum und Uhrzeit umgewandelt werden.",
                StringUtils.quote(value.getClass().getSimpleName())
        );
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
                            "Konflikt beim Schreiben des No-Code-Ergebnisses in Zeile %d: Erwartet wurde ein Objektsegment.",
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
                        "Konflikt beim Schreiben des No-Code-Ergebnisses in Zeile %d: Erwartet wurde ein Arraysegment.",
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
                        "Konflikt beim Schreiben des No-Code-Ergebnisses in Zeile %d: Das Ziel ist kein Objekt.",
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
                    "Konflikt beim Schreiben des No-Code-Ergebnisses in Zeile %d: Das Ziel ist kein Array.",
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
                    "Konflikt beim Schreiben des No-Code-Ergebnisses in Zeile %d: Erwartet wurde ein Array, aber ein anderes Format ist bereits vorhanden.",
                    rowIndex
            );
        }
        if (nextPart instanceof ObjectPathPart && !(existing instanceof Map<?, ?>)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Konflikt beim Schreiben des No-Code-Ergebnisses in Zeile %d: Erwartet wurde ein Objekt, aber ein anderes Format ist bereits vorhanden.",
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

    private record VariableDefinition(
            @Nonnull String name,
            @Nonnull String targetType,
            @Nonnull NoCodeOperand noCode
    ) {
    }

    private sealed interface PathPart permits ObjectPathPart, ArrayPathPart {
    }

    private record ObjectPathPart(@Nonnull String key) implements PathPart {
    }

    private record ArrayPathPart(int index) implements PathPart {
    }


    private class NoCodeActionNodeConfiguration {

    }
}
