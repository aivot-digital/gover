package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.form.input.CodeInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LowCodeActionNodeV1 implements ProcessNodeDefinition {
    private static final String PORT_NAME = "output";

    private static final String CODE_FIELD_KEY = "js_code";
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;

    public LowCodeActionNodeV1(JavascriptEngineFactoryService javascriptEngineFactoryService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "js";
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
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Low-Code ausführen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Führt benutzerdefinierten JavaScript-Code aus.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var codeField = new CodeInputElement();
        codeField.setId(CODE_FIELD_KEY);
        codeField.setLabel("Javascript-Code");
        codeField.setHint("Geben Sie den benutzerdefinierten Javascript-Code ein, der zur Verarbeitung der Daten verwendet werden soll.");
        codeField.setRequired(true);
        layout.addChild(codeField);

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Datenweitergabe",
                        "Die verarbeiteten Daten werden hier weitergegeben."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var configuration = context
                .getConfiguration()
                .getEffectiveValues();

        var code = String.valueOf(configuration.getOrDefault(CODE_FIELD_KEY, ""));

        var jsCode = new JavascriptCode()
                .setCode(code);

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService
                    .fillJsEngineWithData(context.getProcessData(), engine);

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
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Initialisieren der Javascript-Engine."
            );
        }
    }
}
