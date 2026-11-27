package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.ValueFunctionType;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.ElementValueFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.map;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.list;

@Component
public class DataTypeValidationControlNodeV1 implements ProcessNodeDefinition<DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig> {
    public static final String NODE_KEY = "data_type_validation";

    private static final int MAX_EXAMPLE_ARRAY_LENGTH = 1000;
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
    private static final String OUTPUT_ERRORS_TYPE_DEFINITION =
            "Array<{ rowIndex: number; path: string; resolvedPath?: string; error: string; }>";

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var valid = Boolean.TRUE.equals(summary.data(OUTPUT_NAME_IS_VALID));
        var markdown = new StringBuilder(valid ? "Die Prüfung hat keine Abweichungen von den festgelegten Regeln ergeben." : "Die Prüfung hat Abweichungen von den festgelegten Regeln ergeben.");
        markdown.append(detail("Ausgewertete Regeln", summary.data(OUTPUT_NAME_VALIDATED_RULE_COUNT)))
                .append(detail("Geprüfte Werte", summary.data(OUTPUT_NAME_VALIDATED_VALUE_COUNT)));
        for (var entry : list(summary.data(OUTPUT_NAME_ERRORS))) {
            var error = map(entry);
            markdown.append("\n\n- ").append(text(error.get("path"))).append(": ").append(text(error.get("error")));
        }
        return markdown.toString();
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
        return ProcessNodeType.FlowControl;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
    }

    @Nonnull
    @Override
    public String getName() {
        return "Vorgangsdaten prüfen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Prüft automatisch das Vorhandensein und den Datentyp ausgewählter Vorgangsdaten.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Mit diesem Prozesselement prüfen Sie automatisch, ob benötigte Werte in den Vorgangsdaten vorhanden sind und den erwarteten Datentyp haben, zum Beispiel Text, Zahl oder Liste. Anhand des Ergebnisses kann der Vorgang auf unterschiedlichen Wegen fortgesetzt werden.

                Damit prüfen Sie die Struktur der ausgewählten Daten. Eine fachliche Prüfung oder Korrektur durch Mitarbeiter:innen ist damit nicht verbunden.

                # Was richten Sie ein?

                Legen Sie unter **„Prüfregeln“** die benötigten Prüfungen an. Für jede Regel geben Sie den **„Speicherort des Werts“** und den **„Erwarteten Datentyp“** an.

                Ein Speicherort ist ein Datenpfad wie `person.name` oder `person.alter`. Mit `personen[0].name` prüfen Sie einen bestimmten Listeneintrag; die Zählung beginnt bei `0`. Mit `personen[*].name` prüfen Sie die Namen an den vorhandenen Listenpositionen. Der Präfix `$.` wird in der Oberfläche bereits angezeigt.

                Wählen Sie **„Beliebig (nur Vorhandensein)“**, wenn der Wert vorhanden sein soll, sein Datentyp aber keine Rolle spielt. Für eine gezielte Typprüfung stehen **„Text“**, **„Zahl“**, **„Ja/Nein“**, **„Objekt“**, **„Liste“** und **„Nullwert (null)“** bereit.

                Verbinden Sie die Ausgänge **„Prüfregeln erfüllt“** und **„Prüfregeln nicht erfüllt“** mit den jeweils passenden nächsten Schritten.

                # Was passiert bei der Ausführung?

                Prosuna liest die festgelegten Speicherorte und wertet die Regeln automatisch aus. Sind keine Abweichungen festgestellt worden, wird der Vorgang über **„Prüfregeln erfüllt“** fortgesetzt. Andernfalls geht es über **„Prüfregeln nicht erfüllt“** weiter.

                Eine Abweichung in den Daten ist ein reguläres Prüfergebnis. Eine fehlerhaft konfigurierte Regel, etwa ein ungültiger Datenpfad, führt dagegen zu einer fehlgeschlagenen Aufgabe. Dieser Konfigurationsfehler verwendet keinen der beiden Prüfausgänge.

                # Welches Ergebnis steht danach bereit?

                In den Elementdaten stehen das Prüfergebnis, die Anzahl ausgewerteter Regeln, die Anzahl geprüfter Werte und die festgestellten Abweichungen mit Regelnummer und Speicherort bereit. Über die Ausgangsdatenzuordnung können Sie diese Ergebnisse zusätzlich in die Vorgangsdaten übernehmen.

                Das Element selbst korrigiert oder ergänzt keine Vorgangsdaten. Der Vorgangsverlauf zeigt das Ergebnis der festgelegten Prüfregeln und die gespeicherten Abweichungen.

                Im Testbereich steht ein JSON-Beispiel zu den konfigurierten Regeln zur Verfügung. Es dient der Orientierung und ersetzt keinen Testdurchlauf mit Ihren tatsächlichen Daten.

                # Wie sieht ein Beispiel aus?

                Vor der weiteren automatischen Bearbeitung sollen `person.name` als Text und `person.alter` als Zahl vorliegen. Legen Sie dafür zwei Prüfregeln an.

                Bei `person.name: "Ada"` und `person.alter: 37` sind beide Regeln erfüllt. Steht das Alter dagegen als Text `"37"` in den Daten oder fehlt der Name, wird der Vorgang über **„Prüfregeln nicht erfüllt“** fortgesetzt. Dort können Sie beispielsweise „Vorgangsdaten bearbeiten“ oder „Angaben anfordern“ anschließen.

                # Was ist zu beachten?

                Ein vorhandener Nullwert (`null`) ist etwas anderes als ein fehlendes Feld. Er erfüllt **„Beliebig (nur Vorhandensein)“** und **„Nullwert (null)“**, aber zum Beispiel nicht **„Text“** oder **„Zahl“**.

                Leerer Text ist weiterhin Text. Eine leere Liste kann eine Regel mit `[*]` erfüllen, ohne dass Werte geprüft werden. Eine Mindestanzahl von Einträgen oder vollständig ausgefüllte Angaben werden damit nicht sichergestellt.

                Die Auswahl **„Objekt“** oder **„Liste“** prüft nur den jeweiligen Strukturtyp. Für einzelne enthaltene Felder benötigen Sie eigene Regeln. Zahlen werden nicht aus Text umgewandelt, und fachliche Richtigkeit oder Plausibilität werden nicht bewertet.

                Ein Wert kann durch mehrere Regeln mehrfach gezählt werden. Die Zahl geprüfter Werte ist deshalb keine Anzahl unterschiedlicher oder fehlerfreier Datenfelder.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(DataTypeValidationControlNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Einstellungen für die Datenprüfung konnten nicht geladen werden.");
        }

        layout
                .findChild(RULE_TYPE_FIELD_ID, SelectInputElement.class)
                .ifPresent(expectedTypeInput -> {
                    expectedTypeInput.setValue(new ElementValueFunctions()
                            .setType(ValueFunctionType.NoCode)
                            .setNoCode(new NoCodeStaticValue(TYPE_ANY)));
                    expectedTypeInput.setOptions(List.of(
                            SelectInputElementOption.of(TYPE_ANY, typeLabel(TYPE_ANY)),
                            SelectInputElementOption.of(TYPE_STRING, typeLabel(TYPE_STRING)),
                            SelectInputElementOption.of(TYPE_NUMBER, typeLabel(TYPE_NUMBER)),
                            SelectInputElementOption.of(TYPE_BOOLEAN, typeLabel(TYPE_BOOLEAN)),
                            SelectInputElementOption.of(TYPE_OBJECT, typeLabel(TYPE_OBJECT)),
                            SelectInputElementOption.of(TYPE_ARRAY, typeLabel(TYPE_ARRAY)),
                            SelectInputElementOption.of(TYPE_NULL, typeLabel(TYPE_NULL))
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
    public List<ProcessNodePort> getPorts(@Nonnull DataTypeValidationControlNodeConfig configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME_VALID,
                        "Prüfregeln erfüllt",
                        "Der Vorgang wird hier fortgesetzt, wenn keine Abweichungen von den festgelegten Prüfregeln festgestellt wurden."
                ),
                new ProcessNodePort(
                        PORT_NAME_INVALID,
                        "Prüfregeln nicht erfüllt",
                        "Der Vorgang wird hier fortgesetzt, wenn mindestens eine Abweichung von den festgelegten Prüfregeln festgestellt wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_VALIDATED_RULE_COUNT,
                        "Ausgewertete Regeln",
                        "Anzahl der ausgewerteten Regeln, einschließlich Regeln mit festgestellten Abweichungen.",
                        "number"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_VALIDATED_VALUE_COUNT,
                        "Geprüfte Werte",
                        "Anzahl der Werte, die nach vollständigem Lesen der zugehörigen Regel ausgewertet wurden. Auch Werte mit abweichendem Typ und mehrfach geprüfte Werte werden gezählt. Bei „Beliebig“ wird nur das Vorhandensein geprüft.",
                        "number"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_IS_VALID,
                        "Prüfregeln erfüllt",
                        "true, wenn keine Abweichungen von den festgelegten Prüfregeln festgestellt wurden; andernfalls false.",
                        "boolean"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_ERRORS,
                        "Festgestellte Abweichungen",
                        "Liste der festgestellten Abweichungen mit Regelnummer ab 1, konfiguriertem Speicherort und Meldung. Bei Typabweichungen am Endwert enthält resolvedPath zusätzlich den konkreten Speicherort.",
                        OUTPUT_ERRORS_TYPE_DEFINITION
                )
        );
    }

    @Nullable
    @Override
    public GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionTestingLayoutContext<DataTypeValidationControlNodeConfig> context) throws ResponseException {
        var groupLayout = new GroupLayoutElement();
        groupLayout.setId("layout");

        var contentBuilder = new StringBuilder();
        contentBuilder.append("Beispiel für die Vorgangsdaten zu Ihren Prüfregeln:\n\n");

        try {
            var rules = parseRules(context.configuration());
            var examplePayload = createExamplePayloadFromRules(rules);
            var exampleJson = JsonMapperFactory
                    .getInstance()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(examplePayload);

            contentBuilder
                    .append("```json\n")
                    .append(exampleJson)
                    .append("\n```\n");
        } catch (Exception e) {
            contentBuilder
                    .append("Die Beispieldaten konnten nicht aus den aktuellen Prüfregeln erstellt werden.\n\n")
                    .append("```text\n")
                    .append(e.getMessage() != null ? e.getMessage() : "Zu diesem Fehler sind keine weiteren Angaben verfügbar.")
                    .append("\n```\n");
        }

        var contentRtx = new RichTextContentElement();
        contentRtx.setContent(contentBuilder.toString());
        groupLayout.addChild(contentRtx);

        return groupLayout;
    }

    // TODO: Add node-specific authoring checks through validateConfiguration(context).

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<DataTypeValidationControlNodeConfig> context) throws ProcessNodeExecutionException {
        var sourceRoot = context.getCurrentProcessExecutionData().get("$");
        if (!(sourceRoot instanceof Map<?, ?> sourceRootRawMap)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Vorgangsdaten haben ein ungültiges Format. Für die Prüfung wird am Wurzelpfad ($) ein Objekt erwartet."
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
                        "Regel %d: Geben Sie den Speicherort des zu prüfenden Werts an.",
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
            // Preview generation must not allocate enormous sparse arrays merely to display a valid read path.
            if (pathParts.stream().anyMatch(part -> part instanceof IndexPathPart index && index.index() >= MAX_EXAMPLE_ARRAY_LENGTH)) {
                throw new IllegalArgumentException(
                        "Für das JSON-Beispiel sind höchstens 1000 Einträge pro Liste möglich. Die Prüfregel wird dadurch nicht geändert.");
            }
            var exampleValue = createExampleValueForType(rule.expectedType());
            // A wildcard needs one representative row; concrete indices keep their configured position.
            var segments = pathParts.stream().map(part -> switch (part) {
                case ObjectPathPart object -> object.key();
                case IndexPathPart index -> Integer.toString(index.index());
                case WildcardPathPart ignored -> "0";
            }).toList();
            var examplePath = ProcessDataValueUtils.formatDestinationKeySegments(segments);
            if (ProcessDataValueUtils.resolveDestinationKeyValue(root, examplePath) == null) {
                ProcessDataValueUtils.writeDestinationKeyValue(root, examplePath, exampleValue);
            }
        }
        return root;
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
                    "Regel %d: Geben Sie den Speicherort des zu prüfenden Werts an.",
                    rowIndex
            );
        }

        try {
            return ProcessDataValueUtils.parseDestinationKeySegments(trimmedPath, false).stream()
                    .<PathPart>map(segment -> {
                        if ("*".equals(segment)) {
                            return new WildcardPathPart();
                        }
                        if (Character.isDigit(segment.charAt(0))) {
                            return new IndexPathPart(Integer.parseInt(segment));
                        }
                        return new ObjectPathPart(segment);
                    }).toList();
        } catch (IllegalArgumentException exception) {
            throw invalidPathException(trimmedPath, rowIndex,
                    "Verwenden Sie einen Datenpfad wie person.name, personen[0].name oder personen[*].name.");
        }
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
                        "Der Speicherort %s in Regel %d kann nicht gelesen werden. Für das Feld %s wird unter %s ein Objekt erwartet.",
                        StringUtils.quote(configuredPath),
                        rowIndex,
                        StringUtils.quote(objectPathPart.key()),
                        StringUtils.quote(currentPath)
                );
            }

            if (!currentMap.containsKey(objectPathPart.key())) {
                throw new ProcessNodeExecutionExceptionMissingValue(
                        "Der Speicherort %s aus Regel %d ist nicht vollständig vorhanden. Das Feld %s fehlt unter %s.",
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
                    "Der Speicherort %s aus Regel %d kann nicht gelesen werden. Unter %s wird eine Liste erwartet.",
                    StringUtils.quote(configuredPath),
                    rowIndex,
                    StringUtils.quote(currentPath)
            );
        }

        if (currentPart instanceof IndexPathPart index && index.index() >= currentList.size()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Speicherort %s aus Regel %d ist nicht vorhanden. Ein Eintrag mit dem Index %d fehlt unter %s.",
                    StringUtils.quote(configuredPath), rowIndex, index.index(), StringUtils.quote(currentPath)
            );
        }
        var firstIndex = currentPart instanceof IndexPathPart index ? index.index() : 0;
        var endIndex = currentPart instanceof IndexPathPart ? firstIndex + 1 : currentList.size();
        for (int i = firstIndex; i < endIndex; i++) {
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
                "Regel %d: Der Datentyp %s wird nicht unterstützt. Wählen Sie einen angebotenen Datentyp.",
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
                "Regel %d: Der Wert unter %s hat nicht den erwarteten Datentyp. Erwartet: %s; vorhanden: %s.",
                rowIndex,
                StringUtils.quote(resolvedPath),
                StringUtils.quote(typeLabel(expectedType)),
                StringUtils.quote(typeLabel(describeType(value)))
        );
    }

    @Nonnull
    private static ProcessNodeExecutionExceptionInvalidConfiguration invalidPathException(@Nonnull String path,
                                                                                          int rowIndex,
                                                                                          @Nonnull String detail) {
        return new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Regel %d: Der Speicherort ist ungültig. %s Angegebener Pfad: %s",
                rowIndex,
                detail,
                StringUtils.quote(path)
        );
    }

    @Nonnull
    private static String typeLabel(@Nonnull String type) {
        return switch (type) {
            case TYPE_ANY -> "Beliebig (nur Vorhandensein)";
            case TYPE_STRING -> "Text";
            case TYPE_NUMBER -> "Zahl";
            case TYPE_BOOLEAN -> "Ja/Nein";
            case TYPE_OBJECT -> "Objekt";
            case TYPE_ARRAY -> "Liste";
            case TYPE_NULL -> "Nullwert (null)";
            default -> type;
        };
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

    private sealed interface PathPart permits ObjectPathPart, IndexPathPart, WildcardPathPart {
    }

    private record ObjectPathPart(@Nonnull String key) implements PathPart {
    }

    private record IndexPathPart(int index) implements PathPart {
    }

    private record WildcardPathPart() implements PathPart {
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class DataTypeValidationControlNodeConfig {
        public List<DataTypeValidationRuleConfig> rules;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(
            id = RULES_FIELD_ID,
            allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode},
            properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Prüfregeln"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Legen Sie fest, welche Vorgangsdaten vorhanden sein sollen und welche Datentypen Sie erwarten. Jede Regel enthält einen Speicherort und einen erwarteten Datentyp."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "#. Regel"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Regel hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Regel entfernen")
    })
    public static class DataTypeValidationRuleConfig {
        @InputElementPOJOBinding(id = RULE_PATH_FIELD_ID, type = ElementType.Text,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Speicherort des Werts"),
                @ElementPOJOBindingProperty(key = "prefix", strValue = "$."),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie den Datenpfad ohne den bereits angezeigten Präfix $. ein, zum Beispiel person.name. personen[0].name prüft den ersten Listeneintrag; personen[*].name die vorhandenen Listenpositionen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 8.0)
        })
        public String path;

        @InputElementPOJOBinding(id = RULE_TYPE_FIELD_ID, type = ElementType.Select,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Erwarteter Datentyp"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie den Typ, den der Wert haben soll. „Beliebig (nur Vorhandensein)“ prüft nur, ob der Speicherort vorhanden ist; auch ein ausdrücklich gesetzter Nullwert zählt als vorhanden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 4.0)
        })
        public String expectedType;
    }
}
