package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeNotOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.TemplateRenderService;
import de.aivot.GoverBackend.utils.MultipartUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class HttpActionNodeV1 implements ProcessNodeDefinition<HttpActionNodeV1.HttpActionNodeConfig> {
    public static final String NODE_KEY = "http_request";

    private static final String SUCCESS_PORT = "success";
    private static final String ERROR_PORT = "error";

    private static final String GENERAL_GROUP_ID = "general";
    private static final String OUTGOING_GROUP_ID = "outgoing";
    private static final String INCOMING_GROUP_ID = "incoming";

    private static final String METHOD_FIELD_ID = "method";
    private static final String URL_FIELD_ID = "url";
    private static final String HEADERS_FIELD_ID = "headers";
    private static final String QUERY_PARAMETERS_FIELD_ID = "queryParameters";

    private static final String BODY_TYPE_FIELD_ID = "bodyType";
    private static final String JSON_CONTENT_TYPE_FIELD_ID = "jsonContentType";
    private static final String MULTIPART_CONTENT_TYPE_FIELD_ID = "multipartContentType";
    private static final String MANUAL_CONTENT_TYPE_FIELD_ID = "manualContentType";
    private static final String SOURCE_MODE_FIELD_ID = "sourceMode";
    private static final String JSON_SELECTED_PATHS_FIELD_ID = "jsonSelectedPaths";
    private static final String MULTIPART_FIELDS_FIELD_ID = "multipartFields";
    private static final String REQUEST_BODY_CODE_FIELD_ID = "requestBodyCode";

    private static final String RESPONSE_TYPE_FIELD_ID = "responseType";
    private static final String EXPECTED_STATUS_CODE_FIELD_ID = "expectedStatusCode";
    private static final String RESPONSE_PROCESSOR_FIELD_ID = "responseProcessorCode";

    private static final String OUTPUT_NAME_STATUS_CODE = "statusCode";
    private static final String OUTPUT_NAME_HEADERS = "headers";
    private static final String OUTPUT_NAME_RAW_BODY = "rawBody";
    private static final String OUTPUT_NAME_PROCESSED_RESPONSE = "processedResponse";
    private static final String OUTPUT_NAME_FILE_NAME = "fileName";
    private static final String OUTPUT_NAME_MIME_TYPE = "mimeType";
    private static final String OUTPUT_NAME_SIZE_BYTES = "sizeBytes";
    private static final String OUTPUT_NAME_ATTACHMENT_KEY = "attachmentKey";
    private static final String OUTPUT_NAME_STORAGE_PROVIDER_ID = "storageProviderId";
    private static final String OUTPUT_NAME_STORAGE_PATH_FROM_ROOT = "storagePathFromRoot";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PATCH = "PATCH";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_DELETE = "DELETE";

    private static final Set<String> SUPPORTED_METHODS = Set.of(
            METHOD_GET,
            METHOD_POST,
            METHOD_PATCH,
            METHOD_PUT,
            METHOD_DELETE
    );

    private static final String BODY_TYPE_JSON = "json";
    private static final String BODY_TYPE_MULTIPART = "multipart";
    private static final String BODY_TYPE_MANUAL = "manual";

    private static final Set<String> BODY_TYPES = Set.of(
            BODY_TYPE_JSON,
            BODY_TYPE_MULTIPART,
            BODY_TYPE_MANUAL
    );

    private static final String SOURCE_MODE_ALL = "all";
    private static final String SOURCE_MODE_SELECTED = "selected";
    private static final String SOURCE_MODE_LOW_CODE = "lowCode";

    private static final Set<String> SOURCE_MODES = Set.of(
            SOURCE_MODE_ALL,
            SOURCE_MODE_SELECTED,
            SOURCE_MODE_LOW_CODE
    );

    private static final String RESPONSE_TYPE_JSON = "json";
    private static final String RESPONSE_TYPE_XML = "xml";
    private static final String RESPONSE_TYPE_TEXT = "text";
    private static final String RESPONSE_TYPE_FILE = "file";

    private static final Set<String> RESPONSE_TYPES = Set.of(
            RESPONSE_TYPE_JSON,
            RESPONSE_TYPE_XML,
            RESPONSE_TYPE_TEXT,
            RESPONSE_TYPE_FILE
    );

    private final HttpService httpService;
    private final TemplateRenderService templateRenderService;
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;

    public HttpActionNodeV1(HttpService httpService,
                            TemplateRenderService templateRenderService,
                            JavascriptEngineFactoryService javascriptEngineFactoryService,
                            ProcessInstanceAttachmentService processInstanceAttachmentService) {
        this.httpService = httpService;
        this.templateRenderService = templateRenderService;
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
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
            throw ResponseException.internalServerError(
                    e,
                    "Fehler bei der Erstellung des Konfigurationslayouts: %s",
                    e.getMessage()
            );
        }

        layout.findChild(METHOD_FIELD_ID, SelectInputElement.class)
                .ifPresent(field -> {
                    field.setValue(staticValue(METHOD_GET));
                    field.setOptions(List.of(
                            SelectInputElementOption.of(METHOD_GET, METHOD_GET),
                            SelectInputElementOption.of(METHOD_POST, METHOD_POST),
                            SelectInputElementOption.of(METHOD_PATCH, METHOD_PATCH),
                            SelectInputElementOption.of(METHOD_PUT, METHOD_PUT),
                            SelectInputElementOption.of(METHOD_DELETE, METHOD_DELETE)
                    ));
                });

        layout.findChild(BODY_TYPE_FIELD_ID, RadioInputElement.class)
                .ifPresent(field -> {
                    field.setValue(staticValue(BODY_TYPE_JSON));
                    field.setOptions(List.of(
                            RadioInputElementOption.of(BODY_TYPE_JSON, "JSON"),
                            RadioInputElementOption.of(BODY_TYPE_MULTIPART, "Multipart/FormData"),
                            RadioInputElementOption.of(BODY_TYPE_MANUAL, "Manuell")
                    ));
                });

        layout.findChild(SOURCE_MODE_FIELD_ID, RadioInputElement.class)
                .ifPresent(field -> {
                    field.setValue(staticValue(SOURCE_MODE_ALL));
                    field.setOptions(List.of(
                            RadioInputElementOption.of(SOURCE_MODE_ALL, "Alle Vorgangsdaten"),
                            RadioInputElementOption.of(SOURCE_MODE_SELECTED, "Ausgewählte Daten"),
                            RadioInputElementOption.of(SOURCE_MODE_LOW_CODE, "Benutzerdefiniert via Low-Code")
                    ));
                });

        layout.findChild(RESPONSE_TYPE_FIELD_ID, SelectInputElement.class)
                .ifPresent(field -> {
                    field.setValue(staticValue(RESPONSE_TYPE_TEXT));
                    field.setOptions(List.of(
                            SelectInputElementOption.of(RESPONSE_TYPE_JSON, "JSON"),
                            SelectInputElementOption.of(RESPONSE_TYPE_XML, "XML"),
                            SelectInputElementOption.of(RESPONSE_TYPE_TEXT, "Text"),
                            SelectInputElementOption.of(RESPONSE_TYPE_FILE, "Datei")
                    ));
                });

        layout.findChild(EXPECTED_STATUS_CODE_FIELD_ID, NumberInputElement.class)
                .ifPresent(field -> field.setValue(staticValue(200)));

        layout.findChild(JSON_CONTENT_TYPE_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> {
                    field.setDisabled(true);
                    field.setValue(staticValue(HttpServiceHeaders.APPLICATION_JSON));
                    field.setVisibility(visibility(equalsRef(BODY_TYPE_FIELD_ID, BODY_TYPE_JSON)));
                });

        layout.findChild(MULTIPART_CONTENT_TYPE_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> {
                    field.setDisabled(true);
                    field.setValue(staticValue(HttpServiceHeaders.MULTIPART_FORM_DATA));
                    field.setVisibility(visibility(equalsRef(BODY_TYPE_FIELD_ID, BODY_TYPE_MULTIPART)));
                });

        layout.findChild(MANUAL_CONTENT_TYPE_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> field.setVisibility(visibility(equalsRef(BODY_TYPE_FIELD_ID, BODY_TYPE_MANUAL))));

        layout.findChild(OUTGOING_GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> group.setVisibility(visibility(anyOfRef(
                        METHOD_FIELD_ID,
                        METHOD_POST,
                        METHOD_PATCH,
                        METHOD_PUT
                ))));

        layout.findChild(SOURCE_MODE_FIELD_ID, RadioInputElement.class)
                .ifPresent(field -> field.setVisibility(visibility(anyOfRef(
                        BODY_TYPE_FIELD_ID,
                        BODY_TYPE_JSON,
                        BODY_TYPE_MULTIPART
                ))));

        layout.findChild(JSON_SELECTED_PATHS_FIELD_ID, ChipInputElement.class)
                .ifPresent(field -> field.setVisibility(visibility(allOf(
                        equalsRef(BODY_TYPE_FIELD_ID, BODY_TYPE_JSON),
                        equalsRef(SOURCE_MODE_FIELD_ID, SOURCE_MODE_SELECTED)
                ))));

        layout.findChild(MULTIPART_FIELDS_FIELD_ID, ReplicatingContainerLayoutElement.class)
                .ifPresent(field -> field.setVisibility(visibility(allOf(
                        equalsRef(BODY_TYPE_FIELD_ID, BODY_TYPE_MULTIPART),
                        equalsRef(SOURCE_MODE_FIELD_ID, SOURCE_MODE_SELECTED)
                ))));

        layout.findChild(REQUEST_BODY_CODE_FIELD_ID, CodeInputElement.class)
                .ifPresent(field -> {
                    field.setLanguage("javascript");
                    field.setVisibility(visibility(anyOf(
                            equalsRef(BODY_TYPE_FIELD_ID, BODY_TYPE_MANUAL),
                            allOf(
                                    anyOfRef(BODY_TYPE_FIELD_ID, BODY_TYPE_JSON, BODY_TYPE_MULTIPART),
                                    equalsRef(SOURCE_MODE_FIELD_ID, SOURCE_MODE_LOW_CODE)
                            )
                    )));
                });

        layout.findChild(RESPONSE_PROCESSOR_FIELD_ID, CodeInputElement.class)
                .ifPresent(field -> {
                    field.setLanguage("javascript");
                    field.setVisibility(visibility(negate(equalsRef(RESPONSE_TYPE_FIELD_ID, RESPONSE_TYPE_FILE))));
                });

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
                        SUCCESS_PORT,
                        "Erfolg",
                        "Der Prozess wird hier fortgesetzt, wenn die Antwort den erwarteten HTTP-Statuscode liefert."
                ),
                new ProcessNodePort(
                        ERROR_PORT,
                        "Fehler",
                        "Der Prozess wird hier fortgesetzt, wenn der Request fehlschlägt oder die Antwort nicht verarbeitet werden kann."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_NAME_STATUS_CODE, "HTTP-Statuscode", "Der Statuscode der HTTP-Antwort."),
                new ProcessNodeOutput(OUTPUT_NAME_HEADERS, "HTTP-Header", "Die Header der HTTP-Antwort."),
                new ProcessNodeOutput(OUTPUT_NAME_RAW_BODY, "Antwort-Rohtext", "Der Antwort-Body als Text für JSON-, XML- und Text-Antworten."),
                new ProcessNodeOutput(OUTPUT_NAME_PROCESSED_RESPONSE, "Verarbeitete Antwort", "Die verarbeitete Antwort für JSON-, XML- und Text-Antworten."),
                new ProcessNodeOutput(OUTPUT_NAME_FILE_NAME, "Dateiname", "Der Dateiname der gespeicherten Antwortdatei."),
                new ProcessNodeOutput(OUTPUT_NAME_MIME_TYPE, "MIME-Typ", "Der MIME-Typ der gespeicherten Antwortdatei."),
                new ProcessNodeOutput(OUTPUT_NAME_SIZE_BYTES, "Dateigröße", "Die Größe der gespeicherten Antwortdatei in Bytes."),
                new ProcessNodeOutput(OUTPUT_NAME_ATTACHMENT_KEY, "Anhang-Schlüssel", "Der Schlüssel des gespeicherten Prozess-Anhangs."),
                new ProcessNodeOutput(OUTPUT_NAME_STORAGE_PROVIDER_ID, "Speicheranbieter", "Die ID des Speicheranbieters des gespeicherten Prozess-Anhangs."),
                new ProcessNodeOutput(OUTPUT_NAME_STORAGE_PATH_FROM_ROOT, "Speicherpfad", "Der Speicherpfad des gespeicherten Prozess-Anhangs.")
        );
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                           @Nonnull HttpActionNodeConfig configuration) throws ResponseException {
        var errors = new LinkedHashMap<String, List<String>>();

        var general = configuration.general != null ? configuration.general : new HttpActionNodeGeneralConfig();
        var outgoing = configuration.outgoing != null ? configuration.outgoing : new HttpActionNodeOutgoingConfig();
        var incoming = configuration.incoming != null ? configuration.incoming : new HttpActionNodeIncomingConfig();

        var method = normalizeMethod(general.method);
        if (!SUPPORTED_METHODS.contains(method)) {
            addValidationError(errors, METHOD_FIELD_ID, "Die HTTP-Methode ist ungültig.");
        }

        if (StringUtils.isNullOrEmpty(general.url)) {
            addValidationError(errors, URL_FIELD_ID, "Die URL muss angegeben werden.");
        }

        var expectedStatusCode = parseExpectedStatusCode(incoming.expectedStatusCode);
        if (expectedStatusCode == null || expectedStatusCode < 100 || expectedStatusCode > 599) {
            addValidationError(errors, EXPECTED_STATUS_CODE_FIELD_ID, "Der erwartete HTTP-Statuscode muss zwischen 100 und 599 liegen.");
        }

        var responseType = normalizeResponseType(incoming.responseType);
        if (!RESPONSE_TYPES.contains(responseType)) {
            addValidationError(errors, RESPONSE_TYPE_FIELD_ID, "Der erwartete Antworttyp ist ungültig.");
        }

        if (RESPONSE_TYPE_FILE.equals(responseType)) {
            if (StringUtils.isNotNullOrEmpty(incoming.responseProcessorCode)) {
                addValidationError(errors, RESPONSE_PROCESSOR_FIELD_ID, "Für Datei-Antworten darf keine Response-Verarbeitung konfiguriert werden.");
            }
            if (hasOutputMappings(processNodeEntity.getOutputMappings())) {
                addValidationError(errors, RESPONSE_TYPE_FIELD_ID, "Bei Datei-Antworten dürfen keine Ausgabemappings in die Vorgangsdaten konfiguriert werden.");
            }
        }

        if (!supportsBody(method)) {
            return errors.isEmpty() ? null : errors;
        }

        var bodyType = normalizeBodyType(outgoing.bodyType);
        if (!BODY_TYPES.contains(bodyType)) {
            addValidationError(errors, BODY_TYPE_FIELD_ID, "Der Datentyp für ausgehende Daten ist ungültig.");
            return errors.isEmpty() ? null : errors;
        }

        if (BODY_TYPE_MANUAL.equals(bodyType)) {
            if (StringUtils.isNullOrEmpty(outgoing.manualContentType)) {
                addValidationError(errors, MANUAL_CONTENT_TYPE_FIELD_ID, "Für manuelle Bodies muss ein Content-Type angegeben werden.");
            }
            if (StringUtils.isNullOrEmpty(outgoing.requestBodyCode)) {
                addValidationError(errors, REQUEST_BODY_CODE_FIELD_ID, "Für manuelle Bodies muss Low-Code zur Erzeugung des Request-Bodys angegeben werden.");
            }
            return errors.isEmpty() ? null : errors;
        }

        var sourceMode = normalizeSourceMode(outgoing.sourceMode);
        if (!SOURCE_MODES.contains(sourceMode)) {
            addValidationError(errors, SOURCE_MODE_FIELD_ID, "Der Modus für ausgehende Daten ist ungültig.");
            return errors.isEmpty() ? null : errors;
        }

        if (SOURCE_MODE_SELECTED.equals(sourceMode) && BODY_TYPE_JSON.equals(bodyType)) {
            var selectedPaths = normalizeStringList(outgoing.jsonSelectedPaths);
            if (selectedPaths.isEmpty()) {
                addValidationError(errors, JSON_SELECTED_PATHS_FIELD_ID, "Für ausgewählte JSON-Daten muss mindestens ein Pfad angegeben werden.");
            } else {
                for (var path : selectedPaths) {
                    try {
                        normalizeJsonSelectionPath(path);
                    } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
                        addValidationError(errors, JSON_SELECTED_PATHS_FIELD_ID, e.getMessage());
                        break;
                    }
                }
            }
        }

        if (SOURCE_MODE_SELECTED.equals(sourceMode) && BODY_TYPE_MULTIPART.equals(bodyType)) {
            if (outgoing.multipartFields == null || outgoing.multipartFields.isEmpty()) {
                addValidationError(errors, MULTIPART_FIELDS_FIELD_ID, "Für ausgewählte Multipart-Daten muss mindestens ein Feld angegeben werden.");
            } else {
                var rowIndex = 1;
                for (var field : outgoing.multipartFields) {
                    if (field == null || StringUtils.isNullOrEmpty(field.name) || StringUtils.isNullOrEmpty(field.valueKey)) {
                        addValidationError(errors, MULTIPART_FIELDS_FIELD_ID, "Jede Multipart-Zeile benötigt einen Feldnamen und einen Wertpfad.");
                        break;
                    }

                    try {
                        normalizeExecutionDataPath(field.valueKey);
                    } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
                        addValidationError(errors, MULTIPART_FIELDS_FIELD_ID, "Ungültiger Wertpfad in Multipart-Zeile %d: %s".formatted(rowIndex, e.getMessage()));
                        break;
                    }
                    rowIndex++;
                }
            }
        }

        if (SOURCE_MODE_LOW_CODE.equals(sourceMode) && StringUtils.isNullOrEmpty(outgoing.requestBodyCode)) {
            addValidationError(errors, REQUEST_BODY_CODE_FIELD_ID, "Für benutzerdefinierte Daten muss Low-Code angegeben werden.");
        }

        return errors.isEmpty() ? null : errors;
    }

    private static void addValidationError(@Nonnull Map<String, List<String>> errors,
                                           @Nonnull String fieldId,
                                           @Nonnull String message) {
        errors
                .computeIfAbsent(fieldId, ignored -> new LinkedList<>())
                .add(message);
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var general = configuration.general != null ? configuration.general : new HttpActionNodeGeneralConfig();
        var outgoing = configuration.outgoing != null ? configuration.outgoing : new HttpActionNodeOutgoingConfig();
        var incoming = configuration.incoming != null ? configuration.incoming : new HttpActionNodeIncomingConfig();

        var method = normalizeMethod(general.method);
        if (!SUPPORTED_METHODS.contains(method)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die HTTP-Methode %s wird nicht unterstützt.", StringUtils.quote(general.method));
        }

        URI uri = buildUri(context, general);
        var headers = buildHeaders(context, general);
        var responseType = normalizeResponseType(incoming.responseType);
        if (!RESPONSE_TYPES.contains(responseType)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der Antworttyp %s wird nicht unterstützt.", StringUtils.quote(incoming.responseType));
        }

        var expectedStatusCode = parseExpectedStatusCode(incoming.expectedStatusCode);
        if (expectedStatusCode == null || expectedStatusCode < 100 || expectedStatusCode > 599) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der erwartete HTTP-Statuscode ist ungültig.");
        }

        ResponseEntity<byte[]> response;
        try {
            response = executeRequest(context, method, uri, headers, outgoing);
        } catch (HttpConnectionException e) {
            return ProcessNodeExecutionResultTaskCompleted.of(ERROR_PORT)
                    .setNodeData(createBaseMetadata(599, Map.of(), e.getMessage(), null));
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Vorbereiten oder Ausführen des HTTP-Requests: %s",
                    e.getMessage()
            );
        }

        return handleResponse(context, incoming, responseType, expectedStatusCode, uri, response);
    }

    @Nonnull
    private ResponseEntity<byte[]> executeRequest(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                                  @Nonnull String method,
                                                  @Nonnull URI uri,
                                                  @Nonnull HttpServiceHeaders headers,
                                                  @Nonnull HttpActionNodeOutgoingConfig outgoing) throws Exception {
        var httpMethod = HttpMethod.valueOf(method);
        if (!supportsBody(method)) {
            return httpService.request(httpMethod, uri, headers);
        }

        var bodyType = normalizeBodyType(outgoing.bodyType);
        if (!BODY_TYPES.contains(bodyType)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der Datentyp für ausgehende Daten ist ungültig.");
        }

        return switch (bodyType) {
            case BODY_TYPE_JSON -> {
                var payload = buildJsonBody(context, outgoing);
                var requestHeaders = HttpServiceHeaders.create()
                        .with(headers)
                        .withContentType(HttpServiceHeaders.APPLICATION_JSON);
                yield httpService.request(httpMethod, uri, payload, requestHeaders);
            }
            case BODY_TYPE_MULTIPART -> {
                var payload = buildMultipartBody(context, outgoing);
                var requestHeaders = HttpServiceHeaders.create()
                        .with(headers)
                        .withContentType(HttpServiceHeaders.MULTIPART_FORM_DATA);
                yield httpService.request(httpMethod, uri, payload, requestHeaders);
            }
            case BODY_TYPE_MANUAL -> {
                var payload = buildManualBody(context, outgoing);
                var contentType = templateRenderService.interpolate(
                        context.getCurrentProcessExecutionData(),
                        outgoing.manualContentType == null ? "" : outgoing.manualContentType
                );
                if (StringUtils.isNullOrEmpty(contentType)) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für manuelle Bodies muss ein Content-Type angegeben werden.");
                }

                var requestHeaders = HttpServiceHeaders.create()
                        .with(headers)
                        .withContentType(contentType.trim());
                yield httpService.request(httpMethod, uri, payload, requestHeaders);
            }
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der Datentyp für ausgehende Daten ist ungültig.");
        };
    }

    @Nonnull
    private ProcessNodeExecutionResult handleResponse(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                                      @Nonnull HttpActionNodeIncomingConfig incoming,
                                                      @Nonnull String responseType,
                                                      int expectedStatusCode,
                                                      @Nonnull URI requestUri,
                                                      @Nonnull ResponseEntity<byte[]> response) throws ProcessNodeExecutionException {
        var statusCode = response.getStatusCode().value();
        var headers = copyHeaders(response.getHeaders());

        if (RESPONSE_TYPE_FILE.equals(responseType)) {
            var metadata = createBaseMetadata(statusCode, headers, null, null);

            if (statusCode != expectedStatusCode) {
                return ProcessNodeExecutionResultTaskCompleted.of(ERROR_PORT)
                        .setNodeData(metadata);
            }

            byte[] responseBytes = response.getBody() != null ? response.getBody() : new byte[0];
            String fileName = resolveResponseFileName(response, responseBytes, requestUri);
            String mimeType = resolveResponseMimeType(response);

            ProcessInstanceAttachmentEntity attachment;
            try {
                attachment = processInstanceAttachmentService.create(
                        ProcessInstanceAttachmentEntity.of(
                                fileName,
                                context.getThisProcessInstance().getId(),
                                context.getThisTask().getId(),
                                responseBytes
                        )
                );
            } catch (ResponseException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Die HTTP-Antwortdatei konnte nicht als Prozess-Anhang gespeichert werden: %s",
                        e.getMessage()
                );
            }

            metadata.put(OUTPUT_NAME_FILE_NAME, fileName);
            metadata.put(OUTPUT_NAME_MIME_TYPE, mimeType);
            metadata.put(OUTPUT_NAME_SIZE_BYTES, responseBytes.length);
            metadata.put(OUTPUT_NAME_ATTACHMENT_KEY, attachment.getKey());
            metadata.put(OUTPUT_NAME_STORAGE_PROVIDER_ID, attachment.getStorageProviderId());
            metadata.put(OUTPUT_NAME_STORAGE_PATH_FROM_ROOT, attachment.getStoragePathFromRoot());

            return ProcessNodeExecutionResultTaskCompleted.of(SUCCESS_PORT)
                    .setNodeData(metadata);
        }

        String rawBody = decodeResponseBody(response);
        Object parsedResponse;
        try {
            parsedResponse = parseResponseBody(responseType, rawBody);
        } catch (Exception e) {
            return ProcessNodeExecutionResultTaskCompleted.of(ERROR_PORT)
                    .setNodeData(createBaseMetadata(statusCode, headers, rawBody, null));
        }

        Object processedResponse = parsedResponse;
        if (StringUtils.isNotNullOrEmpty(incoming.responseProcessorCode)) {
            try {
                processedResponse = evaluateResponseProcessor(context, incoming.responseProcessorCode, statusCode, headers, rawBody, parsedResponse);
            } catch (Exception e) {
                return ProcessNodeExecutionResultTaskCompleted.of(ERROR_PORT)
                        .setNodeData(createBaseMetadata(statusCode, headers, rawBody, null));
            }
        }

        var resultMetadata = createBaseMetadata(statusCode, headers, rawBody, processedResponse);
        var viaPort = statusCode == expectedStatusCode ? SUCCESS_PORT : ERROR_PORT;

        return ProcessNodeExecutionResultTaskCompleted.of(viaPort)
                .setNodeData(resultMetadata);
    }

    @Nonnull
    private URI buildUri(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                         @Nonnull HttpActionNodeGeneralConfig general) throws ProcessNodeExecutionException {
        var renderedUrl = templateRenderService.interpolate(
                context.getCurrentProcessExecutionData(),
                general.url == null ? "" : general.url
        );
        if (StringUtils.isNullOrEmpty(renderedUrl)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die URL für den HTTP-Request wurde nicht angegeben.");
        }

        var builder = UriComponentsBuilder.fromUriString(renderedUrl.trim());
        if (general.queryParameters != null) {
            for (var row : general.queryParameters) {
                if (row == null) {
                    continue;
                }

                var key = interpolateNullable(context, row.key);
                if (StringUtils.isNullOrEmpty(key)) {
                    continue;
                }

                var value = interpolateNullable(context, row.value);
                builder.queryParam(key.trim(), value == null ? "" : value);
            }
        }

        try {
            return builder.build(true).toUri();
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die URL für den HTTP-Request ist ungültig: %s",
                    renderedUrl
            );
        }
    }

    @Nonnull
    private HttpServiceHeaders buildHeaders(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                            @Nonnull HttpActionNodeGeneralConfig general) {
        var headers = HttpServiceHeaders.create();

        if (general.headers == null) {
            return headers;
        }

        for (var row : general.headers) {
            if (row == null) {
                continue;
            }

            var key = interpolateNullable(context, row.key);
            if (StringUtils.isNullOrEmpty(key)) {
                continue;
            }

            var value = interpolateNullable(context, row.value);
            headers.with(key.trim(), value == null ? "" : value);
        }

        return headers;
    }

    @Nonnull
    private String buildJsonBody(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                 @Nonnull HttpActionNodeOutgoingConfig outgoing) throws Exception {
        var sourceMode = normalizeSourceMode(outgoing.sourceMode);
        if (!SOURCE_MODES.contains(sourceMode)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der Modus für ausgehende JSON-Daten ist ungültig.");
        }

        Object payload = switch (sourceMode) {
            case SOURCE_MODE_ALL -> cloneJsonCompatible(context.getCurrentProcessExecutionData().get("$"));
            case SOURCE_MODE_SELECTED -> buildSelectedJsonBody(context, outgoing);
            case SOURCE_MODE_LOW_CODE -> normalizeJsonCompatible(evaluateRequestCode(context, outgoing.requestBodyCode));
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der Modus für ausgehende JSON-Daten ist ungültig.");
        };

        return ObjectMapperFactory
                .getInstance()
                .writeValueAsString(payload);
    }

    @Nonnull
    private Object buildSelectedJsonBody(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                         @Nonnull HttpActionNodeOutgoingConfig outgoing) throws Exception {
        var selectedPaths = normalizeStringList(outgoing.jsonSelectedPaths);
        if (selectedPaths.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für ausgewählte JSON-Daten muss mindestens ein Pfad angegeben werden.");
        }

        var processRoot = context.getCurrentProcessExecutionData().get("$");
        var result = new LinkedHashMap<String, Object>();
        for (var rawPath : selectedPaths) {
            var normalizedPath = normalizeJsonSelectionPath(rawPath);
            var resolvedValue = resolveSimplePath(processRoot, normalizedPath);
            writeJsonSelection(result, normalizedPath, cloneJsonCompatible(resolvedValue));
        }

        return result;
    }

    @Nonnull
    private MultipartUtils.MultipartBodyPublisher buildMultipartBody(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                                                     @Nonnull HttpActionNodeOutgoingConfig outgoing) throws Exception {
        var sourceMode = normalizeSourceMode(outgoing.sourceMode);
        if (!SOURCE_MODES.contains(sourceMode)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der Modus für ausgehende Multipart-Daten ist ungültig.");
        }

        var publisher = new MultipartUtils.MultipartBodyPublisher();

        switch (sourceMode) {
            case SOURCE_MODE_ALL -> addMultipartFieldsFromMap(publisher, context.getCurrentProcessExecutionData().get("$"));
            case SOURCE_MODE_SELECTED -> addSelectedMultipartFields(publisher, context, outgoing);
            case SOURCE_MODE_LOW_CODE -> addLowCodeMultipartFields(publisher, context, outgoing.requestBodyCode);
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der Modus für ausgehende Multipart-Daten ist ungültig.");
        }

        return publisher;
    }

    private void addMultipartFieldsFromMap(@Nonnull MultipartUtils.MultipartBodyPublisher publisher,
                                           @Nullable Object root) throws JsonProcessingException {
        if (!(root instanceof Map<?, ?> rootMap)) {
            return;
        }

        for (var entry : rootMap.entrySet()) {
            if (!(entry.getKey() instanceof String fieldName) || StringUtils.isNullOrEmpty(fieldName)) {
                continue;
            }

            var value = stringifyMultipartValue(entry.getValue());
            if (value != null) {
                publisher.addPart(fieldName, value);
            }
        }
    }

    private void addSelectedMultipartFields(@Nonnull MultipartUtils.MultipartBodyPublisher publisher,
                                            @Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                            @Nonnull HttpActionNodeOutgoingConfig outgoing) throws Exception {
        if (outgoing.multipartFields == null || outgoing.multipartFields.isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für ausgewählte Multipart-Daten muss mindestens ein Feld angegeben werden.");
        }

        for (var row : outgoing.multipartFields) {
            if (row == null || StringUtils.isNullOrEmpty(row.name) || StringUtils.isNullOrEmpty(row.valueKey)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("Jede Multipart-Zeile benötigt einen Feldnamen und einen Wertpfad.");
            }

            var fieldName = row.name.trim();
            var resolvedValue = resolveExecutionDataPath(context.getCurrentProcessExecutionData(), row.valueKey);
            var stringValue = stringifyMultipartValue(resolvedValue);
            if (stringValue != null) {
                publisher.addPart(fieldName, stringValue);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addLowCodeMultipartFields(@Nonnull MultipartUtils.MultipartBodyPublisher publisher,
                                           @Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                           @Nullable String code) throws Exception {
        var result = evaluateRequestCode(context, code);
        if (!(result instanceof Map<?, ?> rawMap)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Low-Code für Multipart/FormData muss ein Objekt mit Feldnamen und Werten zurückgeben.");
        }

        for (var entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key) || StringUtils.isNullOrEmpty(key)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("Low-Code für Multipart/FormData enthält einen ungültigen Feldnamen.");
            }

            var value = stringifyMultipartValue(entry.getValue());
            if (value != null) {
                publisher.addPart(key, value);
            }
        }
    }

    @Nonnull
    private String buildManualBody(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                   @Nonnull HttpActionNodeOutgoingConfig outgoing) throws Exception {
        var result = evaluateRequestCode(context, outgoing.requestBodyCode);
        if (result == null) {
            return "";
        }
        if (result instanceof Map<?, ?> || result instanceof Collection<?>) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Low-Code für manuelle Bodies muss einen Textwert zurückgeben.");
        }
        return result.toString();
    }

    @Nullable
    private Object evaluateRequestCode(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                       @Nullable String code) throws Exception {
        if (StringUtils.isNullOrEmpty(code)) {
            return null;
        }

        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService.fillJsEngineWithData(context.getCurrentProcessExecutionData(), engine);
            return engine
                    .evaluateCode(new JavascriptCode().setCode(code))
                    .asObject();
        }
    }

    @Nullable
    private Object evaluateResponseProcessor(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                             @Nonnull String code,
                                             @Nullable Integer statusCode,
                                             @Nonnull Map<String, Object> headers,
                                             @Nullable String rawBody,
                                             @Nullable Object parsedResponse) throws Exception {
        try (var engine = javascriptEngineFactoryService.getEngine()) {
            ProcessDataService.fillJsEngineWithData(context.getCurrentProcessExecutionData(), engine);

            var responseContext = new LinkedHashMap<String, Object>();
            responseContext.put("statusCode", statusCode);
            responseContext.put("headers", headers);
            responseContext.put("rawBody", rawBody);
            responseContext.put("body", parsedResponse);
            engine.registerGlobalObject("response", responseContext);

            var result = engine
                    .evaluateCode(new JavascriptCode().setCode(code))
                    .asObject();

            return normalizeJsonCompatible(result);
        }
    }

    @Nullable
    private Object parseResponseBody(@Nonnull String responseType,
                                     @Nullable String rawBody) throws JsonProcessingException {
        if (RESPONSE_TYPE_TEXT.equals(responseType) || RESPONSE_TYPE_XML.equals(responseType)) {
            return rawBody;
        }

        if (StringUtils.isNullOrEmpty(rawBody)) {
            return null;
        }

        return ObjectMapperFactory
                .getInstance()
                .readValue(rawBody, Object.class);
    }

    @Nullable
    private String decodeResponseBody(@Nonnull ResponseEntity<byte[]> response) {
        byte[] body = response.getBody();
        if (body == null) {
            return null;
        }

        Charset charset = StandardCharsets.UTF_8;
        var mediaType = response.getHeaders().getContentType();
        if (mediaType != null && mediaType.getCharset() != null) {
            charset = mediaType.getCharset();
        }

        return new String(body, charset);
    }

    @Nonnull
    private Map<String, Object> createBaseMetadata(@Nullable Integer statusCode,
                                                   @Nonnull Map<String, Object> headers,
                                                   @Nullable String rawBody,
                                                   @Nullable Object processedResponse) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put(OUTPUT_NAME_STATUS_CODE, statusCode);
        metadata.put(OUTPUT_NAME_HEADERS, headers);
        metadata.put(OUTPUT_NAME_RAW_BODY, rawBody);
        metadata.put(OUTPUT_NAME_PROCESSED_RESPONSE, processedResponse);
        metadata.put(OUTPUT_NAME_FILE_NAME, null);
        metadata.put(OUTPUT_NAME_MIME_TYPE, null);
        metadata.put(OUTPUT_NAME_SIZE_BYTES, null);
        metadata.put(OUTPUT_NAME_ATTACHMENT_KEY, null);
        metadata.put(OUTPUT_NAME_STORAGE_PROVIDER_ID, null);
        metadata.put(OUTPUT_NAME_STORAGE_PATH_FROM_ROOT, null);
        return metadata;
    }

    @Nonnull
    private Map<String, Object> copyHeaders(@Nonnull org.springframework.http.HttpHeaders headers) {
        var copied = new LinkedHashMap<String, Object>();
        headers.forEach((key, value) -> copied.put(key, List.copyOf(value)));
        return copied;
    }

    @Nonnull
    private String resolveResponseFileName(@Nonnull ResponseEntity<byte[]> response,
                                           @Nonnull byte[] responseBytes,
                                           @Nonnull URI requestUri) {
        var contentDispositionHeader = response.getHeaders().getFirst(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION);
        if (StringUtils.isNotNullOrEmpty(contentDispositionHeader)) {
            try {
                var parsed = ContentDisposition.parse(contentDispositionHeader);
                if (StringUtils.isNotNullOrEmpty(parsed.getFilename())) {
                    return parsed.getFilename();
                }
            } catch (Exception ignored) {
            }
        }

        var pathSegment = StringUtils.getLastPathSegment(requestUri.getPath());
        if (StringUtils.isNotNullOrEmpty(pathSegment)) {
            return pathSegment;
        }

        if (responseBytes.length == 0) {
            return "response.bin";
        }

        return "response.bin";
    }

    @Nonnull
    private String resolveResponseMimeType(@Nonnull ResponseEntity<byte[]> response) {
        var mediaType = response.getHeaders().getContentType();
        return mediaType != null ? mediaType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    @Nullable
    private String interpolateNullable(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeConfig> context,
                                       @Nullable String value) {
        if (value == null) {
            return null;
        }

        return templateRenderService.interpolate(context.getCurrentProcessExecutionData(), value);
    }

    @Nonnull
    private static String normalizeMethod(@Nullable String method) {
        return StringUtils.isNullOrEmpty(method) ? METHOD_GET : method.trim().toUpperCase(Locale.ROOT);
    }

    @Nonnull
    private static String normalizeBodyType(@Nullable String bodyType) {
        return StringUtils.isNullOrEmpty(bodyType) ? BODY_TYPE_JSON : bodyType.trim();
    }

    @Nonnull
    private static String normalizeSourceMode(@Nullable String sourceMode) {
        return StringUtils.isNullOrEmpty(sourceMode) ? SOURCE_MODE_ALL : sourceMode.trim();
    }

    @Nonnull
    private static String normalizeResponseType(@Nullable String responseType) {
        return StringUtils.isNullOrEmpty(responseType) ? RESPONSE_TYPE_TEXT : responseType.trim();
    }

    private static boolean supportsBody(@Nonnull String method) {
        return METHOD_POST.equals(method) || METHOD_PATCH.equals(method) || METHOD_PUT.equals(method);
    }

    @Nullable
    private static Integer parseExpectedStatusCode(@Nullable Object rawValue) {
        return switch (rawValue) {
            case null -> 200;
            case Integer i -> i;
            case Long l -> l.intValue();
            case Double d -> d.intValue();
            case Float f -> f.intValue();
            case BigDecimal bigDecimal -> bigDecimal.intValue();
            case Number number -> number.intValue();
            case String s -> {
                try {
                    yield Integer.parseInt(s.trim());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    @Nonnull
    private static List<String> normalizeStringList(@Nullable List<String> rawValues) {
        if (rawValues == null) {
            return List.of();
        }

        return rawValues.stream()
                .map(StringUtils::toNullableTrimmedString)
                .filter(Objects::nonNull)
                .toList();
    }

    @Nonnull
    private static String normalizeJsonSelectionPath(@Nullable String rawPath) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var path = StringUtils.toNullableTrimmedString(rawPath);
        if (path == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Ein JSON-Auswahlpfad ist leer.");
        }

        if (path.startsWith("$.")) {
            path = path.substring(2);
        } else if (path.equals("$")) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die JSON-Auswahl der kompletten Vorgangsdatenwurzel wird in diesem Modus nicht unterstützt.");
        } else if (path.startsWith("_") || path.startsWith("$$")) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("JSON-Auswahlpfade dürfen sich nur auf die Vorgangsdatenwurzel $ beziehen.");
        }

        validateSimplePath(path, "JSON-Auswahlpfad");
        return path;
    }

    @Nonnull
    private static ResolvedExecutionDataPath normalizeExecutionDataPath(@Nullable String rawPath) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var path = StringUtils.toNullableTrimmedString(rawPath);
        if (path == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der Wertpfad ist leer.");
        }

        if (path.equals("$")) {
            return new ResolvedExecutionDataPath("$", "");
        }
        if (path.equals("_")) {
            return new ResolvedExecutionDataPath("_", "");
        }

        if (path.startsWith("$.")) {
            path = path.substring(2);
            validateSimplePath(path, "Wertpfad");
            return new ResolvedExecutionDataPath("$", path);
        }

        if (path.startsWith("_.")) {
            path = path.substring(2);
            validateSimplePath(path, "Wertpfad");
            return new ResolvedExecutionDataPath("_", path);
        }

        if (path.startsWith("$$")) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Wertpfade dürfen nur auf $ oder _ zeigen.");
        }

        validateSimplePath(path, "Wertpfad");
        return new ResolvedExecutionDataPath("$", path);
    }

    private static void validateSimplePath(@Nonnull String path,
                                           @Nonnull String fieldLabel) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (StringUtils.isNullOrEmpty(path)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("%s darf nicht leer sein.", fieldLabel);
        }

        var parts = path.split("\\.");
        for (var part : parts) {
            if (StringUtils.isNullOrEmpty(part)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("%s enthält einen leeren Pfadteil.", fieldLabel);
            }
            if (part.contains("*") || part.contains("[") || part.contains("]")) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("%s unterstützt keine Wildcards oder Array-Klammern.", fieldLabel);
            }
        }
    }

    @Nullable
    private static Object resolveExecutionDataPath(@Nonnull ProcessExecutionData processExecutionData,
                                                   @Nonnull String rawPath) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var resolvedPath = normalizeExecutionDataPath(rawPath);
        var root = processExecutionData.get(resolvedPath.rootKey());
        return resolveSimplePath(root, resolvedPath.path());
    }

    @Nullable
    private static Object resolveSimplePath(@Nullable Object root,
                                            @Nullable String path) {
        if (root == null) {
            return null;
        }
        if (StringUtils.isNullOrEmpty(path)) {
            return root;
        }

        Object current = root;
        for (var part : path.split("\\.")) {
            if (current == null) {
                return null;
            }

            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
                continue;
            }

            if (current instanceof List<?> list) {
                try {
                    var index = Integer.parseInt(part);
                    if (index < 0 || index >= list.size()) {
                        return null;
                    }
                    current = list.get(index);
                    continue;
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }

            return null;
        }

        return current;
    }

    private static void writeJsonSelection(@Nonnull Map<String, Object> target,
                                           @Nonnull String path,
                                           @Nullable Object value) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var parts = path.split("\\.");
        Map<String, Object> current = target;
        for (int i = 0; i < parts.length - 1; i++) {
            var part = parts[i];
            if (part.chars().allMatch(Character::isDigit)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("JSON-Auswahlpfade mit numerischen Zwischenpfaden werden in diesem Modus nicht unterstützt.");
            }

            var existing = current.get(part);
            if (existing instanceof Map<?, ?> existingMap) {
                current = castStringObjectMap(existingMap);
                continue;
            }
            if (existing != null) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("JSON-Auswahlpfade überschreiben einen vorhandenen Nicht-Objekt-Pfad.");
            }

            var newObject = new LinkedHashMap<String, Object>();
            current.put(part, newObject);
            current = newObject;
        }

        current.put(parts[parts.length - 1], value);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private static Map<String, Object> castStringObjectMap(@Nonnull Map<?, ?> rawMap) {
        return (Map<String, Object>) rawMap;
    }

    @Nullable
    private static Object normalizeJsonCompatible(@Nullable Object value) throws Exception {
        if (value == null) {
            return null;
        }

        byte[] serialized = ObjectMapperFactory
                .getInstance()
                .writeValueAsBytes(value);

        return ObjectMapperFactory
                .getInstance()
                .readValue(serialized, Object.class);
    }

    @Nullable
    private static Object cloneJsonCompatible(@Nullable Object value) throws Exception {
        return normalizeJsonCompatible(value);
    }

    @Nullable
    private static String stringifyMultipartValue(@Nullable Object value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Character || value instanceof Enum<?>) {
            return value.toString();
        }
        return ObjectMapperFactory
                .getInstance()
                .writeValueAsString(value);
    }

    private static boolean hasOutputMappings(@Nullable Map<String, String> outputMappings) {
        if (outputMappings == null || outputMappings.isEmpty()) {
            return false;
        }

        return outputMappings.values().stream().anyMatch(StringUtils::isNotNullOrEmpty);
    }

    @Nonnull
    private static ElementValueFunctions staticValue(@Nullable Object value) {
        return new ElementValueFunctions()
                .setType(ValueFunctionType.NoCode)
                .setNoCode(new NoCodeStaticValue(value));
    }

    @Nonnull
    private static ElementVisibilityFunctions visibility(@Nonnull NoCodeOperand operand) {
        return ElementVisibilityFunctions
                .of(operand)
                .recalculateReferencedIds();
    }

    @Nonnull
    private static NoCodeOperand equalsRef(@Nonnull String fieldId,
                                           @Nonnull String expectedValue) {
        return NoCodeExpression.of(
                NoCodeEqualsOperator.OPERATOR_ID,
                NoCodeReference.of(fieldId),
                new NoCodeStaticValue(expectedValue)
        );
    }

    @Nonnull
    private static NoCodeOperand negate(@Nonnull NoCodeOperand operand) {
        return NoCodeExpression.of(NoCodeNotOperator.OPERATOR_ID, operand);
    }

    @Nonnull
    private static NoCodeOperand anyOfRef(@Nonnull String fieldId,
                                          @Nonnull String... values) {
        var operands = Arrays.stream(values)
                .map(value -> equalsRef(fieldId, value))
                .toArray(NoCodeOperand[]::new);
        return anyOf(operands);
    }

    @Nonnull
    private static NoCodeOperand anyOf(@Nonnull NoCodeOperand... operands) {
        if (operands.length == 1) {
            return operands[0];
        }
        return NoCodeExpression.of(NoCodeOrOperator.OPERATOR_ID, operands);
    }

    @Nonnull
    private static NoCodeOperand allOf(@Nonnull NoCodeOperand... operands) {
        if (operands.length == 1) {
            return operands[0];
        }

        var negatedOperands = Arrays.stream(operands)
                .map(HttpActionNodeV1::negate)
                .toArray(NoCodeOperand[]::new);

        return negate(NoCodeExpression.of(NoCodeOrOperator.OPERATOR_ID, negatedOperands));
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class HttpActionNodeConfig {
        public HttpActionNodeGeneralConfig general;
        public HttpActionNodeOutgoingConfig outgoing;
        public HttpActionNodeIncomingConfig incoming;
    }

    @LayoutElementPOJOBinding(id = GENERAL_GROUP_ID, type = ElementType.GroupLayout)
    public static class HttpActionNodeGeneralConfig {
        @InputElementPOJOBinding(id = METHOD_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "HTTP-Methode"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 4.0)
        })
        public String method;

        @InputElementPOJOBinding(id = URL_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Endpunkt / URL"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 8.0)
        })
        public String url;

        public List<HttpActionNodeKeyValueRow> headers;
        public List<HttpActionNodeQueryParameterRow> queryParameters;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = HEADERS_FIELD_ID, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Header"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Optionale HTTP-Header für den Request."),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Header #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Header hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Header entfernen")
    })
    public static class HttpActionNodeKeyValueRow {
        @InputElementPOJOBinding(id = "key", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Name"),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 5.0)
        })
        public String key;

        @InputElementPOJOBinding(id = "value", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Wert"),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 7.0)
        })
        public String value;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = QUERY_PARAMETERS_FIELD_ID, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Query-Parameter"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Optionale Query-Parameter für den Request."),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Parameter #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Parameter hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Parameter entfernen")
    })
    public static class HttpActionNodeQueryParameterRow extends HttpActionNodeKeyValueRow {
    }

    @LayoutElementPOJOBinding(id = OUTGOING_GROUP_ID, type = ElementType.GroupLayout)
    public static class HttpActionNodeOutgoingConfig {
        @InputElementPOJOBinding(id = BODY_TYPE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Datentyp"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String bodyType;

        @InputElementPOJOBinding(id = JSON_CONTENT_TYPE_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Content-Type"),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String jsonContentType;

        @InputElementPOJOBinding(id = MULTIPART_CONTENT_TYPE_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Content-Type"),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String multipartContentType;

        @InputElementPOJOBinding(id = MANUAL_CONTENT_TYPE_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Content-Type"),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0)
        })
        public String manualContentType;

        @InputElementPOJOBinding(id = SOURCE_MODE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Datenquelle"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String sourceMode;

        @InputElementPOJOBinding(id = JSON_SELECTED_PATHS_FIELD_ID, type = ElementType.ChipInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Ausgewählte JSON-Pfade"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Pfade innerhalb der Vorgangsdatenwurzel $, z. B. person.vorname oder antrag.id.")
        })
        public List<String> jsonSelectedPaths;

        public List<HttpActionNodeMultipartFieldConfig> multipartFields;

        @InputElementPOJOBinding(id = REQUEST_BODY_CODE_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Low-Code"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Erzeugt die ausgehenden Daten. Für JSON wird ein JSON-kompatibler Wert erwartet, für Multipart ein Objekt und für manuelle Bodies ein Textwert."),
                @ElementPOJOBindingProperty(key = "editorHeight", intValue = 220)
        })
        public String requestBodyCode;
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = MULTIPART_FIELDS_FIELD_ID, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Multipart-Felder"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Pro Zeile wird ein Feldname und ein Wertpfad definiert. Der aufgelöste Wert wird automatisch in einen String umgewandelt."),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "Feld #"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Feld hinzufügen"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Feld entfernen")
    })
    public static class HttpActionNodeMultipartFieldConfig {
        @InputElementPOJOBinding(id = "name", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Feldname"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 5.0)
        })
        public String name;

        @InputElementPOJOBinding(id = "valueKey", type = ElementType.ProcessDataKeyInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Wertpfad"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Pfad, über welchen der Wert für dieses Feld gelesen werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 7.0)
        })
        public String valueKey;
    }

    @LayoutElementPOJOBinding(id = INCOMING_GROUP_ID, type = ElementType.GroupLayout)
    public static class HttpActionNodeIncomingConfig {
        @InputElementPOJOBinding(id = RESPONSE_TYPE_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Erwarteter Datentyp"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0)
        })
        public String responseType;

        @InputElementPOJOBinding(id = EXPECTED_STATUS_CODE_FIELD_ID, type = ElementType.Number, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Erwarteter HTTP-Statuscode"),
                @ElementPOJOBindingProperty(key = "decimalPlaces", intValue = 0),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0)
        })
        public Object expectedStatusCode;

        @InputElementPOJOBinding(id = RESPONSE_PROCESSOR_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Response-Verarbeitung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optionaler Low-Code zur Verarbeitung der Antwort. Verfügbar ist das Objekt response mit statusCode, headers, rawBody und body."),
                @ElementPOJOBindingProperty(key = "editorHeight", intValue = 220)
        })
        public String responseProcessorCode;
    }

    private record ResolvedExecutionDataPath(@Nonnull String rootKey,
                                             @Nonnull String path) {
    }
}
