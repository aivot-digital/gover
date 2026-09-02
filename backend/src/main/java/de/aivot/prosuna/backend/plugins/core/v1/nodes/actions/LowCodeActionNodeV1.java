package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.ProcessDataService;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LowCodeActionNodeV1 implements ProcessNodeDefinition<LowCodeActionNodeV1.LowCodeActionNodeConfig> {
    public static final String NODE_KEY = "js";

    private static final String PORT_NAME = "output";

    private static final String CODE_FIELD_KEY = "js_code";
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public LowCodeActionNodeV1(JavascriptEngineFactoryService javascriptEngineFactoryService) {
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
        return "Low-Code ausführen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Führt benutzerdefinierten JavaScript-Code aus.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Führt benutzerdefinierten JavaScript-Code innerhalb der Prozessausführung aus.

                Das Skript kann die aktuellen Vorgangsdaten verarbeiten und gibt das resultierende Objekt als neue Vorgangsdaten zurück. Standardausgabe und Fehlerausgabe der Ausführung werden in den Elementdaten festgehalten, um die Ausführung nachvollziehbar zu machen.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        try {
            return ElementPOJOMapper.createFromPOJO(LowCodeActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }
    }

    @Nonnull
    @Override
    public Class<LowCodeActionNodeConfig> getNodeConfigurationClass() {
        return LowCodeActionNodeConfig.class;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "JavaScript ausgeführt",
                        "Der Prozess wird hier fortgesetzt, nachdem der JavaScript-Code ausgeführt wurde."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<LowCodeActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        var code = configuration.code == null ? "" : configuration.code;

        var jsCode = new JavascriptCode()
                .setCode(code);

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService
                    .fillJsEngineWithData(context.getCurrentProcessExecutionData(), engine);

            try {
                var result = engine
                        .evaluateCode(jsCode);

                return new ProcessNodeExecutionResultTaskCompleted()
                        .setViaPort(PORT_NAME)
                        .setProcessData(result.asMap())
                        .setNodeData(Map.of(
                                "stdout", result.getStdOutput(),
                                "stderr", result.getErrOutput()
                        ));
            } catch (Exception e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Fehler bei der Ausführung des Low-Code-Skripts."
                );
            }
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Initialisieren der Javascript-Engine."
            );
        }
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class LowCodeActionNodeConfig {
        public static final String CODE = CODE_FIELD_KEY;

        @InputElementPOJOBinding(id = CODE, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Javascript-Code"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie den benutzerdefinierten Javascript-Code ein, der zur Verarbeitung der Daten verwendet werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String code;
    }
}
