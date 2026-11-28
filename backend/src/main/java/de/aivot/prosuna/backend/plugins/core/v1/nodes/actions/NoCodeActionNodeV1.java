package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.ValueFunctionType;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.ElementValueFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NoCodeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NoCodeInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;
import de.aivot.prosuna.backend.utils.MapUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.list;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.map;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;

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
    private static final String OUTPUT_VARIABLES_TYPE_DEFINITION =
            "Array<{ rowIndex: number; configuredPath: string; resolvedPath: string; wildcardIndices: Array<number>; " +
                    "targetType: \"any\" | \"string\" | \"number\" | \"boolean\" | \"date\" | \"time\" | \"datetime\"; value: unknown; }>";

    private static final String TARGET_TYPE_ANY = "any";
    private static final String TARGET_TYPE_STRING = "string";
    private static final String TARGET_TYPE_NUMBER = "number";
    private static final String TARGET_TYPE_BOOLEAN = "boolean";
    private static final String TARGET_TYPE_DATE = "date";
    private static final String TARGET_TYPE_TIME = "time";
    private static final String TARGET_TYPE_DATETIME = "datetime";
    private static final DateTimeFormatter LOCAL_TIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final NoCodeEvaluationService noCodeEvaluationService;

    public NoCodeActionNodeV1(NoCodeEvaluationService noCodeEvaluationService) {
        this.noCodeEvaluationService = noCodeEvaluationService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<NoCodeActionNodeV1.NoCodeActionNodeConfiguration> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var markdown = new StringBuilder("Die No-Code-Verarbeitung wurde abgeschlossen.");
        markdown.append(detail("Gespeicherte Ergebnisse", summary.data(OUTPUT_NAME_VARIABLE_COUNT)));
        for (var entry : list(summary.data(OUTPUT_NAME_VARIABLES))) {
            var result = map(entry);
            markdown.append("\n\n- Berechnung ").append(text(result.get("rowIndex")))
                    .append(": ").append(text(result.get("resolvedPath")));
        }
        return markdown.append(detail("Kurzbeschreibung", context.thisNode().getDescription())).toString();
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
        return "No-Code ausführen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Berechnet Werte mit No-Code-Ausdrücken und speichert die Ergebnisse direkt in den Vorgangsdaten.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Mit diesem Prozesselement berechnen oder übernehmen Sie Werte automatisch, zum Beispiel einen Gesamtbetrag, eine zusammengesetzte Bezeichnung oder ein Datum. Stellen Sie die benötigten No-Code-Ausdrücke mit den verfügbaren Operatoren zusammen.

                Sie können mehrere Berechnungen in einem Schritt ausführen und ihre Ergebnisse für die weitere Bearbeitung bereitstellen.

                # Was richten Sie ein?

                Legen Sie unter **„Berechnungen“** die benötigten Einträge an. Für jeden Eintrag geben Sie den **„Speicherort des Ergebnisses“**, das **„Ergebnisformat“** und einen **„No-Code-Ausdruck“** an.

                Ein Speicherort ist ein Datenschlüssel wie `berechnung.gesamtbetrag` oder `person.bezeichnung`. Mit einem konkreten Listenindex wie `positionen[0].betrag` können Sie einen bestimmten Eintrag beschreiben. Der Präfix `$.` kennzeichnet den Zugriff auf die Vorgangsdatenebene und wird in der Oberfläche bereits angezeigt. Geben Sie den Datenschlüssel ohne diesen Präfix ein.

                Mit **„Beliebig (ohne Umwandlung)“** bleibt das berechnete Ergebnis in seinem bisherigen Format. Alternativ können Sie es in **„Text“**, **„Zahl“**, **„Ja/Nein“**, **„Datum“**, **„Uhrzeit“** oder **„Datum und Uhrzeit“** umwandeln lassen.

                Ordnen Sie die Berechnungen in der gewünschten Reihenfolge an. Eine spätere Berechnung kann ein früheres Ergebnis über dessen Speicherort in den Vorgangsdaten verwenden.

                # Was passiert bei der Ausführung?

                Prosuna verarbeitet die Berechnungen automatisch von oben nach unten. Der Ausdruck wird ausgewertet, das Ergebnis bei Bedarf umgewandelt und anschließend am festgelegten Speicherort eingetragen.

                Ein vorhandener Wert an diesem Speicherort wird ersetzt. Wenn mehrere Berechnungen denselben Speicherort verwenden, bleibt nach dem Abschluss der zuletzt eingetragene Wert bestehen.

                Kann ein Ausdruck nicht ausgewertet, ein Ergebnis nicht umgewandelt oder nicht gespeichert werden, schlägt die Aufgabe fehl. Die Ergebnisse werden dann nicht als erfolgreicher Teilabschluss an den nächsten Schritt übergeben.

                Nach erfolgreichem Abschluss wird der Vorgang über **„Berechnung abgeschlossen“** fortgesetzt.

                # Welches Ergebnis steht danach bereit?

                Die Ergebnisse stehen direkt an den festgelegten Speicherorten in den Vorgangsdaten bereit. Eine zusätzliche Ausgangsdatenzuordnung ist dafür nicht erforderlich.

                In den Elementdaten werden außerdem die Ergebnisdaten je Berechnung und die Anzahl gespeicherter Ergebnisse festgehalten. Die Anzahl bezeichnet Schreibvorgänge; derselbe Speicherort kann mehrfach gezählt werden.

                Die Zusammenfassung im Vorgangsverlauf nennt die Anzahl und die Speicherorte der Ergebnisse. Eine hinterlegte Kurzbeschreibung wird ebenfalls angezeigt. Die gespeicherten Werte können Sie in den Elementdaten und die Unterschiede zwischen den Vorgangsdatenständen unter **„Datenänderungen“** ansehen.

                # Wie sieht ein Beispiel aus?

                Aus der Anzahl `3` und dem Einzelpreis `10` soll zunächst ein Betrag berechnet werden. Stellen Sie dafür einen Ausdruck zur Multiplikation zusammen, wählen Sie **„Zahl“** als Ergebnisformat und `berechnung.betrag` als Speicherort. Das Ergebnis ist `30`.

                Eine zweite Berechnung liest `berechnung.betrag`, addiert Versandkosten von `5` und speichert `35` unter `berechnung.gesamtbetrag`. Stellen Sie diese Berechnung hinter die erste, damit der Betrag bereits zur Verfügung steht.

                # Was ist zu beachten?

                Ein Ergebnisformat wandelt einen Wert um, verlangt aber keinen gefüllten Wert. Ergibt der Ausdruck `null`, bleibt das Ergebnis auch bei **„Zahl“** oder **„Datum“** `null`.

                Ein bestehender Zielwert wird überschrieben. Wenn Sie ein ganzes Objekt oder eine Liste als Ergebnis übernehmen, ersetzen Sie damit die bisherige Struktur an diesem Speicherort.

                Die Ergebnisumwandlung verwendet technische Formate: Zahltext benötigt einen Dezimalpunkt, zum Beispiel `1.5`. Für Datumsangaben verwenden Sie zum Beispiel `2026-11-27`, für Uhrzeiten `09:30` oder `09:30:00`. Zeitstempel werden anhand der Zeitzone der Anwendung verarbeitet.

                Übernehmen Sie Objekte und Listen mit **„Beliebig (ohne Umwandlung)“**, wenn ihre Struktur erhalten bleiben soll. **„Text“** erzeugt daraus eine technische Textdarstellung und keine JSON-Ausgabe.

                Die Reihenfolge ist Teil Ihrer Berechnung. Ein mehrfach verwendeter Speicherort kann einen bereits berechneten Wert ersetzen; die Anzahl gespeicherter Ergebnisse sagt nicht aus, wie viele unterschiedliche Werte sich verändert haben.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(NoCodeActionNodeConfiguration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Einstellungen für die No-Code-Berechnung konnten nicht geladen werden.");
        }

        layout
                .findChild(VARIABLE_TARGET_TYPE_FIELD_ID, SelectInputElement.class)
                .ifPresent(variableTargetTypeInput -> {
                    variableTargetTypeInput.setValue(new ElementValueFunctions()
                            .setType(ValueFunctionType.NoCode)
                            .setNoCode(new NoCodeStaticValue(TARGET_TYPE_ANY)));
                    variableTargetTypeInput.setOptions(List.of(
                            SelectInputElementOption.of(TARGET_TYPE_ANY, formatLabel(TARGET_TYPE_ANY)),
                            SelectInputElementOption.of(TARGET_TYPE_STRING, formatLabel(TARGET_TYPE_STRING)),
                            SelectInputElementOption.of(TARGET_TYPE_NUMBER, formatLabel(TARGET_TYPE_NUMBER)),
                            SelectInputElementOption.of(TARGET_TYPE_BOOLEAN, formatLabel(TARGET_TYPE_BOOLEAN)),
                            SelectInputElementOption.of(TARGET_TYPE_DATE, formatLabel(TARGET_TYPE_DATE)),
                            SelectInputElementOption.of(TARGET_TYPE_TIME, formatLabel(TARGET_TYPE_TIME)),
                            SelectInputElementOption.of(TARGET_TYPE_DATETIME, formatLabel(TARGET_TYPE_DATETIME))
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
    public List<ProcessNodePort> getPorts(@Nonnull NoCodeActionNodeConfiguration configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Berechnung abgeschlossen",
                        "Der Vorgang wird hier fortgesetzt, nachdem die No-Code-Verarbeitung erfolgreich abgeschlossen und die Ergebnisse übernommen wurden."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_VARIABLES,
                        "Berechnete Ergebnisse",
                        "Ergebnisdaten je Schreibvorgang mit Berechnungsnummer, konfiguriertem und tatsächlichem Speicherort, Ergebnisformat und Wert. Bei aufgelösten Listenpositionen sind die zugehörigen Indizes enthalten.",
                        OUTPUT_VARIABLES_TYPE_DEFINITION
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_VARIABLE_COUNT,
                        "Anzahl gespeicherter Ergebnisse",
                        "Anzahl der geschriebenen Ergebnisse. Mehrfach beschriebene Speicherorte zählen mehrfach; die Anzahl ist kein Änderungsvergleich.",
                        "number"
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

    // TODO: Add authoring checks through validateConfiguration(context) for unique variables,
    //       No-Code expression syntax and compatible types.

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<NoCodeActionNodeConfiguration> context) throws ProcessNodeExecutionException {
        var sourceRoot = context.getCurrentProcessExecutionData().get("$");
        if (!(sourceRoot instanceof Map<?, ?>)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Vorgangsdaten haben ein ungültiges Format. Für die No-Code-Berechnung wird am Wurzelpfad ($) ein Objekt erwartet."
            );
        }

        var workingExecutionData = ProcessExecutionData.of(MapUtils.deepCopy(context.getCurrentProcessExecutionData()));
        var variableDefinitions = parseVariableDefinitions(context.getConfigurationOfExecutingNode());
        var variableValues = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < variableDefinitions.size(); i++) {
            var definition = variableDefinitions.get(i);
            var rowIndex = i + 1;

            var targetPath = normalizeDestinationKey(definition.name(), rowIndex, "Speicherort des Ergebnisses");
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
                        "Berechnung %d: Geben Sie den Speicherort des Ergebnisses an.",
                        result.size() + 1
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
                        "Berechnung %d: Legen Sie einen No-Code-Ausdruck fest.",
                        result.size() + 1
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
                    "Berechnung %d: Der No-Code-Ausdruck konnte nicht ausgewertet werden: %s",
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
                    "Das Feld %s in Berechnung %d benötigt einen Speicherort.",
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
                    "Der Speicherort muss in den Vorgangsdaten liegen. Ein führendes $. ist zulässig, andere $-Präfixe nicht."
            );
        }

        if ("_".equals(normalizedPath) || normalizedPath.startsWith("_.")) {
            throw invalidPathException(
                    rawPath,
                    rowIndex,
                    fieldLabel,
                    "Geben Sie einen Speicherort innerhalb der Vorgangsdaten an."
            );
        }

        try {
            return ProcessDataValueUtils.formatDestinationKeySegments(
                    ProcessDataValueUtils.parseDestinationKeySegments(normalizedPath, false));
        } catch (IllegalArgumentException e) {
            throw invalidPathException(
                    rawPath,
                    rowIndex,
                    fieldLabel,
                    "Verwenden Sie einen Datenpfad wie person.name oder positionen[0].betrag."
            );
        }

    }

    @Nonnull
    private static ProcessNodeExecutionExceptionInvalidConfiguration invalidPathException(@Nullable String path,
                                                                                          int rowIndex,
                                                                                          @Nonnull String fieldLabel,
                                                                                          @Nonnull String detail) {
        return new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Der Speicherort im Feld %s in Berechnung %d ist ungültig. %s Angegebener Pfad: %s",
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
                    "Berechnung %d: Das Ergebnis konnte nicht am Speicherort übernommen werden: %s",
                    rowIndex,
                    e.getMessage()
            );
        }
    }

    private static void validateTargetType(@Nonnull String targetType,
                                           int rowIndex) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (
                TARGET_TYPE_ANY.equals(targetType) ||
                        TARGET_TYPE_STRING.equals(targetType) ||
                        TARGET_TYPE_NUMBER.equals(targetType) ||
                        TARGET_TYPE_BOOLEAN.equals(targetType) ||
                        TARGET_TYPE_DATE.equals(targetType) ||
                        TARGET_TYPE_TIME.equals(targetType) ||
                        TARGET_TYPE_DATETIME.equals(targetType)
        ) {
            return;
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Berechnung %d: Das Ergebnisformat %s wird nicht unterstützt. Wählen Sie ein angebotenes Format.",
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
                case TARGET_TYPE_STRING -> castToString(value);
                case TARGET_TYPE_NUMBER -> castToNumber(value);
                case TARGET_TYPE_BOOLEAN -> castToBoolean(value);
                case TARGET_TYPE_DATE -> castToDate(value);
                case TARGET_TYPE_TIME -> castToTime(value);
                case TARGET_TYPE_DATETIME -> castToDateTime(value);
                default -> value;
            };
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Berechnung %d: Das Ergebnis konnte nicht in das Format %s umgewandelt werden. %s",
                    rowIndex,
                    StringUtils.quote(formatLabel(targetType)),
                    e.getMessage()
            );
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Berechnung %d: Das Ergebnis konnte nicht in das Format %s umgewandelt werden.",
                    rowIndex,
                    StringUtils.quote(formatLabel(targetType))
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
                        "Ein leerer Text kann nicht in eine Zahl umgewandelt werden."
                );
            }
            return new BigDecimal(trimmed);
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ein Wert vom Typ %s kann nicht in das Format %s umgewandelt werden.",
                StringUtils.quote(valueTypeLabel(value)),
                StringUtils.quote(formatLabel(TARGET_TYPE_NUMBER))
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
                        "Der Text %s kann nicht in einen Ja/Nein-Wert umgewandelt werden.",
                        StringUtils.quote(s)
                );
            };
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ein Wert vom Typ %s kann nicht in das Format %s umgewandelt werden.",
                StringUtils.quote(valueTypeLabel(value)),
                StringUtils.quote(formatLabel(TARGET_TYPE_BOOLEAN))
        );
    }

    @Nonnull
    private static String castToString(
            Object value
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (value instanceof Instant instant) {
            return IsoTimestampUtils.toOffsetString(instant);
        }
        if (value instanceof OffsetDateTime dateTime) {
            return IsoTimestampUtils.toOffsetString(dateTime.toInstant());
        }
        if (value instanceof ZonedDateTime dateTime) {
            return IsoTimestampUtils.toOffsetString(dateTime.toInstant());
        }
        if (value instanceof LocalDateTime dateTime) {
            return IsoTimestampUtils.toOffsetString(resolveLocalDateTime(dateTime));
        }
        if (value instanceof LocalTime time) {
            return formatLocalTime(time);
        }
        return String.valueOf(value);
    }

    @Nonnull
    private static String castToTime(Object value) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (value instanceof LocalTime time) {
            return formatLocalTime(time);
        }
        if (value instanceof LocalDateTime dateTime) {
            return formatLocalTime(dateTime.toLocalTime());
        }
        if (value instanceof Instant instant) {
            return formatLocalTime(
                    instant.atZone(ApplicationTimeZone.getZoneId()).toLocalTime()
            );
        }
        if (value instanceof OffsetDateTime dateTime) {
            return formatLocalTime(
                    dateTime.toInstant()
                            .atZone(ApplicationTimeZone.getZoneId())
                            .toLocalTime()
            );
        }
        if (value instanceof ZonedDateTime dateTime) {
            return formatLocalTime(
                    dateTime.toInstant()
                            .atZone(ApplicationTimeZone.getZoneId())
                            .toLocalTime()
            );
        }
        if (value instanceof String stringValue) {
            var trimmed = stringValue.trim();
            if (trimmed.matches("^\\d{2}:\\d{2}(?::\\d{2})?$")) {
                try {
                    return formatLocalTime(LocalTime.parse(trimmed));
                } catch (Exception ignored) {
                    // Fall through to the configuration error below.
                }
            }
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ein Wert vom Typ %s kann nicht in das Format %s umgewandelt werden.",
                StringUtils.quote(valueTypeLabel(value)),
                StringUtils.quote(formatLabel(TARGET_TYPE_TIME))
        );
    }

    @Nonnull
    private static String formatLocalTime(@Nonnull LocalTime value) {
        // LocalTime.toString() omits zero seconds. Process data uses the canonical
        // HH:mm:ss form regardless of the precision of the originating input control.
        return value.withNano(0).format(LOCAL_TIME_SECONDS_FORMATTER);
    }

    @Nonnull
    private static String castToDate(Object value) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof YearMonth yearMonth) {
            return yearMonth.toString();
        }
        if (value instanceof Year year) {
            return year.toString();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ApplicationTimeZone.getZoneId()).toLocalDate().toString();
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalDate().toString();
        }
        if (value instanceof ZonedDateTime dateTime) {
            return dateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalDate().toString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate().toString();
        }
        if (value instanceof String s) {
            var trimmed = s.trim();
            if (trimmed.isEmpty()) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Ein leerer Text kann nicht in ein Datum umgewandelt werden."
                );
            }

            try {
                return LocalDate.parse(trimmed).toString();
            } catch (Exception ignored) {
                try {
                    return YearMonth.parse(trimmed).toString();
                } catch (Exception ignoredMonth) {
                    try {
                        return Year.parse(trimmed).toString();
                    } catch (Exception ignoredYear) {
                        try {
                            return IsoTimestampUtils
                                    .parseIsoInstant(trimmed)
                                    .atZone(ApplicationTimeZone.getZoneId())
                                    .toLocalDate()
                                    .toString();
                        } catch (Exception ignoredInstant) {
                            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                                    "Der Text %s kann nicht in ein Datum umgewandelt werden.",
                                    StringUtils.quote(s)
                            );
                        }
                    }
                }
            }
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ein Wert vom Typ %s kann nicht in das Format %s umgewandelt werden.",
                StringUtils.quote(valueTypeLabel(value)),
                StringUtils.quote(formatLabel(TARGET_TYPE_DATE))
        );
    }

    @Nonnull
    private static String castToDateTime(Object value) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (value instanceof Instant instant) {
            return IsoTimestampUtils.toOffsetString(instant);
        }
        if (value instanceof OffsetDateTime dateTime) {
            return IsoTimestampUtils.toOffsetString(dateTime.toInstant());
        }
        if (value instanceof ZonedDateTime dateTime) {
            return IsoTimestampUtils.toOffsetString(dateTime.toInstant());
        }
        if (value instanceof LocalDateTime dateTime) {
            return IsoTimestampUtils.toOffsetString(resolveLocalDateTime(dateTime));
        }
        if (value instanceof LocalDate date) {
            return IsoTimestampUtils.toOffsetString(resolveLocalDateTime(date.atStartOfDay()));
        }
        if (value instanceof String s) {
            var trimmed = s.trim();
            if (trimmed.isEmpty()) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Ein leerer Text kann nicht in Datum und Uhrzeit umgewandelt werden."
                );
            }

            try {
                return IsoTimestampUtils.toOffsetString(IsoTimestampUtils.parseIsoInstant(trimmed));
            } catch (Exception ignored) {
                try {
                    return IsoTimestampUtils.toOffsetString(
                            resolveLocalDateTime(LocalDate.parse(trimmed).atStartOfDay())
                    );
                } catch (Exception ignored2) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Der Text %s kann nicht in Datum und Uhrzeit umgewandelt werden.",
                            StringUtils.quote(s)
                    );
                }
            }
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ein Wert vom Typ %s kann nicht in das Format %s umgewandelt werden.",
                StringUtils.quote(valueTypeLabel(value)),
                StringUtils.quote(formatLabel(TARGET_TYPE_DATETIME))
        );
    }

    @Nonnull
    private static Instant resolveLocalDateTime(
            @Nonnull LocalDateTime value
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var zoneId = ApplicationTimeZone.getZoneId();
        var validOffsets = zoneId.getRules().getValidOffsets(value);
        // LocalDateTime.atZone would silently move a nonexistent DST-gap value
        // forward. Action output must reject it rather than change user-entered fields.
        if (validOffsets.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die lokale Zeit %s existiert in der Zeitzone der Anwendung (%s) nicht.",
                    StringUtils.quote(value.toString()),
                    StringUtils.quote(zoneId.getId())
            );
        }

        // ZoneRules lists the earlier offset first during a DST overlap.
        return value.atOffset(validOffsets.getFirst()).toInstant();
    }

    @Nonnull
    private static String formatLabel(@Nonnull String format) {
        return switch (format) {
            case TARGET_TYPE_ANY -> "Beliebig (ohne Umwandlung)";
            case TARGET_TYPE_STRING -> "Text";
            case TARGET_TYPE_NUMBER -> "Zahl";
            case TARGET_TYPE_BOOLEAN -> "Ja/Nein";
            case TARGET_TYPE_DATE -> "Datum";
            case TARGET_TYPE_TIME -> "Uhrzeit";
            case TARGET_TYPE_DATETIME -> "Datum und Uhrzeit";
            default -> format;
        };
    }

    @Nonnull
    private static String valueTypeLabel(@Nonnull Object value) {
        if (value instanceof String) {
            return formatLabel(TARGET_TYPE_STRING);
        }
        if (value instanceof Number) {
            return formatLabel(TARGET_TYPE_NUMBER);
        }
        if (value instanceof Boolean) {
            return formatLabel(TARGET_TYPE_BOOLEAN);
        }
        if (value instanceof Map<?, ?>) {
            return "Objekt";
        }
        if (value instanceof List<?>) {
            return "Liste";
        }
        if (value instanceof LocalDate || value instanceof YearMonth || value instanceof Year) {
            return formatLabel(TARGET_TYPE_DATE);
        }
        if (value instanceof LocalTime) {
            return formatLabel(TARGET_TYPE_TIME);
        }
        if (value instanceof Instant || value instanceof OffsetDateTime
                || value instanceof ZonedDateTime || value instanceof LocalDateTime) {
            return formatLabel(TARGET_TYPE_DATETIME);
        }
        return value.getClass().getSimpleName();
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
            @ElementPOJOBindingProperty(key = "label", strValue = "Berechnungen"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Legen Sie für jede Berechnung einen No-Code-Ausdruck, das Ergebnisformat und den Speicherort fest. Die Berechnungen werden von oben nach unten ausgeführt."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "#. Berechnung"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Berechnung hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Berechnung entfernen"),
            @ElementPOJOBindingProperty(key = "sortable", boolValue = true)
    })
    public static class NoCodeActionNodeVariableConfiguration {
        @InputElementPOJOBinding(id = VARIABLE_NAME_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Speicherort des Ergebnisses"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Datenschlüssel, unter dem das Ergebnis in den Vorgangsdaten gespeichert wird, zum Beispiel berechnung.gesamtbetrag oder positionen[0].betrag. Der bereits angezeigte Präfix $. steht für die Vorgangsdatenebene. Ein vorhandener Wert an diesem Speicherort wird ersetzt."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 8.0),
                @ElementPOJOBindingProperty(key = "disableWildCards", boolValue = true)
        })
        public String name;

        @InputElementPOJOBinding(id = VARIABLE_TARGET_TYPE_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Ergebnisformat"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie, ob das Ergebnis unverändert übernommen oder in das gewählte Format umgewandelt werden soll. Ein Nullwert bleibt bei jedem Format ein Nullwert."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 4.0)
        })
        public String targetType;

        @InputElementPOJOBinding(id = VARIABLE_EXPRESSION_FIELD_ID, type = ElementType.NoCodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "No-Code-Ausdruck"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Stellen Sie den Ausdruck für diese Berechnung zusammen. Ergebnisse vorheriger Berechnungen können Sie über ihren Speicherort in den Vorgangsdaten verwenden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public NoCodeInputElementItem expression;
    }
}
