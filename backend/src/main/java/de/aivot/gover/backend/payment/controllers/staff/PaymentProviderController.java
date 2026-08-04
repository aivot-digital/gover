package de.aivot.gover.backend.payment.controllers.staff;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.payment.dtos.PaymentProviderRequestDTO;
import de.aivot.gover.backend.payment.dtos.PaymentProviderResponseDTO;
import de.aivot.gover.backend.payment.dtos.PaymentProviderTestDataRequestDTO;
import de.aivot.gover.backend.payment.dtos.PaymentProviderTestDataResponseDTO;
import de.aivot.gover.backend.payment.entities.PaymentProviderEntity;
import de.aivot.gover.backend.payment.filters.PaymentProviderFilter;
import de.aivot.gover.backend.payment.permissions.PaymentProviderPermissionProvider;
import de.aivot.gover.backend.payment.services.PaymentProviderService;
import de.aivot.gover.backend.payment.services.PaymentProviderTestService;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public PaymentProviderController(AuditService auditService,
                                     PaymentProviderService paymentProviderService,
                                     PaymentProviderTestService paymentProviderTestService,
                                     UserService userService,
                                     PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(PaymentProviderController.class, "Zahlungen");
        this.paymentProviderService = paymentProviderService;
        this.paymentProviderTestService = paymentProviderTestService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Payment Providers",
            description = "Retrieve a paginated list of payment providers with optional filtering. " +
                    "Requires the system-level permission `" + PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ + "`."
    )
    public Page<PaymentProviderResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid PaymentProviderFilter filter
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ);

        return paymentProviderService
                .list(pageable, filter)
                .map(PaymentProviderResponseDTO::fromEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Payment Provider",
            description = "Create a new payment provider. " +
                    "Requires the system-level permission `" + PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE + "`."
    )
    public PaymentProviderResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid PaymentProviderRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(execUser.getId(), PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE);

        var created = paymentProviderService
                .create(requestDTO.toEntity());

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Create, PaymentProviderEntity.class, created.getKey(), "key", Map.of(
                "key", created.getKey(),
                "name", created.getName()
        )).withMessage(
                "Der Zahlungsanbieter %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(created.getName()),
                StringUtils.quote(String.valueOf(created.getKey())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return PaymentProviderResponseDTO
                .fromEntity(created);
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Payment Provider",
            description = "Retrieve details of a specific payment provider by its key. " +
                    "Requires the system-level permission `" + PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ + "`."
    )
    public PaymentProviderResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ);

        return paymentProviderService
                .retrieve(key)
                .map(PaymentProviderResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    @Operation(
            summary = "Update Payment Provider",
            description = "Update an existing payment provider. " +
                    "Requires the system-level permission `" + PaymentProviderPermissionProvider.PAYMENT_PROVIDER_UPDATE + "`."
    )
    public PaymentProviderResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @RequestBody @Valid PaymentProviderRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(execUser.getId(), PaymentProviderPermissionProvider.PAYMENT_PROVIDER_UPDATE);

        var existing = paymentProviderService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        if (existing.getIsEnabled() && !requestDTO.isEnabled()) {
            // TODO: Check if this payment provider is still used in any process node configuration and prevent the disable if so.
        }

        var result = paymentProviderService
                .update(key, requestDTO.toEntity());

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Update, PaymentProviderEntity.class, result.getKey(), "key", Map.of(
                "key", result.getKey(),
                "name", result.getName()
        )).withMessage(
                "Der Zahlungsanbieter %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(result.getName()),
                StringUtils.quote(String.valueOf(result.getKey())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return PaymentProviderResponseDTO
                .fromEntity(result);
    }

    @DeleteMapping("{key}/")
    @Operation(
            summary = "Delete Payment Provider",
            description = "Delete an existing payment provider. " +
                    "Requires the system-level permission `" + PaymentProviderPermissionProvider.PAYMENT_PROVIDER_DELETE + "`."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(execUser.getId(), PaymentProviderPermissionProvider.PAYMENT_PROVIDER_DELETE);

        var deleted = paymentProviderService
                .delete(key);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Delete, PaymentProviderEntity.class, deleted.getKey(), "key", Map.of(
                "key", deleted.getKey(),
                "name", deleted.getName()
        )).withMessage(
                "Der Zahlungsanbieter %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(deleted.getName()),
                StringUtils.quote(String.valueOf(deleted.getKey())),
                StringUtils.quote(execUser.getFullName())
        ).log();
    }

    @PostMapping("{key}/test/")
    @Operation(
            summary = "Test Payment Provider",
            description = "Test the configuration of a payment provider by performing a test transaction. " +
                    "Requires the system-level permission `" + PaymentProviderPermissionProvider.PAYMENT_PROVIDER_UPDATE + "`."
    )
    public PaymentProviderTestDataResponseDTO test(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @RequestBody @Valid PaymentProviderTestDataRequestDTO requestDTO
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_UPDATE);

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
