package de.aivot.GoverBackend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataTypeValidationControlNodeV1 implements ProcessNodeDefinition<DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig> {
    public static final String NODE_KEY = "data_type_validation";

    private static final String PORT_NAME_VALID = "valid";
    private static final String PORT_NAME_INVALID = "invalid";

    private static final String RULES_FIELD_ID = "rules";
    private static final String RULE_PATH_FIELD_ID = "path";
    private static final String RULE_TYPE_FIELD_ID = "expectedType";

    private static final String TYPE_ANY = "any";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_ARRAY = "array";
    private static final String TYPE_NULL = "null";

    private static final String OUTPUT_NAME_VALIDATED_RULE_COUNT = "validatedRuleCount";
    private static final String OUTPUT_NAME_VALIDATED_VALUE_COUNT = "validatedValueCount";
    private static final String OUTPUT_NAME_IS_VALID = "isValid";
    private static final String OUTPUT_NAME_ERRORS = "errors";

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
        return ProcessNodeType.FlowControl;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Vorgangsdaten validieren";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Prüft, ob konfigurierte Datenpfade in den Vorgangsdaten vorhanden sind und den erwarteten Datentypen entsprechen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(DataTypeValidationControlNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        layout
                .findChild(RULE_TYPE_FIELD_ID, SelectInputElement.class)
                .ifPresent(expectedTypeInput -> {
                    expectedTypeInput.setValue(new ElementValueFunctions()
                            .setType(ValueFunctionType.NoCode)
                            .setNoCode(new NoCodeStaticValue(TYPE_ANY)));
                    expectedTypeInput.setOptions(List.of(
                            SelectInputElementOption.of(TYPE_ANY, "Beliebig"),
                            SelectInputElementOption.of(TYPE_STRING, "Text"),
                            SelectInputElementOption.of(TYPE_NUMBER, "Zahl"),
                            SelectInputElementOption.of(TYPE_BOOLEAN, "Ja/Nein"),
                            SelectInputElementOption.of(TYPE_OBJECT, "Objekt"),
                            SelectInputElementOption.of(TYPE_ARRAY, "Array"),
                            SelectInputElementOption.of(TYPE_NULL, "Null")
                    ));
                });

        return layout;
    }

    @Nonnull
    @Override
    public Class<DataTypeValidationControlNodeConfig> getNodeConfigurationClass() {
        return DataTypeValidationControlNodeConfig.class;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME_VALID,
                        "Gültig",
                        "Der Prozess wird hier fortgesetzt, wenn alle Validierungsregeln erfüllt sind."
                ),
                new ProcessNodePort(
                        PORT_NAME_INVALID,
                        "Ungültig",
                        "Der Prozess wird hier fortgesetzt, wenn mindestens eine Validierungsregel verletzt ist."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_VALIDATED_RULE_COUNT,
                        "Anzahl Regeln",
                        "Die Anzahl der erfolgreich validierten Regeln."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_VALIDATED_VALUE_COUNT,
                        "Anzahl geprüfter Werte",
                        "Die Anzahl der konkret überprüften Werte über alle Regeln."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_IS_VALID,
                        "Gesamtgültigkeit",
                        "true, wenn alle Regeln erfolgreich validiert wurden, sonst false."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_ERRORS,
                        "Validierungsfehler",
                        "Liste der Regelverletzungen mit Pfad und Fehlermeldung."
                )
        );
    }

    @Nullable
    @Override
    public GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionTestingLayoutContext<DataTypeValidationControlNodeConfig> context) throws ResponseException {
        var groupLayout = new GroupLayoutElement();
        groupLayout.setId("layout");

        var contentBuilder = new StringBuilder();
        contentBuilder.append("Beispiel-Payload für die konfigurierten Validierungsregeln:\n\n");

        try {
            var rules = parseRules(context.configuration());
            var examplePayload = createExamplePayloadFromRules(rules);
            var exampleJson = ObjectMapperFactory
                    .getInstance()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(examplePayload);

            contentBuilder
                    .append("```json\n")
                    .append(exampleJson)
                    .append("\n```\n");
        } catch (Exception e) {
            contentBuilder
                    .append("Die Beispiel-JSON konnte nicht aus der aktuellen Konfiguration erzeugt werden.\n\n")
                    .append("```text\n")
                    .append(e.getMessage() != null ? e.getMessage() : "Unbekannter Fehler")
                    .append("\n```\n");
        }

        var contentRtx = new RichTextContentElement();
        contentRtx.setContent(contentBuilder.toString());
        groupLayout.addChild(contentRtx);

        return groupLayout;
    }

    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                           @Nonnull DataTypeValidationControlNodeConfig configuration) throws ResponseException {
        // TODO: validate
        return null;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<DataTypeValidationControlNodeConfig> context) throws ProcessNodeExecutionException {
        var sourceRoot = context.getCurrentProcessExecutionData().get("$");
        if (!(sourceRoot instanceof Map<?, ?> sourceRootRawMap)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Vorgangsdatenwurzel ($) ist kein Objekt."
            );
        }

        var sourceRootMap = castStringObjectMap(sourceRootRawMap);
        var rules = parseRules(context.getConfigurationOfExecutingNode());

        var checkedValuesCount = 0;
        var errors = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < rules.size(); i++) {
            var rowIndex = i + 1;
            var rule = rules.get(i);
            var path = parsePath(rule.path(), rowIndex);
            var matchedValues = new ArrayList<MatchedValue>();

            try {
                collectMatchedValues(sourceRootMap, path, 0, "$", rowIndex, rule.path(), matchedValues);
            } catch (ProcessNodeExecutionExceptionMissingValue | ProcessNodeExecutionExceptionInvalidDataType e) {
                errors.add(Map.of(
                        "rowIndex", rowIndex,
                        "path", rule.path(),
                        "error", e.getMessage()
                ));
                continue;
            }

            for (var matchedValue : matchedValues) {
                try {
                    validateDataType(matchedValue.path(), matchedValue.value(), rule.expectedType(), rowIndex);
                } catch (ProcessNodeExecutionExceptionInvalidDataType e) {
                    errors.add(Map.of(
                            "rowIndex", rowIndex,
                            "path", rule.path(),
                            "resolvedPath", matchedValue.path(),
                            "error", e.getMessage()
                    ));
                }
            }
            checkedValuesCount += matchedValues.size();
        }

        var isValid = errors.isEmpty();

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(isValid ? PORT_NAME_VALID : PORT_NAME_INVALID)
                .setNodeData(Map.of(
                        OUTPUT_NAME_VALIDATED_RULE_COUNT, rules.size(),
                        OUTPUT_NAME_VALIDATED_VALUE_COUNT, checkedValuesCount,
                        OUTPUT_NAME_IS_VALID, isValid,
                        OUTPUT_NAME_ERRORS, errors
                ));
    }

    @Nonnull
    private List<ValidationRule> parseRules(@Nonnull DataTypeValidationControlNodeConfig configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (configuration.rules == null) {
            return List.of();
        }

        var result = new ArrayList<ValidationRule>(configuration.rules.size());
        for (int i = 0; i < configuration.rules.size(); i++) {
            var rowIndex = i + 1;
            var row = configuration.rules.get(i);

            var path = row == null ? null : StringUtils.toNullableTrimmedString(row.path);
            if (path == null) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Pfadangabe in Zeile %d darf nicht leer sein.",
                        rowIndex
                );
            }

            var expectedType = row == null
                    ? TYPE_ANY
                    : StringUtils.toNullableTrimmedString(row.expectedType);
            if (expectedType == null) {
                expectedType = TYPE_ANY;
            }

            validateExpectedType(expectedType, rowIndex);
            result.add(new ValidationRule(path, expectedType));
        }

        return result;
    }

    @Nonnull
    private static Map<String, Object> createExamplePayloadFromRules(@Nonnull List<ValidationRule> rules) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var root = new LinkedHashMap<String, Object>();
        for (int i = 0; i < rules.size(); i++) {
            var rowIndex = i + 1;
            var rule = rules.get(i);
            var pathParts = parsePath(rule.path(), rowIndex);
            var exampleValue = createExampleValueForType(rule.expectedType());
            mergeExampleValue(root, pathParts, 0, exampleValue);
        }
        return root;
    }

    private static void mergeExampleValue(@Nonnull Map<String, Object> root,
                                          @Nonnull List<PathPart> pathParts,
                                          int partIndex,
                                          Object leafValue) {
        var currentPart = pathParts.get(partIndex);
        var isLast = partIndex == pathParts.size() - 1;

        if (!(currentPart instanceof ObjectPathPart objectPathPart)) {
            return;
        }

        if (isLast) {
            root.putIfAbsent(objectPathPart.key(), leafValue);
            return;
        }

        var nextPart = pathParts.get(partIndex + 1);
        var existing = root.get(objectPathPart.key());

        if (nextPart instanceof WildcardPathPart) {
            List<Object> list;
            if (existing instanceof List<?> existingList) {
                @SuppressWarnings("unchecked")
                var typed = (List<Object>) existingList;
                list = typed;
            } else {
                list = new ArrayList<>();
                root.put(objectPathPart.key(), list);
            }

            if (isLastSegmentAfterWildcard(pathParts, partIndex + 1)) {
                if (list.isEmpty()) {
                    list.add(leafValue);
                }
                return;
            }

            Object item = list.isEmpty() ? null : list.getFirst();
            if (!(item instanceof Map<?, ?>)) {
                item = new LinkedHashMap<String, Object>();
                if (list.isEmpty()) {
                    list.add(item);
                } else {
                    list.set(0, item);
                }
            }

            @SuppressWarnings("unchecked")
            var itemMap = (Map<String, Object>) item;
            mergeExampleValue(itemMap, pathParts, partIndex + 2, leafValue);
            return;
        }

        Map<String, Object> childMap;
        if (existing instanceof Map<?, ?> existingMap) {
            @SuppressWarnings("unchecked")
            var typed = (Map<String, Object>) existingMap;
            childMap = typed;
        } else {
            childMap = new LinkedHashMap<>();
            root.put(objectPathPart.key(), childMap);
        }

        mergeExampleValue(childMap, pathParts, partIndex + 1, leafValue);
    }

    private static boolean isLastSegmentAfterWildcard(@Nonnull List<PathPart> pathParts, int wildcardIndex) {
        return wildcardIndex == pathParts.size() - 1;
    }

    @Nonnull
    private static Object createExampleValueForType(@Nonnull String expectedType) {
        return switch (expectedType) {
            case TYPE_STRING -> "example";
            case TYPE_NUMBER -> 123;
            case TYPE_BOOLEAN -> true;
            case TYPE_OBJECT -> new LinkedHashMap<String, Object>();
            case TYPE_ARRAY -> new ArrayList<>();
            case TYPE_NULL -> null;
            case TYPE_ANY -> "any-value";
            default -> "value";
        };
    }

    @Nonnull
    private static List<PathPart> parsePath(@Nonnull String path, int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var trimmedPath = path.trim();
        if (trimmedPath.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Pfadangabe in Zeile %d darf nicht leer sein.",
                    rowIndex
            );
        }

        var parts = new ArrayList<PathPart>();
        var segmentBuilder = new StringBuilder();

        for (int i = 0; i < trimmedPath.length(); i++) {
            var c = trimmedPath.charAt(i);
            if (c == '.') {
                appendPathSegment(segmentBuilder, parts, trimmedPath, rowIndex);
                continue;
            }
            segmentBuilder.append(c);
        }
        appendPathSegment(segmentBuilder, parts, trimmedPath, rowIndex);

        if (parts.isEmpty()) {
            throw invalidPathException(trimmedPath, rowIndex, "Pfad enthält keine Segmente.");
        }

        if (parts.getFirst() instanceof WildcardPathPart) {
            throw invalidPathException(trimmedPath, rowIndex, "Pfad darf nicht mit * beginnen.");
        }

        return parts;
    }

    private static void appendPathSegment(@Nonnull StringBuilder segmentBuilder,
                                          @Nonnull List<PathPart> target,
                                          @Nonnull String path,
                                          int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var rawSegment = segmentBuilder.toString().trim();
        segmentBuilder.setLength(0);

        if (rawSegment.isEmpty()) {
            throw invalidPathException(path, rowIndex, "Leere Segmente sind nicht erlaubt.");
        }

        if ("*".equals(rawSegment)) {
            target.add(new WildcardPathPart());
            return;
        }

        if (rawSegment.contains("*")) {
            throw invalidPathException(path, rowIndex, "Wildcard * darf nur als eigenes Segment verwendet werden.");
        }

        target.add(new ObjectPathPart(rawSegment));
    }

    private static void collectMatchedValues(Object currentValue,
                                             @Nonnull List<PathPart> pathParts,
                                             int partIndex,
                                             @Nonnull String currentPath,
                                             int rowIndex,
                                             @Nonnull String configuredPath,
                                             @Nonnull List<MatchedValue> target) throws ProcessNodeExecutionException {
        if (partIndex >= pathParts.size()) {
            target.add(new MatchedValue(currentPath, currentValue));
            return;
        }

        var currentPart = pathParts.get(partIndex);
        if (currentPart instanceof ObjectPathPart objectPathPart) {
            if (!(currentValue instanceof Map<?, ?> currentMap)) {
                throw new ProcessNodeExecutionExceptionInvalidDataType(
                        "Der Pfad %s in Zeile %d kann nicht aufgelöst werden. Segment %s erwartet ein Objekt in %s.",
                        StringUtils.quote(configuredPath),
                        rowIndex,
                        StringUtils.quote(objectPathPart.key()),
                        StringUtils.quote(currentPath)
                );
            }

            if (!currentMap.containsKey(objectPathPart.key())) {
                throw new ProcessNodeExecutionExceptionMissingValue(
                        "Der Pfad %s in Zeile %d fehlt. Segment %s ist in %s nicht vorhanden.",
                        StringUtils.quote(configuredPath),
                        rowIndex,
                        StringUtils.quote(objectPathPart.key()),
                        StringUtils.quote(currentPath)
                );
            }

            collectMatchedValues(
                    currentMap.get(objectPathPart.key()),
                    pathParts,
                    partIndex + 1,
                    currentPath + "." + objectPathPart.key(),
                    rowIndex,
                    configuredPath,
                    target
            );
            return;
        }

        if (!(currentValue instanceof List<?> currentList)) {
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    "Der Pfad %s in Zeile %d kann nicht aufgelöst werden. Segment * erwartet ein Array in %s.",
                    StringUtils.quote(configuredPath),
                    rowIndex,
                    StringUtils.quote(currentPath)
            );
        }

        for (int i = 0; i < currentList.size(); i++) {
            collectMatchedValues(
                    currentList.get(i),
                    pathParts,
                    partIndex + 1,
                    String.format("%s[%d]", currentPath, i),
                    rowIndex,
                    configuredPath,
                    target
            );
        }
    }

    private static void validateExpectedType(@Nonnull String expectedType,
                                             int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (
                TYPE_ANY.equals(expectedType) ||
                        TYPE_STRING.equals(expectedType) ||
                        TYPE_NUMBER.equals(expectedType) ||
                        TYPE_BOOLEAN.equals(expectedType) ||
                        TYPE_OBJECT.equals(expectedType) ||
                        TYPE_ARRAY.equals(expectedType) ||
                        TYPE_NULL.equals(expectedType)
        ) {
            return;
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "In Zeile %d ist der Datentyp %s ungültig.",
                rowIndex,
                StringUtils.quote(expectedType)
        );
    }

    private static void validateDataType(@Nonnull String resolvedPath,
                                         Object value,
                                         @Nonnull String expectedType,
                                         int rowIndex) throws ProcessNodeExecutionExceptionInvalidDataType {
        if (TYPE_ANY.equals(expectedType)) {
            return;
        }

        var matches = switch (expectedType) {
            case TYPE_STRING -> value instanceof String;
            case TYPE_NUMBER -> value instanceof Number;
            case TYPE_BOOLEAN -> value instanceof Boolean;
            case TYPE_OBJECT -> value instanceof Map<?, ?>;
            case TYPE_ARRAY -> value instanceof List<?>;
            case TYPE_NULL -> value == null;
            default -> false;
        };

        if (matches) {
            return;
        }

        throw new ProcessNodeExecutionExceptionInvalidDataType(
                "Ungültiger Datentyp in Zeile %d bei %s. Erwartet: %s, erhalten: %s.",
                rowIndex,
                StringUtils.quote(resolvedPath),
                StringUtils.quote(expectedType),
                StringUtils.quote(describeType(value))
        );
    }

    @Nonnull
    private static ProcessNodeExecutionExceptionInvalidConfiguration invalidPathException(@Nonnull String path,
                                                                                          int rowIndex,
                                                                                          @Nonnull String detail) {
        return new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ungültiger Pfad in Zeile %d: %s. Pfad: %s",
                rowIndex,
                detail,
                StringUtils.quote(path)
        );
    }

    @Nonnull
    private static String describeType(Object value) {
        if (value == null) {
            return TYPE_NULL;
        }
        if (value instanceof String) {
            return TYPE_STRING;
        }
        if (value instanceof Number) {
            return TYPE_NUMBER;
        }
        if (value instanceof Boolean) {
            return TYPE_BOOLEAN;
        }
        if (value instanceof List<?>) {
            return TYPE_ARRAY;
        }
        if (value instanceof Map<?, ?>) {
            return TYPE_OBJECT;
        }
        return value.getClass().getSimpleName();
    }

    @Nonnull
    private static Map<String, Object> castStringObjectMap(@Nonnull Map<?, ?> map) {
        var result = new java.util.HashMap<String, Object>();
        for (var entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    private record ValidationRule(@Nonnull String path, @Nonnull String expectedType) {
    }

    private record MatchedValue(@Nonnull String path, Object value) {
    }

    private sealed interface PathPart permits ObjectPathPart, WildcardPathPart {
    }

    private record ObjectPathPart(@Nonnull String key) implements PathPart {
    }

    private record WildcardPathPart() implements PathPart {
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class DataTypeValidationControlNodeConfig {
        public List<DataTypeValidationRuleConfig> rules;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = RULES_FIELD_ID, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Validierungsregeln"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Beispiele: person.name, person.address.street, tags.*, items.*.name"),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Regel #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Regel hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Regel entfernen")
    })
    public static class DataTypeValidationRuleConfig {
        @InputElementPOJOBinding(id = RULE_PATH_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Pfad"),
                @ElementPOJOBindingProperty(key = "prefix", strValue = "$."),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Dot-Notation mit * für Arrays, z. B. addresses.*.street"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 8.0)
        })
        public String path;

        @InputElementPOJOBinding(id = RULE_TYPE_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Datentyp"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Datentyp, den der Wert am Pfad haben muss."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 4.0)
        })
        public String expectedType;
    }
}
