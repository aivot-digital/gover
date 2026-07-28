package de.aivot.gover.backend.plugins.form.v1.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.gover.backend.asset.services.AssetService;
import de.aivot.gover.backend.av.services.AVService;
import de.aivot.gover.backend.captcha.services.CaptchaReplayGuard;
import de.aivot.gover.backend.config.services.SystemConfigService;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.department.services.VDepartmentShadowedService;
import de.aivot.gover.backend.elements.dtos.ElementDerivationResponse;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.EffectiveElementValues;
import de.aivot.gover.backend.elements.models.ElementDerivationOptions;
import de.aivot.gover.backend.elements.models.ElementDerivationRequest;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.gover.backend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.gover.backend.elements.services.ElementDerivationLogger;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.elements.utils.ElementFlattenUtils;
import de.aivot.gover.backend.elements.utils.ElementStreamUtils;
import de.aivot.gover.backend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.gover.backend.identity.controllers.IdentityController;
import de.aivot.gover.backend.identity.entities.IdentityProviderEntity;
import de.aivot.gover.backend.identity.enums.IdentityProviderType;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.identity.services.IdentityProviderService;
import de.aivot.gover.backend.identity.services.IdentityService;
import de.aivot.gover.backend.identity.utils.IdentityCookieUtils;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.models.dtos.MaxFileSizeDto;
import de.aivot.gover.backend.payment.exceptions.PaymentException;
import de.aivot.gover.backend.payment.services.PaymentProviderService;
import de.aivot.gover.backend.plugins.form.v1.services.FormPaymentService;
import de.aivot.gover.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.gover.backend.process.entities.*;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessVersionStatus;
import de.aivot.gover.backend.process.filters.ProcessNodeFilter;
import de.aivot.gover.backend.process.filters.ProcessVersionFilter;
import de.aivot.gover.backend.process.services.*;
import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.services.StorageProviderService;
import de.aivot.gover.backend.submission.services.ElementDataTransformService;
import de.aivot.gover.backend.system.services.SystemService;
import de.aivot.gover.backend.theme.dtos.ThemeResponseDTO;
import de.aivot.gover.backend.theme.entities.ThemeEntity;
import de.aivot.gover.backend.theme.services.ThemeService;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/public/form/{processSlug}/{formSlug}/")
public class FormTriggerControllerV1 {
    public static final String TEST_CLAIM_QUERY_PARAM = "test-claim";
    public static final String VERSION_QUERY_PARAM = "version";

    private final GoverConfig goverConfig;
    private final FormPaymentService paymentService;
    private final IdentityProviderService identityProviderService;
    private final ElementDerivationService elementDerivationService;
    private final AssetService assetService;
    private final ThemeService themeService;
    private final VDepartmentShadowedService vDepartmentShadowedService;
    private final SystemService systemService;
    private final UserService userService;
    private final ProcessService processService;
    private final ProcessNodeService processNodeService;
    private final ProcessTestClaimService processTestClaimService;
    private final ProcessVersionService processVersionService;
    private final ProcessNodeDefinitionService processNodeDefinitionService;
    private final SystemConfigService systemConfigService;
    private final StorageProviderService storageProviderService;
    private final CaptchaReplayGuard captchaReplayGuard;
    private final ProcessInstanceService processInstanceService;
    private final FileUploadMultipartInputService fileUploadMultipartInputService;
    private final ElementDataTransformService elementDataTransformService;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final FormTriggerNodeV1 formTriggerNodeV1;
    private final IdentityService identityService;

    @Autowired
    public FormTriggerControllerV1(GoverConfig goverConfig,
                                   FormPaymentService paymentService,
                                   PaymentProviderService paymentProviderService,
                                   IdentityProviderService identityProviderService,
                                   IdentityCacheRepository identityCacheRepository,
                                   ElementDerivationService elementDerivationService,
                                   AssetService assetService,
                                   ThemeService themeService,
                                   VDepartmentShadowedService vDepartmentShadowedService,
                                   SystemService systemService,
                                   UserService userService,
                                   ProcessService processService,
                                   ProcessNodeService processNodeService,
                                   ProcessTestClaimService processTestClaimService,
                                   ProcessVersionService processVersionService,
                                   ProcessNodeDefinitionService processNodeDefinitionService,
                                   SystemConfigService systemConfigService,
                                   StorageProviderService storageProviderService,
                                   AVService aVService,
                                   CaptchaReplayGuard captchaReplayGuard,
                                   ProcessInstanceService processInstanceService,
                                   ProcessInstanceAttachmentService processInstanceAttachmentService,
                                   FileUploadMultipartInputService fileUploadMultipartInputService,
                                   ElementDataTransformService elementDataTransformService,
                                   ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                                   FormTriggerNodeV1 formTriggerNodeV1,
                                   IdentityService identityService) {
        this.goverConfig = goverConfig;
        this.paymentService = paymentService;
        this.identityProviderService = identityProviderService;
        this.elementDerivationService = elementDerivationService;
        this.assetService = assetService;
        this.themeService = themeService;
        this.vDepartmentShadowedService = vDepartmentShadowedService;
        this.systemService = systemService;
        this.userService = userService;
        this.processService = processService;
        this.processNodeService = processNodeService;
        this.processTestClaimService = processTestClaimService;
        this.processVersionService = processVersionService;
        this.processNodeDefinitionService = processNodeDefinitionService;
        this.systemConfigService = systemConfigService;
        this.storageProviderService = storageProviderService;
        this.captchaReplayGuard = captchaReplayGuard;
        this.processInstanceService = processInstanceService;
        this.fileUploadMultipartInputService = fileUploadMultipartInputService;
        this.elementDataTransformService = elementDataTransformService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.formTriggerNodeV1 = formTriggerNodeV1;
        this.identityService = identityService;
    }

    @GetMapping("")
    public RetrieveResponse retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                     @Nonnull @PathVariable String processSlug,
                                     @Nonnull @PathVariable String formSlug,
                                     @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                     @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId) throws ResponseException {
        var execUser = getExecUser(jwt);

        var process = getProcessEntity(processSlug);
        var processVersion = getProcessVersionEntity(testClaimAccessKey, null, process, execUser);
        var node = getProcessNodeEntity(formSlug, process, processVersion);
        var provider = getProvider(node);
        var config = getConfigurationDetails(node, provider, execUser);
        var identitySlots = getIdentitySlots(node, identitySessionId, config);

        var shouldObfuscateSteps = identitySlots
                .stream()
                .anyMatch(s -> s.getIsRequired() && !s.isAuthenticated());

        var formLayout = config
                .configuration()
                .formLayout;

        ElementStreamUtils.applyAction(formLayout, (element) -> {
            element.removeInternalInformation();
            if (shouldObfuscateSteps && element instanceof GenericStepElement stepElement) {
                stepElement.setChildren(List.of());
            }
        });

        return new RetrieveResponse(
                formLayout,
                node,
                process,
                processVersion,
                identitySlots
        );
    }

    @Nonnull
    private List<IdentitySlot> getIdentitySlots(@Nonnull ProcessNodeEntity node,
                                                @Nullable String identitySessionId,
                                                @Nonnull ProcessNodeService.ProcessConfigurationDetails<FormTriggerConfigV1> config) {
        if (config.configuration().identities == null) {
            return new LinkedList<>();
        }

        var identityDataMap = identityService
                .getIdentityDataMap(identitySessionId, node.getId());

        return config
                .configuration()
                .identities
                .stream()
                .map((slot) -> getIdentitySlot(slot, identityDataMap))
                .toList();
    }

    @Nonnull
    private IdentitySlot getIdentitySlot(IdentityConfigElementSlot slot, IdentityDataMap identityDataMap) {
        if (slot.getId() == null) {
            throw new IllegalArgumentException("Slot ID is null");
        }

        var options = slot.getOptions();

        if (options == null) {
            return new IdentitySlot(
                    slot.getId(),
                    slot.getTitle(),
                    slot.getDescription(),
                    Boolean.TRUE.equals(slot.getIsOptional()),
                    Boolean.TRUE.equals(slot.getAllowsMail()),
                    identityDataMap.containsKey(slot.getId()),
                    List.of()
            );
        }

        var identityData = identityDataMap.get(slot.getId());

        List<IdentityProvider> identityProviders = options
                .stream()
                .filter(opt -> opt.getIdentityProviderKey() != null)
                .map((opt) -> {
                    IdentityProviderEntity idpEntity;
                    try {
                        idpEntity = identityProviderService
                                .retrieve(opt.getIdentityProviderKey())
                                .orElseThrow(ResponseException::notFound);
                    } catch (ResponseException e) {
                        throw new RuntimeException(e);
                    }

                    return new IdentityProvider(
                            idpEntity.getKey(),
                            idpEntity.getName(),
                            idpEntity.getIconAssetKey(),
                            idpEntity.getType(),
                            identityData != null && Objects.equals(identityData.providerKey(), idpEntity.getKey()),
                            opt.getAdditionalScopes() != null ? opt.getAdditionalScopes() : List.of()
                    );
                })
                .sorted(Comparator.comparing(IdentityProvider::identityProviderName))
                .toList();

        return new IdentitySlot(
                slot.getId(),
                slot.getTitle(),
                slot.getDescription(),
                Boolean.TRUE.equals(slot.getIsOptional()),
                Boolean.TRUE.equals(slot.getAllowsMail()),
                identityDataMap.containsKey(slot.getId()),
                identityProviders
        );
    }

    public record RetrieveResponse(
            @Nonnull
            FormLayoutElement layoutElement,
            @Nonnull
            ProcessNodeEntity node,
            @Nonnull
            ProcessEntity process,
            @Nonnull
            ProcessVersionEntity version,
            @Nonnull
            List<IdentitySlot> identitySlots
    ) {
    }

    public record IdentitySlot(
            @Nonnull
            String id,
            @Nullable
            String title,
            @Nullable
            String description,
            @Nonnull
            Boolean isOptional,
            @Nonnull
            Boolean allowsEmail,
            @Nonnull
            Boolean isAuthenticated,
            @Nonnull
            List<IdentityProvider> availableIdentityProviders
    ) {
        public Boolean getIsRequired() {
            return !isOptional;
        }
    }

    public record IdentityProvider(
            @Nonnull
            UUID identityProviderKey,
            @Nonnull
            String identityProviderName,
            @Nullable
            UUID identityProviderAssetKey,
            @Nonnull
            IdentityProviderType identityProviderType,
            @Nonnull
            Boolean isAuthenticatedWithThis,
            @Nonnull
            List<String> additionalScopes
    ) {
    }

    @Nonnull
    private ProcessNodeService.ProcessConfigurationDetails<FormTriggerConfigV1> getConfigurationDetails(ProcessNodeEntity node, FormTriggerNodeV1 provider, UserEntity execUser) throws ResponseException {
        var config = processNodeService
                .deriveConfiguration(node, provider, execUser, true);
        return config;
    }

    @GetMapping("max-file-size/")
    @Operation(
            summary = "Get maximum file size for attachments in a form",
            description = "Retrieve the maximum allowed file size for attachments in the specified form. " +
                    "If the form is linked to a destination with specific file size limits, those limits will be returned. " +
                    "Otherwise, a default value will be provided."
    )
    public MaxFileSizeDto getMaxFileSize(@Nullable @AuthenticationPrincipal Jwt jwt,
                                         @Nonnull @PathVariable String processSlug,
                                         @Nonnull @PathVariable String formSlug,
                                         @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                         @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId) throws ResponseException {
        var val = systemConfigService
                .retrieve(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY)
                .getValueAsInteger()
                .orElseThrow(ResponseException::internalServerError);

        var max = storageProviderService
                .retrieve(val)
                .map(StorageProviderEntity::getMaxFileSizeInBytes)
                .orElse(0L);

        MaxFileSizeDto maxFileSizeDto = new MaxFileSizeDto();
        maxFileSizeDto.setMaxFileSize(max);
        return maxFileSizeDto;
    }

    @PostMapping("costs/")
    @Operation(
            summary = "Calculate costs for a form based on customer data",
            description = "Calculate the total costs for a form based on the provided customer data. " +
                    "If the form has an associated payment provider, the costs will be calculated accordingly. " +
                    "If no payment provider is linked, the response will indicate that there are no costs."
    )
    public FormTriggerCostCalculationResponseV1 calculateCosts(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                               @Nonnull @PathVariable String processSlug,
                                                               @Nonnull @PathVariable String formSlug,
                                                               @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                                               @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) UUID identitySessionId,
                                                               @Nonnull @RequestBody AuthoredElementValues values) throws PaymentException, ResponseException {
        // TODO: Implement with the paymentService
        var exists = paymentService != null;
        return new FormTriggerCostCalculationResponseV1(BigDecimal.ZERO, List.of(), "");
    }

    @PostMapping("derive/")
    @Operation(
            summary = "Derive element data based on input for a form",
            description = "Derive the element data for a form based on the provided input data. " +
                    "This process evaluates the form's logic, visibility rules, and calculations to produce the derived data. " +
                    "Options are available to skip certain derivation aspects for specific elements."
    )
    public ElementDerivationResponse derive(@Nullable @AuthenticationPrincipal Jwt jwt,
                                            @Nonnull @PathVariable String processSlug,
                                            @Nonnull @PathVariable String formSlug,
                                            @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                            @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
                                            @Nonnull @Valid @RequestBody AuthoredElementValues authoredElementValues,
                                            @Nullable @RequestParam(value = "skipErrorsFor") List<String> skipErrorsFor,
                                            @Nullable @RequestParam(value = "skipVisibilitiesFor") List<String> skipVisibilitiesFor,
                                            @Nullable @RequestParam(value = "skipValuesFor") List<String> skipValuesFor,
                                            @Nullable @RequestParam(value = "skipOverridesFor") List<String> skipOverridesFor) throws ResponseException {
        var execUser = getExecUser(jwt);
        var process = getProcessEntity(processSlug);
        var processVersion = getProcessVersionEntity(testClaimAccessKey, null, process, execUser);
        var node = getProcessNodeEntity(formSlug, process, processVersion);
        var provider = getProvider(node);
        var config = getConfigurationDetails(node, provider, execUser);

        var options = new ElementDerivationOptions()
                .setSkipValuesForElementIds(skipValuesFor)
                .setSkipOverridesForElementIds(skipOverridesFor)
                .setSkipErrorsForElementIds(skipErrorsFor)
                .setSkipVisibilitiesForElementIds(skipVisibilitiesFor);

        var request = new ElementDerivationRequest(
                config.configuration().formLayout,
                authoredElementValues,
                options
        );

        var identities = identityService
                .getIdentityDataMap(identitySessionId, node.getId());

        var derivationLogger = new ElementDerivationLogger();
        var derivedElementData = elementDerivationService
                .derive(request, identities, derivationLogger);

        return ElementDerivationResponse
                .from(derivedElementData, derivationLogger, jwt != null);
    }

    @PostMapping("submit/")
    public FormTriggerSubmissionStatusResponseV1 submit(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                        @Nonnull @PathVariable String processSlug,
                                                        @Nonnull @PathVariable String formSlug,
                                                        @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                                        @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
                                                        @Nonnull @RequestParam(value = "inputs", required = true) String rawInputs,
                                                        @Nullable @RequestParam(value = "files", required = false) MultipartFile[] files,
                                                        @Nullable @RequestParam(value = "fileUris", required = false) List<String> fileUris,
                                                        @Nonnull HttpServletResponse response) throws ResponseException {
        var execUser = getExecUser(jwt);
        var process = getProcessEntity(processSlug);
        var processVersion = getProcessVersionEntity(testClaimAccessKey, null, process, execUser);
        var node = getProcessNodeEntity(formSlug, process, processVersion);
        var provider = getProvider(node);
        var config = getConfigurationDetails(node, provider, execUser);

        AuthoredElementValues inputs;
        try {
            inputs = ObjectMapperFactory
                    .getInstance()
                    .readValue(rawInputs, AuthoredElementValues.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.badRequest();
        }

        var identities = identityService
                .getIdentityDataMap(identitySessionId, node.getId());

        // Perform derivation
        var logger = new ElementDerivationLogger();
        var derivationRequest = new ElementDerivationRequest(
                config.configuration().formLayout,
                inputs,
                new ElementDerivationOptions()
        );
        var derivedRuntimeElementData = elementDerivationService
                .derive(derivationRequest, identities, logger);

        if (derivedRuntimeElementData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeElementData);
        }

        var effectiveValues = derivedRuntimeElementData.getEffectiveValues();

        // Only bind a started instance to a test claim when the caller explicitly
        // proves the claim through the public URL. Looking up by process/version alone
        // would accidentally mark normal public submissions as test submissions.
        var testClaim = testClaimAccessKey != null
                ? processTestClaimService
                .retrieveByAccessKey(process.getId(), testClaimAccessKey)
                .orElseThrow(ResponseException::notFound)
                : null;


        testCaptchaReplayProtection(config.configuration().formLayout, effectiveValues);


        var processInstance = startProcess(
                testClaim,
                config.configuration().formLayout,
                node,
                inputs,
                files,
                fileUris,
                identities
        );

        response.addCookie(IdentityCookieUtils.createExpiredIdentityCookie());
        return new FormTriggerSubmissionStatusResponseV1(processInstance.getAccessKey());
    }

    void testCaptchaReplayProtection(@Nonnull FormLayoutElement form,
                                     @Nonnull EffectiveElementValues effectiveValues) throws ResponseException {
        var captchaPayload = extractCaptchaPayload(form, effectiveValues);
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
                                               @Nonnull FormLayoutElement form,
                                               @Nonnull ProcessNodeEntity nodeEntity,
                                               @Nonnull AuthoredElementValues inputs,
                                               @Nullable MultipartFile[] files,
                                               @Nullable List<String> fileUris,
                                               @Nonnull IdentityDataMap identities) throws ResponseException {
        var startedAt = Instant.now();
        var instance = new ProcessInstanceEntity(
                null,
                null,
                null,
                nodeEntity.getProcessId(),
                nodeEntity.getProcessVersion(),
                ProcessInstanceStatus.Paused, // Start paused to prevent the ProcessStarter from picking it up before we have added the attachments and initial payload
                null,
                null,
                List.of(),
                identities,
                startedAt,
                startedAt,
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
            var normalizationResult = fileUploadMultipartInputService.normalizeInputs(
                    form,
                    inputs,
                    files,
                    fileUris,
                    createdInstance.getId(),
                    null,
                    null
            );
            var normalizedInputs = normalizationResult.inputs();
            var normalizedDerivedRuntimeElementData = elementDerivationService.derive(
                    new ElementDerivationRequest(
                            form,
                            normalizedInputs,
                            new ElementDerivationOptions()
                    ),
                    identities,
                    new ElementDerivationLogger()
            );

            if (normalizedDerivedRuntimeElementData.hasAnyError()) {
                throw ResponseException.badRequest(normalizedDerivedRuntimeElementData);
            }

            var normalizedEffectiveValues = normalizedDerivedRuntimeElementData.getEffectiveValues();
            var attachments = normalizationResult.createdAttachments();

            var initialPayload = new HashMap<String, Object>();
            initialPayload.put(
                    FormTriggerNodeV1.DATA_KEY_PAYLOAD,
                    elementDataTransformService.buildPayload(
                            form,
                            normalizedEffectiveValues,
                            normalizedDerivedRuntimeElementData.getElementStates()
                    )
            );
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_UNMAPPED, normalizedEffectiveValues);
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_ATTACHMENTS, attachments.stream().map((a) -> Map.<String, Object>of(
                    "key", a.getKey(),
                    "fileName", a.getFileName(),
                    "originalFileName", a.getOriginalFileName(),
                    "storageProviderId", a.getStorageProviderId(),
                    "storagePathFromRoot", a.getStoragePathFromRoot()
            )).toList());
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_STARTED, startedAt);

            createdInstance
                    .setInitialPayload(initialPayload)
                    .setStatus(ProcessInstanceStatus.Created);
        } catch (Exception e) {
            createdInstance.setStatus(ProcessInstanceStatus.Failed);

            var logger = processNodeExecutionLoggerFactory
                    .create(
                            createdInstance.getId(),
                            null,
                            null,
                            null
                    );
            logger.logException(e);

            processInstanceService.update(createdInstance.getId(), createdInstance);

            if (e instanceof ResponseException responseException) {
                throw responseException;
            }

            throw ResponseException.internalServerError(e);
        }

        return processInstanceService.update(createdInstance.getId(), createdInstance);
    }

    @GetMapping("theme/")
    @Operation(
            summary = "Get theme details for a form",
            description = "Retrieve the theme details associated with the specified form. " +
                    "Includes information such as colors, fonts, logos, and other visual elements that define the form's appearance."
    )
    public ThemeResponseDTO getTheme(@Nullable @AuthenticationPrincipal Jwt jwt,
                                     @Nonnull @PathVariable String processSlug,
                                     @Nonnull @PathVariable String formSlug,
                                     @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                     @Nullable @RequestParam(value = VERSION_QUERY_PARAM, required = false) Integer processVersion
    ) throws ResponseException {
        var context = resolveFormTriggerContext(jwt, processSlug, formSlug, testClaimAccessKey, processVersion);
        var theme = getFormTheme(context.formLayout());
        return ThemeResponseDTO.fromEntity(theme);
    }

    @GetMapping("logo/")
    @Operation(
            summary = "Get the logo for a form",
            description = "Get the logo image associated with the specified form. " +
                    "If the form does not resolve to a custom theme, a default logo URL will be provided."
    )
    public void getLogo(@Nullable @AuthenticationPrincipal Jwt jwt,
                        @Nonnull @PathVariable String processSlug,
                        @Nonnull @PathVariable String formSlug,
                        @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                        @Nullable @RequestParam(value = VERSION_QUERY_PARAM, required = false) Integer processVersion,
                        @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        var context = resolveFormTriggerContext(jwt, processSlug, formSlug, testClaimAccessKey, processVersion);
        var logoResolution = getFormLogoResolution(context.formLayout());

        String redirectUrl;
        if (logoResolution.assetKey() == null && logoResolution.allowDefaultFallback()) {
            redirectUrl = goverConfig.getDefaultLogoUrl();
        } else if (logoResolution.assetKey() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } else {
            redirectUrl = assetService.createUrl(logoResolution.assetKey());
        }

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("favicon/")
    @Operation(
            summary = "Get the favicon for a form",
            description = "Get the favicon image associated with the specified form. " +
                    "If the form does not have a custom favicon, a default favicon URL will be provided."
    )
    public void getFavicon(@Nullable @AuthenticationPrincipal Jwt jwt,
                           @Nonnull @PathVariable String processSlug,
                           @Nonnull @PathVariable String formSlug,
                           @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                           @Nullable @RequestParam(value = VERSION_QUERY_PARAM, required = false) Integer processVersion,
                           @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        var context = resolveFormTriggerContext(jwt, processSlug, formSlug, testClaimAccessKey, processVersion);
        var faviconKey = getFormFaviconKey(context.formLayout());

        String redirectUrl;
        if (faviconKey == null) {
            redirectUrl = goverConfig.getDefaultFaviconUrl();
        } else {
            redirectUrl = assetService.createUrl(faviconKey);
        }

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("submit/{instanceAccessKey}/print/")
    @Operation(
            summary = "Get the favicon for a form",
            description = "Get the favicon image associated with the specified form. " +
                    "If the form does not have a custom favicon, a default favicon URL will be provided."
    )
    public void getPrint(@Nullable @AuthenticationPrincipal Jwt jwt,
                         @Nonnull @PathVariable String processSlug,
                         @Nonnull @PathVariable String formSlug,
                         @Nonnull @PathVariable UUID instanceAccessKey,
                         @Nullable @RequestParam(value = "version", required = false) Integer version,
                         @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {

    }

    @GetMapping("submit/{instanceAccessKey}/status/")
    @Operation(
            summary = "Get the favicon for a form",
            description = "Get the favicon image associated with the specified form. " +
                    "If the form does not have a custom favicon, a default favicon URL will be provided."
    )
    public void getStatus(@Nullable @AuthenticationPrincipal Jwt jwt,
                          @Nonnull @PathVariable String processSlug,
                          @Nonnull @PathVariable String formSlug,
                          @Nonnull @PathVariable UUID instanceAccessKey,
                          @Nullable @RequestParam(value = "version", required = false) Integer version,
                          @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {

    }

    @Nonnull
    private ResolvedFormTriggerContext resolveFormTriggerContext(@Nullable Jwt jwt,
                                                                 @Nonnull String processSlug,
                                                                 @Nonnull String formSlug,
                                                                 @Nullable String testClaimAccessKey,
                                                                 @Nullable Integer processVersion) throws ResponseException {
        var execUser = getExecUser(jwt);
        var process = getProcessEntity(processSlug);
        var processVersionEntity = getProcessVersionEntity(testClaimAccessKey, processVersion, process, execUser);
        var node = getProcessNodeEntity(formSlug, process, processVersionEntity);
        var provider = getProvider(node);
        var config = getConfigurationDetails(node, provider, execUser);
        var formLayout = config.configuration().formLayout;

        if (formLayout == null) {
            throw ResponseException.internalServerError("Die Konfiguration des Formulareingangs enthält kein Formular.");
        }

        return new ResolvedFormTriggerContext(process, processVersionEntity, node, formLayout);
    }

    @Nonnull
    private ThemeEntity getFormTheme(@Nonnull FormLayoutElement formLayout) {
        return getFormThemesInOrderOfImportance(formLayout).get(0);
    }

    @Nonnull
    private List<ThemeEntity> getCustomFormThemesInOrderOfImportance(@Nonnull FormLayoutElement formLayout) {
        var themes = new ArrayList<ThemeEntity>();

        if (formLayout.getThemeId() != null) {
            themeService
                    .retrieve(formLayout.getThemeId())
                    .ifPresent(themes::add);
        }

        addDepartmentTheme(themes, formLayout.getResponsibleDepartmentId());
        addDepartmentTheme(themes, formLayout.getManagingDepartmentId());

        return themes;
    }

    @Nonnull
    private List<ThemeEntity> getFormThemesInOrderOfImportance(@Nonnull FormLayoutElement formLayout) {
        var themes = getCustomFormThemesInOrderOfImportance(formLayout);
        themes.add(systemService.retrieveDefaultTheme());

        return themes;
    }

    private void addDepartmentTheme(@Nonnull List<ThemeEntity> themes,
                                    @Nullable Integer departmentId) {
        if (departmentId == null) {
            return;
        }

        vDepartmentShadowedService
                .retrieve(departmentId)
                .ifPresent(department -> {
                    if (department.getThemeId() != null) {
                        themeService
                                .retrieve(department.getThemeId())
                                .ifPresent(themes::add);
                    }
                });
    }

    @Nullable
    private UUID getFirstLogoKey(@Nonnull List<ThemeEntity> themes) {
        for (var theme : themes) {
            if (theme.getLogoKey() != null) {
                return theme.getLogoKey();
            }
        }

        return null;
    }

    @Nonnull
    private LogoResolution getFormLogoResolution(@Nonnull FormLayoutElement formLayout) {
        var customThemes = getCustomFormThemesInOrderOfImportance(formLayout);

        // A resolved custom theme chain without a logo should stay logo-less instead of inheriting
        // the system theme logo. Only forms without custom themes fall back to the system/default logo.
        if (!customThemes.isEmpty()) {
            return new LogoResolution(getFirstLogoKey(customThemes), false);
        }

        var systemTheme = systemService.retrieveDefaultTheme();
        if (systemTheme.getLogoKey() != null) {
            return new LogoResolution(systemTheme.getLogoKey(), true);
        }

        return new LogoResolution(null, true);
    }

    @Nullable
    private UUID getFormFaviconKey(@Nonnull FormLayoutElement formLayout) {
        var themes = getFormThemesInOrderOfImportance(formLayout);

        for (var theme : themes) {
            if (theme.getFaviconKey() != null) {
                return theme.getFaviconKey();
            }
        }

        return null;
    }

    private record LogoResolution(@Nullable UUID assetKey, boolean allowDefaultFallback) {
    }


    @Nonnull
    private FormTriggerNodeV1 getProvider(ProcessNodeEntity node) throws ResponseException {
        return processNodeDefinitionService
                .getProcessNodeDefinition(node, FormTriggerNodeV1.class)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private ProcessNodeEntity getProcessNodeEntity(@Nonnull String formSlug, ProcessEntity process, ProcessVersionEntity processVersion) throws ResponseException {
        var nodeFilter = ProcessNodeFilter
                .create()
                .setProcessId(process.getId())
                .setProcessVersion(processVersion.getProcessVersion())
                .setProcessNodeDefinitionKey(formTriggerNodeV1.getKey())
                .setProcessNodeDefinitionVersion(1)
                .addConfigEquals(FormTriggerConfigV1.FORM_SLUG, formSlug);

        return processNodeService
                .retrieve(nodeFilter)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private ProcessVersionEntity getProcessVersionEntity(@Nullable String testClaimAccessKey,
                                                         @Nullable Integer processVersion,
                                                         ProcessEntity process,
                                                         @Nullable UserEntity execUser) throws ResponseException {
        var processVersionFilter = ProcessVersionFilter
                .create()
                .setProcessId(process.getId());

        if (processVersion != null) {
            // Explicit version access is only for authenticated staff tooling.
            // Public citizen URLs must resolve through the published version or a test claim.
            if (execUser == null) {
                throw ResponseException.notFound();
            }

            processVersionFilter
                    .setProcessVersion(processVersion);
        } else if (testClaimAccessKey != null) {
            var testClaim = processTestClaimService
                    .retrieveByAccessKey(process.getId(), testClaimAccessKey)
                    .orElseThrow(ResponseException::notFound);
            processVersionFilter
                    .setProcessVersion(testClaim.getProcessVersion());
        } else {
            processVersionFilter
                    .setStatus(ProcessVersionStatus.Published);
        }
        return processVersionService
                .retrieve(processVersionFilter)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private ProcessEntity getProcessEntity(@Nonnull String processSlug) throws ResponseException {
        return processService
                .retrieveBySlugOrHistory(processSlug)
                .orElseThrow(ResponseException::notFound);
    }

    @Nullable
    private UserEntity getExecUser(@Nullable Jwt jwt) throws ResponseException {
        return userService
                .fromJWT(jwt)
                .orElse(null);
    }

    private record ResolvedFormTriggerContext(
            @Nonnull ProcessEntity process,
            @Nonnull ProcessVersionEntity processVersion,
            @Nonnull ProcessNodeEntity node,
            @Nonnull FormLayoutElement formLayout
    ) {
    }
}
