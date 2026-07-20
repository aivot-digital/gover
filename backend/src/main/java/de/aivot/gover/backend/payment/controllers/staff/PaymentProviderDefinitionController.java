package de.aivot.gover.backend.payment.controllers.staff;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.payment.permissions.PaymentProviderPermissionProvider;
import de.aivot.gover.backend.payment.dtos.PaymentProviderDefinitionResponseDTO;
import de.aivot.gover.backend.payment.models.PaymentProviderDefinition;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.permissions.services.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/payment-provider-definitions/")
@Tag(
        name = "Payment Provider Definitions",
        description = "Endpoints for retrieving payment provider definitions"
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class PaymentProviderDefinitionController {
    private final List<PaymentProviderDefinition> paymentProviderDefinitions;
    private final PermissionService permissionService;

    @Autowired
    public PaymentProviderDefinitionController(List<PaymentProviderDefinition> paymentProviderDefinitions,
                                               PermissionService permissionService) {
        this.paymentProviderDefinitions = paymentProviderDefinitions;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Payment Provider Definitions",
            description = "Retrieve a list of all available payment provider definitions. " +
                    "This requires the permission „" + PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ + "“."
    )
    public List<PaymentProviderDefinitionResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ);

        return paymentProviderDefinitions
                .stream()
                .map(entity -> {
                    try {
                        return PaymentProviderDefinitionResponseDTO.from(entity);
                    } catch (ResponseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Payment Provider Definition",
            description = "Retrieve a specific payment provider definition by its unique key. " +
                    "This requires the permission „" + PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ + "“."
    )
    public PaymentProviderDefinitionResponseDTO retrieveLatest(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ);

        var definition = paymentProviderDefinitions
                .stream()
                .filter(def -> def.getKey().equals(key))
                .max(Comparator.comparing(PaymentProviderDefinition::getMajorVersion))
                .orElseThrow(ResponseException::notFound);

        return PaymentProviderDefinitionResponseDTO
                .from(definition);
    }

    @GetMapping("{key}/{version}/")
    @Operation(
            summary = "Retrieve Payment Provider Definition",
            description = "Retrieve a specific payment provider definition by its unique key and version. " +
                    "This requires the permission „" + PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ + "“."
    )
    public PaymentProviderDefinitionResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ);

        var definition = paymentProviderDefinitions
                .stream()
                .filter(def -> def.getKey().equals(key) && def.getMajorVersion().equals(version))
                .findFirst()
                .orElseThrow(ResponseException::notFound);

        return PaymentProviderDefinitionResponseDTO
                .from(definition);
    }
}
