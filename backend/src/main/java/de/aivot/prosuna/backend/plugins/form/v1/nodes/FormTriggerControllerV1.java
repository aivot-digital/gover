package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.captcha.services.CaptchaReplayGuard;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.dtos.ElementDerivationResponse;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.prosuna.backend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationLogger;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.utils.ElementFlattenUtils;
import de.aivot.prosuna.backend.elements.utils.ElementStreamUtils;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.identity.controllers.IdentityController;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.identity.utils.IdentityCookieUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.models.dtos.MaxFileSizeDto;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.process.configs.DefaultStorageProcessAttachmentsSystemConfigDefinition;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.filters.ProcessVersionFilter;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.services.PdfService;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.services.StorageProviderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.dtos.ThemeResponseDTO;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/public/form/{processSlug}/{formSlug}/")
public class FormTriggerControllerV1 {
    public static final String TEST_CLAIM_QUERY_PARAM = "test-claim";
    public static final String VERSION_QUERY_PARAM = "version";

    private final ProsunaConfig prosunaConfig;
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
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final StorageService storageService;
    private final FileUploadMultipartInputService fileUploadMultipartInputService;
    private final ElementDataTransformService elementDataTransformService;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final FormTriggerNodeV1 formTriggerNodeV1;
    private final IdentityService identityService;
    private final PaymentPayloadCreationService paymentRequestCreationService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PdfService pdfService;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;

    @Autowired
    public FormTriggerControllerV1(ProsunaConfig prosunaConfig,
                                   IdentityProviderService identityProviderService,
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
                                   CaptchaReplayGuard captchaReplayGuard,
                                   ProcessInstanceService processInstanceService,
                                   ProcessInstanceTaskService processInstanceTaskService,
                                   ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                                   ProcessInstanceAttachmentService processInstanceAttachmentService,
                                   StorageService storageService,
                                   FileUploadMultipartInputService fileUploadMultipartInputService,
                                   ElementDataTransformService elementDataTransformService,
                                   ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                                   FormTriggerNodeV1 formTriggerNodeV1,
                                   IdentityService identityService,
                                   PaymentPayloadCreationService paymentRequestCreationService,
                                   PaymentTransactionService paymentTransactionService,
                                   PaymentProviderRepository paymentProviderRepository,
                                   PdfService pdfService, PaymentProviderDefinitionsService paymentProviderDefinitionsService) {
        this.prosunaConfig = prosunaConfig;
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
        this.processInstanceTaskService = processInstanceTaskService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.storageService = storageService;
        this.fileUploadMultipartInputService = fileUploadMultipartInputService;
        this.elementDataTransformService = elementDataTransformService;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.formTriggerNodeV1 = formTriggerNodeV1;
        this.identityService = identityService;
        this.paymentRequestCreationService = paymentRequestCreationService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderRepository = paymentProviderRepository;
        this.pdfService = pdfService;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
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
        return processNodeService
                .deriveConfiguration(node, provider, execUser, true);
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
        var execUser = getExecUser(jwt);
        var process = getProcessEntity(processSlug);
        var processVersion = getProcessVersionEntity(testClaimAccessKey, null, process, execUser);
        var node = getProcessNodeEntity(formSlug, process, processVersion);

        FormTriggerConfigV1 config = processNodeService
                .deriveConfiguration(node, formTriggerNodeV1, execUser, true)
                .configuration();

        PaymentConfigElementValue paymentConfig = config.payment;

        if (paymentConfig == null) {
            // No payment required
            return FormTriggerCostCalculationResponseV1.empty();
        }

        if (paymentConfig.paymentProviderKey() == null) {
            throw ResponseException.internalServerError("Für den Formularauslöser ist kein gültiger Zahlungsanbieter hinterlegt.");
        }

        PaymentProviderEntity paymentProvider = paymentProviderRepository
                .findById(paymentConfig.paymentProviderKey())
                .orElseThrow(ResponseException::internalServerError);

        PaymentProviderDefinition paymentProviderDefinition = paymentProviderDefinitionsService
                .getProviderDefinition(paymentProvider.getPaymentProviderDefinitionKey(), paymentProvider.getPaymentProviderDefinitionVersion())
                .orElseThrow(ResponseException::internalServerError);

        var options = new ElementDerivationOptions()
                .setSkipValuesForElementIds(List.of())
                .setSkipOverridesForElementIds(List.of())
                .setSkipErrorsForElementIds(List.of(ElementDerivationOptions.ALL_ELEMENTS))
                .setSkipVisibilitiesForElementIds(List.of());

        var request = new ElementDerivationRequest(
                config.formLayout,
                values,
                options
        );

        var derivationLogger = new ElementDerivationLogger();
        var derivedElementData = elementDerivationService
                .derive(request, new IdentityDataMap(), derivationLogger);

        var payloadInstanceData = elementDataTransformService.buildPayload(
                config.formLayout,
                derivedElementData.getEffectiveValues(),
                derivedElementData.getElementStates()
        );

        ProcessExecutionData execData = new ProcessExecutionData();
        execData.addProcessData(payloadInstanceData);

        Optional<PaymentPayload> paymentRequest = paymentRequestCreationService.createRequest(
                paymentConfig,
                derivedElementData,
                execData
        );

        // No payment required
        return paymentRequest
                .map(paymentPayload -> FormTriggerCostCalculationResponseV1.of(
                        paymentPayload,
                        paymentProviderDefinition
                ))
                .orElseGet(FormTriggerCostCalculationResponseV1::empty);
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
            initialPayload.put(FormTriggerNodeV1.DATA_KEY_ATTACHMENTS, attachments.stream().map((a) -> {
                var attachmentData = new LinkedHashMap<String, Object>();
                attachmentData.put("key", a.getKey());
                attachmentData.put("fileName", a.getFileName());
                attachmentData.put("originalFileName", a.getOriginalFileName());
                attachmentData.put("group", a.getGroup());
                attachmentData.put("storageProviderId", a.getStorageProviderId());
                attachmentData.put("storagePathFromRoot", a.getStoragePathFromRoot());
                return attachmentData;
            }).toList());
            // TODO: Apply TimeZone here
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
        var theme = getFormTheme(context.processVersion(), context.formLayout());
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
                        @Nullable @RequestParam(value = "color-scheme", required = false) String colorScheme,
                        @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        var context = resolveFormTriggerContext(jwt, processSlug, formSlug, testClaimAccessKey, processVersion);
        var logoResolution = getFormLogoResolution(
                context.processVersion(),
                context.formLayout(),
                "dark".equalsIgnoreCase(colorScheme)
        );

        String redirectUrl;
        if (logoResolution.assetKey() == null && logoResolution.allowDefaultFallback()) {
            redirectUrl = prosunaConfig.getDefaultLogoUrl();
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
        var faviconKey = getFormFaviconKey(context.processVersion(), context.formLayout());

        String redirectUrl;
        if (faviconKey == null) {
            redirectUrl = prosunaConfig.getDefaultFaviconUrl();
        } else {
            redirectUrl = assetService.createUrl(faviconKey);
        }

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("submit/{instanceAccessKey}/{taskAccessKey}/print/")
    @Operation(
            summary = "Get submitted form summary PDF",
            description = "Download the printable summary PDF created for a submitted form."
    )
    public void getPrint(@Nullable @AuthenticationPrincipal Jwt jwt,
                         @Nonnull @PathVariable String processSlug,
                         @Nonnull @PathVariable String formSlug,
                         @Nonnull @PathVariable String instanceAccessKey,
                         @Nonnull @PathVariable String taskAccessKey,
                         @Nullable @RequestParam(value = VERSION_QUERY_PARAM, required = false) Integer version,
                         @Nonnull HttpServletResponse response
    ) throws ResponseException, IOException {
        var attachment = resolveSubmittedSummaryAttachment(
                jwt,
                processSlug,
                formSlug,
                instanceAccessKey,
                taskAccessKey,
                version
        );

        response.setContentType("application/pdf");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition
                        .attachment()
                        .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString()
        );

        try (var inputStream = storageService.getDocumentContent(
                attachment.getStorageProviderId(),
                attachment.getStoragePathFromRoot()
        )) {
            inputStream.transferTo(response.getOutputStream());
        }
    }

    @GetMapping("submit/{instanceAccessKey}/{taskAccessKey}/payment-confirmation/")
    @Operation(
            summary = "Get submitted form payment confirmation PDF",
            description = "Download the payment confirmation PDF for a paid submitted form."
    )
    public void getPaymentConfirmation(@Nullable @AuthenticationPrincipal Jwt jwt,
                                       @Nonnull @PathVariable String processSlug,
                                       @Nonnull @PathVariable String formSlug,
                                       @Nonnull @PathVariable String instanceAccessKey,
                                       @Nonnull @PathVariable String taskAccessKey,
                                       @Nullable @RequestParam(value = VERSION_QUERY_PARAM, required = false) Integer version,
                                       @Nonnull HttpServletResponse response) throws ResponseException, IOException {
        var context = resolveSubmittedFormTriggerTaskContext(
                jwt,
                processSlug,
                formSlug,
                instanceAccessKey,
                taskAccessKey,
                version
        );

        var transaction = resolvePaymentTransaction(context, instanceAccessKey, taskAccessKey);
        if (transaction.getStatus() != XBezahldienstStatus.PAYED) {
            throw ResponseException.notFound();
        }

        var department = resolvePaymentConfirmationDepartment(context);
        var logoUrl = resolvePaymentConfirmationLogoUrl(context.processVersion(), context.formLayout());

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePaymentConfirmation(
                    transaction,
                    context.instance().getCaseNumber(),
                    logoUrl,
                    department
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ResponseException.internalServerError(e, "Die PDF-Erstellung der Zahlungsbestätigung wurde unterbrochen.");
        } catch (IOException | URISyntaxException | TemplateProcessingException e) {
            throw ResponseException.internalServerError(e, "Fehler beim Erzeugen der Zahlungsbestätigung: %s", e.getMessage());
        }

        response.setContentType("application/pdf");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition
                        .attachment()
                        .filename("Zahlungsbestaetigung-" + context.instance().getCaseNumber() + ".pdf", StandardCharsets.UTF_8)
                        .build()
                        .toString()
        );

        response.getOutputStream().write(pdfBytes);
    }

    @Nonnull
    private ProcessInstanceAttachmentEntity resolveSubmittedSummaryAttachment(@Nullable Jwt jwt,
                                                                              @Nonnull String processSlug,
                                                                              @Nonnull String formSlug,
                                                                              @Nonnull String instanceAccessKey,
                                                                              @Nonnull String taskAccessKey,
                                                                              @Nullable Integer version) throws ResponseException {
        var context = resolveSubmittedFormTriggerTaskContext(
                jwt,
                processSlug,
                formSlug,
                instanceAccessKey,
                taskAccessKey,
                version
        );

        var attachmentSet = processInstanceAttachmentSetService
                .retrieveLatestByProcessInstanceIdAndTaskIdAndDataKey(
                        context.instance().getId(),
                        context.task().getId(),
                        context.node().getDataKey()
                )
                .orElseThrow(ResponseException::notFound);

        return processInstanceAttachmentService
                .findAllByAttachmentSetId(attachmentSet.getId())
                .stream()
                .findFirst()
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private ResolvedSubmittedFormTriggerTaskContext resolveSubmittedFormTriggerTaskContext(@Nullable Jwt jwt,
                                                                                          @Nonnull String processSlug,
                                                                                          @Nonnull String formSlug,
                                                                                          @Nonnull String instanceAccessKey,
                                                                                          @Nonnull String taskAccessKey,
                                                                                          @Nullable Integer version) throws ResponseException {
        var execUser = getExecUser(jwt);
        var process = getProcessEntity(processSlug);
        var instance = processInstanceService
                .retrieveByAccessKey(instanceAccessKey)
                .orElseThrow(ResponseException::notFound);

        if (!Objects.equals(process.getId(), instance.getProcessId())) {
            throw ResponseException.notFound();
        }

        var task = processInstanceTaskService
                .retrieveByProcessInstanceIdAndAccessKey(instance.getId(), taskAccessKey)
                .orElseThrow(ResponseException::notFound);

        if (version != null && !Objects.equals(version, task.getProcessVersion())) {
            throw ResponseException.notFound();
        }

        var node = processNodeService
                .retrieve(task.getProcessNodeId())
                .orElseThrow(ResponseException::notFound);

        if (!Objects.equals(process.getId(), node.getProcessId()) ||
                !Objects.equals(task.getProcessVersion(), node.getProcessVersion()) ||
                !Objects.equals(formTriggerNodeV1.getKey(), node.getProcessNodeDefinitionKey()) ||
                !Objects.equals(1, node.getProcessNodeDefinitionVersion())) {
            throw ResponseException.notFound();
        }

        var processVersion = processVersionService
                .retrieve(ProcessVersionEntityId.of(node.getProcessId(), node.getProcessVersion()))
                .orElseThrow(ResponseException::notFound);

        var provider = getProvider(node);
        var config = getConfigurationDetails(node, provider, execUser);
        if (!Objects.equals(formSlug, config.configuration().formSlug)) {
            throw ResponseException.notFound();
        }

        var formLayout = config.configuration().formLayout;
        if (formLayout == null) {
            throw ResponseException.internalServerError("Die Konfiguration des Formulareingangs enthält kein Formular.");
        }

        return new ResolvedSubmittedFormTriggerTaskContext(
                process,
                instance,
                task,
                node,
                processVersion,
                formLayout
        );
    }

    @Nonnull
    private PaymentTransactionEntity resolvePaymentTransaction(@Nonnull ResolvedSubmittedFormTriggerTaskContext context,
                                                               @Nonnull String instanceAccessKey,
                                                               @Nonnull String taskAccessKey) throws ResponseException {
        var expectedRedirectUrl = prosunaConfig.createUrl("/process/", instanceAccessKey, "tasks", taskAccessKey);
        var runtimeData = context.task().getRuntimeData();
        var transactionKey = runtimeData != null ? runtimeData.get(FormTriggerNodeV1.DATA_KEY_PAYMENT_TRANSACTION_KEY) : null;

        var transaction = transactionKey != null
                ? paymentTransactionService
                .retrieve(String.valueOf(transactionKey))
                .orElseThrow(ResponseException::notFound)
                : paymentTransactionService
                .retrieveByRedirectUrl(expectedRedirectUrl)
                .orElseThrow(ResponseException::notFound);

        if (!Objects.equals(transaction.getRedirectUrl(), expectedRedirectUrl)) {
            throw ResponseException.notFound();
        }

        return transaction;
    }

    @Nonnull
    private VDepartmentShadowedEntity resolvePaymentConfirmationDepartment(@Nonnull ResolvedSubmittedFormTriggerTaskContext context) throws ResponseException {
        var formDepartmentId = context.formLayout().getRelevantDepartmentId();
        if (formDepartmentId != null) {
            var formDepartment = vDepartmentShadowedService.retrieve(formDepartmentId);
            if (formDepartment.isPresent()) {
                return formDepartment.get();
            }
        }

        return vDepartmentShadowedService
                .retrieve(context.process().getDepartmentId())
                .orElseThrow(() -> ResponseException.internalServerError("Keine zuständige Organisationseinheit für die Zahlungsbestätigung gefunden."));
    }

    @Nullable
    private String resolvePaymentConfirmationLogoUrl(@Nonnull ProcessVersionEntity processVersion,
                                                     @Nonnull FormLayoutElement formLayout) {
        var logoResolution = getFormLogoResolution(processVersion, formLayout, false); // We never use the dark logo for printouts
        if (logoResolution.assetKey() != null) {
            return assetService.createUrl(logoResolution.assetKey());
        }

        return logoResolution.allowDefaultFallback() ? prosunaConfig.getDefaultLogoUrl() : null;
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
    private ThemeEntity getFormTheme(@Nonnull ProcessVersionEntity processVersion,
                                     @Nonnull FormLayoutElement formLayout) {
        return getFormThemesInOrderOfImportance(processVersion, formLayout).getFirst();
    }

    @Nonnull
    private List<ThemeEntity> getCustomFormThemesInOrderOfImportance(@Nonnull ProcessVersionEntity processVersion,
                                                                     @Nonnull FormLayoutElement formLayout) {
        var themes = new ArrayList<ThemeEntity>();

        if (processVersion.getThemeId() != null) {
            themeService
                    .retrieve(processVersion.getThemeId())
                    .ifPresent(themes::add);
        }

        addDepartmentTheme(themes, formLayout.getResponsibleDepartmentId());
        addDepartmentTheme(themes, formLayout.getManagingDepartmentId());

        return themes;
    }

    @Nonnull
    private List<ThemeEntity> getFormThemesInOrderOfImportance(@Nonnull ProcessVersionEntity processVersion,
                                                               @Nonnull FormLayoutElement formLayout) {
        var themes = getCustomFormThemesInOrderOfImportance(processVersion, formLayout);
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
    private UUID getFirstLogoKey(@Nonnull List<ThemeEntity> themes, boolean darkColorScheme) {
        for (var theme : themes) {
            var logoKey = darkColorScheme && theme.getLogoKeyDark() != null
                    ? theme.getLogoKeyDark()
                    : theme.getLogoKey();
            if (logoKey != null) {
                return logoKey;
            }
        }

        return null;
    }

    @Nonnull
    private LogoResolution getFormLogoResolution(@Nonnull ProcessVersionEntity processVersion,
                                                 @Nonnull FormLayoutElement formLayout,
                                                 boolean darkColorScheme) {
        var customThemes = getCustomFormThemesInOrderOfImportance(processVersion, formLayout);

        // A resolved custom theme chain without a logo should stay logo-less instead of inheriting
        // the system theme logo. Only forms without custom themes fall back to the system/default logo.
        if (!customThemes.isEmpty()) {
            return new LogoResolution(getFirstLogoKey(customThemes, darkColorScheme), false);
        }

        var systemTheme = systemService.retrieveDefaultTheme();
        var systemLogoKey = darkColorScheme && systemTheme.getLogoKeyDark() != null
                ? systemTheme.getLogoKeyDark()
                : systemTheme.getLogoKey();
        if (systemLogoKey != null) {
            return new LogoResolution(systemLogoKey, true);
        }

        return new LogoResolution(null, true);
    }

    @Nullable
    private UUID getFormFaviconKey(@Nonnull ProcessVersionEntity processVersion,
                                   @Nonnull FormLayoutElement formLayout) {
        var themes = getFormThemesInOrderOfImportance(processVersion, formLayout);

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

    private record ResolvedSubmittedFormTriggerTaskContext(
            @Nonnull ProcessEntity process,
            @Nonnull ProcessInstanceEntity instance,
            @Nonnull ProcessInstanceTaskEntity task,
            @Nonnull ProcessNodeEntity node,
            @Nonnull ProcessVersionEntity processVersion,
            @Nonnull FormLayoutElement formLayout
    ) {
    }
}
