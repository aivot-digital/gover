package de.aivot.GoverBackend.payment.controllers.staff;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.form.repositories.VFormVersionWithDetailsRepository;
import de.aivot.GoverBackend.form.services.FormRevisionService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderRequestDTO;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderResponseDTO;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderTestDataRequestDTO;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderTestDataResponseDTO;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.filters.PaymentProviderFilter;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.payment.services.PaymentProviderTestService;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-providers/")
@Tag(
        name = "Payment Providers",
        description = "Payment providers are used to handle payments within forms or processes."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class PaymentProviderController {
    private final ScopedAuditService auditService;

    private final PaymentProviderService paymentProviderService;
    private final PaymentProviderTestService paymentProviderTestService;
    private final FormRevisionService formRevisionService;
    private final FormVersionRepository formVersionRepository;
    private final VFormVersionWithDetailsRepository vFormVersionWithDetailsRepository;
    private final UserService userService;

    @Autowired
    public PaymentProviderController(AuditService auditService,
                                     PaymentProviderService paymentProviderService,
                                     PaymentProviderTestService paymentProviderTestService,
                                     FormRevisionService formRevisionService,
                                     FormVersionRepository formVersionRepository,
                                     VFormVersionWithDetailsRepository vFormVersionWithDetailsRepository,
                                     UserService userService) {
        this.auditService = auditService.createScopedAuditService(PaymentProviderController.class);
        this.paymentProviderService = paymentProviderService;
        this.paymentProviderTestService = paymentProviderTestService;
        this.formRevisionService = formRevisionService;
        this.formVersionRepository = formVersionRepository;
        this.vFormVersionWithDetailsRepository = vFormVersionWithDetailsRepository;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Payment Providers",
            description = "Retrieve a paginated list of payment providers with optional filtering."
    )
    public Page<PaymentProviderResponseDTO> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid PaymentProviderFilter filter
    ) throws ResponseException {
        return paymentProviderService
                .list(pageable, filter)
                .map(PaymentProviderResponseDTO::fromEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Payment Provider",
            description = "Create a new payment provider. Requires super admin permissions."
    )
    public PaymentProviderResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid PaymentProviderRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var created = paymentProviderService
                .create(requestDTO.toEntity());

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(execUser, AuditAction.Create, PaymentProviderEntity.class, Map.of(
                        "key", created.getKey(),
                        "name", created.getName()
                )));

        return PaymentProviderResponseDTO
                .fromEntity(created);
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Payment Provider",
            description = "Retrieve details of a specific payment provider by its key."
    )
    public PaymentProviderResponseDTO retrieve(
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        return paymentProviderService
                .retrieve(key)
                .map(PaymentProviderResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    @Operation(
            summary = "Update Payment Provider",
            description = "Update an existing payment provider. Requires super admin permissions."
    )
    public PaymentProviderResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @RequestBody @Valid PaymentProviderRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var existing = paymentProviderService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        if (existing.getIsEnabled() && !requestDTO.isEnabled()) {
            var filterAllPublishedForms = VFormVersionWithDetailsFilter
                    .create()
                    .setPaymentProviderKey(key)
                    .setStatus(FormStatus.Published)
                    .build();

            if (vFormVersionWithDetailsRepository.exists(filterAllPublishedForms)) {
                throw ResponseException.conflict(
                        "Der Zahlungsanbieter kann nicht deaktiviert werden, da er noch in einem oder mehreren Formularen verwendet wird."
                );
            } else {
                var filterAllRelatedForms = VFormVersionWithDetailsFilter
                        .create()
                        .setPaymentProviderKey(key)
                        .build();

                for (var form : vFormVersionWithDetailsRepository.findAll(filterAllRelatedForms)) {
                    var formClone = form.clone();

                    form.setPaymentProviderKey(null);
                    formVersionRepository.save(form.toFormVersionEntity());

                    formRevisionService
                            .create(execUser, form, formClone);
                }
            }
        }

        var result = paymentProviderService
                .update(key, requestDTO.toEntity());

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(execUser, AuditAction.Update, PaymentProviderEntity.class, Map.of(
                        "key", result.getKey(),
                        "name", result.getName()
                )));

        return PaymentProviderResponseDTO
                .fromEntity(result);
    }

    @DeleteMapping("{key}/")
    @Operation(
            summary = "Delete Payment Provider",
            description = "Delete an existing payment provider. Requires super admin permissions."
    )
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID key
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var deleted = paymentProviderService
                .delete(key);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(execUser, AuditAction.Delete, PaymentProviderEntity.class, Map.of(
                        "key", deleted.getKey(),
                        "name", deleted.getName()
                )));
    }

    @PostMapping("{key}/test/")
    @Operation(
            summary = "Test Payment Provider",
            description = "Test the configuration of a payment provider by performing a test transaction."
    )
    public PaymentProviderTestDataResponseDTO test(
            @PathVariable UUID key,
            @RequestBody @Valid PaymentProviderTestDataRequestDTO requestDTO
    ) throws ResponseException {
        var result = paymentProviderTestService.test(
                key,
                requestDTO.purpose(),
                requestDTO.description(),
                requestDTO.amount()
        );

        return new PaymentProviderTestDataResponseDTO(
                result.ok(),
                result.request(),
                result.transaction(),
                result.errorMessage()
        );
    }
}
