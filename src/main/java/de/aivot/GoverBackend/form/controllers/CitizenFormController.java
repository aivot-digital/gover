package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.form.dtos.FormCitizenDetailsResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormCitizenListResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormCostCalculationResponseDTO;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.form.filters.FormFilter;
import de.aivot.GoverBackend.form.services.FormPaymentService;
import de.aivot.GoverBackend.form.services.FormService;
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
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public/forms/")
public class CitizenFormController {
    private final FormPaymentService paymentService;
    private final PaymentProviderService paymentProviderService;
    private final FormService formService;
    private final DestinationService destinationService;
    private final IdentityProviderService identityProviderService;
    private final IdentityCacheRepository identityCacheRepository;

    @Autowired
    public CitizenFormController(
            FormPaymentService paymentService,
            PaymentProviderService paymentProviderService,
            FormService formService,
            DestinationService destinationService,
            IdentityProviderService identityProviderService, IdentityCacheRepository identityCacheRepository) {
        this.paymentService = paymentService;
        this.paymentProviderService = paymentProviderService;
        this.formService = formService;
        this.destinationService = destinationService;
        this.identityProviderService = identityProviderService;
        this.identityCacheRepository = identityCacheRepository;
    }

    @GetMapping("")
    public Page<FormCitizenListResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid FormFilter filter
    ) throws ResponseException {
        filter.setStatus(FormStatus.Published);
        filter.setType(FormType.Public); // Only show public forms in the list view. Allow access to internal forms via direct link with retrieve.

        return formService
                .list(pageable, filter)
                .map(FormCitizenListResponseDTO::fromEntity);
    }

    @GetMapping("{slug}/{version}/")
    public FormCitizenDetailsResponseDTO retrieveSlugVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String slug,
            @Nonnull @PathVariable String version,
            @Nullable @RequestHeader(value = IdentityController.IDENTITY_HEADER_NAME, required = false) String identityId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElse(null);

        var filter = new FormFilter()
                .setSlug(slug)
                .setVersion(version)
                .setStatus(FormStatus.Published);

        if (user != null) {
            filter.setStatus(null);
        }

        var form = formService
                .retrieve(filter.build())
                .orElseThrow(ResponseException::notFound);

        var identityCache = identityId == null ? Optional.empty() : identityCacheRepository
                .findById(identityId);

        var obfuscateSteps = (
                form.getType() == FormType.Internal &&
                form.getIdentityRequired() &&
                identityCache.isEmpty()
        );

        return FormCitizenDetailsResponseDTO
                .fromEntity(form, obfuscateSteps);
    }

    @GetMapping("{slug}/")
    public FormCitizenDetailsResponseDTO retrievePublic(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @PathVariable String slug,
            @Nullable @RequestHeader(name = IdentityController.IDENTITY_HEADER_NAME, required = false) String identityId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElse(null);

        var version = formService
                .retrieveLatestPublishedVersionBySlug(slug)
                .orElseThrow(ResponseException::notFound);

        var filter = new FormFilter()
                .setSlug(slug)
                .setVersion(version)
                .setStatus(FormStatus.Published);

        if (user != null) {
            filter.setStatus(null);
        }

        var form = formService
                .retrieve(filter.build())
                .orElseThrow(ResponseException::notFound);

        var identityCache = identityId == null ? Optional.empty() : identityCacheRepository
                .findById(identityId);

        var obfuscateSteps = (
                form.getType() == FormType.Internal &&
                form.getIdentityRequired() &&
                identityCache.isEmpty()
        );

        return FormCitizenDetailsResponseDTO
                .fromEntity(form, obfuscateSteps);
    }


    @GetMapping("{applicationId}/max-file-size/")
    public MaxFileSizeDto getMaxFileSize(
            @PathVariable Integer applicationId
    ) throws ResponseException {
        var form = formService
                .retrieve(applicationId)
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

    @PostMapping("{applicationId}/costs/")
    public FormCostCalculationResponseDTO calculateCosts(
            @PathVariable Integer applicationId,
            @RequestBody Map<String, Object> customerData
    ) throws PaymentException, ResponseException {
        var form = formService
                .retrieve(applicationId)
                .orElseThrow(ResponseException::notFound);

        if (form.getPaymentProvider() == null) {
            return new FormCostCalculationResponseDTO(null, null, null);
        }

        var paymentProvider = paymentProviderService
                .retrieve(form.getPaymentProvider());

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

    @GetMapping("{formId}/identity-providers/")
    public Page<IdentityDetailsDTO> getIdentityProviders(
            @PathVariable Integer formId
    ) throws ResponseException {
        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        var identityProviderKeys = form
                .getIdentityProviders()
                .stream()
                .map(IdentityProviderLink::getIdentityProviderKey)
                .toList();

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
