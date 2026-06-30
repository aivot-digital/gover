package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.gover.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.gover.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.gover.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.gover.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.gover.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.ProcessDataKeyInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.nocode.models.NoCodeExpression;
import de.aivot.gover.backend.nocode.models.NoCodeReference;
import de.aivot.gover.backend.plugins.core.CorePlugin;
import de.aivot.gover.backend.plugins.core.v1.operators.bool.NoCodeNotOperator;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessDataValueUtils;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.models.ProcessNodePort;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.utils.MapUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DataMappingActionNodeV1 implements ProcessNodeDefinition<DataMappingActionNodeV1.DataMappingActionNodeV1Config> {
    public static final String NODE_KEY = "data_mapping";

    private static final String PORT_NAME = "output";

    private record RuleExecutionResult(Object sourceValue,
                                       Object mappedValue) {
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
        return "Kopiert Werte von Quellfeldern in Zielfelder. Kann Optional verwendet werden, um Quellfelder zu löschen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layoutElement;
        try {
            layoutElement = ElementPOJOMapper.createFromPOJO(DataMappingActionNodeV1Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e);
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
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Datenfelder verarbeitet",
                        "Der Prozess wird hier fortgesetzt, nachdem die Abbildungsregeln ausgeführt wurden."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<DataMappingActionNodeV1Config> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        if (configuration.rules == null || configuration.rules.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Es wurde keine Abbildungsregel angegeben."
            );
        }

        var workingExecutionData = ProcessExecutionData.of(MapUtils.deepCopy(context.getCurrentProcessExecutionData()));
        var mappedValues = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < configuration.rules.size(); i++) {
            var rule = configuration.rules.get(i);
            var rowIndex = i + 1;

            if (rule == null) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Abbildungsregel in Zeile %d ist ungültig.",
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
                        "Fehler in Abbildungsregel Zeile %d: %s",
                        rowIndex,
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
                );
            } catch (Exception e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Fehler in Abbildungsregel Zeile %d: %s",
                        rowIndex,
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
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

            @InputElementPOJOBinding(id = SOURCE_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Ausgangspfad"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Der Ausgangspfad für den abzubildenden oder zu löschenden Wert. Wenn der Pfad nicht existiert, wird null verwendet. Pfade verwenden Datenvariablen-Syntax mit Punktnotation, numerischen Array-Segmenten und dem Array-Wildcard-Segment *, z. B. person.name, items.0.price oder hunde.*.col.farbe. Klammer-Schreibweisen wie [0] oder [*] sind nicht erlaubt."),
                    @ElementPOJOBindingProperty(key = "required", boolValue = true),
                    @ElementPOJOBindingProperty(key = "disableWildCards", falseValue = true),
            })
            public String source;

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

            public static final String TARGET_FIELD_ID = "target";

            @InputElementPOJOBinding(id = TARGET_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                    @ElementPOJOBindingProperty(key = "label", strValue = "Zielpfad"),
                    @ElementPOJOBindingProperty(key = "hint", strValue = "Der Zielpfad, auf den der Wert abgebildet werden soll. Wenn der Pfad nicht existiert, wird er automatisch erstellt. Pfade verwenden dieselbe Datenvariablen-Syntax wie Ausgangspfade und unterstützen das Array-Wildcard-Segment *. Klammer-Schreibweisen wie [0] oder [*] sind nicht erlaubt. Kann leer bleiben, wenn der Wert nur gelöscht werden soll."),
                    @ElementPOJOBindingProperty(key = "disableWildCards", falseValue = true),
            })
            public String target;
        }
    }
}
