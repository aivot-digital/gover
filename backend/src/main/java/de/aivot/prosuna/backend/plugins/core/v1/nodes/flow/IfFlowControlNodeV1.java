package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.ConditionEvaluationResult;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_LOW_CODE_FIELD_ID;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_NO_CODE_FIELD_ID;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_TYPE_FIELD_ID;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_TYPE_VALUE_LOW_CODE;
import static de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.FlowConditionSupport.CONDITION_TYPE_VALUE_NO_CODE;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;

@Component
public class IfFlowControlNodeV1 implements ProcessNodeDefinition<IfFlowControlNodeV1.IfFlowControlNodeConfig> {
    public static final String NODE_KEY = "if";
    private static final String CONDITION_SUBJECT = "der bedingten Verzweigung";

    private static final String PORT_NAME_TRUE = "true";
    private static final String PORT_NAME_FALSE = "false";

    private static final String OUTPUT_NAME_CONDITION_EXPRESSION = "conditionExpression";
    private static final String OUTPUT_NAME_CONDITION_EVALUATED = "conditionEvaluated";
    private static final String OUTPUT_NAME_CONDITION_VALUE = "conditionValue";

    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;

    public IfFlowControlNodeV1(JavascriptEngineFactoryService javascriptEngineFactoryService,
                               NoCodeEvaluationService noCodeEvaluationService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<IfFlowControlNodeV1.IfFlowControlNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var fulfilled = Boolean.TRUE.equals(summary.data(OUTPUT_NAME_CONDITION_VALUE));
        var path = PORT_NAME_TRUE.equals(context.viaPort()) ? "Bedingung erfüllt" : "Bedingung nicht erfüllt";
        return "Für die Fortsetzung wurde der Ausgang „" + path + "“ gewählt."
                + detail("Bedingung erfüllt", fulfilled ? "Ja" : "Nein")
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
        return "Bedingte Verzweigung";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Prüft eine Bedingung und setzt den Vorgang über „Bedingung erfüllt“ oder „Bedingung nicht erfüllt“ fort.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Verwenden Sie dieses Prozesselement, wenn der weitere Ablauf von einer Bedingung abhängt, zum Beispiel von einem Betrag oder einer vorherigen Entscheidung. Prosuna prüft die Bedingung automatisch und wählt einen von zwei Ausgängen. Das entspricht einer Wenn-Dann-Sonst-Entscheidung.

                # Was richten Sie ein?

                Wählen Sie unter „Bedingungsart“, wie Sie die Bedingung festlegen:

                - **No-Code:** Stellen Sie die Bedingung mit den verfügbaren Funktionen und Wertquellen zusammen. Diese Art ist vorbelegt.
                - **Low-Code (JavaScript):** Hinterlegen Sie die Bedingung als eigenes Skript. Das Ergebnis des letzten Ausdrucks muss `true` für „erfüllt“ oder `false` für „nicht erfüllt“ sein.

                Beide Arten können Vorgangsdaten, geschützte Vorgangsdaten und Elementdaten verwenden. In einem Low-Code-Skript greifen Sie mit `$` auf die Vorgangsdatenebene zu, zum Beispiel `$.betrag`. `$$` steht für die geschützte Vorgangsdatenebene und `_` für die Elementdatenebene.

                Verbinden Sie die Ausgänge „Bedingung erfüllt“ und „Bedingung nicht erfüllt“ mit den passenden nächsten Schritten.

                # Was passiert bei der Ausführung?

                Sobald der Vorgang dieses Element erreicht, wertet Prosuna die hinterlegte Bedingung aus:

                - Ist die Bedingung erfüllt, wird „Bedingung erfüllt“ gewählt.
                - Ist die Bedingung nicht erfüllt, wird „Bedingung nicht erfüllt“ gewählt.

                Nur der gewählte Ausgang bestimmt die Fortsetzung. Auch bei nicht erfüllter Bedingung wird der Vorgang fortgesetzt. Fehlt die Bedingung oder kann sie nicht ausgewertet werden, schlägt die Aufgabe fehl.

                # Welches Ergebnis steht danach bereit?

                In den Elementdaten werden die hinterlegte Bedingung, das ursprüngliche Ergebnis als Text und die abschließende Ja/Nein-Bewertung gespeichert. „Bedingung erfüllt“ enthält `true` für Ja oder `false` für Nein. Diese Ergebnisse können Sie in weiteren Schritten verwenden oder über eine Ausgangsdatenzuordnung in die Vorgangsdaten übernehmen.

                Die vorhandenen Vorgangsdaten werden weitergegeben. Wenn Sie Ausgangsdatenzuordnungen einrichten, werden die ausgewählten Ergebnisse an den festgelegten Speicherorten eingetragen; vorhandene Werte dort werden ersetzt.

                Die Zusammenfassung im Vorgangsverlauf nennt den gewählten Ausgang und ob die Bedingung erfüllt war. Eine hinterlegte Kurzbeschreibung wird ebenfalls angezeigt. Beschreiben Sie dort die fachliche Bedingung, damit die Entscheidung im Verlauf verständlich bleibt.

                # Wie sieht ein Beispiel aus?

                Ein Betrag über `1000` soll zu einem zusätzlichen Prüfschritt führen. Wählen Sie „No-Code“ und die Funktion „Größer als“. Verwenden Sie den Vorgangsdatenwert `betrag` als Zahl und `1000` als Vergleichszahl. Verbinden Sie „Bedingung erfüllt“ mit dem zusätzlichen Prüfschritt und „Bedingung nicht erfüllt“ mit dem regulären nächsten Schritt.

                Bei `betrag: 1500` wird „Bedingung erfüllt“ gewählt. Bei `betrag: 800` oder genau `1000` wird „Bedingung nicht erfüllt“ gewählt.

                Dieselbe Bedingung können Sie unter „Low-Code (JavaScript)“ mit `$.betrag > 1000` festlegen.

                # Was ist zu beachten?

                Das Element hat genau zwei Ausgänge. Für weitere aufeinanderfolgende Entscheidungen können Sie mehrere Verzweigungen verbinden oder „Fallunterscheidung“ verwenden.

                Ein fehlender Bedingungsausdruck führt zu einem Fehler. Liefert ein hinterlegter No-Code-Ausdruck dagegen `null` oder leeren Text, gilt die Bedingung als nicht erfüllt. Auch die Texte `true` oder `wahr` sowie `false` oder `falsch` werden als entsprechende Wahrheitswerte erkannt. Andere Ergebnisse, etwa Zahlen oder die Texte `ja` und `nein`, sind keine gültigen Bedingungsergebnisse.

                Bei Low-Code muss das letzte Ergebnis ein tatsächlicher JavaScript-Wahrheitswert sein: `true` oder `false`, jeweils ohne Anführungszeichen. `null`, `undefined`, Texte und Zahlen führen zu einem Fehler. Ein alleinstehendes `return` auf oberster Skriptebene ist unzulässig; wenn Sie eine Funktion verwenden, rufen Sie diese im Skript auf.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(IfFlowControlNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Einstellungen für „Bedingte Verzweigung“ konnten nicht geladen werden.");
        }

        FlowConditionSupport.configureConditionFields(layout);

        return layout;
    }

    @Nonnull
    @Override
    public Class<IfFlowControlNodeConfig> getNodeConfigurationClass() {
        return IfFlowControlNodeConfig.class;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull IfFlowControlNodeConfig configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME_TRUE,
                        "Bedingung erfüllt",
                        "Der Vorgang wird hier fortgesetzt, wenn die Bedingung erfüllt ist."
                ),
                new ProcessNodePort(
                        PORT_NAME_FALSE,
                        "Bedingung nicht erfüllt",
                        "Der Vorgang wird hier fortgesetzt, wenn die Bedingung nicht erfüllt ist."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_EXPRESSION,
                        "Hinterlegte Bedingung",
                        "Die hinterlegte Bedingung als JavaScript-Skript oder als technische JSON-Darstellung des No-Code-Ausdrucks.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_EVALUATED,
                        "Ergebnis des Ausdrucks als Text",
                        "Das ursprüngliche Ergebnis des Bedingungsausdrucks als Text, bevor es als erfüllt oder nicht erfüllt bewertet wird.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_VALUE,
                        "Bedingung erfüllt",
                        "Gibt an, ob die Bedingung erfüllt ist: true bedeutet Ja, false bedeutet Nein.",
                        "boolean"
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<IfFlowControlNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var lowCodeCondition = toNullableTrimmedString(configuration.condition);

        var noCodeCondition = parseNoCodeCondition(configuration);

        var configuredConditionType = toNullableTrimmedString(configuration.conditionType);

        var conditionType = resolveConditionType(
                configuredConditionType,
                lowCodeCondition,
                noCodeCondition
        );

        final ConditionEvaluationResult evaluationResult = switch (conditionType) {
            case LOW_CODE -> evaluateLowCodeCondition(context, lowCodeCondition);
            case NO_CODE -> evaluateNoCodeCondition(context, noCodeCondition);
        };

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(evaluationResult.conditionValue() ? PORT_NAME_TRUE : PORT_NAME_FALSE)
                .setNodeData(Map.of(
                        OUTPUT_NAME_CONDITION_EXPRESSION, evaluationResult.conditionExpression(),
                        OUTPUT_NAME_CONDITION_EVALUATED, evaluationResult.conditionEvaluated(),
                        OUTPUT_NAME_CONDITION_VALUE, evaluationResult.conditionValue()
                ));
    }

    @Nonnull
    private ConditionType resolveConditionType(String configuredConditionType,
                                               String lowCodeCondition,
                                               NoCodeOperand noCodeCondition) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (StringUtils.isNullOrEmpty(configuredConditionType)) {
            // Backward compatibility for older If-nodes without explicit condition type.
            if (StringUtils.isNotNullOrEmpty(lowCodeCondition) && noCodeCondition == null) {
                return ConditionType.LOW_CODE;
            }
            return ConditionType.NO_CODE;
        }

        return switch (configuredConditionType) {
            case CONDITION_TYPE_VALUE_LOW_CODE -> ConditionType.LOW_CODE;
            case CONDITION_TYPE_VALUE_NO_CODE -> ConditionType.NO_CODE;
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Bedingungsart %s wird nicht unterstützt. Wählen Sie „No-Code“ oder „Low-Code (JavaScript)“.",
                    StringUtils.quote(configuredConditionType)
            );
        };
    }

    @Nonnull
    private ConditionEvaluationResult evaluateLowCodeCondition(@Nonnull ProcessNodeExecutionInitContext<IfFlowControlNodeConfig> context,
                                                               String lowCodeCondition) throws ProcessNodeExecutionException {
        if (StringUtils.isNullOrEmpty(lowCodeCondition)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Legen Sie die Bedingung als Low-Code-Skript in JavaScript fest."
            );
        }

        return FlowConditionSupport.evaluateLowCode(
                javascriptEngineFactoryService,
                context.getCurrentProcessExecutionData(),
                lowCodeCondition,
                CONDITION_SUBJECT
        );
    }

    @Nonnull
    private ConditionEvaluationResult evaluateNoCodeCondition(@Nonnull ProcessNodeExecutionInitContext<IfFlowControlNodeConfig> context,
                                                              NoCodeOperand noCodeCondition) throws ProcessNodeExecutionException {
        if (noCodeCondition == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Legen Sie einen No-Code-Ausdruck für die Bedingung fest."
            );
        }

        return FlowConditionSupport.evaluateNoCode(
                noCodeEvaluationService,
                context.getCurrentProcessExecutionData(),
                noCodeCondition,
                CONDITION_SUBJECT
        );
    }

    private NoCodeOperand parseNoCodeCondition(@Nonnull IfFlowControlNodeConfig configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (configuration.conditionNoCode == null) {
            return null;
        }

        return configuration.conditionNoCode.getNoCode();
    }

    private static String toNullableTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        var str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }

    private enum ConditionType {
        LOW_CODE,
        NO_CODE
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class IfFlowControlNodeConfig {
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
