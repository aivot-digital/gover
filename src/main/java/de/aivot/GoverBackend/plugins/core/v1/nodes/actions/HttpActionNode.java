package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpActionNode implements ProcessNodeDefinition, PluginComponent {
    private static final String METHOD_FIELD_ID = "method";
    private static final String URL_FIELD_ID = "url";
    private static final String IS_JSON_FIELD_ID = "isJson";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_STATUS_CODE = "statusCode";
    private static final String OUTPUT_NAME_HEADERS = "headers";
    private static final String OUTPUT_NAME_BODY = "body";

    private final HttpService httpService;
    private final ProcessDataService processDataService;

    public HttpActionNode(HttpService httpService, ProcessDataService processDataService) {
        this.httpService = httpService;
        this.processDataService = processDataService;
    }

    @Override
    public @Nonnull String getKey() {
        return "http";
    }

    @Nonnull
    @Override
    public Integer getVersion() {
        return 1;
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
        return "Externer HTTP Aufruf";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Führt HTTP-Requests zu externen Systemen durch.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var method = new SelectInputElement();
        method.setId(METHOD_FIELD_ID);
        method.setLabel("HTTP-Methode");
        method.setHint("Die HTTP-Methode, die für die Anfrage verwendet werden soll.");
        method.setRequired(true);
        method.setOptions(List.of(
                new RadioInputElementOption()
                        .setLabel("GET")
                        .setValue("GET"),
                new RadioInputElementOption()
                        .setLabel("POST")
                        .setValue("POST")
        ));
        layout.addChild(method);

        var url = new TextInputElement();
        url.setId(URL_FIELD_ID);
        url.setLabel("URL");
        url.setHint("Die URL, von der die Daten geladen werden sollen.");
        url.setRequired(true);
        layout.addChild(url);


        var isJSON = new CheckboxInputElement();
        isJSON.setId(IS_JSON_FIELD_ID);
        isJSON.setLabel("JSON-Antwort");
        isJSON.setHint("Geben Sie an, ob die Antwort im JSON-Format erwartet wird. Falls ja, wird diese automatisch verarbeitet.");
        layout.addChild(isJSON);

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Datenweitergabe",
                        "Die geladenen Daten werden hier weitergegeben."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_STATUS_CODE,
                        "Statuscode der HTTP-Antwort",
                        "Der HTTP-Statuscode der Antwort."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_HEADERS,
                        "Header der HTTP-Antwort",
                        "Die Header der HTTP-Antwort."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_BODY,
                        "Inhalt der HTTP-Antwort",
                        "Der Inhalt der HTTP-Antwort, entweder als String oder als JSON-Objekt/Array."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var configuration = context.getThisNode().getConfiguration();

        var method = configuration
                .get(METHOD_FIELD_ID)
                .getOptionalValue()
                .orElse("GET")
                .toString();

        var url = processDataService
                .interpolate(
                        context.getProcessData(),
                        configuration
                                .get(URL_FIELD_ID)
                                .getOptionalValue()
                                .orElse("")
                                .toString()
                );

        var isJson = StringUtils.isNotNullOrEmpty(
                configuration
                        .get(IS_JSON_FIELD_ID)
                        .getOptionalValue()
                        .orElse("")
                        .toString()
        );

        var uri = URI.create(url);

        HttpResponse<String> response;
        if (method.equals("GET")) {
            try {
                response = httpService.get(uri);
            } catch (HttpConnectionException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Fehler beim HTTP-GET-Aufruf: %s",
                        e.getMessage()
                );
            }
        } else {
            try {
                response = httpService.post(uri, "{}");
            } catch (HttpConnectionException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Fehler beim HTTP-POST-Aufruf: %s",
                        e.getMessage()
                );
            }
        }

        var metadata = new HashMap<String, Object>();
        metadata.put(OUTPUT_NAME_STATUS_CODE, response.statusCode());
        metadata.put(OUTPUT_NAME_HEADERS, response.headers() != null ? response.headers().map() : Map.of());

        var bodyStr = response.body();
        if (isJson && StringUtils.isNotNullOrEmpty(bodyStr)) {
            try {
                var isObj = bodyStr.trim().startsWith("{");
                var isArr = bodyStr.trim().startsWith("[");

                if (isObj) {
                    var body = ObjectMapperFactory
                            .getInstance()
                            .readValue(bodyStr, Map.class);
                    metadata.put(OUTPUT_NAME_BODY, body);
                } else if (isArr) {
                    var body = ObjectMapperFactory
                            .getInstance()
                            .readValue(bodyStr, List.class);
                    metadata.put(OUTPUT_NAME_BODY, body);
                } else {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Die Antwort ist kein gültiges JSON-Objekt oder -Array."
                    );
                }
            } catch (Exception e) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        e,
                        "Fehler beim Verarbeiten der JSON-Antwort: %s",
                        e.getMessage()
                );
            }
        } else {
            metadata.put(OUTPUT_NAME_BODY, bodyStr);
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata);
    }
}
