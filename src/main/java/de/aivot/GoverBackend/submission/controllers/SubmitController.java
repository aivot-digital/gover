package de.aivot.GoverBackend.submission.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.av.services.AVService;
import de.aivot.GoverBackend.captcha.services.RedisCaptchaReplayGuard;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.elements.utils.ElementFlattenUtils;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.repositories.VFormVersionWithDetailsRepository;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.services.PluginUtils;
import de.aivot.GoverBackend.plugins.form.Form;
import de.aivot.GoverBackend.plugins.form.v1.nodes.FormTriggerNodeV1;
import de.aivot.GoverBackend.process.entities.*;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.process.filters.ProcessNodeFilter;
import de.aivot.GoverBackend.process.filters.ProcessTestClaimFilter;
import de.aivot.GoverBackend.process.services.*;
import de.aivot.GoverBackend.submission.dtos.SubmissionStatusResponseDTO;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilderJsonEquals;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilderJsonEqualsNull;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class SubmitController {
    private final AVService avService;
    private final ElementDerivationService elementDerivationService;
    private final VFormVersionWithDetailsRepository formVersionWithDetailsRepository;
    private final ProcessNodeService processNodeService;
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessVersionService processVersionService;
    private final ProcessTestClaimService processTestClaimService;
    private final ElementDataTransformService elementDataTransformService;
    private final RedisCaptchaReplayGuard captchaReplayGuard;

    @Autowired
    public SubmitController(AVService avService,
                            ElementDerivationService elementDerivationService,
                            VFormVersionWithDetailsRepository formVersionWithDetailsRepository,
                            ProcessNodeService processNodeService,
                            ProcessInstanceService processInstanceService,
                            ProcessInstanceAttachmentService processInstanceAttachmentService,
                            ProcessVersionService processVersionService,
                            ProcessTestClaimService processTestClaimService,
                            ElementDataTransformService elementDataTransformService,
                            RedisCaptchaReplayGuard captchaReplayGuard) {
        this.avService = avService;
        this.elementDerivationService = elementDerivationService;
        this.formVersionWithDetailsRepository = formVersionWithDetailsRepository;
        this.processNodeService = processNodeService;
        this.processInstanceService = processInstanceService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processVersionService = processVersionService;
        this.processTestClaimService = processTestClaimService;
        this.elementDataTransformService = elementDataTransformService;
        this.captchaReplayGuard = captchaReplayGuard;
    }

    @PostMapping("/api/public/submit/{formId}/{formVersion}/")
    public SubmissionStatusResponseDTO submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId,
            @PathVariable Integer formVersion,
            @RequestParam(value = "inputs", required = true) String rawInputs,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) UUID identityId
    ) throws ResponseException {
        AuthoredElementValues inputs;
        try {
            inputs = ObjectMapperFactory
                    .getInstance()
                    .readValue(rawInputs, AuthoredElementValues.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.badRequest();
        }

        // Fetch form
        var form = formVersionWithDetailsRepository
                .findById(new VFormVersionWithDetailsEntityId(formId, formVersion))
                .orElseThrow(ResponseException::notFound);

        // TODO: Hydrate the customer input with the data from an idp
        // hydrateCustomerInputWithIdpData(form, optionalIdp, derivedRuntimeElementData);

        // Perform derivation
        var logger = new ElementDerivationLogger();
        var derivationRequest = new ElementDerivationRequest(
                form.getRootElement(),
                inputs,
                new ElementDerivationOptions()
        );
        var derivedRuntimeElementData = elementDerivationService
                .derive(derivationRequest, logger);

        if (derivedRuntimeElementData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeElementData);
        }

        var effectiveValues = derivedRuntimeElementData.getEffectiveValues();

        // Test files for viruses
        avService.testMultipartFiles(files);
        // TODO: Check with default process instance attachment storage provider max file size

        var filter = ProcessNodeFilter
                .create()
                .setProcessNodeDefinitionKey(PluginUtils.combineComponentKey(Form.PLUGIN_KEY, FormTriggerNodeV1.NODE_KEY))
                .addConfigEquals(FormTriggerNodeV1.FormTriggerConfig.FORM_ID, form.getId().toString())
                .addAdditionalSpecification((root, query, builder) -> {
                    var explicitVersionCheck = new SpecificationBuilderJsonEquals<ProcessNodeEntity>(
                            "configuration",
                            List.of(FormTriggerNodeV1.FormTriggerConfig.FORM_VERSION),
                            formVersion.toString()
                    )
                            .toPredicate(root, query, builder);

                    var emptyVersionCheck = new SpecificationBuilderJsonEquals<ProcessNodeEntity>(
                            "configuration",
                            List.of(FormTriggerNodeV1.FormTriggerConfig.FORM_VERSION),
                            ""
                    )
                            .toPredicate(root, query, builder);

                    var nullVersionCheck =  new SpecificationBuilderJsonEqualsNull<ProcessNodeEntity>(
                            "configuration",
                            List.of(FormTriggerNodeV1.FormTriggerConfig.FORM_VERSION)
                    ).toPredicate(root, query, builder);

                    return builder.or(explicitVersionCheck, emptyVersionCheck, nullVersionCheck);
                });
        var nodes = processNodeService
                .list(filter);

        var startCandidates = new LinkedList<ProcessStartCandidate>();
        for (var node : nodes) {
            var processVersion = processVersionService
                    .retrieve(ProcessVersionEntityId.of(node.getProcessId(), node.getProcessVersion()))
                    .orElseThrow(() -> ResponseException.internalServerError("Der Prozess zum Formulareingangsknoten konnte nicht gefunden werden."));

            var testClaim = processTestClaimService
                    .retrieve(ProcessTestClaimFilter
                            .create()
                            .setProcessId(processVersion.getProcessId())
                            .setProcessVersion(processVersion.getProcessVersion())
                    )
                    .orElse(null);

            if (processVersion.getStatus() == ProcessVersionStatus.Published || testClaim != null) {
                startCandidates.add(new ProcessStartCandidate(node, testClaim));
            }
        }

        if (startCandidates.isEmpty()) {
            throw ResponseException.forbidden();
        }

        testCaptchaReplayProtection(form, effectiveValues);

        var startedProcesses = new LinkedList<String>();
        for (var candidate : startCandidates) {
            var processInstance = startProcess(
                    candidate.testClaim(),
                    form,
                    candidate.node(),
                    effectiveValues,
                    files
            );
            startedProcesses.add(processInstance.getAccessKey().toString());
        }

        return new SubmissionStatusResponseDTO(startedProcesses);
    }

    void testCaptchaReplayProtection(@Nonnull VFormVersionWithDetailsEntity form,
                                     @Nonnull EffectiveElementValues effectiveValues) throws ResponseException {
        var captchaPayload = extractCaptchaPayload(form.getRootElement(), effectiveValues);
        if (captchaPayload != null && captchaReplayGuard.isUsed(captchaPayload)) {
            throw ResponseException.badRequest("Die Captcha-Bestätigung wurde bereits verwendet. Bitte erneut bestätigen.");
        }
    }

    @Nullable
    static String extractCaptchaPayload(@Nonnull BaseElement rootElement,
                                        @Nonnull EffectiveElementValues effectiveValues) {
        return ElementFlattenUtils
                .flattenElements(rootElement)
                .stream()
                .filter(SubmitStepElement.class::isInstance)
                .map(SubmitStepElement.class::cast)
                .map(SubmitStepElement::getId)
                .map(effectiveValues::get)
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(map -> map.get("payload"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(payload -> !payload.isBlank())
                .findFirst()
                .orElse(null);
    }

    private ProcessInstanceEntity startProcess(@Nullable ProcessTestClaimEntity testClaimEntity,
                                               @Nonnull VFormVersionWithDetailsEntity form,
                                               @Nonnull ProcessNodeEntity nodeEntity,
                                               @Nonnull EffectiveElementValues effectiveValues,
                                               @Nullable MultipartFile[] files) throws ResponseException {
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

        var createdInstance = processInstanceService
                .create(instance);

        try {
            var attachments = new LinkedList<ProcessInstanceAttachmentEntity>();
            if (files != null) {
                for (var file : files) {
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
            }

            var initialPayload = new HashMap<String, Object>();
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_PAYLOAD, elementDataTransformService.buildPayload(form.getRootElement(), effectiveValues));
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_FORM_ID, form.getId());
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_FORM_VERSION, form.getVersion());
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_ATTACHMENTS, attachments.stream().map((a) -> Map.<String, Object>of(
                    "key", a.getKey(),
                    "filename", a.getFileName(),
                    "storageProviderId", a.getStorageProviderId(),
                    "storagePathFromRoot", a.getStoragePathFromRoot()
            )).toList());

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

        return instance;
    }

    private record ProcessStartCandidate(
            @Nonnull ProcessNodeEntity node,
            @Nullable ProcessTestClaimEntity testClaim
    ) {
    }
}
