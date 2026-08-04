package de.aivot.gover.backend.payment.controllers.staff;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.payment.permissions.PaymentProviderPermissionProvider;
import de.aivot.gover.backend.payment.dtos.PaymentProviderDefinitionResponseDTO;
import de.aivot.gover.backend.payment.models.PaymentProviderDefinition;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.utils.StringUtils;
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
                    "Requires at least one of the system-level permissions `" +
                    PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ + "` or `" +
                    PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE + "`."
    )
    public List<PaymentProviderDefinitionResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        requireDefinitionAccess(jwt);

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
                    "Requires at least one of the system-level permissions `" +
                    PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ + "` or `" +
                    PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE + "`."
    )
    public PaymentProviderDefinitionResponseDTO retrieveLatest(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        requireDefinitionAccess(jwt);

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
                    "Requires at least one of the system-level permissions `" +
                    PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ + "` or `" +
                    PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE + "`."
    )
    public PaymentProviderDefinitionResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        requireDefinitionAccess(jwt);

        var definition = paymentProviderDefinitions
                .stream()
                .filter(def -> def.getKey().equals(key) && def.getMajorVersion().equals(version))
                .findFirst()
                .orElseThrow(ResponseException::notFound);

        return PaymentProviderDefinitionResponseDTO
                .from(definition);
    }

    private void requireDefinitionAccess(@Nullable Jwt jwt) throws ResponseException {
        // Definition metadata is needed both for reading existing providers and for configuring a new one.
        // Do not require payment_provider.read here, otherwise create-only users cannot open the create form.
        if (
                !permissionService.hasSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ) &&
                        !permissionService.hasSystemPermission(jwt, PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE)
        ) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s oder %s auf Systemebene.",
                    StringUtils.quote(PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ),
                    StringUtils.quote(PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE)
            );
        }
    }
}
