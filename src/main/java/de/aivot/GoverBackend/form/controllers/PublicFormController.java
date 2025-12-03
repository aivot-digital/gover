package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.elements.dtos.ElementDerivationResponse;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.form.dtos.FormCitizenDetailsResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormCitizenListResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormCostCalculationResponseDTO;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.services.FormPaymentService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.FormVersionService;
import de.aivot.GoverBackend.form.services.VFormVersionWithDetailsService;
import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.identity.dtos.IdentityDetailsDTO;
import de.aivot.GoverBackend.identity.filters.IdentityProviderFilter;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.dtos.MaxFileSizeDto;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.models.PaymentItem;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.theme.dtos.ThemeResponseDTO;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/forms/")
@Tag(
        name = "Forms",
        description = "Forms are built for collecting data from users. " +
                      "They can be designed with various elements and configurations to suit different data collection needs. " +
                      "Forms can be published, managed, and analyzed within the system."
)
public class PublicFormController {
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

    @Autowired
    public PublicFormController(GoverConfig goverConfig,
                                FormPaymentService paymentService,
                                PaymentProviderService paymentProviderService,
                                DestinationService destinationService,
                                IdentityProviderService identityProviderService,
                                IdentityCacheRepository identityCacheRepository,
                                ElementDerivationService elementDerivationService,
                                AssetService assetService,
                                FormVersionService formVersionService,
                                VFormVersionWithDetailsService vFormVersionWithDetailsService, UserService userService) {
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
    }

    @GetMapping("")
    @Operation(
            summary = "List published forms for citizens",
            description = "List all published forms that are available for citizens to fill out. " +
                          "Only published public forms are included in the list."
    )
    public Page<FormCitizenListResponseDTO> list(@Nonnull @ParameterObject @PageableDefault Pageable pageable,
                                                 @Nonnull @ParameterObject @Valid VFormVersionWithDetailsFilter filter) throws ResponseException {
        filter.setStatus(FormStatus.Published);
        filter.setType(FormType.Public);

        return vFormVersionWithDetailsService
                .list(pageable, filter)
                .map(FormCitizenListResponseDTO::fromEntity);
    }

    @GetMapping("{slug}/")
    @Operation(
            summary = "Retrieve form details for citizens",
            description = "Retrieve detailed information about a specific form available for citizens to fill out. " +
                          "Includes form structure, elements, and configuration details. " +
                          "Internal forms without identity verification will have all steps, except the first, obfuscated."
    )
    public FormCitizenDetailsResponseDTO retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                  @Nonnull @PathVariable String slug,
                                                  @Nullable @RequestParam(value = "version", required = false) Integer version,
                                                  @Nullable @RequestHeader(value = IdentityController.IDENTITY_HEADER_NAME, required = false) UUID identityId) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt, false);

        var identityCache = identityId == null ? Optional.empty() : identityCacheRepository
                .findById(identityId);

        var obfuscateSteps = (
                formVersion.getType() == FormType.Internal &&
                formVersion.getIdentityVerificationRequired() &&
                identityCache.isEmpty()
        );

        return FormCitizenDetailsResponseDTO
                .fromEntity(formVersion, obfuscateSteps);
    }

    @GetMapping("{slug}/max-file-size/")
    @Operation(
            summary = "Get maximum file size for attachments in a form",
            description = "Retrieve the maximum allowed file size for attachments in the specified form. " +
                          "If the form is linked to a destination with specific file size limits, those limits will be returned. " +
                          "Otherwise, a default value will be provided."
    )
    public MaxFileSizeDto getMaxFileSize(@Nullable @AuthenticationPrincipal Jwt jwt,
                                         @Nonnull @PathVariable String slug,
                                         @Nullable @RequestParam(value = "version", required = false) Integer version) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt, false);

        MaxFileSizeDto maxFileSizeDto = new MaxFileSizeDto();
        maxFileSizeDto.setMaxFileSize(100);

        if (formVersion.getDestinationId() == null) {
            return maxFileSizeDto;
        }

        var destination = destinationService
                .retrieve(formVersion.getDestinationId());

        if (destination.isEmpty()) {
            return maxFileSizeDto;
        }

        if (destination.get().getMaxAttachmentMegaBytes() != null) {
            maxFileSizeDto.setMaxFileSize(destination.get().getMaxAttachmentMegaBytes());
        }

        return maxFileSizeDto;
    }

    @PostMapping("{slug}/costs/")
    @Operation(
            summary = "Calculate costs for a form based on customer data",
            description = "Calculate the total costs for a form based on the provided customer data. " +
                          "If the form has an associated payment provider, the costs will be calculated accordingly. " +
                          "If no payment provider is linked, the response will indicate that there are no costs."
    )
    public FormCostCalculationResponseDTO calculateCosts(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                         @Nonnull @PathVariable String slug,
                                                         @Nullable @RequestParam(value = "version", required = false) Integer version,
                                                         @Nonnull @RequestBody ElementData customerData) throws PaymentException, ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt, false);

        if (formVersion.getPaymentProviderKey() == null) {
            return new FormCostCalculationResponseDTO(null, null, null);
        }

        var paymentProvider = paymentProviderService
                .retrieve(formVersion.getPaymentProviderKey());

        if (paymentProvider.isEmpty()) {
            return new FormCostCalculationResponseDTO(null, null, null);
        }

        var paymentProviderDefinition = paymentProviderService
                .getProviderDefinition(paymentProvider.get().getProviderKey());

        if (paymentProviderDefinition.isEmpty()) {
            return new FormCostCalculationResponseDTO(null, null, null);
        }

        var paymentItems = paymentService
                .createPaymentItems(formVersion, customerData);

        var costs = paymentItems
                .stream()
                .map(PaymentItem::getTotalPrice)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);

        return new FormCostCalculationResponseDTO(costs, paymentItems, paymentProviderDefinition.get().getProviderName());
    }

    @GetMapping("{slug}/identity-providers/")
    @Operation(
            summary = "List identity providers linked to a form",
            description = "Retrieve a list of identity providers that are linked to the specified form. " +
                          "Only enabled identity providers will be included in the response."
    )
    public Page<IdentityDetailsDTO> getIdentityProviders(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                         @Nonnull @PathVariable String slug,
                                                         @Nullable @RequestParam(value = "version", required = false) Integer version
    ) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt, false);

        var identityProviderKeys = formVersion
                .getIdentityProviders()
                .stream()
                .map(IdentityProviderLink::getIdentityProviderKey)
                .toList();

        if (identityProviderKeys.isEmpty()) {
            return Page.empty();
        }

        var filter = new IdentityProviderFilter()
                .setKeys(identityProviderKeys)
                .setIsEnabled(true);

        var pageable = Pageable
                .unpaged(Sort.by(Sort.Direction.ASC, "name"));

        var identityProviders = identityProviderService
                .list(pageable, filter);

        return identityProviders
                .map(IdentityDetailsDTO::from);
    }

    @PostMapping("{slug}/derive")
    @Operation(
            summary = "Derive element data based on input for a form",
            description = "Derive the element data for a form based on the provided input data. " +
                          "This process evaluates the form's logic, visibility rules, and calculations to produce the derived data. " +
                          "Options are available to skip certain derivation aspects for specific elements."
    )
    public ElementDerivationResponse derive(@Nullable @AuthenticationPrincipal Jwt jwt,
                                            @Nonnull @PathVariable String slug,
                                            @Nullable @RequestParam(value = "version", required = false) Integer version,
                                            @Nonnull @Valid @RequestBody ElementData elementData,
                                            @Nullable @RequestParam(value = "skipErrorsFor") List<String> skipErrorsFor,
                                            @Nullable @RequestParam(value = "skipVisibilitiesFor") List<String> skipVisibilitiesFor,
                                            @Nullable @RequestParam(value = "skipValuesFor") List<String> skipValuesFor,
                                            @Nullable @RequestParam(value = "skipOverridesFor") List<String> skipOverridesFor) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt, false);

        var options = new ElementDerivationOptions()
                .setSkipValuesForElementIds(skipValuesFor)
                .setSkipOverridesForElementIds(skipOverridesFor)
                .setSkipErrorsForElementIds(skipErrorsFor)
                .setSkipVisibilitiesForElementIds(skipVisibilitiesFor);

        var request = new ElementDerivationRequest()
                .setElement(formVersion.getRootElement())
                .setElementData(elementData)
                .setOptions(options);

        var derivationLogger = new ElementDerivationLogger();
        var derivedElementData = elementDerivationService
                .derive(request, derivationLogger);

        var inputIdValue = elementData
                .getOrDefault(IdentityValueKey.IdCustomerInputKey, new ElementDataObject(ElementType.SubmittedStep))
                .setComputedErrors(null); // Clear any previous computed errors

        derivedElementData.put(IdentityValueKey.IdCustomerInputKey, inputIdValue);

        if (options.notContainsSkipErrors(formVersion.getRootElement().getIntroductionStep().getId())) {
            if (formVersion.getIdentityVerificationRequired() && inputIdValue.isEmpty()) {
                inputIdValue.setComputedErrors(List.of("Bitte melden Sie sich mit einem der Nutzerkonten an."));
            }
        }

        return ElementDerivationResponse
                .from(elementData, derivationLogger, jwt != null);
    }

    @GetMapping("{slug}/theme/")
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

    @GetMapping("{slug}/logo/")
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

    @GetMapping("{slug}/favicon/")
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

    private VFormVersionWithDetailsEntity getFormVersionWithDetailsEntity(@Nonnull String slug,
                                                                          @Nullable Integer version,
                                                                          @Nullable @AuthenticationPrincipal Jwt jwt,
                                                                          @Nonnull Boolean acceptUnauthenticated) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElse(null);

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
}
