package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.form.dtos.FormCitizenDetailsResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormCitizenListResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormCostCalculationResponseDTO;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.form.filters.FormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.services.FormPaymentService;
import de.aivot.GoverBackend.form.services.FormVersionWithDetailsService;
import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.identity.dtos.IdentityDetailsDTO;
import de.aivot.GoverBackend.identity.filters.IdentityProviderFilter;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.dtos.MaxFileSizeDto;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.models.PaymentItem;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for citizen-facing form operations.
 * <p>
 * Provides public API endpoints for listing, retrieving, and interacting with published forms.
 * Handles cost calculation, file size limits, identity provider listing, and element data derivation.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>List published forms</li>
 *   <li>Retrieve form details</li>
 *   <li>Get max file size for attachments</li>
 *   <li>Calculate submission costs</li>
 *   <li>List enabled identity providers</li>
 *   <li>Derive computed element data</li>
 * </ul>
 * <p>
 * Security:
 * <ul>
 *   <li>Supports JWT authentication for user context</li>
 *   <li>Handles both authenticated and unauthenticated access</li>
 * </ul>
 * <p>
 * Error Handling:
 * <ul>
 *   <li>Throws ResponseException for not found/invalid requests</li>
 *   <li>Throws PaymentException for payment errors</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/public/forms/")
public class CitizenFormController {
    private final FormPaymentService paymentService;
    private final PaymentProviderService paymentProviderService;
    private final DestinationService destinationService;
    private final IdentityProviderService identityProviderService;
    private final IdentityCacheRepository identityCacheRepository;
    private final FormVersionWithDetailsService formVersionWithDetailsService;
    private final ElementDerivationService elementDerivationService;

    @Autowired
    public CitizenFormController(FormPaymentService paymentService,
                                 PaymentProviderService paymentProviderService,
                                 DestinationService destinationService,
                                 IdentityProviderService identityProviderService,
                                 IdentityCacheRepository identityCacheRepository,
                                 FormVersionWithDetailsService formVersionWithDetailsService, ElementDerivationService elementDerivationService) {
        this.paymentService = paymentService;
        this.paymentProviderService = paymentProviderService;
        this.destinationService = destinationService;
        this.identityProviderService = identityProviderService;
        this.identityCacheRepository = identityCacheRepository;
        this.formVersionWithDetailsService = formVersionWithDetailsService;
        this.elementDerivationService = elementDerivationService;
    }

    /**
     * Lists all published forms available to citizens.
     *
     * @param jwt      JWT token for authentication (optional).
     * @param pageable Pagination and sorting information.
     * @param filter   Filter criteria for forms.
     * @return Page of FormCitizenListResponseDTO representing published forms.
     * @throws ResponseException if an error occurs during retrieval.
     */
    @GetMapping("")
    public Page<FormCitizenListResponseDTO> list(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                 @Nonnull @PageableDefault Pageable pageable,
                                                 @Nonnull @Valid FormVersionWithDetailsFilter filter) throws ResponseException {
        return formVersionWithDetailsService
                .list(pageable, filter.setStatus(FormStatus.Published))
                .map(FormCitizenListResponseDTO::fromEntity);
    }

    /**
     * Retrieves details of a specific published form version for citizens.
     *
     * @param jwt        JWT token for authentication (optional).
     * @param slug       Unique identifier for the form.
     * @param version    Optional version number of the form.
     * @param identityId Optional identity cache ID for user context.
     * @return FormCitizenDetailsResponseDTO containing form details.
     * @throws ResponseException if the form is not found or invalid.
     */
    @GetMapping("{slug}/")
    public FormCitizenDetailsResponseDTO retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                  @Nonnull @PathVariable String slug,
                                                  @Nullable @RequestParam(value = "version", required = false) Integer version,
                                                  @Nullable @RequestHeader(value = IdentityController.IDENTITY_HEADER_NAME, required = false) String identityId) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt);

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

    /**
     * Gets the maximum allowed file size for attachments for a specific form version.
     *
     * @param jwt     JWT token for authentication (optional).
     * @param slug    Unique identifier for the form.
     * @param version Optional version number of the form.
     * @return MaxFileSizeDto containing the maximum file size in megabytes.
     * @throws ResponseException if the form is not found or invalid.
     */
    @GetMapping("{slug}/max-file-size/")
    public MaxFileSizeDto getMaxFileSize(@Nullable @AuthenticationPrincipal Jwt jwt,
                                         @Nonnull @PathVariable String slug,
                                         @Nullable @RequestParam(value = "version", required = false) Integer version) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt);

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

    /**
     * Calculates the total costs for a form submission based on user input data.
     *
     * @param jwt          JWT token for authentication (optional).
     * @param slug         Unique identifier for the form.
     * @param version      Optional version number of the form.
     * @param customerData User-provided data for cost calculation.
     * @return FormCostCalculationResponseDTO containing cost details and payment items.
     * @throws PaymentException  if payment calculation fails.
     * @throws ResponseException if the form is not found or invalid.
     */
    @PostMapping("{slug}/costs/")
    public FormCostCalculationResponseDTO calculateCosts(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                         @Nonnull @PathVariable String slug,
                                                         @Nullable @RequestParam(value = "version", required = false) Integer version,
                                                         @Nonnull @RequestBody ElementData customerData) throws PaymentException, ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt);

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

    /**
     * Lists enabled identity providers for a specific form version.
     *
     * @param jwt     JWT token for authentication (optional).
     * @param slug    Unique identifier for the form.
     * @param version Optional version number of the form.
     * @return Page of IdentityDetailsDTO representing enabled identity providers.
     * @throws ResponseException if the form is not found or invalid.
     */
    @GetMapping("{slug}/identity-providers/")
    public Page<IdentityDetailsDTO> getIdentityProviders(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                         @Nonnull @PathVariable String slug,
                                                         @Nullable @RequestParam(value = "version", required = false) Integer version
    ) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt);

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

    /**
     * Derives computed element data for a form, including error and visibility handling.
     *
     * @param jwt                 JWT token for authentication (optional).
     * @param slug                Unique identifier for the form.
     * @param version             Optional version number of the form.
     * @param elementData         User-provided element data for derivation.
     * @param skipErrorsFor       List of element IDs to skip error calculation for.
     * @param skipVisibilitiesFor List of element IDs to skip visibility calculation for.
     * @param skipValuesFor       List of element IDs to skip value calculation for.
     * @param skipOverridesFor    List of element IDs to skip override calculation for.
     * @return Derived ElementData with computed values, errors, and visibilities.
     * @throws ResponseException if the form is not found or invalid.
     */
    @PostMapping("{slug}/derive")
    public ElementData derive(@Nullable @AuthenticationPrincipal Jwt jwt,
                              @Nonnull @PathVariable String slug,
                              @Nullable @RequestParam(value = "version", required = false) Integer version,
                              @Nonnull @Valid @RequestBody ElementData elementData,
                              @Nullable @RequestParam(value = "skipErrorsFor") List<String> skipErrorsFor,
                              @Nullable @RequestParam(value = "skipVisibilitiesFor") List<String> skipVisibilitiesFor,
                              @Nullable @RequestParam(value = "skipValuesFor") List<String> skipValuesFor,
                              @Nullable @RequestParam(value = "skipOverridesFor") List<String> skipOverridesFor) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt);

        var options = new ElementDerivationOptions()
                .setSkipValuesForElementIds(skipValuesFor)
                .setSkipOverridesForElementIds(skipOverridesFor)
                .setSkipErrorsForElementIds(skipErrorsFor)
                .setSkipVisibilitiesForElementIds(skipVisibilitiesFor);

        var request = new ElementDerivationRequest()
                .setElement(formVersion.getRootElement())
                .setElementData(elementData)
                .setOptions(options);

        var derivedElementData = elementDerivationService
                .derive(request);

        var inputIdValue = elementData
                .getOrDefault(IdentityValueKey.IdCustomerInputKey, new ElementDataObject(ElementType.SubmittedStep))
                .setComputedErrors(null); // Clear any previous computed errors

        derivedElementData.put(IdentityValueKey.IdCustomerInputKey, inputIdValue);

        if (options.notContainsSkipErrors(formVersion.getRootElement().getIntroductionStep().getId())) {
            if (formVersion.getIdentityVerificationRequired() && inputIdValue.isEmpty()) {
                inputIdValue.setComputedErrors(List.of("Bitte melden Sie sich mit einem der Nutzerkonten an."));
            }
        }

        return derivedElementData;
    }

    /**
     * Retrieves the form version entity for a given slug and version, considering authentication context.
     *
     * @param slug    Unique identifier for the form.
     * @param version Optional version number of the form.
     * @param jwt     JWT token for authentication (optional).
     * @return FormVersionWithDetailsEntity for the requested form and version.
     * @throws ResponseException if the form is not found or invalid.
     */
    private FormVersionWithDetailsEntity getFormVersionWithDetailsEntity(@Nonnull String slug,
                                                                         @Nullable Integer version,
                                                                         @Nullable @AuthenticationPrincipal Jwt jwt) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElse(null);

        FormVersionWithDetailsEntity formVersion;
        if (user == null) {
            formVersion = formVersionWithDetailsService
                    .retrieve(FormVersionWithDetailsFilter
                            .create()
                            .setSlug(slug)
                            .setStatus(FormStatus.Published))
                    .orElseThrow(ResponseException::notFound);
        } else {
            if (version != null) {
                formVersion = formVersionWithDetailsService
                        .findBySlugAndVersion(slug, version)
                        .orElseThrow(ResponseException::notFound);
            } else {
                formVersion = formVersionWithDetailsService
                        .retrieve(FormVersionWithDetailsFilter
                                .create()
                                .setSlug(slug)
                                .setStatus(FormStatus.Published))
                        .orElseThrow(ResponseException::notFound);
            }
        }
        return formVersion;
    }
}
