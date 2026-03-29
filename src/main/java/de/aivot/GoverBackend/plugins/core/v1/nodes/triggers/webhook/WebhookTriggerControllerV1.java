package de.aivot.GoverBackend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.dtos.HttpOptionsResponseDto;
import de.aivot.GoverBackend.process.entities.*;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.process.repositories.ProcessTestClaimRepository;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.ProcessInstanceService;
import de.aivot.GoverBackend.process.services.ProcessNodeService;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class WebhookTriggerControllerV1 {
    public static final String TEST_CLAIM_QUERY_PARAM = "test-claim";
    public static final String AUTH_TOKEN_QUERY_PARAM = "token";
    public static final String AUTH_HEADER_NAME = "Authorization";

    private final ProcessInstanceService processInstanceService;
    private final ProcessTestClaimRepository processTestClaimRepository;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessNodeService processNodeService;

    @Autowired
    public WebhookTriggerControllerV1(ProcessInstanceService processInstanceService,
                                      ProcessTestClaimRepository processTestClaimRepository,
                                      ProcessInstanceAttachmentService processInstanceAttachmentService,
                                      ProcessNodeService processNodeService) {
        this.processInstanceService = processInstanceService;
        this.processTestClaimRepository = processTestClaimRepository;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processNodeService = processNodeService;
    }

    @RequestMapping(value = "/api/public/webhooks/v1/{slug}/", method = {
            RequestMethod.OPTIONS,
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HttpOptionsResponseDto> handleOptions(
            @Nonnull @PathVariable String slug,
            @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey
    ) throws ResponseException {
        var testClaim = getTestClaim(testClaimAccessKey);
        var nodeEntity = retrieveWebhookNode(slug, testClaim);
        var config = getWebhookConfig(nodeEntity);

        var allow = getAllow(config);
        var response = new HttpOptionsResponseDto(
                RequestMethod.OPTIONS.name(),
                allow,
                config.requestMethod,
                getAcceptedContentType(config),
                MediaType.APPLICATION_JSON_VALUE
        );

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ALLOW, String.join(", ", allow))
                .body(response);
    }

    @RequestMapping(value = "/api/public/webhooks/v1/{slug}/", method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE,
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response handleWithoutBody(
            @Nonnull HttpServletRequest request,
            @Nonnull @PathVariable String slug,
            @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
            @Nullable @RequestParam(value = AUTH_TOKEN_QUERY_PARAM, required = false) String authToken,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) UUID identityId,
            @Nullable @RequestHeader(name = AUTH_HEADER_NAME, required = false) String authorizationHeader
    ) throws ResponseException {
        return handleRequest(request, slug, new HashMap<>(), Map.of(), testClaimAccessKey, authToken, authorizationHeader);
    }

    @RequestMapping(value = "/api/public/webhooks/v1/{slug}/", method = {
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE,
    }, consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response handleXmlAndJson(
            @Nonnull HttpServletRequest request,
            @Nonnull @PathVariable String slug,
            @Nonnull @RequestBody Map<String, Object> payload,
            @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
            @Nullable @RequestParam(value = AUTH_TOKEN_QUERY_PARAM, required = false) String authToken,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) UUID identityId,
            @Nullable @RequestHeader(name = AUTH_HEADER_NAME, required = false) String authorizationHeader
    ) throws ResponseException {
        return handleRequest(request, slug, payload, Map.of(), testClaimAccessKey, authToken, authorizationHeader);
    }

    @RequestMapping(value = "/api/public/webhooks/v1/{slug}/", method = {
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE,
    }, consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response handleFormData(
            @Nonnull StandardMultipartHttpServletRequest request,
            @Nonnull @PathVariable String slug,
            @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
            @Nullable @RequestParam(value = AUTH_TOKEN_QUERY_PARAM, required = false) String authToken,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) UUID identityId,
            @Nullable @RequestHeader(name = AUTH_HEADER_NAME, required = false) String authorizationHeader
    ) throws ResponseException {
        var payload = new HashMap<String, Object>();
        for (var entry : request.getParameterMap().entrySet()) {
            var key = entry.getKey();
            var values = entry.getValue();
            if (values.length > 0) {
                payload.put(key, values[0]);
            } else {
                payload.put(key, null);
            }
        }
        return handleRequest(request, slug, payload, request.getFileMap(), testClaimAccessKey, authToken, authorizationHeader);
    }

    @Nonnull
    private Response handleRequest(@Nonnull HttpServletRequest request,
                                   @Nonnull String slug,
                                   @Nonnull Map<String, Object> payload,
                                   @Nonnull Map<String, MultipartFile> files,
                                   @Nullable String testClaimAccessKey,
                                   @Nullable String authToken,
                                   @Nullable String authorizationHeader) throws ResponseException {
        var testClaim = getTestClaim(testClaimAccessKey);
        var nodeEntity = retrieveWebhookNode(slug, testClaim);

        startProcess(testClaim, nodeEntity, request, payload, files, authToken, authorizationHeader);

        return new Response("Webhook empfangen und verarbeitet.");
    }

    @Nullable
    private ProcessTestClaimEntity getTestClaim(@Nullable String testClaimAccessKey) {
        return testClaimAccessKey != null ? processTestClaimRepository
                .findByAccessKey(testClaimAccessKey)
                .orElse(null) : null;
    }

    @Nonnull
    private ProcessNodeEntity retrieveWebhookNode(@Nonnull String slug,
                                                  @Nullable ProcessTestClaimEntity testClaim) throws ResponseException {
        var specBuilder = SpecificationBuilder
                .create(ProcessNodeEntity.class)
                .withJsonEquals("configuration", List.of(WebhookTriggerConfigV1.SLUG_CONFIG_KEY), slug);

        if (testClaim != null) {
            specBuilder = specBuilder
                    .withEquals("processId", testClaim.getProcessId())
                    .withEquals("processVersion", testClaim.getProcessVersion());
        } else {
            specBuilder.withSpecification((root, query, builder) -> {
                Subquery<ProcessVersionEntity> subquery = query
                        .subquery(ProcessVersionEntity.class);

                Root<ProcessVersionEntity> versionRoot = subquery
                        .from(ProcessVersionEntity.class);

                subquery.select(versionRoot).where(
                        builder.equal(versionRoot.get("processId"), root.get("processId")),
                        builder.equal(versionRoot.get("processVersion"), root.get("processVersion")),
                        builder.equal(versionRoot.get("status"), ProcessVersionStatus.Published)
                );

                return builder.exists(subquery);
            });
        }

        return processNodeService
                .retrieve(specBuilder.build())
                .orElseThrow(() -> ResponseException.notFound("Kein Webhook-Knoten mit dem angegebenen Slug gefunden."));
    }

    private void startProcess(@Nullable ProcessTestClaimEntity testClaimEntity,
                              @Nonnull ProcessNodeEntity nodeEntity,
                              @Nonnull HttpServletRequest request,
                              @Nonnull Map<String, Object> payload,
                              @Nonnull Map<String, MultipartFile> files,
                              @Nullable String authToken,
                              @Nullable String authorizationHeader) throws ResponseException {
        var config = getWebhookConfig(nodeEntity);

        // Check if the request method is correct
        if (!request.getMethod().equalsIgnoreCase(config.requestMethod)) {
            throw ResponseException.methodNotAllowed(
                    "Ungültige Anfragemethode für diesen Webhook. Erwartet wird %s",
                    StringUtils.quote(config.requestMethod)
            );
        }

        // Check if authentication is required and valid
        checkAuthentication(config, authToken, authorizationHeader);

        var instance = new ProcessInstanceEntity(
                null,
                null,
                nodeEntity.getProcessId(),
                nodeEntity.getProcessVersion(),
                ProcessInstanceStatus.Paused, // Start paused to prevent the ProcessStarter from picking it up before we have added the attachments and initial payload
                null,
                null,
                List.of(),
                Map.of(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                new HashMap<>(),
                nodeEntity.getId(),
                null,
                testClaimEntity != null ? testClaimEntity.getId() : null
        );

        var createdInstance = processInstanceService.create(instance);

        try {
            var attachments = new LinkedList<ProcessInstanceAttachmentEntity>();
            for (var fileEntry : files.entrySet()) {
                var file = fileEntry.getValue();

                byte[] bytes;
                try {
                    bytes = file.getBytes();
                } catch (IOException e) {
                    throw ResponseException.internalServerError(e, "Fehler beim Lesen der hochgeladenen Datei.");
                }

                var attachment = ProcessInstanceAttachmentEntity.of(
                        file.getOriginalFilename() != null ? file.getOriginalFilename() : "Unbenannte Datei.dat",
                        createdInstance.getId(),
                        null,
                        bytes
                );

                var createdAttachment = processInstanceAttachmentService
                        .create(attachment);

                attachments.add(createdAttachment);
            }

            var initialPayload = new HashMap<String, Object>();
            initialPayload.put(WebhookTriggerNodeV1.DATA_KEY_PAYLOAD, payload);
            initialPayload.put(WebhookTriggerNodeV1.DATA_KEY_ATTACHMENTS, attachments.stream().map((a) -> Map.<String, Object>of(
                    "key", a.getKey(),
                    "filename", a.getFileName(),
                    "storageProviderId", a.getStorageProviderId(),
                    "storagePathFromRoot", a.getStoragePathFromRoot()
            )).toList());

            var requestData = new HashMap<String, Object>();
            requestData.put("method", request.getMethod());
            requestData.put("headers", Collections
                    .list(request.getHeaderNames())
                    .stream()
                    .collect(Collectors.toMap(
                            headerName -> headerName,
                            headerName -> Collections.list(request.getHeaders(headerName))
                    )));
            requestData.put("queryParameters", request.getParameterMap());
            initialPayload.put(WebhookTriggerNodeV1.DATA_KEY_REQUEST, requestData);

            createdInstance
                    .setInitialPayload(initialPayload)
                    .setStatus(ProcessInstanceStatus.Created);

            processInstanceService.update(createdInstance.getId(), createdInstance);
        } catch (Exception e) {
            // TODO: Log the exception
            createdInstance.setStatus(ProcessInstanceStatus.Failed);
            processInstanceService.update(createdInstance.getId(), createdInstance);
            throw e;
        }
    }

    @Nonnull
    private static List<String> getAllow(@Nonnull WebhookTriggerConfigV1 config) {
        var allow = new LinkedHashSet<String>();
        allow.add(RequestMethod.OPTIONS.name());

        if (!StringUtils.isNullOrEmpty(config.requestMethod)) {
            allow.add(config.requestMethod);
        }

        return List.copyOf(allow);
    }

    @Nullable
    private static String getAcceptedContentType(@Nonnull WebhookTriggerConfigV1 config) {
        if (!requestAllowsBody(config)) {
            return null;
        }

        if (config.requestBodyConfig != null &&
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON.equals(config.requestBodyConfig.requestBodyType)) {
            return MediaType.APPLICATION_JSON_VALUE;
        }

        if (config.requestBodyConfig != null &&
                WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_FORM.equals(config.requestBodyConfig.requestBodyType)) {
            return MediaType.MULTIPART_FORM_DATA_VALUE;
        }

        return MediaType.APPLICATION_XML_VALUE;
    }

    private static boolean requestAllowsBody(@Nonnull WebhookTriggerConfigV1 config) {
        return WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST.equals(config.requestMethod) ||
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PATCH.equals(config.requestMethod) ||
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PUT.equals(config.requestMethod);
    }

    @Nonnull
    private WebhookTriggerConfigV1 getWebhookConfig(@Nonnull ProcessNodeEntity nodeEntity) throws ResponseException {
        var derivedConfiguration = processNodeService
                .deriveConfiguration(nodeEntity, true);

        try {
            return ElementPOJOMapper
                    .mapToPOJO(derivedConfiguration.getEffectiveValues(), WebhookTriggerConfigV1.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Konfiguration des Webhook-Trigger-Knotens ist ungültig.");
        }
    }

    private static void checkAuthentication(@Nonnull WebhookTriggerConfigV1 config,
                                            @Nullable String tokenQueryParam,
                                            @Nullable String authHeader) throws ResponseException {
        if (Boolean.FALSE.equals(config.authRequired)) {
            return;
        }

        if (config.authConfig == null) {
            return;
        }

        if (config.authConfig.authMethod == null) {
            return;
        }

        switch (config.authConfig.authMethod) {
            case WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BASIC:
                if (authHeader == null || !authHeader.startsWith("Basic ")) {
                    throw ResponseException.unauthorized("Fehlender oder ungültiger Autorisierungs-Header für den Webhook-Trigger-Knoten.");
                }
                var base64Credentials = authHeader.substring("Basic ".length());
                var credentials = StringUtils.decodeBase64String(base64Credentials).split(":", 2);
                if (credentials.length != 2 ||
                        !credentials[0].equals(config.authConfig.authUsername) ||
                        !credentials[1].equals(config.authConfig.authPassword)) {
                    throw ResponseException.unauthorized("Ungültiger Benutzername oder Passwort für den Webhook-Trigger-Knoten.");
                }
                break;
            case WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BEARER:
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw ResponseException.unauthorized("Fehlender oder ungültiger Autorisierungs-Header für den Webhook-Trigger-Knoten.");
                }
                var token = authHeader.substring("Bearer ".length());
                if (!token.equals(config.authConfig.authToken)) {
                    throw ResponseException.unauthorized("Ungültiger Token für den Webhook-Trigger-Knoten.");
                }
                break;
            case WebhookTriggerConfigV1.AUTH_METHOD_OPTION_QUERY_PARAM:
                if (tokenQueryParam == null || !tokenQueryParam.equals(config.authConfig.authToken)) {
                    throw ResponseException.unauthorized("Ungültiger Token für den Webhook-Trigger-Knoten.");
                }
                break;
            default:
                throw ResponseException.internalServerError("Die Authentifizierungsmethode des Webhook-Trigger-Knotens ist ungültig.");
        }
    }

    public record Response(String message) {

    }
}
