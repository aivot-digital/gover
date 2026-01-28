package de.aivot.GoverBackend.plugins.core.v1.nodes.triggers.webhook;

import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.process.repositories.ProcessTestClaimRepository;
import de.aivot.GoverBackend.process.services.ProcessInstanceService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class WebhookTriggerController {
    private final ProcessInstanceService processInstanceService;
    private final ProcessTestClaimRepository processTestClaimRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    public WebhookTriggerController(ProcessInstanceService processInstanceService,
                                    ProcessTestClaimRepository processTestClaimRepository,
                                    EntityManagerFactory entityManagerFactory) {
        this.processInstanceService = processInstanceService;
        this.processTestClaimRepository = processTestClaimRepository;
        this.entityManagerFactory = entityManagerFactory;
    }

    @RequestMapping(value = "/api/public/webhooks/{slug}/", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response handleXmlAndJson(
            @Nonnull HttpServletRequest request,
            @Nonnull @PathVariable String slug,
            @Nonnull @RequestBody Map<String, Object> payload,
            @Nullable @RequestParam(value = "test-claim", required = false) String testClaimAccessKey,
            @Nullable @RequestParam(value = "token", required = false) String authToken,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) UUID identityId,
            @Nullable @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) throws ResponseException {
        return handleRequest(request, slug, payload, Map.of(), testClaimAccessKey, authToken, authorizationHeader);
    }

    @RequestMapping(value = "/api/public/webhooks/{slug}/", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response handleFormData(
            @Nonnull StandardMultipartHttpServletRequest request,
            @Nonnull @PathVariable String slug,
            @Nullable @RequestParam(value = "test-claim", required = false) String testClaimAccessKey,
            @Nullable @RequestParam(value = "token", required = false) String authToken,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) UUID identityId,
            @Nullable @RequestHeader(name = "Authorization", required = false) String authorizationHeader
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
        var testClaim = testClaimAccessKey != null ? processTestClaimRepository
                .findByAccessKey(testClaimAccessKey)
                .orElse(null) : null;

        ProcessNodeEntity nodeEntity;
        try (var em = entityManagerFactory.createEntityManager()) {
            Query query = em
                    .createNativeQuery("""
                            SELECT nod.* FROM process_nodes nod
                                INNER JOIN process_versions ver ON nod.process_id = ver.process_id AND
                                                                   nod.process_version = ver.process_version
                                LEFT JOIN process_test_claims clm ON ver.process_id = clm.process_id AND
                                                                     ver.process_version = clm.process_version AND
                                                                     clm.access_key = :testClaimAccessKey
                            WHERE process_node_definition_key = :processNodeDefinitionKey AND
                                  configuration->'slug'->>'inputValue' = :slug AND
                                  (clm.access_key IS NOT NULL OR ver.status = :statusPublished)
                            """, ProcessNodeEntity.class)
                    .setParameter("processNodeDefinitionKey", WebhookTriggerNode.NODE_KEY)
                    .setParameter("slug", slug)
                    .setParameter("testClaimAccessKey", testClaim != null ? testClaim.getAccessKey() : null)
                    .setParameter("statusPublished", ProcessVersionStatus.Published);

            try {
                nodeEntity = (ProcessNodeEntity) query.getSingleResult();
            } catch (ClassCastException e) {
                throw ResponseException.internalServerError(e, "Der Webhook-Trigger-Knoten konnte nicht geladen werden.");
            } catch (jakarta.persistence.NoResultException e) {
                throw ResponseException.notFound("Kein Webhook-Knoten mit dem angegebenen Slug gefunden.");
            }
        }

        startProcess(testClaim, nodeEntity, request, payload, files, authToken, authorizationHeader);

        return new Response("Webhook empfangen und verarbeitet.");
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

        //var payload = extractPayload();

        var instance = new ProcessInstanceEntity(
                null,
                null,
                nodeEntity.getProcessId(),
                ProcessInstanceStatus.Created,
                null,
                null,
                List.of(),
                Map.of(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                payload,
                nodeEntity.getId(),
                null,
                testClaimEntity != null ? testClaimEntity.getId() : null
        );

        processInstanceService.create(instance);

        for (var fileEntry : files.entrySet()) {
            var file = fileEntry.getValue();
            // TODO: Handle Process File Attaching
        }
    }

    @Nonnull
    private static WebhookTriggerConfig getWebhookConfig(@Nonnull ProcessNodeEntity nodeEntity) throws ResponseException {
        WebhookTriggerConfig config;
        try {
            config = ElementPOJOMapper
                    .mapToPOJO(nodeEntity.getConfiguration(), WebhookTriggerConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Konfiguration des Webhook-Trigger-Knotens ist ungültig.");
        }
        return config;
    }

    private static void checkAuthentication(@Nonnull WebhookTriggerConfig config,
                                            @Nullable String tokenQueryParam,
                                            @Nullable String authHeader) throws ResponseException {
        if (Boolean.FALSE.equals(config.authRequired)) {
            return;
        }

        switch (config.authConfig.authMethod) {
            case WebhookTriggerConfig.AUTH_METHOD_OPTION_BASIC:
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
            case WebhookTriggerConfig.AUTH_METHOD_OPTION_BEARER:
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw ResponseException.unauthorized("Fehlender oder ungültiger Autorisierungs-Header für den Webhook-Trigger-Knoten.");
                }
                var token = authHeader.substring("Bearer ".length());
                if (!token.equals(config.authConfig.authToken)) {
                    throw ResponseException.unauthorized("Ungültiger Token für den Webhook-Trigger-Knoten.");
                }
                break;
            case WebhookTriggerConfig.AUTH_METHOD_OPTION_QUERY_PARAM:
                if (tokenQueryParam == null || !tokenQueryParam.equals(config.authConfig.authToken)) {
                    throw ResponseException.unauthorized("Ungültiger Token für den Webhook-Trigger-Knoten.");
                }
                break;
            default:
                throw ResponseException.internalServerError("Die Authentifizierungsmethode des Webhook-Trigger-Knotens ist ungültig.");
        }
    }

    private static Map<String, Object> extractPayload() {
        return Map.of();
    }

    public record Response(String message) {

    }
}
