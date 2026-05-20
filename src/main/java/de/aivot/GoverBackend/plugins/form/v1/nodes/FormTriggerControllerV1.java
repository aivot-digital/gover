package de.aivot.GoverBackend.plugins.form.v1.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.av.services.AVService;
import de.aivot.GoverBackend.captcha.services.CaptchaReplayGuard;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.elements.dtos.ElementDerivationResponse;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.elements.utils.ElementFlattenUtils;
import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.form.dtos.FormCostCalculationResponseDTO;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.services.FormPaymentService;
import de.aivot.GoverBackend.form.services.FormVersionService;
import de.aivot.GoverBackend.form.services.VFormVersionWithDetailsService;
import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.dtos.MaxFileSizeDto;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.GoverBackend.process.entities.*;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.process.filters.ProcessNodeFilter;
import de.aivot.GoverBackend.process.filters.ProcessTestClaimFilter;
import de.aivot.GoverBackend.process.filters.ProcessVersionFilter;
import de.aivot.GoverBackend.process.services.*;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.services.StorageProviderService;
import de.aivot.GoverBackend.submission.dtos.SubmissionStatusResponseDTO;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import de.aivot.GoverBackend.theme.dtos.ThemeResponseDTO;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
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
@RequestMapping("/api/public/forms/v1/{processAccessKey}/{formSlug}/")
public class FormTriggerControllerV1 {
    public static final String TEST_CLAIM_QUERY_PARAM = "test-claim";

    private final GoverConfig goverConfig;
    private final FormPaymentService paymentService;
    private final PaymentProviderService paymentProviderService;
    private final DestinationService destinationService;
    private final IdentityProviderService identityProviderService;
    private final IdentityCacheRepository identityCacheRepository;
    private final ElementDerivationService elementDerivationService;
    private final AssetService assetService;
    private final FormVersionService formVersionService;
    private final VFormVersionWithDetailsService vFormVersionWithDetailsService;
    private final UserService userService;
    private final ProcessService processService;
    private final ProcessNodeService processNodeService;
    private final ProcessTestClaimService processTestClaimService;
    private final ProcessVersionService processVersionService;
    private final ProcessNodeDefinitionService processNodeDefinitionService;
    private final SystemConfigService systemConfigService;
    private final StorageProviderService storageProviderService;
    private final AVService aVService;
    private final CaptchaReplayGuard captchaReplayGuard;
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ElementDataTransformService elementDataTransformService;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final FormTriggerNodeV1 formTriggerNodeV1;

    @Autowired
    public FormTriggerControllerV1(GoverConfig goverConfig,
                                   FormPaymentService paymentService,
                                   PaymentProviderService paymentProviderService,
                                   DestinationService destinationService,
                                   IdentityProviderService identityProviderService,
                                   IdentityCacheRepository identityCacheRepository,
                                   ElementDerivationService elementDerivationService,
                                   AssetService assetService,
                                   FormVersionService formVersionService,
                                   VFormVersionWithDetailsService vFormVersionWithDetailsService,
                                   UserService userService,
                                   ProcessService processService,
                                   ProcessNodeService processNodeService,
                                   ProcessTestClaimService processTestClaimService,
                                   ProcessVersionService processVersionService,
                                   ProcessNodeDefinitionService processNodeDefinitionService, SystemConfigService systemConfigService, StorageProviderService storageProviderService, AVService aVService, CaptchaReplayGuard captchaReplayGuard, ProcessInstanceService processInstanceService, ProcessInstanceAttachmentService processInstanceAttachmentService, ElementDataTransformService elementDataTransformService, ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory, FormTriggerNodeV1 formTriggerNodeV1) {
        this.goverConfig = goverConfig;
        this.paymentService = paymentService;
        this.paymentProviderService = paymentProviderService;
        this.destinationService = destinationService;
        this.identityProviderService = identityProviderService;
        this.identityCacheRepository = identityCacheRepository;
        this.elementDerivationService = elementDerivationService;
        this.assetService = assetService;
        this.formVersionService = formVersionService;
        this.vFormVersionWithDetailsService = vFormVersionWithDetailsService;
        this.userService = userService;
        this.processService = processService;
        this.processNodeService = processNodeService;
        this.processTestClaimService = processTestClaimService;
        this.processVersionService = processVersionService;
        this.processNodeDefinitionService = processNodeDefinitionService;
        this.systemConfigService = systemConfigService;
        this.storageProviderService = storageProviderService;
        this.aVService = aVService;
        this.captchaReplayGuard = captchaReplayGuard;
        this.processInstanceService = processInstanceService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.elementDataTransformService = elementDataTransformService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.formTriggerNodeV1 = formTriggerNodeV1;
    }

    @GetMapping("")
    public RetrieveResponse retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                     @Nonnull @PathVariable UUID processAccessKey,
                                     @Nonnull @PathVariable String formSlug,
                                     @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                     @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) UUID identityId) throws ResponseException {
        var execUser = getExecUser(jwt);

        var process = getProcessEntity(processAccessKey);
        var processVersion = getProcessVersionEntity(testClaimAccessKey, process);
        var node = getProcessNodeEntity(formSlug, process, processVersion);
        var provider = getProvider(node);
        var config = getConfigurationDetails(node, provider, execUser);

        /*
        TODO: Reimplement

        var identityCache = identityId == null
                ? Optional.empty()
                : identityCacheRepository.findById(identityId);

        var obfuscateSteps = (
                formVersion.getType() == FormType.Internal &&
                        formVersion.getIdentityVerificationRequired() &&
                        identityCache.isEmpty()
        );
         */

        var formLayout = config
                .configuration()
                .formLayout;

        ElementStreamUtils.applyAction(formLayout, BaseElement::removeInternalInformation);

        return new RetrieveResponse(
                formLayout,
                node,
                process,
                processVersion
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
            ProcessVersionEntity version
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
                                         @Nonnull @PathVariable UUID processAccessKey,
                                         @Nonnull @PathVariable String formSlug,
                                         @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                         @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) UUID identityId) throws ResponseException {
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
    public FormCostCalculationResponseDTO calculateCosts(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                         @Nonnull @PathVariable UUID processAccessKey,
                                                         @Nonnull @PathVariable String formSlug,
                                                         @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                                         @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) UUID identityId,
                                                         @Nonnull @RequestBody AuthoredElementValues values) throws PaymentException, ResponseException {
        // TODO: Implement
        return new FormCostCalculationResponseDTO(BigDecimal.ZERO, List.of(), "");
    }

    @PostMapping("derive/")
    @Operation(
            summary = "Derive element data based on input for a form",
            description = "Derive the element data for a form based on the provided input data. " +
                    "This process evaluates the form's logic, visibility rules, and calculations to produce the derived data. " +
                    "Options are available to skip certain derivation aspects for specific elements."
    )
    public ElementDerivationResponse derive(@Nullable @AuthenticationPrincipal Jwt jwt,
                                            @Nonnull @PathVariable UUID processAccessKey,
                                            @Nonnull @PathVariable String formSlug,
                                            @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                            @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) UUID identityId,
                                            @Nonnull @Valid @RequestBody AuthoredElementValues authoredElementValues,
                                            @Nullable @RequestParam(value = "skipErrorsFor") List<String> skipErrorsFor,
                                            @Nullable @RequestParam(value = "skipVisibilitiesFor") List<String> skipVisibilitiesFor,
                                            @Nullable @RequestParam(value = "skipValuesFor") List<String> skipValuesFor,
                                            @Nullable @RequestParam(value = "skipOverridesFor") List<String> skipOverridesFor) throws ResponseException {
        var execUser = getExecUser(jwt);
        var process = getProcessEntity(processAccessKey);
        var processVersion = getProcessVersionEntity(testClaimAccessKey, process);
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

        var derivationLogger = new ElementDerivationLogger();
        var derivedElementData = elementDerivationService
                .derive(request, derivationLogger);

        return ElementDerivationResponse
                .from(derivedElementData, derivationLogger, jwt != null);
    }

    @PostMapping("submit/")
    public SubmissionStatusResponseDTO submit(@Nullable @AuthenticationPrincipal Jwt jwt,
                                              @Nonnull @PathVariable UUID processAccessKey,
                                              @Nonnull @PathVariable String formSlug,
                                              @Nullable @RequestParam(value = TEST_CLAIM_QUERY_PARAM, required = false) String testClaimAccessKey,
                                              @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) UUID identityId,
                                              @Nonnull @RequestParam(value = "inputs", required = true) String rawInputs,
                                              @Nullable @RequestParam(value = "files", required = false) MultipartFile[] files) throws ResponseException {
        var execUser = getExecUser(jwt);
        var process = getProcessEntity(processAccessKey);
        var processVersion = getProcessVersionEntity(testClaimAccessKey, process);
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

        // TODO: Hydrate the customer input with the data from an idp
        // hydrateCustomerInputWithIdpData(form, optionalIdp, derivedRuntimeElementData);

        // Perform derivation
        var logger = new ElementDerivationLogger();
        var derivationRequest = new ElementDerivationRequest(
                config.configuration().formLayout,
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
        aVService.testMultipartFiles(files);
        // TODO: Check with default process instance attachment storage provider max file size


        var testClaim = processTestClaimService
                .retrieve(ProcessTestClaimFilter
                        .create()
                        .setProcessId(processVersion.getProcessId())
                        .setProcessVersion(processVersion.getProcessVersion())
                )
                .orElse(null);


        testCaptchaReplayProtection(config.configuration().formLayout, effectiveValues);


        var processInstance = startProcess(
                testClaim,
                config.configuration().formLayout,
                node,
                effectiveValues,
                files
        );

        return new SubmissionStatusResponseDTO(processInstance.getAccessKey());
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
                                               @Nonnull EffectiveElementValues effectiveValues,
                                               @Nullable MultipartFile[] files) throws ResponseException {
        var startedAt = Instant.now();
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
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_PAYLOAD, elementDataTransformService.buildPayload(form, effectiveValues));
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_UNMAPPED, effectiveValues);
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_ATTACHMENTS, attachments.stream().map((a) -> Map.<String, Object>of(
                    "key", a.getKey(),
                    "filename", a.getFileName(),
                    "storageProviderId", a.getStorageProviderId(),
                    "storagePathFromRoot", a.getStoragePathFromRoot()
            )).toList());

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
                                     @Nonnull @PathVariable String slug,
                                     @Nullable @RequestParam(value = "version", required = false) Integer version
    ) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt, true);
        var theme = getFormTheme(formVersion);
        return ThemeResponseDTO.fromEntity(theme);
    }

    @GetMapping("logo/")
    @Operation(
            summary = "Get the logo for a form",
            description = "Get the logo image associated with the specified form. " +
                    "If the form does not have a custom logo, a default logo URL will be provided."
    )
    public void getLogo(@Nullable @AuthenticationPrincipal Jwt jwt,
                        @Nonnull @PathVariable String slug,
                        @Nullable @RequestParam(value = "version", required = false) Integer version,
                        @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt, true);
        var logoKey = getFormLogoKey(formVersion);

        String redirectUrl;
        if (logoKey == null) {
            redirectUrl = goverConfig.getDefaultLogoUrl();
        } else {
            redirectUrl = assetService.createUrl(logoKey);
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
                           @Nonnull @PathVariable String slug,
                           @Nullable @RequestParam(value = "version", required = false) Integer version,
                           @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt, true);
        var faviconKey = getFormFaviconKey(formVersion);

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
                         @Nonnull @PathVariable UUID processAccessKey,
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
                          @Nonnull @PathVariable UUID processAccessKey,
                          @Nonnull @PathVariable String formSlug,
                          @Nonnull @PathVariable UUID instanceAccessKey,
                          @Nullable @RequestParam(value = "version", required = false) Integer version,
                          @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {

    }

    private VFormVersionWithDetailsEntity getFormVersionWithDetailsEntity(@Nonnull String slug,
                                                                          @Nullable Integer version,
                                                                          @Nullable @AuthenticationPrincipal Jwt jwt,
                                                                          @Nonnull Boolean acceptUnauthenticated) throws ResponseException {
        var user = getExecUser(jwt);

        VFormVersionWithDetailsEntity formVersion;
        if (user == null && !acceptUnauthenticated) {
            formVersion = vFormVersionWithDetailsService
                    .retrieve(VFormVersionWithDetailsFilter
                            .create()
                            .setSlug(slug)
                            .setStatus(FormStatus.Published))
                    .orElseThrow(ResponseException::notFound);
        } else {
            if (version != null) {
                formVersion = vFormVersionWithDetailsService
                        .findBySlugAndVersion(slug, version)
                        .orElseThrow(ResponseException::notFound);
            } else {
                formVersion = vFormVersionWithDetailsService
                        .retrieve(VFormVersionWithDetailsFilter
                                .create()
                                .setSlug(slug)
                                .setStatus(FormStatus.Published))
                        .orElseThrow(ResponseException::notFound);
            }
        }
        return formVersion;
    }

    @Nonnull
    private ThemeEntity getFormTheme(VFormVersionWithDetailsEntity formVersion) throws ResponseException {
        return formVersionService
                .getFormThemesInOrderOfImportance(formVersion.getFormId(), formVersion.getVersion())
                .getFirst();
    }

    @Nullable
    private UUID getFormLogoKey(VFormVersionWithDetailsEntity formVersion) throws ResponseException {
        var themes = formVersionService
                .getFormThemesInOrderOfImportance(formVersion.getFormId(), formVersion.getVersion());

        for (var theme : themes) {
            if (theme.getLogoKey() != null) {
                return theme.getLogoKey();
            }
        }

        return null;
    }

    @Nullable
    private UUID getFormFaviconKey(VFormVersionWithDetailsEntity formVersion) throws ResponseException {
        var themes = formVersionService
                .getFormThemesInOrderOfImportance(formVersion.getFormId(), formVersion.getVersion());

        for (var theme : themes) {
            if (theme.getFaviconKey() != null) {
                return theme.getFaviconKey();
            }
        }

        return null;
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
    private ProcessVersionEntity getProcessVersionEntity(@Nullable String testClaimAccessKey, ProcessEntity process) throws ResponseException {
        var processVersionFilter = ProcessVersionFilter
                .create()
                .setProcessId(process.getId());
        if (testClaimAccessKey != null) {
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
    private ProcessEntity getProcessEntity(@Nonnull UUID processAccessKey) throws ResponseException {
        return processService
                .retrieveByAccessKey(processAccessKey)
                .orElseThrow(ResponseException::notFound);
    }

    @Nullable
    private UserEntity getExecUser(@Nullable Jwt jwt) throws ResponseException {
        return userService
                .fromJWT(jwt)
                .orElse(null);
    }
}
