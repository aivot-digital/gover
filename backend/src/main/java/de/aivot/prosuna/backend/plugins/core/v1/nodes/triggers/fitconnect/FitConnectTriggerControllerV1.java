package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessTestClaimRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.utils.StringUtils;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Public endpoint for authenticated FIT-Connect new-submission callbacks. */
@RestController
public class FitConnectTriggerControllerV1 {
    public static final String CALLBACK_AUTH_HEADER = "callback-authentication";
    public static final String CALLBACK_TIMESTAMP_HEADER = "callback-timestamp";
    public static final String TEST_CLAIM_QUERY_PARAM = "test-claim";

    private static final String NEW_SUBMISSIONS_CALLBACK_TYPE =
            "https://schema.fitko.de/fit-connect/submission-api/callbacks/new-submissions";

    private final ProcessTestClaimRepository processTestClaimRepository;
    private final ProcessNodeService processNodeService;
    private final ProcessService processService;
    private final ProcessNodeRepository processNodeRepository;
    private final FitConnectTriggerNodeV1 triggerNodeDefinition;
    private final FitConnectTriggerCallbackAuthenticationServiceV1 callbackAuthenticationService;
    private final FitConnectTriggerSubmissionImportServiceV1 submissionImportService;
    private final JsonMapper jsonMapper;

    public FitConnectTriggerControllerV1(
            ProcessTestClaimRepository processTestClaimRepository,
            ProcessNodeService processNodeService,
            ProcessService processService,
            ProcessNodeRepository processNodeRepository,
            FitConnectTriggerNodeV1 triggerNodeDefinition,
            FitConnectTriggerCallbackAuthenticationServiceV1 callbackAuthenticationService,
            FitConnectTriggerSubmissionImportServiceV1 submissionImportService,
            JsonMapper jsonMapper) {
        this.processTestClaimRepository = processTestClaimRepository;
        this.processNodeService = processNodeService;
        this.processService = processService;
        this.processNodeRepository = processNodeRepository;
        this.triggerNodeDefinition = triggerNodeDefinition;
        this.callbackAuthenticationService = callbackAuthenticationService;
        this.submissionImportService = submissionImportService;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping(value = "/api/public/fit-connect/{processSlug}/{slug}/")
    public void handleCallback(
            @Nonnull @PathVariable String processSlug,
            @Nonnull @PathVariable String slug,
            @Nonnull @RequestBody byte[] rawBody,
            @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
            @Nullable @RequestHeader(name = CALLBACK_AUTH_HEADER, required = false) String authHeader,
            @Nullable @RequestHeader(name = CALLBACK_TIMESTAMP_HEADER, required = false) String timestampHeader
    ) throws ResponseException {
        var process = getProcess(processSlug);
        var testClaim = getTestClaim(process, testClaimAccessKey);
        var node = retrieveTriggerNode(process, slug, testClaim);
        var config = getTriggerConfig(node);

        // Authentication deliberately precedes JSON parsing so unauthenticated input is never interpreted.
        callbackAuthenticationService.authenticate(
                config.callbackSecret,
                authHeader,
                timestampHeader,
                rawBody
        );

        var body = parseCallbackBody(rawBody);
        var configuredDestinationId = parseConfiguredDestinationId(config.destinationId);
        for (var submission : body.submissions()) {
            if (!configuredDestinationId.equals(submission.destinationId())) {
                throw ResponseException.badRequest(
                        "Der FIT-Connect-Callback enthält eine Einreichung für einen anderen Zustellpunkt."
                );
            }
        }

        var startedAt = Instant.now();
        for (var submission : body.submissions()) {
            submissionImportService.importSubmission(testClaim, node, config, submission, startedAt);
        }
    }

    @Nonnull
    private FitConnectTriggerCallbackPayloadV1 parseCallbackBody(@Nonnull byte[] rawBody) throws ResponseException {
        final FitConnectTriggerCallbackPayloadV1 body;
        try {
            body = jsonMapper.readValue(rawBody, FitConnectTriggerCallbackPayloadV1.class);
        } catch (RuntimeException e) {
            throw ResponseException.badRequest("Der FIT-Connect-Callback enthält kein gültiges JSON.", e);
        }

        if (body == null || !NEW_SUBMISSIONS_CALLBACK_TYPE.equals(body.type())) {
            throw ResponseException.badRequest("Der FIT-Connect-Callback hat einen nicht unterstützten Typ.");
        }
        if (body.submissions() == null || body.submissions().isEmpty() || body.submissions().contains(null)) {
            throw ResponseException.badRequest("Der FIT-Connect-Callback enthält keine gültigen Einreichungen.");
        }
        for (var submission : body.submissions()) {
            if (submission.destinationId() == null || submission.submissionId() == null || submission.caseId() == null) {
                throw ResponseException.badRequest("Der FIT-Connect-Callback enthält unvollständige Einreichungsreferenzen.");
            }
        }
        return body;
    }

    @Nonnull
    private ProcessEntity getProcess(@Nonnull String processSlug) throws ResponseException {
        return processService
                .retrieveBySlugOrHistory(processSlug)
                .orElseThrow(() -> ResponseException.notFound("Kein Prozess mit dem angegebenen Slug gefunden."));
    }

    @Nullable
    private ProcessTestClaimEntity getTestClaim(@Nonnull ProcessEntity process,
                                                @Nullable String testClaimAccessKey) throws ResponseException {
        if (testClaimAccessKey == null) {
            return null;
        }

        return processTestClaimRepository
                .findByProcessIdAndAccessKey(process.getId(), testClaimAccessKey)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private ProcessNodeEntity retrieveTriggerNode(@Nonnull ProcessEntity process,
                                                  @Nonnull String slug,
                                                  @Nullable ProcessTestClaimEntity testClaim) throws ResponseException {
        var specBuilder = SpecificationBuilder
                .create(ProcessNodeEntity.class)
                .withEquals("processId", process.getId())
                .withEquals("processNodeDefinitionKey", triggerNodeDefinition.getKey())
                .withEquals("processNodeDefinitionVersion", triggerNodeDefinition.getMajorVersion())
                .withJsonEquals("configuration", List.of(FitConnectTriggerConfigV1.SLUG_CONFIG_KEY), slug);

        if (testClaim != null) {
            specBuilder.withEquals("processVersion", testClaim.getProcessVersion());
        } else {
            specBuilder.withSpecification((root, query, builder) -> {
                Subquery<ProcessVersionEntity> subquery = query.subquery(ProcessVersionEntity.class);
                Root<ProcessVersionEntity> versionRoot = subquery.from(ProcessVersionEntity.class);

                subquery.select(versionRoot).where(
                        builder.equal(versionRoot.get("processId"), root.get("processId")),
                        builder.equal(versionRoot.get("processVersion"), root.get("processVersion")),
                        builder.equal(versionRoot.get("status"), ProcessVersionStatus.Published)
                );
                return builder.exists(subquery);
            });
        }

        var matches = processNodeRepository.findAll(specBuilder.build());
        if (matches.isEmpty()) {
            throw ResponseException.notFound("Kein FIT-Connect-Trigger mit dem angegebenen URL-Segment gefunden.");
        }
        if (matches.size() > 1) {
            throw ResponseException.internalServerError(
                    "Das URL-Segment ist mehreren FIT-Connect-Triggern in derselben Prozessversion zugeordnet."
            );
        }
        return matches.getFirst();
    }

    @Nonnull
    private FitConnectTriggerConfigV1 getTriggerConfig(@Nonnull ProcessNodeEntity node) throws ResponseException {
        var derivedConfiguration = processNodeService
                .deriveConfiguration(node, triggerNodeDefinition, null, true);
        return derivedConfiguration.configuration();
    }

    @Nonnull
    private UUID parseConfiguredDestinationId(@Nullable String rawDestinationId) throws ResponseException {
        var destinationId = StringUtils.toNullableTrimmedString(rawDestinationId);
        if (destinationId == null) {
            throw ResponseException.internalServerError(
                    "Die Zustellpunkt-ID des FIT-Connect-Trigger-Knotens ist nicht konfiguriert."
            );
        }
        try {
            return UUID.fromString(destinationId);
        } catch (IllegalArgumentException e) {
            throw ResponseException.internalServerError(
                    "Die Zustellpunkt-ID des FIT-Connect-Trigger-Knotens ist ungültig konfiguriert.",
                    e
            );
        }
    }
}
