package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.core.exceptions.HttpConnectionException;
import de.aivot.prosuna.backend.core.models.HttpServiceHeaders;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.input.InputModePolicy;
import de.aivot.prosuna.backend.elements.models.input.DynamicTextPolicy;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.TemplateOutputFormat;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.MultipartUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.safeUrl;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.document;

@Component
public class HttpActionNodeV1 implements ProcessNodeDefinition<HttpActionNodeV1Config> {
    public static final String NODE_KEY = "http_request";

    private static final String SUCCESS_PORT = "success";

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
    private static final String OUTPUT_NAME_FILES = "files";
    private static final String OUTPUT_HEADERS_TYPE_DEFINITION = "Record<string, Array<string>>";
    private static final String OUTPUT_FILES_TYPE_DEFINITION =
            "Array<{ name: string; originalFileName: string; uri: string; size: number; }>";

    private static final Set<String> SUPPORTED_METHODS = Set.of(
            HttpActionNodeV1Config.HTTP_METHOD_OPT_GET,
            HttpActionNodeV1Config.HTTP_METHOD_OPT_POST,
            HttpActionNodeV1Config.HTTP_METHOD_OPT_PUT,
            HttpActionNodeV1Config.HTTP_METHOD_OPT_PATCH,
            HttpActionNodeV1Config.HTTP_METHOD_OPT_DELETE
    );

    private static final Set<String> BODY_METHODS = Set.of(
            HttpActionNodeV1Config.HTTP_METHOD_OPT_POST,
            HttpActionNodeV1Config.HTTP_METHOD_OPT_PUT,
            HttpActionNodeV1Config.HTTP_METHOD_OPT_PATCH
    );

    private static final Set<String> REQUEST_CONTENT_TYPES = Set.of(
            HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_JSON,
            HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_MULTIPARTFORMDATA,
            HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_FORMURLENCODED,
            HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_MANUELL
    );

    private static final Set<String> RESPONSE_BODY_TYPES = Set.of(
            HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_JSON,
            HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_TEXT,
            HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_DATEI
    );

    private static final String MULTIPART_ATTACHMENT_FIELD_NAME = "files";
    private static final InputModePolicy DYNAMIC_TEXT_INPUT_MODE_POLICY = new InputModePolicy(
            List.of(InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode),
            InputMode.Literal
    );
    private static final DynamicTextPolicy DYNAMIC_TEXT_POLICY = new DynamicTextPolicy();

    @Value("classpath:/nodes/configs/HttpActionNodeV1Config.json")
    private Resource configResource;

    private final HttpService httpService;
    private final TemplateRenderService templateRenderService;
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final StorageService storageService;
    private final SecretService secretService;

    public HttpActionNodeV1(HttpService httpService,
                            TemplateRenderService templateRenderService,
                            JavascriptEngineFactoryService javascriptEngineFactoryService,
                            ProcessInstanceAttachmentService processInstanceAttachmentService,
                            ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                            StorageService storageService,
                            SecretService secretService) {
        this.httpService = httpService;
        this.templateRenderService = templateRenderService;
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.storageService = storageService;
        this.secretService = secretService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<HttpActionNodeV1Config> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var configuration = context.configurationOfExecutingNode();
        var endpoint = summary.metadata("endpoint");
        var url = endpoint == null ? safeUrl(configuration.url) : endpoint.toString();
        var code = summary.data(OUTPUT_NAME_STATUS_CODE);
        var category = code instanceof Number number ? switch (number.intValue() / 100) {
            case 2 -> "Erfolgreiche HTTP-Antwort";
            case 3 -> "Weiterleitung";
            case 4 -> "Fehler bei der Anfrage";
            case 5 -> "Fehler beim angesprochenen Server";
            default -> "Information";
        } : "";
        var attachmentKey = StringUtils.toNullableTrimmedString(summary.data(OUTPUT_NAME_ATTACHMENT_KEY));
        return "Die HTTP-Schnittstelle wurde" + summary.completedAt() + " aufgerufen."
                + section("Endpunkt", url)
                + section("HTTP-Statuscode", text(code) + (category.isEmpty() ? "" : " (" + category + ")"))
                + (attachmentKey == null ? "" : section("Antwortanlage", document(attachmentKey, "Antwortanlage ansehen")))
                + detail("Dateiname", summary.data(OUTPUT_NAME_FILE_NAME))
                + detail("Dateityp (MIME-Typ)", summary.data(OUTPUT_NAME_MIME_TYPE))
                + detail("Dateigröße in Bytes", summary.data(OUTPUT_NAME_SIZE_BYTES))
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
        return "HTTP-Schnittstelle aufrufen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Ruft eine HTTP-Schnittstelle auf, übermittelt bei Bedarf Daten und stellt die Antwort im Vorgang bereit.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                ### Wofür verwenden Sie dieses Prozesselement?

                Verbinden Sie einen Vorgang mit einem Fachsystem oder einem anderen Dienst, der eine HTTP-Schnittstelle bereitstellt. Sie können dort Informationen abrufen oder Daten aus dem Vorgang übermitteln und die Antwort in weiteren Schritten verwenden.

                ### Was richten Sie ein?

                Geben Sie die Adresse der HTTP-Schnittstelle an und wählen Sie die vom Zielsystem verlangte HTTP-Methode. Falls ein Zugang erforderlich ist, richten Sie Basic mit Nutzername und gespeichertem Passwort oder einen Bearer-Token ein. Zusätzliche HTTP-Header können Sie gesondert ergänzen.

                Für POST, PUT und PATCH wählen Sie das Format des Anfrageinhalts. JSON kann aus einem Vorgangsdatenpfad gelesen oder mit Low-Code zusammengestellt werden. Bei Formulardaten legen Sie Feldnamen und Werte fest. Mit „Formulardaten mit Dateien (Multipart)“ können Sie zusätzlich vollständige Anlagensätze übertragen. „Ohne Anfrageinhalt“ sendet keine Daten im Anfragekörper. GET und DELETE werden hier ebenfalls ohne Anfrageinhalt ausgeführt.

                Tragen Sie die zugelassenen HTTP-Statuscodes ein, zum Beispiel 200 oder 201. Legen Sie anschließend fest, ob die Antwort als JSON, Text oder Datei verarbeitet werden soll. Für eine Datei geben Sie den gewünschten Namen einschließlich Dateiendung an.

                ### Was passiert bei der Ausführung?

                Prosuna setzt die benötigten Angaben aus dem Vorgang ein, sendet die Anfrage und wartet auf die Antwort. JSON-Antworten werden eingelesen, Textantworten als Text übernommen. Eine als Datei empfangene Antwort wird als Anlage zum Vorgang gespeichert.

                Wenn der Statuscode zugelassen ist und die Antwort verarbeitet werden konnte, setzt Prosuna den Vorgang über „Antwort verarbeitet“ fort. Andernfalls schlägt die Aufgabe fehl. Für diesen Schritt entsteht keine Mitarbeitendenaufgabe.

                ### Welches Ergebnis steht danach bereit?

                In den Elementdaten stehen der HTTP-Statuscode und die Antwortheader bereit. Bei JSON- und Textantworten stehen dort zusätzlich der Antworttext und die Antwortdaten. Bei einer Datei stehen stattdessen Angaben zur gespeicherten Anlage und ein Dateiverweis bereit.

                Die Ergebnisse werden nicht automatisch in die Vorgangsdaten übernommen. Weitere Prozesselemente können die Elementdaten verwenden. Der Vorgangsverlauf zeigt den aufgerufenen Endpunkt und den erhaltenen HTTP-Statuscode.

                ### Wie sieht ein Beispiel aus?

                Ein Fachsystem soll die Angaben zur Person erhalten. Sie wählen POST, das Format JSON und den Vorgangsdatenpfad „person“. Die Schnittstelle antwortet mit 201 und einer JSON-Antwort mit der externen Kennung. Wenn Sie 201 zugelassen haben, stellt Prosuna die Antwort in den Elementdaten für den nächsten Schritt bereit.

                ### Was ist zu beachten?

                Die Einstellungen müssen zu den Vorgaben der angesprochenen Schnittstelle passen. Ein zugelassener HTTP-Statuscode bedeutet nicht automatisch, dass die gewünschte fachliche Bearbeitung im Zielsystem abgeschlossen ist. Prüfen Sie bei Bedarf die Antwortdaten in einem nachfolgenden Schritt.

                Ein Bearer-Token muss bei der Ausführung gültig sein. Dieses Element beschafft oder erneuert ihn nicht selbst.

                Wenn die Aufgabe nach dem Absenden fehlschlägt, kann die Anfrage im Zielsystem bereits eine Änderung ausgelöst haben. Prüfen Sie das Ergebnis dort, bevor Sie einen solchen Aufruf wiederholen.
                """;
    }

    @Nonnull
    @Override
    public Class<HttpActionNodeV1Config> getNodeConfigurationClass() {
        return HttpActionNodeV1Config.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        var layout = loadConfigLayoutFromResource(configResource);

        // This legacy node loads its layout from a static resource instead of its annotated configuration class.
        // Keep dynamic execution opt-in in Java so the trusted policy remains visible next to the node behavior.
        for (var fieldId : List.of(
                HttpActionNodeV1Config.URL_FIELD_ID,
                HttpActionNodeV1Config.BearerAuthConfig.BEARER_TOKEN_FIELD_ID
        )) {
            layout.findChild(fieldId, TextInputElement.class)
                    .ifPresent(field -> field.setInputModePolicy(DYNAMIC_TEXT_INPUT_MODE_POLICY));
        }

        for (var fieldId : List.of(
                HttpActionNodeV1Config.URL_FIELD_ID,
                HttpActionNodeV1Config.BearerAuthConfig.BEARER_TOKEN_FIELD_ID,
                HttpActionNodeV1Config.ResponseConfig.RESPONSE_FILE_NAME_FIELD_ID
        )) {
            layout.findChild(fieldId, TextInputElement.class)
                    .ifPresent(field -> field.setDynamicTextPolicy(DYNAMIC_TEXT_POLICY));
        }

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull HttpActionNodeV1Config configuration) {
        return List.of(
                new ProcessNodePort(
                        SUCCESS_PORT,
                        "Antwort verarbeitet",
                        "Der Vorgang wird hier fortgesetzt, wenn der Statuscode zugelassen ist und die Antwort wie festgelegt verarbeitet wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_NAME_STATUS_CODE, "HTTP-Statuscode", "Der Statuscode der HTTP-Antwort.", "number"),
                new ProcessNodeOutput(OUTPUT_NAME_HEADERS, "Antwortheader", "Die Header der HTTP-Antwort mit ihren jeweiligen Werten.", OUTPUT_HEADERS_TYPE_DEFINITION),
                new ProcessNodeOutput(OUTPUT_NAME_RAW_BODY, "Antworttext", "Der als Text gelesene Antwortinhalt bei JSON- und Textantworten.", "string | null"),
                new ProcessNodeOutput(OUTPUT_NAME_PROCESSED_RESPONSE, "Antwortdaten", "Der eingelesene JSON-Wert beziehungsweise der Antworttext. Eine zusätzliche Nachbearbeitung findet hier nicht statt.", "unknown"),
                new ProcessNodeOutput(OUTPUT_NAME_FILE_NAME, "Dateiname der Antwortanlage", "Der Dateiname der gespeicherten Antwortanlage.", "string | null"),
                new ProcessNodeOutput(OUTPUT_NAME_MIME_TYPE, "Dateityp (MIME-Typ)", "Der von der Schnittstelle angegebene Dateityp der Antwortdatei; ohne Angabe wird application/octet-stream verwendet.", "string | null"),
                new ProcessNodeOutput(OUTPUT_NAME_SIZE_BYTES, "Dateigröße in Bytes", "Die Größe der gespeicherten Antwortdatei in Bytes.", "number | null"),
                new ProcessNodeOutput(OUTPUT_NAME_ATTACHMENT_KEY, "Anlagenschlüssel", "Der Schlüssel der gespeicherten Anlage.", "string | null"),
                new ProcessNodeOutput(OUTPUT_NAME_STORAGE_PROVIDER_ID, "Speicheranbieter-ID", "Die ID des Speicheranbieters der gespeicherten Anlage.", "number | null"),
                new ProcessNodeOutput(OUTPUT_NAME_STORAGE_PATH_FROM_ROOT, "Speicherpfad", "Der Speicherpfad der gespeicherten Anlage.", "string | null"),
                new ProcessNodeOutput(OUTPUT_NAME_FILES, "Dateiverweise", "Eine Liste mit dem Dateiverweis auf die gespeicherte Antwortanlage. Bei JSON- und Textantworten bleibt diese Liste leer.", OUTPUT_FILES_TYPE_DEFINITION)
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull HttpActionNodeV1Config configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        if (HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_DATEI.equals(configuration.responseConfig.responseBodyType)) {
            return ProcessNodeDefinitionMetadata
                    .reuse(previousMetadata)
                    .addForwardedAttachmentSet(
                            processNodeEntity.getDataKey(),
                            StringUtils.isNotNullOrEmpty(configuration.responseConfig.responseFileName)
                                    ? configuration.responseConfig.responseFileName
                                    : Objects.requireNonNullElse(processNodeEntity.getName(), "HTTP-Anfrage"),
                            null,
                            false,
                            processNodeEntity
                    );
        }

        return previousMetadata;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var method = normalizeMethod(configuration.httpMethod);
        if (!SUPPORTED_METHODS.contains(method)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die HTTP-Methode %s wird nicht unterstützt.", StringUtils.quote(configuration.httpMethod));
        }

        var uri = buildUri(context, configuration.url);
        var headers = buildHeaders(context, configuration);
        var responseType = normalizeResponseBodyType(configuration.responseConfig);
        var allowedStatusCodes = parseAllowedStatusCodes(configuration.responseConfig);

        ResponseEntity<byte[]> response;
        try {
            response = executeRequest(context, configuration, method, uri, headers);
        } catch (HttpConnectionException e) {
            // The two-argument overload drops the cause; use the cause-preserving formatted overload.
            throw new ProcessNodeExecutionExceptionIO(
                    e,
                    "%s",
                    "Bei der Kommunikation mit der HTTP-Schnittstelle ist ein Fehler aufgetreten."
            );
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die HTTP-Anfrage konnte nicht vorbereitet oder ausgeführt werden."
            );
        }

        var result = handleResponse(context, responseType, allowedStatusCodes, uri, response);
        result.setRuntimeData(ExecutionSummaryMarkdown.withMetadata(result.getRuntimeData(),
                Map.of("endpoint", safeUrl(uri))));
        return result;
    }

    @Nonnull
    private ResponseEntity<byte[]> executeRequest(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                                  @Nonnull HttpActionNodeV1Config configuration,
                                                  @Nonnull String method,
                                                  @Nonnull URI uri,
                                                  @Nonnull HttpServiceHeaders headers) throws Exception {
        var httpMethod = HttpMethod.valueOf(method);
        var requestData = configuration.requestData;
        if (!BODY_METHODS.contains(method) || requestData == null) {
            return httpService.request(httpMethod, uri, headers);
        }

        var requestContentType = normalizeRequestContentType(requestData.requestContentType);
        if (!REQUEST_CONTENT_TYPES.contains(requestContentType)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Datentyp für ausgehende Daten %s wird nicht unterstützt.",
                    StringUtils.quote(requestData.requestContentType)
            );
        }

        return switch (requestContentType) {
            case HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_JSON -> {
                var payload = buildJsonBody(context, requestData);
                var requestHeaders = HttpServiceHeaders
                        .create()
                        .with(headers)
                        .withContentType(HttpServiceHeaders.APPLICATION_JSON);
                yield httpService.request(httpMethod, uri, payload, requestHeaders);
            }
            case HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_FORMURLENCODED -> {
                var payload = buildFormUrlEncodedBody(context, requestData);
                var requestHeaders = HttpServiceHeaders
                        .create()
                        .with(headers)
                        .withContentType(HttpServiceHeaders.APPLICATION_X_WWW_FORM_URLENCODED);
                yield httpService.request(httpMethod, uri, payload, requestHeaders);
            }
            case HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_MULTIPARTFORMDATA -> {
                var payload = buildMultipartBody(context, requestData);
                var requestHeaders = HttpServiceHeaders
                        .create()
                        .with(headers)
                        .withContentType(HttpServiceHeaders.MULTIPART_FORM_DATA);
                yield httpService.request(httpMethod, uri, payload, requestHeaders);
            }
            case HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_MANUELL -> httpService.request(httpMethod, uri, headers);
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der Datentyp für ausgehende Daten ist ungültig.");
        };
    }

    @Nonnull
    private ProcessNodeExecutionResult handleResponse(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                                      @Nonnull String responseType,
                                                      @Nonnull Set<Integer> allowedStatusCodes,
                                                      @Nonnull URI requestUri,
                                                      @Nonnull ResponseEntity<byte[]> response) throws ProcessNodeExecutionException {
        var statusCode = response.getStatusCode().value();
        var headers = copyHeaders(response.getHeaders());
        var statusCodeAllowed = allowedStatusCodes.contains(statusCode);

        if (HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_DATEI.equals(responseType)) {
            var metadata = createBaseMetadata(statusCode, headers, null, null);

            if (!statusCodeAllowed) {
                throw new ProcessNodeExecutionExceptionIO(
                        new ExternalServiceResponseException(decodeResponseBody(response)),
                        "Die HTTP-Schnittstelle hat mit dem Statuscode %d geantwortet. Für diese Aufgabe sind folgende Statuscodes zugelassen: %s.",
                        statusCode,
                        allowedStatusCodes.stream().map(String::valueOf).toList()
                );
            }

            byte[] responseBytes = response.getBody() != null ? response.getBody() : new byte[0];
            String fileName = resolveResponseFileName(context, response, responseBytes, requestUri);
            String mimeType = resolveResponseMimeType(response);

            ProcessInstanceAttachmentEntity attachment;
            try {
                var attachmentSet = processInstanceAttachmentSetService.create(
                        new ProcessInstanceAttachmentSetEntity()
                                .setName(fileName)
                                .setDataKey(context.getThisNode().getDataKey())
                                .setProcessInstanceId(context.getThisProcessInstance().getId())
                                .setProcessInstanceTaskId(context.getThisTask().getId())
                );

                attachment = processInstanceAttachmentService.create(
                        ProcessInstanceAttachmentEntity.of(
                                fileName,
                                1,
                                context.getThisProcessInstance().getId(),
                                context.getThisTask().getId(),
                                responseBytes
                        ).setAttachmentSetId(attachmentSet.getId())
                );
            } catch (ResponseException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Die als Datei empfangene HTTP-Antwort konnte nicht als Anlage zum Vorgang gespeichert werden."
                );
            }

            metadata.put(OUTPUT_NAME_FILE_NAME, fileName);
            metadata.put(OUTPUT_NAME_MIME_TYPE, mimeType);
            metadata.put(OUTPUT_NAME_SIZE_BYTES, responseBytes.length);
            metadata.put(OUTPUT_NAME_ATTACHMENT_KEY, attachment.getKey());
            metadata.put(OUTPUT_NAME_STORAGE_PROVIDER_ID, attachment.getStorageProviderId());
            metadata.put(OUTPUT_NAME_STORAGE_PATH_FROM_ROOT, attachment.getStoragePathFromRoot());
            try {
                metadata.put(OUTPUT_NAME_FILES, List.of(FileUploadMultipartInputService.buildAttachmentItem(attachment, responseBytes.length)));
            } catch (ResponseException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Der Dateiverweis für die als Datei empfangene HTTP-Antwort konnte nicht bereitgestellt werden."
                );
            }

            return ProcessNodeExecutionResultTaskCompleted.of(SUCCESS_PORT)
                    .setNodeData(metadata);
        }

        String rawBody = decodeResponseBody(response);
        Object processedResponse;
        try {
            processedResponse = parseResponseBody(responseType, rawBody);
        } catch (JacksonException e) {
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    e,
                    "Die Antwort der HTTP-Schnittstelle konnte nicht als %s verarbeitet werden.",
                    responseType.equals(HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_JSON) ? "JSON" : "Text"
            );
        }

        if (!statusCodeAllowed) {
            throw new ProcessNodeExecutionExceptionIO(
                    new ExternalServiceResponseException(rawBody),
                    "Die HTTP-Schnittstelle hat mit dem Statuscode %d geantwortet. Für diese Aufgabe sind folgende Statuscodes zugelassen: %s.",
                    statusCode,
                    allowedStatusCodes.stream().map(String::valueOf).toList()
            );
        }

        return ProcessNodeExecutionResultTaskCompleted
                .of(SUCCESS_PORT)
                .setNodeData(createBaseMetadata(statusCode, headers, rawBody, processedResponse));
    }

    @Nonnull
    private URI buildUri(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                         @Nullable String url) throws ProcessNodeExecutionException {
        var renderedUrl = url;
        if (StringUtils.isNullOrEmpty(renderedUrl)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Geben Sie die Zieladresse (URL) für den HTTP-Aufruf an.");
        }

        try {
            return UriComponentsBuilder
                    .fromUriString(renderedUrl.trim())
                    .build(true)
                    .toUri();
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Zieladresse (URL) für den HTTP-Aufruf ist ungültig."
            );
        }
    }

    @Nonnull
    private HttpServiceHeaders buildHeaders(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                            @Nonnull HttpActionNodeV1Config configuration) throws ProcessNodeExecutionException {
        var headers = HttpServiceHeaders.create();
        addAdditionalHeaders(context, headers, configuration.additionalSettings);
        addAuthenticationHeader(context, headers, configuration);
        return headers;
    }

    private void addAdditionalHeaders(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                      @Nonnull HttpServiceHeaders headers,
                                      @Nullable HttpActionNodeV1Config.AdditionalSettings additionalSettings) {
        if (additionalSettings == null || additionalSettings.additionalHeaders == null) {
            return;
        }

        for (var row : additionalSettings.additionalHeaders) {
            if (row == null) {
                continue;
            }

            // Header names and values must stay on one line.
            var key = interpolateNullable(context, readRowValue(row, "field"), TemplateOutputFormat.SingleLineText);
            if (StringUtils.isNullOrEmpty(key)) {
                continue;
            }

            var value = interpolateNullable(context, readRowValue(row, "value"), TemplateOutputFormat.SingleLineText);
            headers.with(key.trim(), value == null ? "" : value);
        }
    }

    private void addAuthenticationHeader(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                         @Nonnull HttpServiceHeaders headers,
                                         @Nonnull HttpActionNodeV1Config configuration) throws ProcessNodeExecutionException {
        var authMethod = StringUtils.toNullableTrimmedString(configuration.authMethod);
        if (authMethod == null) {
            return;
        }

        switch (authMethod) {
            case HttpActionNodeV1Config.AUTH_METHOD_OPT_BASIC_NUTZERNAME_UND_PASSWORT -> {
                var basicAuthConfig = configuration.basicAuthConfig;
                if (basicAuthConfig == null) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration("Geben Sie den Nutzernamen und das gespeicherte Passwort für die Basic-Authentifizierung an.");
                }

                var username = basicAuthConfig.username;
                if (StringUtils.isNullOrEmpty(username)) {
                    throw new ProcessNodeExecutionExceptionMissingValue("Geben Sie den Nutzernamen für die Basic-Authentifizierung an.");
                }

                var password = readSecretValue(basicAuthConfig.passwordSecretKey);
                headers.withBasicAuth(username.trim(), password);
            }
            case HttpActionNodeV1Config.AUTH_METHOD_OPT_BEARER_HEADERTOKEN -> {
                var bearerAuthConfig = configuration.bearerAuthConfig;
                if (bearerAuthConfig == null) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration("Geben Sie den Bearer-Token für den Zugang zur HTTP-Schnittstelle an.");
                }

                var bearerToken = bearerAuthConfig.bearerToken;
                if (StringUtils.isNullOrEmpty(bearerToken)) {
                    throw new ProcessNodeExecutionExceptionMissingValue("Geben Sie den Bearer-Token für den Zugang zur HTTP-Schnittstelle an.");
                }

                headers.withAuthorizationBearer(bearerToken.trim());
            }
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Authentifizierungsmethode %s wird nicht unterstützt.",
                    StringUtils.quote(authMethod)
            );
        }
    }

    @Nonnull
    private String readSecretValue(@Nullable String rawSecretKey) throws ProcessNodeExecutionException {
        var secretKeyString = StringUtils.toNullableTrimmedString(rawSecretKey);
        if (secretKeyString == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Wählen Sie das gespeicherte Geheimnis mit dem Passwort für die Basic-Authentifizierung aus.");
        }

        UUID secretKey;
        try {
            secretKey = UUID.fromString(secretKeyString);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Der Geheimnis-Schlüssel %s ist ungültig.",
                    StringUtils.quote(secretKeyString)
            );
        }

        var secret = secretService
                .retrieve(secretKey)
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Das Geheimnis mit dem Schlüssel %s wurde nicht gefunden.",
                        StringUtils.quote(secretKeyString)
                ));

        try {
            return secretService.decrypt(secret);
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Das gespeicherte Passwort für die Basic-Authentifizierung konnte nicht gelesen werden."
            );
        }
    }

    @Nonnull
    private String buildJsonBody(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                 @Nonnull HttpActionNodeV1Config.RequestData requestData) throws Exception {
        var jsonConfig = requestData.requestContentTypeJsonConfig;
        if (jsonConfig == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Legen Sie fest, welche Daten als JSON-Anfrageinhalt gesendet werden sollen.");
        }

        var jsonSource = StringUtils.toNullableTrimmedString(jsonConfig.requestJsonSource);
        if (Objects.equals(jsonSource, HttpActionNodeV1Config.RequestData.RequestContentTypeJsonConfig.REQUEST_JSON_SOURCE_OPT_LOWCODE)) {
            return JsonMapperFactory
                    .getInstance()
                    .writeValueAsString(normalizeJsonCompatible(evaluateRequestCode(context, jsonConfig.requestJsonLowCode)));
        }

        if (jsonSource != null && !Objects.equals(jsonSource, HttpActionNodeV1Config.RequestData.RequestContentTypeJsonConfig.REQUEST_JSON_SOURCE_OPT_VORGANGSDATEN)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die JSON-Datenquelle %s wird nicht unterstützt.",
                    StringUtils.quote(jsonSource)
            );
        }

        var payload = resolveConfiguredProcessDataValue(context, jsonConfig.requestJsonProcessDataKey);
        return JsonMapperFactory
                .getInstance()
                .writeValueAsString(payload);
    }

    @Nullable
    private Object resolveConfiguredProcessDataValue(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                                     @Nullable String rawProcessDataKey) throws ProcessNodeExecutionException {
        var processDataKey = normalizeProcessDataKey(rawProcessDataKey);
        if (processDataKey == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Geben Sie den Vorgangsdatenpfad für den JSON-Anfrageinhalt an.");
        }

        if (processDataKey.isEmpty()) {
            return context.getCurrentProcessExecutionData().getProcessData();
        }

        try {
            return ProcessDataValueUtils.resolveProcessDataValue(
                    context.getCurrentProcessExecutionData(),
                    processDataKey
            );
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Der Vorgangsdatenpfad %s für den JSON-Anfrageinhalt ist ungültig.",
                    StringUtils.quote(rawProcessDataKey)
            );
        }
    }

    @Nonnull
    private String buildFormUrlEncodedBody(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                           @Nonnull HttpActionNodeV1Config.RequestData requestData) throws ProcessNodeExecutionException {
        var fields = buildFormFields(context, requestData);
        var body = new StringBuilder();
        for (var entry : fields.entrySet()) {
            if (!body.isEmpty()) {
                body.append('&');
            }
            body
                    .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return body.toString();
    }

    @Nonnull
    private MultipartUtils.MultipartBodyPublisher buildMultipartBody(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                                                     @Nonnull HttpActionNodeV1Config.RequestData requestData) throws ProcessNodeExecutionException {
        var publisher = new MultipartUtils.MultipartBodyPublisher();

        for (var entry : buildFormFields(context, requestData).entrySet()) {
            publisher.addPart(entry.getKey(), entry.getValue());
        }

        if (requestData.requestFormAttachmentSetDataKeys != null) {
            for (var rawAttachmentSetDataKey : requestData.requestFormAttachmentSetDataKeys) {
                for (var attachment : resolveProcessAttachmentsBySetDataKey(context, rawAttachmentSetDataKey)) {
                    try (var attachmentContent = storageService.getDocumentContent(
                            attachment.getStorageProviderId(),
                            attachment.getStoragePathFromRoot()
                    )) {
                        publisher.addPart(
                                MULTIPART_ATTACHMENT_FIELD_NAME,
                                attachment.getFileName(),
                                attachmentContent.readAllBytes()
                        );
                    } catch (IOException | ResponseException e) {
                        throw new ProcessNodeExecutionExceptionUnknown(
                                e,
                                "Der Inhalt der Anlage %s konnte für die HTTP-Anfrage nicht geladen werden.",
                                StringUtils.quote(attachment.getFileName())
                        );
                    }
                }
            }
        }

        return publisher;
    }

    @Nonnull
    private Map<String, String> buildFormFields(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                                @Nonnull HttpActionNodeV1Config.RequestData requestData) throws ProcessNodeExecutionException {
        var fields = new LinkedHashMap<String, String>();
        if (requestData.requestFormFields == null) {
            return fields;
        }

        for (var row : requestData.requestFormFields) {
            if (row == null) {
                continue;
            }

            // The HTTP client encodes form fields, so values keep their line breaks.
            var name = interpolateNullable(context, readRowValue(row, "name"), TemplateOutputFormat.SingleLineText);
            var value = interpolateNullable(context, readRowValue(row, "value"), TemplateOutputFormat.PlainText);
            if (StringUtils.isNullOrEmpty(name)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("Geben Sie für jedes Formularfeld einen Feldnamen an.");
            }

            fields.put(name.trim(), value == null ? "" : value);
        }

        return fields;
    }

    @Nonnull
    private List<ProcessInstanceAttachmentEntity> resolveProcessAttachmentsBySetDataKey(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                                                                        @Nonnull String attachmentSetDataKey) throws ProcessNodeExecutionException {
        var normalizedDataKey = StringUtils.toNullableTrimmedString(attachmentSetDataKey);
        if (normalizedDataKey == null) {
            return List.of();
        }

        var attachmentSets = processInstanceAttachmentSetService
                .findAllByProcessInstanceIdAndDataKey(context.getThisProcessInstance().getId(), normalizedDataKey);

        if (attachmentSets.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s wurde im Vorgang %d nicht gefunden.",
                    StringUtils.quote(normalizedDataKey),
                    context.getThisProcessInstance().getId()
            );
        }

        var attachments = new ArrayList<ProcessInstanceAttachmentEntity>();
        for (var attachmentSet : attachmentSets) {
            attachments.addAll(processInstanceAttachmentService.findAllByAttachmentSetId(attachmentSet.getId()));
        }

        if (attachments.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s enthält keine Anlagen.",
                    StringUtils.quote(normalizedDataKey)
            );
        }

        return attachments;
    }

    @Nullable
    private Object evaluateRequestCode(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
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
    private Object parseResponseBody(@Nonnull String responseType,
                                     @Nullable String rawBody) throws JacksonException {
        if (HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_TEXT.equals(responseType)) {
            return rawBody;
        }

        if (StringUtils.isNullOrEmpty(rawBody)) {
            return null;
        }

        return JsonMapperFactory
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
        metadata.put(OUTPUT_NAME_FILES, List.of());
        return metadata;
    }

    @Nonnull
    private Map<String, Object> copyHeaders(@Nonnull org.springframework.http.HttpHeaders headers) {
        var copied = new LinkedHashMap<String, Object>();
        headers.forEach((key, value) -> copied.put(key, List.copyOf(value)));
        return copied;
    }

    @Nonnull
    private String resolveResponseFileName(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                           @Nonnull ResponseEntity<byte[]> response,
                                           @Nonnull byte[] responseBytes,
                                           @Nonnull URI requestUri) {
        var configuredFileName = context.getConfigurationOfExecutingNode().responseConfig == null
                ? null
                : context.getConfigurationOfExecutingNode().responseConfig.responseFileName;
        if (StringUtils.isNotNullOrEmpty(configuredFileName)) {
            return configuredFileName.trim();
        }

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
    private String interpolateNullable(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                       @Nullable Object value,
                                       @Nonnull TemplateOutputFormat outputFormat) {
        if (value == null) {
            return null;
        }

        return templateRenderService.interpolate(context.getCurrentProcessExecutionData(), value.toString(), outputFormat);
    }

    @Nullable
    private static String readRowValue(@Nonnull Map<String, Object> row,
                                       @Nonnull String key) {
        var value = row.get(key);
        return value == null ? null : value.toString();
    }

    @Nonnull
    private static String normalizeMethod(@Nullable String method) {
        return StringUtils.isNullOrEmpty(method)
                ? HttpActionNodeV1Config.HTTP_METHOD_OPT_GET
                : method.trim().toUpperCase(Locale.ROOT);
    }

    @Nonnull
    private static String normalizeRequestContentType(@Nullable String requestContentType) {
        return StringUtils.isNullOrEmpty(requestContentType)
                ? HttpActionNodeV1Config.RequestData.REQUEST_CONTENT_TYPE_OPT_JSON
                : requestContentType.trim();
    }

    @Nonnull
    private static String normalizeResponseBodyType(@Nullable HttpActionNodeV1Config.ResponseConfig responseConfig) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var responseBodyType = responseConfig == null ? null : responseConfig.responseBodyType;
        var normalizedResponseBodyType = StringUtils.isNullOrEmpty(responseBodyType)
                ? HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_TEXT
                : responseBodyType.trim();

        if (!RESPONSE_BODY_TYPES.contains(normalizedResponseBodyType)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Antworttyp %s wird nicht unterstützt.",
                    StringUtils.quote(responseBodyType)
            );
        }

        return normalizedResponseBodyType;
    }

    @Nonnull
    private static Set<Integer> parseAllowedStatusCodes(@Nullable HttpActionNodeV1Config.ResponseConfig responseConfig) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (responseConfig == null || responseConfig.responseStatusCode == null || responseConfig.responseStatusCode.isEmpty()) {
            return Set.of(200);
        }

        var statusCodes = new LinkedHashSet<Integer>();
        for (var rawStatusCode : responseConfig.responseStatusCode) {
            var rawStatusCodeString = StringUtils.toNullableTrimmedString(rawStatusCode);
            if (rawStatusCodeString == null) {
                continue;
            }

            Integer statusCode = parseStatusCode(rawStatusCodeString);
            if (statusCode == null || statusCode < 100 || statusCode > 599) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der zugelassene HTTP-Statuscode %s ist ungültig. Geben Sie eine ganze Zahl von 100 bis 599 an.",
                        StringUtils.quote(rawStatusCodeString)
                );
            }
            statusCodes.add(statusCode);
        }

        return statusCodes.isEmpty() ? Set.of(200) : Set.copyOf(statusCodes);
    }

    @Nullable
    private static Integer parseStatusCode(@Nullable Object rawValue) {
        return switch (rawValue) {
            case null -> null;
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

    @Nullable
    private static String normalizeProcessDataKey(@Nullable String rawProcessDataKey) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var processDataKey = StringUtils.toNullableTrimmedString(rawProcessDataKey);
        if (processDataKey == null) {
            return null;
        }

        if (processDataKey.equals("$")) {
            return "";
        }
        if (processDataKey.startsWith("$.")) {
            return processDataKey.substring(2);
        }
        if (processDataKey.startsWith("_") || processDataKey.startsWith("$$")) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Wählen Sie für den JSON-Anfrageinhalt einen Pfad aus der Vorgangsdatenebene.");
        }

        return processDataKey;
    }

    @Nullable
    private static Object normalizeJsonCompatible(@Nullable Object value) throws Exception {
        if (value == null) {
            return null;
        }

        byte[] serialized = JsonMapperFactory
                .getInstance()
                .writeValueAsBytes(value);

        return JsonMapperFactory
                .getInstance()
                .readValue(serialized, Object.class);
    }
}
