package de.aivot.gover.backend.elements.controllers;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.ElementDerivationRequest;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.services.ElementDerivationLogger;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.elements.utils.ElementStreamUtils;
import de.aivot.gover.backend.identity.constants.IdentityQueryParameterConstants;
import de.aivot.gover.backend.identity.controllers.IdentityController;
import de.aivot.gover.backend.identity.services.IdentityService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/elements/")
@Tag(
        name = OpenApiConstants.Tags.ElementsName,
        description = OpenApiConstants.Tags.ElementsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ElementDerivationController {
    private final ElementDerivationService elementDerivationServiceV2;
    private final IdentityService identityService;

    public ElementDerivationController(ElementDerivationService elementDerivationServiceV2, IdentityService identityService) {
        this.elementDerivationServiceV2 = elementDerivationServiceV2;
        this.identityService = identityService;
    }

    @PostMapping("derive/")
    @Operation(
            summary = "Derive Element",
            description = "Derives an element based on the provided data in the request."
    )
    public DerivedRuntimeElementData derive(
            @Nonnull @RequestBody @Valid ElementDerivationRequest request,
            @Nullable @CookieValue(value = IdentityController.IDENTITY_COOKIE_NAME, required = false) String identitySessionId,
            @Nullable @RequestParam(value = IdentityQueryParameterConstants.RELATED_PROCESS_NODE_ID, required = false) Integer relatedProcessNodeId
    ) throws ResponseException {
        ElementStreamUtils
                .applyAction(request.element(), BaseElement::recalculateReferencedIds);

        var identities = identityService
                .getIdentityDataMap(identitySessionId, relatedProcessNodeId);

        return elementDerivationServiceV2
                .derive(request, identities, new ElementDerivationLogger());
    }

    @PostMapping("recalculate-referenced-ids/")
    @Operation(
            summary = "Recalculate Referenced IDs",
            description = "Recalculates the referenced IDs of the provided element and all its children. This is necessary, when the element structure is changed"
    )
    public BaseElement recalculateReferencedIds(
            @Nonnull @RequestBody @Valid BaseElement element
    ) throws ResponseException {
        ElementStreamUtils
                .applyAction(element, BaseElement::recalculateReferencedIds);

        return element;
    }
}
