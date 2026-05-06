package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.CodeInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.services.TemplateRenderService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpActionNodeV1 implements ProcessNodeDefinition<HttpActionNodeV1.HttpActionNodeConfig> {
    public static final String NODE_KEY = "http_request";

    private static final String METHOD_FIELD_ID = "method";
    private static final String URL_FIELD_ID = "url";
    private static final String PAYLOAD_FIELD_ID = "payload";
    private static final String IS_JSON_FIELD_ID = "isJson";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_STATUS_CODE = "statusCode";
    private static final String OUTPUT_NAME_HEADERS = "headers";
    private static final String OUTPUT_NAME_BODY = "body";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";

    private final HttpService httpService;
    private final TemplateRenderService templateRenderService;

    public HttpActionNodeV1(HttpService httpService, TemplateRenderService templateRenderService) {
        this.httpService = httpService;
        this.templateRenderService = templateRenderService;
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
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(HttpActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        layout
                .findChild(METHOD_FIELD_ID, SelectInputElement.class)
                .ifPresent(methodField -> {
                    methodField.setValue(new ElementValueFunctions()
                            .setType(ValueFunctionType.NoCode)
                            .setNoCode(new NoCodeStaticValue(METHOD_GET)));
                    methodField.setOptions(List.of(
                            SelectInputElementOption.of(METHOD_GET, METHOD_GET),
                            SelectInputElementOption.of(METHOD_POST, METHOD_POST)
                    ));
                });

        layout
                .findChild(PAYLOAD_FIELD_ID, CodeInputElement.class)
                .ifPresent(payloadField -> payloadField.setVisibility(buildMethodVisibility(METHOD_POST)));

        return layout;
    }

    @Nonnull
    @Override
    public Class<HttpActionNodeConfig> getNodeConfigurationClass() {
        return HttpActionNodeConfig.class;
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
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        var method = StringUtils.isNullOrEmpty(configuration.method)
                ? METHOD_GET
                : configuration.method;

        var url = templateRenderService
                .interpolate(
                        context.getCurrentProcessExecutionData(),
                        configuration.url == null ? "" : configuration.url
                );

        var isJson = Boolean.TRUE.equals(configuration.isJson);

        var uri = URI.create(url);

        HttpResponse<String> response;
        if (method.equals(METHOD_GET)) {
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
            var payload = templateRenderService
                    .interpolate(
                            context.getCurrentProcessExecutionData(),
                            configuration.payload == null ? "{}" : configuration.payload
                    );

            if (StringUtils.isNullOrEmpty(payload)) {
                payload = "{}";
            }

            try {
                response = httpService.post(uri, payload);
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

    @Nonnull
    private static ElementVisibilityFunctions buildMethodVisibility(@Nonnull String expectedMethod) {
        return ElementVisibilityFunctions
                .of(NoCodeExpression.of(
                        NoCodeEqualsOperator.OPERATOR_ID,
                        new NoCodeReference(METHOD_FIELD_ID),
                        new NoCodeStaticValue(expectedMethod)
                ))
                .recalculateReferencedIds();
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class HttpActionNodeConfig {
        public static final String METHOD = METHOD_FIELD_ID;
        @InputElementPOJOBinding(id = METHOD, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "HTTP-Methode"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Die HTTP-Methode, die für die Anfrage verwendet werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String method;

        public static final String URL = URL_FIELD_ID;
        @InputElementPOJOBinding(id = URL, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "URL"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Die URL, von der die Daten geladen werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String url;

        public static final String PAYLOAD = PAYLOAD_FIELD_ID;
        @InputElementPOJOBinding(id = PAYLOAD, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "JSON-Payload"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der JSON-Request-Body für POST-Anfragen. Sie können Platzhalter zur String-Interpolation verwenden."),
                @ElementPOJOBindingProperty(key = "language", strValue = "json")
        })
        public String payload;

        public static final String IS_JSON = IS_JSON_FIELD_ID;
        @InputElementPOJOBinding(id = IS_JSON, type = ElementType.Checkbox, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "JSON-Antwort"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie an, ob die Antwort im JSON-Format erwartet wird. Falls ja, wird diese automatisch verarbeitet.")
        })
        public Boolean isJson;
    }
}
