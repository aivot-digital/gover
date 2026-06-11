package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.utils.ApplicationTimeZone;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.utils.MapUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import de.aivot.GoverBackend.utils.IsoTimestampUtils;

@Component
public class NoCodeActionNodeV1 implements ProcessNodeDefinition<NoCodeActionNodeV1.NoCodeActionNodeConfiguration> {
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
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(NoCodeActionNodeConfiguration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        layout
                .findChild(VARIABLE_TARGET_TYPE_FIELD_ID, SelectInputElement.class)
                .ifPresent(variableTargetTypeInput -> {
                    variableTargetTypeInput.setValue(new ElementValueFunctions()
                            .setType(ValueFunctionType.NoCode)
                            .setNoCode(new NoCodeStaticValue(TARGET_TYPE_ANY)));
                    variableTargetTypeInput.setOptions(List.of(
                            SelectInputElementOption.of(TARGET_TYPE_ANY, "Beliebig"),
                            SelectInputElementOption.of(TARGET_TYPE_STRING, "Text"),
                            SelectInputElementOption.of(TARGET_TYPE_NUMBER, "Zahl"),
                            SelectInputElementOption.of(TARGET_TYPE_BOOLEAN, "Ja/Nein"),
                            SelectInputElementOption.of(TARGET_TYPE_DATE, "Datum"),
                            SelectInputElementOption.of(TARGET_TYPE_DATETIME, "Datum und Uhrzeit")
                    ));
                });

        layout
                .findChild(VARIABLE_EXPRESSION_FIELD_ID, NoCodeInputElement.class)
                .ifPresent(variableExpressionInput -> variableExpressionInput.setReturnType(NoCodeInputElement.NoCodeInputReturnType.RUNTIME));

        return layout;
    }

    @Nonnull
    @Override
    public Class<NoCodeActionNodeConfiguration> getNodeConfigurationClass() {
        return NoCodeActionNodeConfiguration.class;
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
                        "Die berechneten Variablen als Liste aufgelöster Zielpfade."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_VARIABLE_COUNT,
                        "Anzahl Variablen",
                        "Die Anzahl der tatsächlich geschriebenen Zielwerte."
                )
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull NoCodeActionNodeConfiguration configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        if (configuration.variables == null) {
            return previousMetadata;
        }

        var metadata = ProcessNodeDefinitionMetadata
                .reuse(previousMetadata);

        for (int i = 0; i < configuration.variables.size(); i++) {
            var row = configuration.variables.get(i);
            var variableName = row == null
                    ? null
                    : StringUtils.toNullableTrimmedString(row.name);

            if (variableName == null) {
                continue;
            }

            metadata
                    .addForwardedProcessDataKey(
                            variableName,
                            variableName,
                            null,
                            processNodeEntity
                    );
        }

        return metadata;
    }

    @Override
    public Map<String, String> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull NoCodeActionNodeConfiguration configuration) throws ResponseException {
        // TODO: Check validity of this node configuration.
        //       - All variables need to be unique.
        //       - No-Code expressions should be checked for syntax errors (if possible).
        //       - All types are correct
        return null;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<NoCodeActionNodeConfiguration> context) throws ProcessNodeExecutionException {
        var sourceRoot = context.getCurrentProcessExecutionData().get("$");
        if (!(sourceRoot instanceof Map<?, ?>)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Vorgangsdatenwurzel ($) ist kein Objekt."
            );
        }

        var workingExecutionData = ProcessExecutionData.of(MapUtils.deepCopy(context.getCurrentProcessExecutionData()));
        var variableDefinitions = parseVariableDefinitions(context.getConfigurationOfExecutingNode());
        var variableValues = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < variableDefinitions.size(); i++) {
            var definition = variableDefinitions.get(i);
            var rowIndex = i + 1;

            var targetPath = normalizeDestinationKey(definition.name(), rowIndex, "Variablenname");
            var rowEvaluationData = ProcessExecutionData.of(MapUtils.deepCopy(workingExecutionData));
            var wildcardBindings = resolveTargetWildcardBindings(rowEvaluationData, targetPath);

            for (var wildcardBinding : wildcardBindings) {
                var evaluatedValue = evaluateVariableValue(
                        definition,
                        rowIndex,
                        rowEvaluationData,
                        wildcardBinding
                );
                var castedValue = castToTargetType(
                        evaluatedValue,
                        definition.targetType(),
                        rowIndex
                );
                var resolvedPath = wildcardBinding.isEmpty()
                        ? targetPath
                        : ProcessDataValueUtils.materializeDestinationKey(targetPath, wildcardBinding);

                writeProcessDataValue(workingExecutionData, targetPath, castedValue, wildcardBinding, rowIndex);
                variableValues.add(createVariableValueEntry(
                        rowIndex,
                        definition,
                        targetPath,
                        resolvedPath,
                        wildcardBinding,
                        castedValue
                ));
            }
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setProcessData(workingExecutionData.getProcessData())
                .setNodeData(Map.of(
                        OUTPUT_NAME_VARIABLES, variableValues,
                        OUTPUT_NAME_VARIABLE_COUNT, variableValues.size()
                ));
    }

    @Nonnull
    private List<VariableDefinition> parseVariableDefinitions(@Nonnull NoCodeActionNodeConfiguration configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (configuration.variables == null) {
            return List.of();
        }

        var result = new ArrayList<VariableDefinition>(configuration.variables.size());
        for (var row : configuration.variables) {
            var variableName = row == null
                    ? null
                    : StringUtils.toNullableTrimmedString(row.name);
            if (variableName == null) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "In der Zeile ist kein gültiger Variablenname angegeben."
                );
            }

            var targetType = row == null
                    ? TARGET_TYPE_ANY
                    : StringUtils.toNullableTrimmedString(row.targetType);
            if (targetType == null) {
                targetType = TARGET_TYPE_ANY;
            }
            validateTargetType(targetType, result.size() + 1);

            var expressionOperand = row != null && row.expression != null
                    ? row.expression.getNoCode()
                    : null;

            if (expressionOperand == null) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "In der Zeile ist kein gültiger No-Code-Ausdruck angegeben."
                );
            }

            result.add(new VariableDefinition(
                    variableName,
                    targetType,
                    expressionOperand
            ));
        }

        return result;
    }

    @Nonnull
    private static DerivedRuntimeElementData createEvaluationContext(@Nonnull VariableDefinition definition) {
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put(VARIABLE_NAME_FIELD_ID, definition.name());
        effectiveValues.put(VARIABLE_TARGET_TYPE_FIELD_ID, definition.targetType());
        effectiveValues.put(VARIABLE_EXPRESSION_FIELD_ID, new NoCodeInputElementItem(definition.noCode()));
        return new DerivedRuntimeElementData().setEffectiveValues(effectiveValues);
    }

    @Nonnull
    private List<List<Integer>> resolveTargetWildcardBindings(@Nonnull ProcessExecutionData processDataContext,
                                                              @Nonnull String targetPath) {
        if (!ProcessDataValueUtils.hasWildcardSegment(targetPath)) {
            return List.of(List.of());
        }

        return ProcessDataValueUtils.resolveMatchingProcessDataValues(processDataContext, targetPath)
                .stream()
                .map(ProcessDataValueUtils.ResolvedProcessDataValue::wildcardIndices)
                .toList();
    }

    @Nullable
    private Object evaluateVariableValue(@Nonnull VariableDefinition definition,
                                         int rowIndex,
                                         @Nonnull ProcessExecutionData processDataContext,
                                         @Nonnull List<Integer> wildcardBinding) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        try {
            return noCodeEvaluationService
                    .evaluate(
                            definition.noCode(),
                            createEvaluationContext(definition),
                            processDataContext,
                            wildcardBinding
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
    }

    @Nonnull
    private static Map<String, Object> createVariableValueEntry(int rowIndex,
                                                                @Nonnull VariableDefinition definition,
                                                                @Nonnull String configuredPath,
                                                                @Nonnull String resolvedPath,
                                                                @Nonnull List<Integer> wildcardIndices,
                                                                @Nullable Object value) {
        var entry = new LinkedHashMap<String, Object>();
        entry.put("rowIndex", rowIndex);
        entry.put("configuredPath", configuredPath);
        entry.put("resolvedPath", resolvedPath);
        entry.put("wildcardIndices", List.copyOf(wildcardIndices));
        entry.put("targetType", definition.targetType());
        entry.put("value", value);
        return entry;
    }

    @Nonnull
    private static String normalizeDestinationKey(@Nullable String rawPath,
                                                  int rowIndex,
                                                  @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var normalizedPath = StringUtils.toNullableTrimmedString(rawPath);
        if (normalizedPath == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Pfadangabe %s in Zeile %d darf nicht leer sein.",
                    StringUtils.quote(fieldLabel),
                    rowIndex
            );
        }

        if (normalizedPath.startsWith("$.")) {
            normalizedPath = normalizedPath.substring(2).trim();
        } else if (normalizedPath.startsWith("$")) {
            throw invalidPathException(
                    rawPath,
                    rowIndex,
                    fieldLabel,
                    "Pfade müssen relativ zur Vorgangsdatenwurzel angegeben werden. Ein führendes $. ist optional, andere $-Präfixe sind nicht erlaubt."
            );
        }

        if ("_".equals(normalizedPath) || normalizedPath.startsWith("_.")) {
            throw invalidPathException(
                    rawPath,
                    rowIndex,
                    fieldLabel,
                    "Pfade dürfen nur auf Vorgangsdaten ($) zeigen."
            );
        }

        try {
            ProcessDataValueUtils.validateDestinationKey(normalizedPath);
            validateNumericArraySegments(normalizedPath, rowIndex, fieldLabel);
        } catch (IllegalArgumentException e) {
            throw invalidPathException(
                    rawPath,
                    rowIndex,
                    fieldLabel,
                    describePathValidationError(normalizedPath, e)
            );
        }

        return normalizedPath;
    }

    private static void validateNumericArraySegments(@Nonnull String destinationKey,
                                                     int rowIndex,
                                                     @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        for (var segment : splitDestinationKeySegments(destinationKey)) {
            if ("*".equals(segment) || !segment.chars().allMatch(Character::isDigit)) {
                continue;
            }

            try {
                Integer.parseInt(segment);
            } catch (NumberFormatException e) {
                throw invalidPathException(
                        destinationKey,
                        rowIndex,
                        fieldLabel,
                        "Array-Indizes müssen gültige 32-Bit-Ganzzahlen sein."
                );
            }
        }
    }

    @Nonnull
    private static String describePathValidationError(@Nonnull String path,
                                                      @Nonnull IllegalArgumentException exception) {
        if (path.contains("[*]")) {
            return "Array-Wildcards müssen als eigenes Segment '*' angegeben werden; [*] ist nicht erlaubt.";
        }
        if (path.contains("[") || path.contains("]")) {
            return "Array-Indizes müssen als numerische Segmente angegeben werden, z. B. items.0.name. Die Klammer-Schreibweise ist nicht erlaubt.";
        }
        return exception.getMessage() != null
                ? exception.getMessage()
                : "Der Pfad ist ungültig.";
    }

    @Nonnull
    private static ProcessNodeExecutionExceptionInvalidConfiguration invalidPathException(@Nullable String path,
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

    private static void writeProcessDataValue(@Nonnull ProcessExecutionData processExecutionData,
                                              @Nonnull String destinationKey,
                                              @Nullable Object value,
                                              @Nonnull List<Integer> wildcardIndices,
                                              int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        try {
            if (wildcardIndices.isEmpty()) {
                ProcessDataValueUtils.writeProcessDataValue(processExecutionData, destinationKey, value);
            } else {
                ProcessDataValueUtils.writeProcessDataValue(processExecutionData, destinationKey, value, wildcardIndices);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Konflikt beim Schreiben des No-Code-Ergebnisses in Zeile %d: %s",
                    rowIndex,
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private static List<String> splitDestinationKeySegments(@Nonnull String destinationKey) {
        return Arrays.stream(destinationKey.split("\\.", -1))
                .map(String::trim)
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
        if (value instanceof Instant instant) {
            return instant.atZone(ApplicationTimeZone.getZoneId()).toLocalDate().toString();
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
                    return IsoTimestampUtils
                            .parseIsoTimestamp(trimmed, ApplicationTimeZone.getZoneId())
                            .atZone(ApplicationTimeZone.getZoneId())
                            .toLocalDate()
                            .toString();
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
        // Normalize runtime datetime outputs to UTC ISO-8601 while interpreting local
        // date/date-time inputs in the configured business timezone.
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.atZone(ApplicationTimeZone.getZoneId()).toInstant().toString();
        }
        if (value instanceof LocalDate date) {
            return date.atStartOfDay(ApplicationTimeZone.getZoneId()).toInstant().toString();
        }
        if (value instanceof String s) {
            var trimmed = s.trim();
            if (trimmed.isEmpty()) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Leere Zeichenkette kann nicht als Datum und Uhrzeit umgewandelt werden."
                );
            }

            try {
                return IsoTimestampUtils.parseIsoTimestamp(trimmed, ApplicationTimeZone.getZoneId()).toString();
            } catch (Exception ignored) {
                try {
                    return LocalDate.parse(trimmed).atStartOfDay(ApplicationTimeZone.getZoneId()).toInstant().toString();
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

    private record VariableDefinition(
            @Nonnull String name,
            @Nonnull String targetType,
            @Nonnull NoCodeOperand noCode
    ) {
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class NoCodeActionNodeConfiguration {
        public List<NoCodeActionNodeVariableConfiguration> variables;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = VARIABLES_FIELD_ID, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "No-Code-Aktion"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Pro Eintrag wird ein Variablenname und der zu berechnende No-Code-Ausdruck definiert."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Eintrag #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Eintrag hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Eintrag entfernen")
    })
    public static class NoCodeActionNodeVariableConfiguration {
        @InputElementPOJOBinding(id = VARIABLE_NAME_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Variablenname"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Dieser Name wird als Zielpfad in den Vorgangsdaten gespeichert. Pfade verwenden Destination-Key-Syntax mit Punktnotation und numerischen Array-Segmenten, z. B. person.name oder items.0.name. Klammer-Schreibweisen wie [0], [*] oder * sind nicht erlaubt."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 8.0),
                @ElementPOJOBindingProperty(key = "disableWildCards", boolValue = true)
        })
        public String name;

        @InputElementPOJOBinding(id = VARIABLE_TARGET_TYPE_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zieltyp"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Gibt an, in welchen Typ das Ergebnis umgewandelt wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 4.0)
        })
        public String targetType;

        @InputElementPOJOBinding(id = VARIABLE_EXPRESSION_FIELD_ID, type = ElementType.NoCodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "No-Code-Ausdruck"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Ausdruck wird beim Ausführen des Knotens ausgewertet."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public NoCodeInputElementItem expression;
    }
}
