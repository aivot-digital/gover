package de.aivot.GoverBackend.elements.controllers;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elements/")
@Tag(
        name = OpenApiConstants.Tags.ElementsName,
        description = OpenApiConstants.Tags.ElementsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ElementDerivationController {
    private final ElementDerivationService elementDerivationServiceV2;

    public ElementDerivationController(ElementDerivationService elementDerivationServiceV2) {
        this.elementDerivationServiceV2 = elementDerivationServiceV2;
    }

    @PostMapping("derive/")
    @Operation(
            summary = "Derive Element",
            description = "Derives an element based on the provided data in the request."
    )
    public DerivedRuntimeElementData derive(
            @Nonnull @RequestBody @Valid ElementDerivationRequest request
    ) throws ResponseException {
        var derivationLogger = new ElementDerivationLogger();

        return elementDerivationServiceV2
                .derive(request, derivationLogger);
    }
}
