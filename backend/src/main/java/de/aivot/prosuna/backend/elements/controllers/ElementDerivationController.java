package de.aivot.prosuna.backend.elements.controllers;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.utils.ElementStreamUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Provides structural operations on element trees.
 * <p>
 * Element trees are deliberately not derived here. Deriving executes element functions on the server, so each
 * derivation must use a dedicated endpoint that loads or authorizes the element tree for its resource.
 */
@RestController
@RequestMapping("/api/elements/")
@Tag(
        name = OpenApiConstants.Tags.ElementsName,
        description = OpenApiConstants.Tags.ElementsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ElementDerivationController {
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
