package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.gover.backend.core.exceptions.HttpConnectionException;
import de.aivot.gover.backend.core.models.HttpServiceHeaders;
import de.aivot.gover.backend.core.services.HttpService;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.plugins.core.CorePlugin;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import de.aivot.gover.backend.process.exceptions.*;
import de.aivot.gover.backend.process.models.*;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.services.ProcessDataService;
import de.aivot.gover.backend.process.services.FileUploadMultipartInputService;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import de.aivot.gover.backend.secrets.repositories.SecretRepository;
import de.aivot.gover.backend.secrets.services.SecretService;
import de.aivot.gover.backend.storage.services.StorageService;
import de.aivot.gover.backend.utils.MultipartUtils;
import de.aivot.gover.backend.utils.StringUtils;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    @Value("classpath:/nodes/configs/HttpActionNodeV1Config.json")
    private Resource configResource;

    private final HttpService httpService;
    private final TemplateRenderService templateRenderService;
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final StorageService storageService;
    private final SecretRepository secretRepository;
    private final SecretService secretService;

    public HttpActionNodeV1(HttpService httpService,
                            TemplateRenderService templateRenderService,
                            JavascriptEngineFactoryService javascriptEngineFactoryService,
                            ProcessInstanceAttachmentService processInstanceAttachmentService,
                            ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                            StorageService storageService,
                            SecretRepository secretRepository,
                            SecretService secretService) {
        this.httpService = httpService;
        this.templateRenderService = templateRenderService;
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.storageService = storageService;
        this.secretRepository = secretRepository;
        this.secretService = secretService;
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
    public Class<HttpActionNodeV1Config> getNodeConfigurationClass() {
        return HttpActionNodeV1Config.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        var layout = loadConfigLayoutFromResource(configResource);

        layout.findChild(HttpActionNodeV1Config.BasicAuthConfig.PASSWORD_SECRET_KEY_FIELD_ID, SelectInputElement.class)
                .ifPresent(field -> field.setOptions(secretRepository
                        .findAll()
                        .stream()
                        .map(secret -> SelectInputElementOption.of(secret.getKey().toString(), secret.getName()))
                        .toList()));

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        SUCCESS_PORT,
                        "HTTP erfolgreich",
                        "Der Prozess wird hier fortgesetzt, wenn die Antwort einen erlaubten HTTP-Statuscode liefert."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_NAME_STATUS_CODE, "HTTP-Statuscode", "Der Statuscode der HTTP-Antwort."),
                new ProcessNodeOutput(OUTPUT_NAME_HEADERS, "HTTP-Header", "Die Header der HTTP-Antwort."),
                new ProcessNodeOutput(OUTPUT_NAME_RAW_BODY, "Antwort-Rohtext", "Der Antwort-Body als Text für JSON- und Text-Antworten."),
                new ProcessNodeOutput(OUTPUT_NAME_PROCESSED_RESPONSE, "Verarbeitete Antwort", "Die verarbeitete Antwort für JSON- und Text-Antworten."),
                new ProcessNodeOutput(OUTPUT_NAME_FILE_NAME, "Dateiname", "Der Dateiname der gespeicherten Antwortdatei."),
                new ProcessNodeOutput(OUTPUT_NAME_MIME_TYPE, "MIME-Typ", "Der MIME-Typ der gespeicherten Antwortdatei."),
                new ProcessNodeOutput(OUTPUT_NAME_SIZE_BYTES, "Dateigröße", "Die Größe der gespeicherten Antwortdatei in Bytes."),
                new ProcessNodeOutput(OUTPUT_NAME_ATTACHMENT_KEY, "Anhang-Schlüssel", "Der Schlüssel des gespeicherten Prozess-Anhangs."),
                new ProcessNodeOutput(OUTPUT_NAME_STORAGE_PROVIDER_ID, "Speicheranbieter", "Die ID des Speicheranbieters des gespeicherten Prozess-Anhangs."),
                new ProcessNodeOutput(OUTPUT_NAME_STORAGE_PATH_FROM_ROOT, "Speicherpfad", "Der Speicherpfad des gespeicherten Prozess-Anhangs."),
                new ProcessNodeOutput(OUTPUT_NAME_FILES, "Dateien", "Die gespeicherten Antwortdateien im Format des Datei-Upload-Feldes.")
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
                                    : Objects.requireNonNullElse(processNodeEntity.getName(), "Http-Anfrage"),
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
            throw new ProcessNodeExecutionExceptionIO(e);
        } catch (ProcessNodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Vorbereiten oder Ausführen des HTTP-Requests: %s",
                    e.getMessage()
            );
        }

        return handleResponse(context, responseType, allowedStatusCodes, uri, response);
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
                        "Der HTTP-Statuscode %d ist nicht in der Liste der erlaubten Statuscodes: %s. Die Antwort des Servers war: %s",
                        statusCode,
                        allowedStatusCodes.stream().map(String::valueOf).toList(),
                        decodeResponseBody(response)
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
            try {
                metadata.put(OUTPUT_NAME_FILES, List.of(FileUploadMultipartInputService.buildAttachmentItem(attachment, responseBytes.length)));
            } catch (ResponseException e) {
                throw new ProcessNodeExecutionExceptionUnknown(
                        e,
                        "Die HTTP-Antwortdatei konnte nicht für das Datei-Upload-Feld aufbereitet werden: %s",
                        e.getMessage()
                );
            }

            return ProcessNodeExecutionResultTaskCompleted.of(SUCCESS_PORT)
                    .setNodeData(metadata);
        }

        String rawBody = decodeResponseBody(response);
        Object processedResponse;
        try {
            processedResponse = parseResponseBody(responseType, rawBody);
        } catch (JsonProcessingException e) {
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    e,
                    "Die HTTP-Antwort konnte nicht als %s verarbeitet werden werden: %s",
                    responseType.equals(HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_JSON) ? "JSON" : "Text",
                    e.getMessage()
            );
        }

        if (!statusCodeAllowed) {
            throw new ProcessNodeExecutionExceptionIO(
                    "Der HTTP-Statuscode %d ist nicht in der Liste der erlaubten Statuscodes: %s. Die Antwort des Servers war: %s",
                    statusCode,
                    allowedStatusCodes.stream().map(String::valueOf).toList(),
                    rawBody
            );
        }

        return ProcessNodeExecutionResultTaskCompleted
                .of(SUCCESS_PORT)
                .setNodeData(createBaseMetadata(statusCode, headers, rawBody, processedResponse));
    }

    @Nonnull
    private URI buildUri(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                         @Nullable String url) throws ProcessNodeExecutionException {
        var renderedUrl = interpolateNullable(context, url);
        if (StringUtils.isNullOrEmpty(renderedUrl)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Die URL für den HTTP-Request wurde nicht angegeben.");
        }

        try {
            return UriComponentsBuilder
                    .fromUriString(renderedUrl.trim())
                    .build(true)
                    .toUri();
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die URL für den HTTP-Request ist ungültig: %s",
                    renderedUrl
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

            var key = interpolateNullable(context, readRowValue(row, "field"));
            if (StringUtils.isNullOrEmpty(key)) {
                continue;
            }

            var value = interpolateNullable(context, readRowValue(row, "value"));
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
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die Basic-Authentifizierung ist nicht vollständig konfiguriert.");
                }

                var username = interpolateNullable(context, basicAuthConfig.username);
                if (StringUtils.isNullOrEmpty(username)) {
                    throw new ProcessNodeExecutionExceptionMissingValue("Der Nutzername für die Basic-Authentifizierung wurde nicht angegeben.");
                }

                var password = readSecretValue(basicAuthConfig.passwordSecretKey);
                headers.withBasicAuth(username.trim(), password);
            }
            case HttpActionNodeV1Config.AUTH_METHOD_OPT_BEARER_HEADERTOKEN -> {
                var bearerAuthConfig = configuration.bearerAuthConfig;
                if (bearerAuthConfig == null) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die Bearer-Authentifizierung ist nicht vollständig konfiguriert.");
                }

                var bearerToken = interpolateNullable(context, bearerAuthConfig.bearerToken);
                if (StringUtils.isNullOrEmpty(bearerToken)) {
                    throw new ProcessNodeExecutionExceptionMissingValue("Der Bearer-Token wurde nicht angegeben.");
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
            throw new ProcessNodeExecutionExceptionMissingValue("Der Geheimnis-Schlüssel für das Passwort wurde nicht angegeben.");
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
                    "Das Geheimnis mit dem Schlüssel %s konnte nicht entschlüsselt werden: %s",
                    StringUtils.quote(secretKeyString),
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private String buildJsonBody(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                 @Nonnull HttpActionNodeV1Config.RequestData requestData) throws Exception {
        var jsonConfig = requestData.requestContentTypeJsonConfig;
        if (jsonConfig == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Die JSON-Anfragedaten sind nicht vollständig konfiguriert.");
        }

        var jsonSource = StringUtils.toNullableTrimmedString(jsonConfig.requestJsonSource);
        if (Objects.equals(jsonSource, HttpActionNodeV1Config.RequestData.RequestContentTypeJsonConfig.REQUEST_JSON_SOURCE_OPT_LOWCODE)) {
            return ObjectMapperFactory
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
        return ObjectMapperFactory
                .getInstance()
                .writeValueAsString(payload);
    }

    @Nullable
    private Object resolveConfiguredProcessDataValue(@Nonnull ProcessNodeExecutionInitContext<HttpActionNodeV1Config> context,
                                                     @Nullable String rawProcessDataKey) throws ProcessNodeExecutionException {
        var processDataKey = normalizeProcessDataKey(rawProcessDataKey);
        if (processDataKey == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Der Prozessdaten-Schlüssel für den JSON-Anfragekörper wurde nicht angegeben.");
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
                    "Der Prozessdaten-Schlüssel %s ist ungültig.",
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
                                "Der Inhalt des Prozess-Anhangs %s konnte nicht geladen werden: %s",
                                StringUtils.quote(attachment.getFileName()),
                                e.getMessage()
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

            var name = interpolateNullable(context, readRowValue(row, "name"));
            var value = interpolateNullable(context, readRowValue(row, "value"));
            if (StringUtils.isNullOrEmpty(name)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("Jede Datenfeld-Zeile benötigt einen Namen.");
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
                    "Der Anlagensatz mit dem Datenschlüssel %s wurde in der Prozess-Instanz %d nicht gefunden.",
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
                    "Der Anlagensatz mit dem Datenschlüssel %s enthält keine Anhänge.",
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
                                     @Nullable String rawBody) throws JsonProcessingException {
        if (HttpActionNodeV1Config.ResponseConfig.RESPONSE_BODY_TYPE_OPT_TEXT.equals(responseType)) {
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
                : interpolateNullable(context, context.getConfigurationOfExecutingNode().responseConfig.responseFileName);
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
                                       @Nullable Object value) {
        if (value == null) {
            return null;
        }

        return templateRenderService.interpolate(context.getCurrentProcessExecutionData(), value.toString());
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
                        "Der erlaubte HTTP-Statuscode %s ist ungültig.",
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
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Der JSON-Anfragekörper darf nur aus Vorgangsdaten gelesen werden.");
        }

        return processDataKey;
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
}
