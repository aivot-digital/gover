package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.form.dtos.FormCitizenDetailsResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormCitizenListResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormCostCalculationResponseDTO;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.form.filters.FormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.repositories.FormVersionWithDetailsRepository;
import de.aivot.GoverBackend.form.services.FormPaymentService;
import de.aivot.GoverBackend.form.services.FormVersionWithDetailsService;
import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
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
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final FormVersionWithDetailsRepository formVersionWithDetailsRepository;

    @Autowired
    public CitizenFormController(FormPaymentService paymentService,
                                 PaymentProviderService paymentProviderService,
                                 DestinationService destinationService,
                                 IdentityProviderService identityProviderService,
                                 IdentityCacheRepository identityCacheRepository,
                                 FormVersionWithDetailsService formVersionWithDetailsService,
                                 FormVersionWithDetailsRepository formVersionWithDetailsRepository) {
        this.paymentService = paymentService;
        this.paymentProviderService = paymentProviderService;
        this.destinationService = destinationService;
        this.identityProviderService = identityProviderService;
        this.identityCacheRepository = identityCacheRepository;
        this.formVersionWithDetailsService = formVersionWithDetailsService;
        this.formVersionWithDetailsRepository = formVersionWithDetailsRepository;
    }

    @GetMapping("")
    public Page<FormCitizenListResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid FormVersionWithDetailsFilter filter
    ) throws ResponseException {
        return formVersionWithDetailsRepository
                .findAllByPublishedIsNotNullAndRevokedIsNull(pageable, filter.build())
                .map(FormCitizenListResponseDTO::fromEntity);
    }

    @GetMapping("{slug}/")
    public FormCitizenDetailsResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String slug,
            @Nullable @RequestHeader(value = IdentityController.IDENTITY_HEADER_NAME, required = false) String identityId,
            @Nullable @RequestParam(value = "version", required = false) Integer version
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElse(null);

        FormVersionWithDetailsEntity formVersion;
        if (user == null) {
            formVersion = formVersionWithDetailsService
                    .findLatestForSlugAndStatus(slug, FormStatus.Published)
                    .orElseThrow(ResponseException::notFound);
        } else {
            if (version != null) {
                formVersion = formVersionWithDetailsService
                        .findVersionForSlugAndVersion(slug, version)
                        .orElseThrow(ResponseException::notFound);
            } else {
                formVersion = formVersionWithDetailsService
                        .findLatestForSlug(slug)
                        .orElseThrow(ResponseException::notFound);
            }
        }

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

    @GetMapping("{formId}/{formVersion}/max-file-size/")
    public MaxFileSizeDto getMaxFileSize(
            @PathVariable Integer formId,
            @PathVariable Integer formVersion
    ) throws ResponseException {
        var id = FormVersionWithDetailsEntityId
                .of(formId, formVersion);

        var form = formVersionWithDetailsRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        MaxFileSizeDto maxFileSizeDto = new MaxFileSizeDto();
        maxFileSizeDto.setMaxFileSize(100);

        if (form.getDestinationId() == null) {
            return maxFileSizeDto;
        }

        var destination = destinationService
                .retrieve(form.getDestinationId());

        if (destination.isEmpty()) {
            return maxFileSizeDto;
        }

        if (destination.get().getMaxAttachmentMegaBytes() != null) {
            maxFileSizeDto.setMaxFileSize(destination.get().getMaxAttachmentMegaBytes());
        }

        return maxFileSizeDto;
    }

    @PostMapping("{formId}/{formVersion}/costs/")
    public FormCostCalculationResponseDTO calculateCosts(
            @PathVariable Integer formId,
            @PathVariable Integer formVersion,
            @RequestBody ElementData customerData
    ) throws PaymentException, ResponseException {
        var id = FormVersionWithDetailsEntityId
                .of(formId, formVersion);

        var form = formVersionWithDetailsRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        if (form.getPaymentProviderKey() == null) {
            return new FormCostCalculationResponseDTO(null, null, null);
        }

        var paymentProvider = paymentProviderService
                .retrieve(form.getPaymentProviderKey());

        if (paymentProvider.isEmpty()) {
            return new FormCostCalculationResponseDTO(null, null, null);
        }

        var paymentProviderDefinition = paymentProviderService
                .getProviderDefinition(paymentProvider.get().getProviderKey());

        if (paymentProviderDefinition.isEmpty()) {
            return new FormCostCalculationResponseDTO(null, null, null);
        }

        var paymentItems = paymentService
                .createPaymentItems(form, customerData);

        var costs = paymentItems
                .stream()
                .map(PaymentItem::getTotalPrice)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);

        return new FormCostCalculationResponseDTO(costs, paymentItems, paymentProviderDefinition.get().getProviderName());
    }

    @GetMapping("{formId}/{formVersion}/identity-providers/")
    public Page<IdentityDetailsDTO> getIdentityProviders(
            @PathVariable Integer formId,
            @PathVariable Integer formVersion
    ) throws ResponseException {
        var id = FormVersionWithDetailsEntityId
                .of(formId, formVersion);

        var form = formVersionWithDetailsRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        var identityProviderKeys = form
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
}
