package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessDataKeyInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.bool.NoCodeNotOperator;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.utils.MapUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.*;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.map;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.list;

@Component
public class DataMappingActionNodeV1 implements ProcessNodeDefinition<DataMappingActionNodeV1.DataMappingActionNodeV1Config> {
    public static final String NODE_KEY = "data_mapping";

    private static final String PORT_NAME = "output";

    private record RuleExecutionResult(Object sourceValue,
                                       Object mappedValue) {
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<DataMappingActionNodeV1.DataMappingActionNodeV1Config> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var ruleCount = summary.data("mappedRuleCount");
        var singular = ruleCount instanceof Number count && count.longValue() == 1;
        var markdown = new StringBuilder((singular ? "Es wurde " : "Es wurden ") + text(ruleCount)
                + (singular ? " Regel" : " Regeln") + " zur Bearbeitung der Vorgangsdaten ausgeführt.");
        for (var entry : list(summary.data("mappedValues"))) {
            var rule = map(entry);
            var action = Boolean.TRUE.equals(rule.get("deleteOnly")) ? "Löschregel"
                    : Boolean.TRUE.equals(rule.get("cleanupSource")) ? "Verschieberegel" : "Kopierregel";
            markdown.append("\n\n- ").append(action).append(": ").append(text(rule.get("originalPath")));
            if (!Boolean.TRUE.equals(rule.get("deleteOnly"))) {
                markdown.append(" → ").append(text(rule.get("newPath")));
            }
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
        return "Datenfelder kopieren, verschieben oder löschen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Kopiert, verschiebt oder löscht ausgewählte Vorgangsdaten automatisch nach festgelegten Regeln.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Mit diesem Prozesselement ordnen Sie vorhandene Vorgangsdaten für die weitere Bearbeitung neu an. Sie können Werte an einen anderen Speicherort kopieren, dorthin verschieben oder vollständig entfernen. Dafür ist kein eigenes Skript nötig.

                Das Element arbeitet innerhalb der Vorgangsdaten. Für eine manuelle Eingabe oder Korrektur durch Mitarbeiter:innen verwenden Sie „Vorgangsdaten bearbeiten“.

                # Was richten Sie ein?

                Legen Sie mindestens eine **„Regel für die Datenbearbeitung“** an. Der **„Bisherige Speicherort“** ist der Ausgangspfad des Werts, zum Beispiel `person.name`. Für das Kopieren oder Verschieben geben Sie außerdem den **„Neuen Speicherort“** als Zielpfad an, zum Beispiel `kontakt.name`.

                - **Kopieren:** Lassen Sie **„Wert am bisherigen Speicherort entfernen“** ausgeschaltet. Der Ausgangswert bleibt bestehen.
                - **Verschieben:** Aktivieren Sie **„Wert am bisherigen Speicherort entfernen“** und geben Sie den neuen Speicherort an.
                - **Löschen:** Aktivieren Sie zusätzlich **„Nur löschen“**. Ein neuer Speicherort ist dann nicht erforderlich.

                Bringen Sie die Regeln in die gewünschte Reihenfolge. Bei Bedarf aktivieren Sie **„Leere übergeordnete Objekte und Listen entfernen“**.

                # Was passiert bei der Ausführung?

                Prosuna führt die Regeln automatisch von oben nach unten aus. Jede Regel verwendet den Datenstand, den die vorherige Regel hinterlassen hat. Bereits vorhandene Werte am neuen Speicherort werden ersetzt.

                Bei Listen können Sie einen einzelnen Eintrag mit `[0]`, `[1]` und weiteren Indizes auswählen. Mit `[*]` verarbeiten Sie alle vorhandenen Listenpositionen. Für das Kopieren und Verschieben müssen Ausgangs- und Zielpfad dieselbe Anzahl solcher Platzhalter enthalten.

                Nach erfolgreicher Ausführung aller Regeln wird der Vorgang über **„Datenfelder verarbeitet“** fortgesetzt. Die Aufgabe wartet auf keine manuelle Bestätigung.

                # Welches Ergebnis steht danach bereit?

                Das Ergebnis ist der neue Stand der Vorgangsdaten. Beim Löschen wird der betreffende Datenschlüssel entfernt. Das unterscheidet sich vom Leeren eines Felds, bei dem der Schlüssel mit einem leeren Wert erhalten bleiben kann.

                In den Elementdaten werden die Anzahl ausgeführter Regeln und Angaben zu jeder Regel gespeichert, darunter die beteiligten Pfade und zugeordnete Werte. Die Anzahl der Regeln sagt nicht aus, wie viele Datenfelder tatsächlich verändert wurden.

                Der Vorgangsverlauf zeigt die ausgeführten Kopier-, Verschiebe- und Löschregeln. Unter **„Datenänderungen“** können Sie die gespeicherten Datenstände vergleichen, sofern der vorherige Stand verfügbar ist.

                # Wie sieht ein Beispiel aus?

                Der Name einer Person steht unter `person.name` und soll zusätzlich unter `kontakt.name` bereitstehen. Legen Sie eine Regel mit dem bisherigen Speicherort `person.name` und dem neuen Speicherort `kontakt.name` an. Lassen Sie **„Wert am bisherigen Speicherort entfernen“** ausgeschaltet. Nach der Ausführung steht der Name an beiden Speicherorten.

                Soll der Name nur noch unter `kontakt.name` stehen, aktivieren Sie die Entfernung am bisherigen Speicherort. Soll er vollständig entfallen, aktivieren Sie zusätzlich **„Nur löschen“** und lassen den neuen Speicherort weg.

                # Was ist zu beachten?

                Fehlt ein Wert an einem konkreten Ausgangspfad, wird beim Kopieren oder Verschieben am Zielpfad `null` gespeichert. Ein dort bereits vorhandener Wert wird damit überschrieben. Gibt es bei einer Listenregel keine passenden Listenpositionen, findet keine Übertragung statt.

                Das Löschen eines Listeneintrags verändert die Positionen der nachfolgenden Einträge. Die Bereinigung leerer übergeordneter Objekte und Listen kann weitere leere Strukturen entfernen. Prüfen Sie deshalb die Reihenfolge Ihrer Regeln.

                Eine Regel kann ohne Änderung der Daten ausgeführt werden, etwa beim Löschen eines bereits fehlenden Werts. Die Zusammenfassung bestätigt die ausgeführten Regeln; die tatsächlichen Unterschiede zeigt der Datenvergleich.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layoutElement;
        try {
            layoutElement = ElementPOJOMapper.createFromPOJO(DataMappingActionNodeV1Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Einstellungen für die Datenbearbeitung konnten nicht geladen werden.");
        }

        layoutElement
                .findChild(DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.DELETE_ONLY_FIELD_ID, CheckboxInputElement.class)
                .ifPresent(field -> field.setVisibility(
                        ElementVisibilityFunctions
                                .of(NoCodeReference.of(DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.CLEANUP_SOURCE_FIELD_ID))
                                .recalculateReferencedIds()
                ));

        layoutElement
                .findChild(DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.TARGET_FIELD_ID, ProcessDataKeyInputElement.class)
                .ifPresent(field -> field.setVisibility(
                        ElementVisibilityFunctions
                                .of(NoCodeExpression.of(
                                        NoCodeNotOperator.OPERATOR_ID,
                                        NoCodeReference.of(DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule.DELETE_ONLY_FIELD_ID)
                                ))
                                .recalculateReferencedIds()
                ));

        return layoutElement;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull DataMappingActionNodeV1Config configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Datenfelder verarbeitet",
                        "Der Vorgang wird hier fortgesetzt, nachdem alle Regeln für die Datenbearbeitung erfolgreich ausgeführt wurden."
                )
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull DataMappingActionNodeV1Config configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        if (configuration.rules == null) {
            return previousMetadata;
        }

        var metadata = ProcessNodeDefinitionMetadata.reuse(previousMetadata);
        for (var rule : configuration.rules) {
            if (rule == null || Boolean.TRUE.equals(rule.deleteOnly)) {
                continue;
            }

            var targetPath = StringUtils.toNullableTrimmedString(rule.target);
            if (targetPath == null) {
                continue;
            }

            metadata.addForwardedProcessDataKey(
                    targetPath,
                    targetPath,
                    null,
                    processNodeEntity
            );
        }
        return metadata;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<DataMappingActionNodeV1Config> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        if (configuration.rules == null || configuration.rules.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Fügen Sie mindestens eine Regel für die Datenbearbeitung hinzu."
            );
        }

        var workingExecutionData = ProcessExecutionData.of(MapUtils.deepCopy(context.getCurrentProcessExecutionData()));
        var mappedValues = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < configuration.rules.size(); i++) {
            var rule = configuration.rules.get(i);
            var rowIndex = i + 1;

            if (rule == null) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Regel %d enthält keine gültigen Einstellungen. Prüfen Sie diese Regel.",
                        rowIndex
                );
            }

            try {
                var ruleResult = executeRule(
                        workingExecutionData,
                        rule,
                        Boolean.TRUE.equals(configuration.cleanupEmptyContainers)
                );
                mappedValues.add(createMappedValueEntry(rule, ruleResult.sourceValue(), ruleResult.mappedValue()));
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        e,
                        "Regel %d konnte nicht ausgeführt werden: %s",
                        rowIndex,
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
                );
            } catch (Exception e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Regel %d konnte wegen eines internen Fehlers nicht ausgeführt werden.",
                        rowIndex
                );
            }
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setProcessData(workingExecutionData.getProcessData())
                .setNodeData(Map.of(
                        "mappedRuleCount", configuration.rules.size(),
                        "mappedValues", mappedValues
                ));
    }

    @Nonnull
    private RuleExecutionResult executeRule(@Nonnull ProcessExecutionData workingExecutionData,
                                            @Nonnull DataMappingActionNodeV1Config.DataMappingActionNodeV1Rule rule,
                                            boolean cleanupEmptyContainers) {
        var sourcePath = rule.source;
        var targetPath = rule.target;
        var deleteOnly = Boolean.TRUE.equals(rule.deleteOnly);
        var cleanupSource = Boolean.TRUE.equals(rule.cleanupSource);
        var sourceHasWildcard = ProcessDataValueUtils.hasWildcardSegment(sourcePath, false);

        if (sourceHasWildcard) {
            var sourceMatches = ProcessDataValueUtils.resolveMatchingProcessDataValues(workingExecutionData, sourcePath);
            var sourceValue = sourceMatches.stream()
                    .map(ProcessDataValueUtils.ResolvedProcessDataValue::value)
                    .toList();

            if (deleteOnly) {
                cleanupSourceValue(workingExecutionData, sourcePath, targetPath, cleanupSource, true, cleanupEmptyContainers);
                return new RuleExecutionResult(sourceValue, null);
            }

            var mappedValues = new ArrayList<Object>(sourceMatches.size());
            for (var sourceMatch : sourceMatches) {
                var mappedValue = sourceMatch.value();
                ProcessDataValueUtils.writeProcessDataValue(
                        workingExecutionData,
                        targetPath,
                        mappedValue,
                        sourceMatch.wildcardIndices()
                );
                mappedValues.add(mappedValue);
            }

            cleanupSourceValue(workingExecutionData, sourcePath, targetPath, cleanupSource, false, cleanupEmptyContainers);
            return new RuleExecutionResult(sourceValue, List.copyOf(mappedValues));
        }

        var sourceValue = ProcessDataValueUtils.resolveProcessDataValue(workingExecutionData, sourcePath);
        if (deleteOnly) {
            cleanupSourceValue(workingExecutionData, sourcePath, targetPath, cleanupSource, true, cleanupEmptyContainers);
            return new RuleExecutionResult(sourceValue, null);
        }

        ProcessDataValueUtils.writeProcessDataValue(workingExecutionData, targetPath, sourceValue);
        cleanupSourceValue(workingExecutionData, sourcePath, targetPath, cleanupSource, false, cleanupEmptyContainers);
        return new RuleExecutionResult(sourceValue, sourceValue);
    }

    private void cleanupSourceValue(@Nonnull ProcessExecutionData workingExecutionData,
                                    String sourcePath,
                                    String targetPath,
                                    boolean cleanupSource,
                                    boolean deleteOnly,
                                    boolean cleanupEmptyContainers) {
        if (deleteOnly || (cleanupSource && !Objects.equals(sourcePath, targetPath))) {
            ProcessDataValueUtils.removeProcessDataValue(workingExecutionData, sourcePath, cleanupEmptyContainers);
        }
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
                @ElementPOJOBindingProperty(key = "label", strValue = "Leere übergeordnete Objekte und Listen entfernen"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Entfernt nach dem Löschen auch dadurch leer gewordene übergeordnete Objekte und Listen. Wenn die Option ausgeschaltet ist, bleiben diese leeren Strukturen bestehen.")
        })
        public Boolean cleanupEmptyContainers;

        public List<DataMappingActionNodeV1Rule> rules;

        @ReplicatingContainerLayoutElementElementPOJOBinding(id = RULES_FIELD_ID, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Regeln für die Datenbearbeitung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Legen Sie fest, welche Vorgangsdaten kopiert, verschoben oder gelöscht werden sollen. Die Regeln werden von oben nach unten ausgeführt. Spätere Regeln verwenden den bereits bearbeiteten Datenstand."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "#. Regel"),
                @ElementPOJOBindingProperty(key = "addLabel", strValue = "Regel hinzufügen"),
                @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Regel entfernen"),
                @ElementPOJOBindingProperty(key = "sortable", boolValue = true)
        })
        public static class DataMappingActionNodeV1Rule {
            public static final String SOURCE_FIELD_ID = "source";

            @InputElementPOJOBinding(id = SOURCE_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Bisheriger Speicherort"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Datenschlüssel des bisherigen Werts, zum Beispiel person.name oder personen[0].name. Mit personen[*].name verarbeiten Sie vorhandene Listenpositionen. Fehlt ein Wert an einem konkreten Pfad, wird beim Kopieren oder Verschieben null am neuen Speicherort gesetzt."),
                    @ElementPOJOBindingProperty(key = "required", boolValue = true),
                    @ElementPOJOBindingProperty(key = "disableWildCards", falseValue = true),
            })
            public String source;

            public static final String CLEANUP_SOURCE_FIELD_ID = "cleanupSource";

            @InputElementPOJOBinding(id = CLEANUP_SOURCE_FIELD_ID, type = ElementType.Checkbox, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Wert am bisherigen Speicherort entfernen"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Entfernt den Wert samt Datenschlüssel nach dem Kopieren. Damit wird der Wert verschoben. Für eine reine Löschung aktivieren Sie zusätzlich „Nur löschen“.")
            })
            public Boolean cleanupSource;

            public static final String DELETE_ONLY_FIELD_ID = "deleteOnly";

            @InputElementPOJOBinding(id = DELETE_ONLY_FIELD_ID, type = ElementType.Checkbox, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Nur löschen"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Entfernt den Wert samt Datenschlüssel am bisherigen Speicherort, ohne ihn an einen anderen Speicherort zu kopieren.")
            })
            public Boolean deleteOnly;

            public static final String TARGET_FIELD_ID = "target";

            @InputElementPOJOBinding(id = TARGET_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Neuer Speicherort"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Datenschlüssel für den kopierten oder verschobenen Wert. Fehlende Zielstrukturen werden angelegt; vorhandene Zielwerte werden ersetzt. Bei Listen muss die Anzahl der Platzhalter [*] mit dem bisherigen Speicherort übereinstimmen. Bei „Nur löschen“ ist kein neuer Speicherort nötig."),
                    @ElementPOJOBindingProperty(key = "disableWildCards", falseValue = true),
            })
            public String target;
        }
    }
}
