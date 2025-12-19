package de.aivot.GoverBackend.plugins.corePlugin.components.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.form.input.CheckboxField;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioFieldOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectField;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextField;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.corePlugin.Core;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionEntity;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
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
public class HttpActionNode implements ProcessNodeProvider, PluginComponent {
    private static final String METHOD_FIELD_ID = "method";
    private static final String URL_FIELD_ID = "url";
    private static final String IS_JSON_FIELD_ID = "isJson";

    private static final String PORT_NAME = "output";

    private final HttpService httpService;
    private final ProcessDataService processDataService;

    public HttpActionNode(HttpService httpService, ProcessDataService processDataService) {
        this.httpService = httpService;
        this.processDataService = processDataService;
    }

    @Nonnull
    @Override
    public String getKey() {
        return "http";
    }

    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public GroupLayout getConfigurationLayout(@Nonnull UserEntity user,
                                              @Nonnull ProcessDefinitionEntity processDefinition,
                                              @Nonnull ProcessDefinitionVersionEntity processDefinitionVersion,
                                              @Nullable ProcessDefinitionNodeEntity thisNode) {
        var layout = new GroupLayout();
        layout.setId(getKey() + "-config");

        var method = new SelectField();
        method.setId(METHOD_FIELD_ID);
        method.setLabel("HTTP-Methode");
        method.setHint("Die HTTP-Methode, die für die Anfrage verwendet werden soll.");
        method.setRequired(true);
        method.setOptions(List.of(
                new RadioFieldOption()
                        .setLabel("GET")
                        .setValue("GET"),
                new RadioFieldOption()
                        .setLabel("POST")
                        .setValue("POST")
        ));
        layout.addChild(method);

        var url = new TextField();
        url.setId(URL_FIELD_ID);
        url.setLabel("URL");
        url.setHint("Die URL, von der die Daten geladen werden sollen.");
        url.setRequired(true);
        layout.addChild(url);


        var isJSON = new CheckboxField();
        isJSON.setId(IS_JSON_FIELD_ID);
        isJSON.setLabel("JSON-Antwort");
        isJSON.setHint("Geben Sie an, ob die Antwort im JSON-Format erwartet wird. Falls ja, wird diese automatisch verarbeitet.");
        layout.addChild(isJSON);

        return layout;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public String getName() {
        return "HTTP-Anfrage";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Lädt Daten von einer externen HTTP-Quelle.";
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

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessInstanceEntity processInstance,
                                           @Nonnull ProcessDefinitionNodeEntity thisNode,
                                           @Nonnull Map<String, Object> workingData) throws Exception {
        var configuration = thisNode.getConfiguration();

        var method = configuration
                .get(METHOD_FIELD_ID)
                .getOptionalValue()
                .orElse("GET")
                .toString();

        var url = processDataService
                .interpolate(
                        workingData,
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
            response = httpService.get(uri);
        } else {
            response = httpService.post(uri, "");
        }

        var metadata = new HashMap<String, Object>();
        metadata.put("statusCode", response.statusCode());
        metadata.put("headers", response.headers() != null ? response.headers().map() : Map.of());

        var bodyStr = response.body();
        if (isJson && StringUtils.isNotNullOrEmpty(bodyStr)) {
            try {
                var isObj = bodyStr.trim().startsWith("{");
                var isArr = bodyStr.trim().startsWith("[");

                if (isObj) {
                    var body = ObjectMapperFactory
                            .getInstance()
                            .readValue(bodyStr, Map.class);
                    metadata.put("body", body);
                } else if (isArr) {
                    var body = ObjectMapperFactory
                            .getInstance()
                            .readValue(bodyStr, List.class);
                    metadata.put("body", body);
                } else {
                    return ProcessNodeExecutionResultError
                            .of("Die Antwort ist kein gültiges JSON-Objekt oder -Array.");
                }
            } catch (Exception e) {
                return ProcessNodeExecutionResultError.of(e);
            }
        } else {
            metadata.put("body", bodyStr);
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata)
                .addEvent(ProcessNodeExecutionEvent.of(
                        ProcessHistoryEventType.Complete,
                        "HTTP-Anfrage erfolgreich abgeschlossen.",
                        "Die HTTP-Anfrage an " + url + " wurde erfolgreich ausgeführt. Der Statuscode ist " + response.statusCode() + "."
                ));
    }
}
