package de.aivot.GoverBackend.payment.controllers.staff;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderDefinitionResponseDTO;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nullable;
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

    @Autowired
    public PaymentProviderDefinitionController(List<PaymentProviderDefinition> paymentProviderDefinitions) {
        this.paymentProviderDefinitions = paymentProviderDefinitions;
    }

    @GetMapping("")
    @Operation(
            summary = "List Payment Provider Definitions",
            description = "Retrieve a list of all available payment provider definitions."
    )
    public List<PaymentProviderDefinitionResponseDTO> list() throws ResponseException {
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
            description = "Retrieve a specific payment provider definition by its unique key."
    )
    public PaymentProviderDefinitionResponseDTO retrieve(
            @Nullable @PathVariable String key
    ) throws ResponseException {
        var definition = paymentProviderDefinitions
                .stream()
                .filter(def -> def.getKey().equals(key))
                .findFirst()
                .orElseThrow(ResponseException::notFound);

        return PaymentProviderDefinitionResponseDTO
                .from(definition);
    }
}
