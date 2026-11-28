package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerRowIdPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NoCodeInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_LOW_CODE_FIELD_ID;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_NO_CODE_FIELD_ID;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_TYPE_FIELD_ID;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_TYPE_VALUE_LOW_CODE;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_TYPE_VALUE_NO_CODE;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;

@Component
public class SwitchFlowControlNodeV1 implements ProcessNodeDefinition<SwitchFlowControlNodeV1.SwitchFlowControlNodeConfig> {
    public static final String NODE_KEY = "switch";
    static final String CASES_FIELD_ID = "cases";
    static final String CASE_LABEL_FIELD_ID = "caseLabel";
    static final String DEFAULT_PORT_LABEL_FIELD_ID = "defaultPortLabel";

    static final String PORT_NAME_DEFAULT = "default";
    static final String DEFAULT_PORT_LABEL = "Standard";

    private static final String OUTPUT_NAME_MATCHED_CASE_ID = "matchedCaseId";
    private static final String OUTPUT_NAME_MATCHED_CASE_LABEL = "matchedCaseLabel";
    private static final String OUTPUT_NAME_DEFAULT_USED = "defaultUsed";

    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;

    public SwitchFlowControlNodeV1(JavascriptEngineFactoryService javascriptEngineFactoryService,
                                   NoCodeEvaluationService noCodeEvaluationService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<SwitchFlowControlNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        if (Boolean.TRUE.equals(summary.data(OUTPUT_NAME_DEFAULT_USED))) {
            return "Keiner der Fälle trifft zu. Für die Fortsetzung wurde der Standardausgang „"
                    + text(getDefaultPortLabel(context.configurationOfExecutingNode())) + "“ gewählt."
                    + detail("Kurzbeschreibung", context.thisNode().getDescription());
        }
        var caseLabel = summary.value(OUTPUT_NAME_MATCHED_CASE_LABEL);
        return "Als erster zutreffender Fall wurde „" + caseLabel + "“ ermittelt. Für die Fortsetzung wurde der Ausgang „"
                + caseLabel + "“ gewählt."
                + detail("Kurzbeschreibung", context.thisNode().getDescription());
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
        return "Fallunterscheidung";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Prüft mehrere Bedingungen der Reihe nach und wählt den Ausgang des ersten zutreffenden Falls oder den Standardausgang.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Verwenden Sie dieses Prozesselement, wenn mehrere Fälle zu unterschiedlichen nächsten Schritten führen können. Prosuna prüft die Bedingungen in der festgelegten Reihenfolge und wählt den ersten zutreffenden Fall. Trifft kein Fall zu, wird der Standardausgang gewählt.

                Für eine einzelne Bedingung mit zwei Ausgängen können Sie „Bedingte Verzweigung“ verwenden.

                # Was richten Sie ein?

                Legen Sie unter „Fälle“ mindestens einen Eintrag an. Jeder Fall erhält eine „Bezeichnung des Ausgangs“ und eine „Bedingung“. Benennen Sie den Ausgang so, dass der Fall fachlich erkennbar ist, zum Beispiel „Betrag über 1.000 Euro“.

                Wählen Sie für jeden Fall die „Bedingungsart“:

                - **No-Code:** Stellen Sie die Bedingung mit den verfügbaren Funktionen und Wertquellen zusammen. Diese Art ist für neue Fälle vorbelegt.
                - **Low-Code (JavaScript):** Hinterlegen Sie die Bedingung als eigenes Skript. Das Ergebnis des letzten Ausdrucks muss `true` für „erfüllt“ oder `false` für „nicht erfüllt“ sein.

                Beide Arten können Vorgangsdaten, geschützte Vorgangsdaten und Elementdaten verwenden. In einem Low-Code-Skript greifen Sie mit `$` auf die Vorgangsdatenebene zu, zum Beispiel `$.betrag`. `$$` steht für die geschützte Vorgangsdatenebene und `_` für die Elementdatenebene.

                Ordnen Sie die Fälle in der gewünschten Reihenfolge von oben nach unten an. Unter „Bezeichnung des Standardausgangs“ können Sie den zusätzlichen Ausgang benennen. Ohne eigene Angabe heißt er „Standard“. Verbinden Sie alle Fallausgänge und den Standardausgang mit den passenden nächsten Schritten.

                # Was passiert bei der Ausführung?

                Prosuna prüft die erste Fallbedingung. Ist sie nicht erfüllt, folgt die nächste. Sobald eine Bedingung erfüllt ist, steht der Ausgang fest; die späteren Fallbedingungen werden nicht mehr ausgewertet.

                Sind alle Fallbedingungen nicht erfüllt, wird der Standardausgang gewählt. Kann eine tatsächlich geprüfte Bedingung nicht ausgewertet werden, schlägt die Aufgabe fehl.

                # Welches Ergebnis steht danach bereit?

                Die Elementdaten enthalten die Kennung und die Bezeichnung des gewählten Falls. Außerdem wird unter „Standardausgang gewählt“ ein Ja/Nein-Wert gespeichert. Wird der Standardausgang gewählt, ist dieser Wert `true`; Kennung und Bezeichnung eines gewählten Falls sind dann `null`.

                Diese Ergebnisse können Sie in weiteren Schritten verwenden oder über eine Ausgangsdatenzuordnung in die Vorgangsdaten übernehmen. Die bisherigen Vorgangsdaten werden weitergegeben. Eingerichtete Ausgangsdatenzuordnungen schreiben die ausgewählten Ergebnisse an die festgelegten Speicherorte und ersetzen dort vorhandene Werte.

                Die Zusammenfassung im Vorgangsverlauf nennt den ersten zutreffenden Fall beziehungsweise den gewählten Standardausgang. Eine hinterlegte Kurzbeschreibung wird ebenfalls angezeigt. Beschreiben Sie dort den fachlichen Zweck der Fallunterscheidung.

                # Wie sieht ein Beispiel aus?

                Abhängig vom Betrag soll ein unterschiedlicher Prüfschritt folgen. Legen Sie diese Fälle in der angegebenen Reihenfolge an:

                1. **Betrag über 1.000 Euro:** Wählen Sie „No-Code“ und die Funktion „Größer als“. Verwenden Sie den Vorgangsdatenwert `betrag` als Zahl und `1000` als Vergleichszahl.
                2. **Betrag über 500 Euro:** Verwenden Sie dieselbe Funktion mit dem Vergleichswert `500`.

                Benennen Sie den Standardausgang „Weitere Bearbeitung“ und verbinden Sie die drei Ausgänge mit den vorgesehenen nächsten Schritten.

                Bei `betrag: 1500` wird der erste Fall gewählt. Bei `betrag: 750` wird der zweite Fall gewählt. Bei genau `500` oder einem kleineren Betrag wird der Standardausgang gewählt.

                Die Reihenfolge ist entscheidend: Stünde „Betrag über 500 Euro“ an erster Stelle, würde dieser Fall auch bei `1500` zuerst zutreffen. Die Prüfung würde dort enden.

                # Was ist zu beachten?

                Richten Sie alle angelegten Fälle vollständig und gültig ein. Vor der Ausführung wird die gesamte Konfiguration geprüft. Entfernen Sie nicht benötigte Fälle und prüfen Sie die zugehörigen Verbindungen im Prozessmodell.

                Die Bezeichnungen der Fallausgänge und des Standardausgangs müssen eindeutig sein. Groß- und Kleinschreibung sowie Leerzeichen am Anfang und Ende unterscheiden die Bezeichnungen nicht. Beide Bezeichnungsfelder erlauben höchstens 64 Zeichen.

                Liefert ein hinterlegter No-Code-Ausdruck `null` oder leeren Text, gilt die Bedingung als nicht erfüllt. Auch die Texte `true` oder `wahr` sowie `false` oder `falsch` werden als entsprechende Wahrheitswerte erkannt. Andere Ergebnisse, etwa Zahlen oder die Texte `ja` und `nein`, führen zu einem Fehler. Eine fehlende Bedingung ist ebenfalls ein Fehler.

                Bei Low-Code muss das letzte Ergebnis ein tatsächlicher JavaScript-Wahrheitswert sein: `true` oder `false`, jeweils ohne Anführungszeichen. `null`, `undefined`, Texte und Zahlen führen zu einem Fehler. Ein alleinstehendes `return` auf oberster Skriptebene ist unzulässig; wenn Sie eine Funktion verwenden, rufen Sie diese im Skript auf.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(SwitchFlowControlNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Einstellungen für „Fallunterscheidung“ konnten nicht geladen werden.");
        }

        FlowConditionSupport.configureConditionFields(layout);

        return layout;
    }

    @Nonnull
    @Override
    public Class<SwitchFlowControlNodeConfig> getNodeConfigurationClass() {
        return SwitchFlowControlNodeConfig.class;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull SwitchFlowControlNodeConfig configuration) {
        var cases = getCases(configuration);
        var ports = new ArrayList<ProcessNodePort>(cases.size() + 1);
        for (var i = 0; i < cases.size(); i++) {
            var caseConfig = cases.get(i);
            ports.add(new ProcessNodePort(
                    getPortKey(caseConfig),
                    getCaseLabel(caseConfig, i),
                    "Der Vorgang wird hier fortgesetzt, wenn die Bedingung dieses Falls als erste erfüllt ist."
            ));
        }
        // The default port is always the last one, so it reads as the fallback after all cases.
        ports.add(new ProcessNodePort(
                PORT_NAME_DEFAULT,
                getDefaultPortLabel(configuration),
                "Der Vorgang wird hier fortgesetzt, wenn keine Fallbedingung erfüllt ist."
        ));
        return ports;
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_MATCHED_CASE_ID,
                        "Kennung des gewählten Falls",
                        "Die automatisch vergebene Kennung des ersten Falls mit erfüllter Bedingung. Beim Standardausgang ist der Wert null.",
                        "string | null"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_MATCHED_CASE_LABEL,
                        "Bezeichnung des gewählten Falls",
                        "Die Bezeichnung des ersten Falls mit erfüllter Bedingung. Beim Standardausgang ist der Wert null.",
                        "string | null"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_DEFAULT_USED,
                        "Standardausgang gewählt",
                        "Gibt an, ob der Standardausgang gewählt wurde: true bedeutet Ja, false bedeutet Nein.",
                        "boolean"
                )
        );
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeConfigurationValidationContext<SwitchFlowControlNodeConfig> context) {
        var configuration = context.configuration();
        var cases = getCases(configuration);

        // Port labels must stay distinguishable in the process model; the effective default label belongs to the default port.
        var labelOwners = new HashMap<String, String>();
        labelOwners.put(normalizeLabel(getDefaultPortLabel(configuration)), "den Standardausgang");

        var errors = new ArrayList<String>();
        for (var i = 0; i < cases.size(); i++) {
            var label = StringUtils.toNullableTrimmedString(cases.get(i).caseLabel);
            if (label == null) {
                // The required check of the label field reports missing labels.
                continue;
            }
            var owner = labelOwners.putIfAbsent(normalizeLabel(label), "Fall " + (i + 1));
            if (owner != null) {
                errors.add("Fall %d: Die Bezeichnung %s wird bereits für %s verwendet. Vergeben Sie eine eindeutige Bezeichnung.".formatted(
                        i + 1,
                        StringUtils.quote(label),
                        owner
                ));
            }
        }

        return errors.isEmpty() ? null : Map.of(CASES_FIELD_ID, errors);
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<SwitchFlowControlNodeConfig> context) throws ProcessNodeExecutionException {
        var cases = getCases(context.getConfigurationOfExecutingNode());

        // Only the first matching case selects the port; later cases are not evaluated.
        for (var i = 0; i < cases.size(); i++) {
            var caseConfig = cases.get(i);
            var label = getCaseLabel(caseConfig, i);
            if (evaluateCase(context, caseConfig, label)) {
                var nodeData = new HashMap<String, Object>();
                nodeData.put(OUTPUT_NAME_MATCHED_CASE_ID, caseConfig.id);
                nodeData.put(OUTPUT_NAME_MATCHED_CASE_LABEL, label);
                nodeData.put(OUTPUT_NAME_DEFAULT_USED, false);
                return new ProcessNodeExecutionResultTaskCompleted()
                        .setViaPort(getPortKey(caseConfig))
                        .setNodeData(nodeData);
            }
        }

        var nodeData = new HashMap<String, Object>();
        nodeData.put(OUTPUT_NAME_MATCHED_CASE_ID, null);
        nodeData.put(OUTPUT_NAME_MATCHED_CASE_LABEL, null);
        nodeData.put(OUTPUT_NAME_DEFAULT_USED, true);
        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME_DEFAULT)
                .setNodeData(nodeData);
    }

    private boolean evaluateCase(@Nonnull ProcessNodeExecutionInitContext<SwitchFlowControlNodeConfig> context,
                                 @Nonnull SwitchCaseConfig caseConfig,
                                 @Nonnull String label) throws ProcessNodeExecutionException {
        var subject = "des Falls " + StringUtils.quote(label);
        var conditionType = Objects.requireNonNullElse(StringUtils.toNullableTrimmedString(caseConfig.conditionType), CONDITION_TYPE_VALUE_NO_CODE);

        return switch (conditionType) {
            case CONDITION_TYPE_VALUE_LOW_CODE -> {
                var lowCodeCondition = StringUtils.toNullableTrimmedString(caseConfig.condition);
                if (lowCodeCondition == null) {
                    throw new ProcessNodeExecutionExceptionMissingValue(
                            "Die Low-Code-Bedingung %s fehlt. Hinterlegen Sie ein JavaScript-Skript.",
                            subject
                    );
                }
                yield FlowConditionSupport
                        .evaluateLowCode(javascriptEngineFactoryService, context.getCurrentProcessExecutionData(), lowCodeCondition, subject)
                        .conditionValue();
            }
            case CONDITION_TYPE_VALUE_NO_CODE -> {
                var noCodeCondition = caseConfig.conditionNoCode != null ? caseConfig.conditionNoCode.getNoCode() : null;
                if (noCodeCondition == null) {
                    throw new ProcessNodeExecutionExceptionMissingValue(
                            "Die No-Code-Bedingung %s fehlt. Legen Sie einen Ausdruck fest.",
                            subject
                    );
                }
                yield FlowConditionSupport
                        .evaluateNoCode(noCodeEvaluationService, context.getCurrentProcessExecutionData(), noCodeCondition, subject)
                        .conditionValue();
            }
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Bedingungsart %s wird nicht unterstützt. Konfigurierter Wert: %s. Wählen Sie „No-Code“ oder „Low-Code (JavaScript)“.",
                    subject,
                    StringUtils.quote(conditionType)
            );
        };
    }

    @Nonnull
    private static List<SwitchCaseConfig> getCases(@Nonnull SwitchFlowControlNodeConfig configuration) {
        return configuration.cases != null ? configuration.cases : List.of();
    }

    /**
     * Uses the stable row ID as port key, so edges survive label changes and reordering. Row IDs are UUIDs; without
     * hyphens they fit the 32-character limit of port keys. The port resolver reports rows without an ID as empty keys.
     */
    @Nonnull
    private static String getPortKey(@Nonnull SwitchCaseConfig caseConfig) {
        return caseConfig.id != null ? caseConfig.id.replace("-", "") : "";
    }

    /** Falls back to the case number while the required label is still missing, so the other ports stay resolvable. */
    @Nonnull
    private static String getCaseLabel(@Nonnull SwitchCaseConfig caseConfig, int index) {
        return Objects.requireNonNullElse(StringUtils.toNullableTrimmedString(caseConfig.caseLabel), "Fall " + (index + 1));
    }

    @Nonnull
    private static String getDefaultPortLabel(@Nonnull SwitchFlowControlNodeConfig configuration) {
        return Objects.requireNonNullElse(StringUtils.toNullableTrimmedString(configuration.defaultPortLabel), DEFAULT_PORT_LABEL);
    }

    @Nonnull
    private static String normalizeLabel(@Nonnull String label) {
        return label.trim().toLowerCase(Locale.ROOT);
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class SwitchFlowControlNodeConfig {
        public List<SwitchCaseConfig> cases;

        @InputElementPOJOBinding(id = DEFAULT_PORT_LABEL_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bezeichnung des Standardausgangs"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Dieser Ausgang wird gewählt, wenn keine Fallbedingung erfüllt ist. Ohne eigene Bezeichnung heißt er „Standard“."),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = DEFAULT_PORT_LABEL),
                @ElementPOJOBindingProperty(key = "maxCharacters", intValue = 64)
        })
        public String defaultPortLabel;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = CASES_FIELD_ID, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Fälle"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Die Fälle werden von oben nach unten geprüft. Sobald eine Fallbedingung erfüllt ist, wird ihr Ausgang gewählt. Spätere Fallbedingungen werden nicht mehr ausgewertet."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Fall #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Fall hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Fall entfernen"),
            @ElementPOJOBindingProperty(key = "sortable", boolValue = true)
    })
    public static class SwitchCaseConfig {
        @ReplicatingContainerRowIdPOJOBinding
        public String id;

        @InputElementPOJOBinding(id = CASE_LABEL_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bezeichnung des Ausgangs"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Die Bezeichnung erscheint am Ausgang und im Ergebnis der Fallunterscheidung. Verwenden Sie eine eindeutige Bezeichnung, die sich von den anderen Fällen und dem Standardausgang unterscheidet."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "maxCharacters", intValue = 64)
        })
        public String caseLabel;

        @InputElementPOJOBinding(id = CONDITION_TYPE_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "presentation", strValue = "radio"),
                @ElementPOJOBindingProperty(key = "label", strValue = "Bedingungsart"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie, ob Sie die Bedingung als No-Code-Ausdruck oder als Low-Code-Skript in JavaScript festlegen."),
                @ElementPOJOBindingProperty(key = "toggleButtons", boolValue = true),
                @ElementPOJOBindingProperty(key = "displayInline", boolValue = true),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String conditionType;

        @InputElementPOJOBinding(id = CONDITION_LOW_CODE_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bedingung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das Ergebnis des letzten JavaScript-Ausdrucks muss true (erfüllt) oder false (nicht erfüllt) sein, zum Beispiel $.betrag > 1000."),
                @ElementPOJOBindingProperty(key = "editorHeight", intValue = 140),
                @ElementPOJOBindingProperty(key = "wordWrap", boolValue = true),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String condition;

        @InputElementPOJOBinding(id = CONDITION_NO_CODE_FIELD_ID, type = ElementType.NoCodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bedingung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Stellen Sie eine Bedingung zusammen, die „Wahr“ (erfüllt) oder „Falsch“ (nicht erfüllt) ergibt."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public NoCodeInputElementItem conditionNoCode;
    }
}
