package de.aivot.GoverBackend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.core.converters.ElementDataConverter;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class DataTypeValidationActionNodeV1 implements ProcessNodeDefinition {
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

    private final ElementDataConverter elementDataConverter = new ElementDataConverter();

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
        return ProcessNodeType.FlowControl;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Prozessdaten validieren";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Prüft, ob konfigurierte Datenpfade in den Prozessdaten vorhanden sind und den erwarteten Datentypen entsprechen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var rulesInput = new ReplicatingContainerLayoutElement();
        rulesInput.setId(RULES_FIELD_ID);
        rulesInput.setLabel("Validierungsregeln");
        rulesInput.setHint("Beispiele: person.name, person.address.street, tags.*, items.*.name");
        rulesInput.setRequired(true);
        rulesInput.setMinimumRequiredSets(1);
        rulesInput.setHeadlineTemplate("Regel #");
        rulesInput.setAddLabel("Regel hinzufügen");
        rulesInput.setRemoveLabel("Regel entfernen");

        var pathInput = new TextInputElement();
        pathInput.setId(RULE_PATH_FIELD_ID);
        pathInput.setLabel("Pfad");
        pathInput.setHint("Dot-Notation mit * für Arrays, z. B. addresses.*.street");
        pathInput.setRequired(true);
        pathInput.setWeight(8.0);

        var expectedTypeInput = new SelectInputElement();
        expectedTypeInput.setId(RULE_TYPE_FIELD_ID);
        expectedTypeInput.setLabel("Datentyp");
        expectedTypeInput.setHint("Der Datentyp, den der Wert am Pfad haben muss.");
        expectedTypeInput.setRequired(true);
        expectedTypeInput.setWeight(4.0);
        expectedTypeInput.setValue(new ElementValueFunctions()
                .setType(ValueFunctionType.NoCode)
                .setNoCode(new NoCodeStaticValue(TYPE_ANY)));
        expectedTypeInput.setOptions(List.of(
                RadioInputElementOption.of(TYPE_ANY, "Beliebig"),
                RadioInputElementOption.of(TYPE_STRING, "Text"),
                RadioInputElementOption.of(TYPE_NUMBER, "Zahl"),
                RadioInputElementOption.of(TYPE_BOOLEAN, "Ja/Nein"),
                RadioInputElementOption.of(TYPE_OBJECT, "Objekt"),
                RadioInputElementOption.of(TYPE_ARRAY, "Array"),
                RadioInputElementOption.of(TYPE_NULL, "Null")
        ));

        rulesInput.setChildren(List.of(pathInput, expectedTypeInput));
        layout.addChild(rulesInput);
        return layout;
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
    public GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionContextTesting context) throws ResponseException {
        var groupLayout = new GroupLayoutElement();
        groupLayout.setId("layout");

        var contentBuilder = new StringBuilder();
        contentBuilder.append("<p>Beispiel-Payload für die konfigurierten Validierungsregeln:</p>\n");

        try {
            var rules = parseRules(context.thisNode().getConfiguration());
            var examplePayload = createExamplePayloadFromRules(rules);
            var exampleJson = ObjectMapperFactory
                    .getInstance()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(examplePayload);

            contentBuilder
                    .append("<pre class=\"code-block\">")
                    .append(escapeHtml(exampleJson))
                    .append("</pre>\n");
        } catch (Exception e) {
            contentBuilder
                    .append("<p>Die Beispiel-JSON konnte nicht aus der aktuellen Konfiguration erzeugt werden.</p>\n")
                    .append("<pre class=\"code-block\">")
                    .append(escapeHtml(e.getMessage() != null ? e.getMessage() : "Unbekannter Fehler"))
                    .append("</pre>\n");
        }

        var contentRtx = new RichTextContentElement();
        contentRtx.setContent(contentBuilder.toString());
        groupLayout.addChild(contentRtx);

        return groupLayout;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var sourceRoot = context.getProcessData().get("$");
        if (!(sourceRoot instanceof Map<?, ?> sourceRootRawMap)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Prozessdatenwurzel ($) ist kein Objekt."
            );
        }

        var sourceRootMap = castStringObjectMap(sourceRootRawMap);
        var rules = parseRules(context.getThisNode().getConfiguration());

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
    private List<ValidationRule> parseRules(@Nonnull ElementData configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var rawRules = configuration
                .getOpt(RULES_FIELD_ID)
                .map(ElementDataObject::getValue)
                .orElse(List.of());

        if (!(rawRules instanceof Collection<?> rows)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Konfiguration im Feld %s ist ungültig. Es wird eine Liste erwartet.",
                    StringUtils.quote(RULES_FIELD_ID)
            );
        }

        if (rows.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Es wurde keine Validierungsregel konfiguriert."
            );
        }

        var result = new ArrayList<ValidationRule>();
        var usedPaths = new LinkedHashSet<String>();
        int rowIndex = 1;
        for (var rawRow : rows) {
            var row = toElementData(rawRow, rowIndex);

            var path = row
                    .getOpt(RULE_PATH_FIELD_ID)
                    .map(ElementDataObject::getValue)
                    .map(DataTypeValidationActionNodeV1::toNullableTrimmedString)
                    .orElse(null);

            var expectedType = row
                    .getOpt(RULE_TYPE_FIELD_ID)
                    .map(ElementDataObject::getValue)
                    .map(DataTypeValidationActionNodeV1::toNullableTrimmedString)
                    .filter(type -> !StringUtils.isNullOrEmpty(type))
                    .orElse(TYPE_ANY);

            if (StringUtils.isNullOrEmpty(path)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "In Zeile %d fehlt ein gültiger Pfad.",
                        rowIndex
                );
            }

            if (!usedPaths.add(path)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der Pfad %s wird mehrfach verwendet.",
                        StringUtils.quote(path)
                );
            }

            validateExpectedType(expectedType, rowIndex);
            result.add(new ValidationRule(path, expectedType));
            rowIndex++;
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
    private static String escapeHtml(@Nonnull String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Nonnull
    private ElementData toElementData(@Nonnull Object rawRow, int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (rawRow instanceof ElementData elementData) {
            return elementData;
        }

        try {
            return elementDataConverter.convertObjectToEntityAttribute(rawRow);
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Validierungsregel in Zeile %d ist ungültig.",
                    rowIndex
            );
        }
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

    private static String toNullableTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        var str = value.toString().trim();
        return str.isEmpty() ? null : str;
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
}
