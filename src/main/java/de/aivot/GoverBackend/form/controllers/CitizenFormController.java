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

    @GetMapping("")
    public Page<FormCitizenListResponseDTO> list(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                 @Nonnull @PageableDefault Pageable pageable,
                                                 @Nonnull @Valid FormVersionWithDetailsFilter filter) throws ResponseException {
        return formVersionWithDetailsService
                .findAllByIsCurrentlyPublishedVersionIsTrue(pageable, filter.build())
                .map(FormCitizenListResponseDTO::fromEntity);
    }

    @GetMapping("{slug}/")
    public FormCitizenDetailsResponseDTO retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                  @Nonnull @PathVariable String slug,
                                                  @Nullable @RequestParam(value = "version", required = false) Integer version,
                                                  @Nullable @RequestHeader(value = IdentityController.IDENTITY_HEADER_NAME, required = false) String identityId) throws ResponseException {
        var formVersion = getFormVersionWithDetailsEntity(slug, version, jwt);

        var identityCache = identityId == null ? Optional.empty() : identityCacheRepository
                .findById(identityId);

        var obfuscateSteps = (
                formVersion.getFormType() == FormType.Internal &&
                formVersion.getIdentityVerificationRequired() &&
                identityCache.isEmpty()
        );

        return FormCitizenDetailsResponseDTO
                .fromEntity(formVersion, obfuscateSteps);
    }


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

    @PostMapping("{slug}/derive")
    public ElementData derive(@Nullable @AuthenticationPrincipal Jwt jwt,
                              @Nonnull @PathVariable String slug,
                              @Nullable @RequestParam(value = "version", required = false) Integer version,
                              @Nonnull @Valid @RequestBody ElementData elementData,
                              @Nonnull @RequestParam(defaultValue = ElementDerivationOptions.ALL_ELEMENTS, value = "skipErrorsFor") List<String> skipErrorsFor,
                              @Nonnull @RequestParam(defaultValue = ElementDerivationOptions.ALL_ELEMENTS, value = "skipVisibilitiesFor") List<String> skipVisibilitiesFor,
                              @Nonnull @RequestParam(defaultValue = ElementDerivationOptions.ALL_ELEMENTS, value = "skipValuesFor") List<String> skipValuesFor,
                              @Nonnull @RequestParam(defaultValue = ElementDerivationOptions.ALL_ELEMENTS, value = "skipOverridesFor") List<String> skipOverridesFor) throws ResponseException {
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

    private FormVersionWithDetailsEntity getFormVersionWithDetailsEntity(@Nonnull String slug,
                                                                         @Nullable Integer version,
                                                                         @Nullable @AuthenticationPrincipal Jwt jwt) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElse(null);

        FormVersionWithDetailsEntity formVersion;
        if (user == null) {
            formVersion = formVersionWithDetailsService
                    .findBySlugAndIsCurrentlyPublishedVersionIsTrue(slug)
                    .orElseThrow(ResponseException::notFound);
        } else {
            if (version != null) {
                formVersion = formVersionWithDetailsService
                        .findBySlugAndVersion(slug, version)
                        .orElseThrow(ResponseException::notFound);
            } else {
                formVersion = formVersionWithDetailsService
                        .findBySlugAndIsCurrentlyPublishedVersionIsTrue(slug)
                        .orElseThrow(ResponseException::notFound);
            }
        }
        return formVersion;
    }
}
