package de.aivot.GoverBackend.plugins.core.v1.nodes.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class IfFlowControlNodeV1 implements ProcessNodeDefinition {
    public static final String NODE_KEY = "if";

    private static final String PORT_NAME_TRUE = "true";
    private static final String PORT_NAME_FALSE = "false";

    private static final String OUTPUT_NAME_CONDITION_EXPRESSION = "conditionExpression";
    private static final String OUTPUT_NAME_CONDITION_EVALUATED = "conditionEvaluated";
    private static final String OUTPUT_NAME_CONDITION_VALUE = "conditionValue";

    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public IfFlowControlNodeV1(JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
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
        return ProcessNodeType.FlowControl;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Konditionelle Verzweigung (Wenn-Dann-Sonst)";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Leitet den Vorgang basierend auf einer Bedingung in unterschiedliche Pfade ein.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) throws ResponseException {
        try {
            return ElementPOJOMapper
                    .createFromPOJO(IfFlowControlNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Fehler beim Erstellen des Konfigurations-Layouts für den If-Knoten: %s",
                    e.getMessage()
            );
        }
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME_TRUE,
                        "Wahr",
                        "Der Prozessfluss wird hier fortgesetzt, wenn die Bedingung erfüllt ist."
                ),
                new ProcessNodePort(
                        PORT_NAME_FALSE,
                        "Falsch",
                        "Der Prozessfluss wird hier fortgesetzt, wenn die Bedingung nicht erfüllt ist."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_EXPRESSION,
                        "Bedingungsausdruck",
                        "Der konfigurierte Bedingungsausdruck."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_EVALUATED,
                        "Ausgewerteter Bedingungswert",
                        "Der als JavaScript ausgewertete Rückgabewert des Bedingungsausdrucks."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONDITION_VALUE,
                        "Boolesches Ergebnis",
                        "Das boolesche Ergebnis der Bedingungsauswertung."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        IfFlowControlNodeConfig configuration;
        try {
            configuration = ElementPOJOMapper
                    .mapToPOJO(context.getThisNode().getConfiguration(), IfFlowControlNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des If-Knotens ist ungültig: %s",
                    e.getMessage()
            );
        }

        if (StringUtils.isNullOrEmpty(configuration.condition)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Bedingung für den If-Knoten wurde nicht angegeben."
            );
        }

        final String conditionEvaluated;
        final Boolean conditionValue;

        var jsCode = JavascriptCode
                .of(configuration.condition);

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService
                    .fillJsEngineWithData(context.getProcessData(), engine);

            try {
                var jsResult = engine
                        .registerGlobalObject("$", context.getProcessData().get("$"))
                        .evaluateCode(jsCode);

                conditionEvaluated = jsResult.toString();
                conditionValue = jsResult.asBoolean();
            } catch (Exception e) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        e,
                        "Die Bedingung des If-Knotens konnte nicht als JavaScript ausgeführt werden: %s",
                        e.getMessage()
                );
            }
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Initialisieren der JavaScript-Engine für den If-Knoten: %s",
                    e.getMessage()
            );
        }

        if (conditionValue == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Bedingung des If-Knotens muss einen booleschen JavaScript-Wert ergeben. Übergeben wurde: %s",
                    StringUtils.quote(conditionEvaluated)
            );
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(conditionValue ? PORT_NAME_TRUE : PORT_NAME_FALSE)
                .setNodeData(Map.of(
                        OUTPUT_NAME_CONDITION_EXPRESSION, configuration.condition,
                        OUTPUT_NAME_CONDITION_EVALUATED, conditionEvaluated,
                        OUTPUT_NAME_CONDITION_VALUE, conditionValue
                ));
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class IfFlowControlNodeConfig {
        public static final String CONDITION_FIELD_ID = "condition";

        @InputElementPOJOBinding(id = CONDITION_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bedingung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "JavaScript-Ausdruck oder -Funktion, der/die direkt zu true oder false ausgewertet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "editorHeight", intValue = 140),
                @ElementPOJOBindingProperty(key = "wordWrap", boolValue = true)
        })
        public String condition;
    }
}
