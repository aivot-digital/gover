package de.aivot.gover.backend.preset.controllers;

import de.aivot.gover.backend.elements.dtos.ElementDerivationResponse;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.ElementDerivationOptions;
import de.aivot.gover.backend.elements.models.ElementDerivationRequest;
import de.aivot.gover.backend.elements.services.ElementDerivationLogger;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.preset.entities.PresetVersionEntityId;
import de.aivot.gover.backend.preset.permissions.PresetPermissionProvider;
import de.aivot.gover.backend.preset.repositories.PresetRepository;
import de.aivot.gover.backend.preset.repositories.PresetVersionRepository;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.permissions.services.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(
        name = "Preset Derivation",
        description = "Endpoints for deriving elements from presets"
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class PresetDerivationController {
    private final PresetRepository presetRepository;
    private final PresetVersionRepository presetVersionRepository;
    private final ElementDerivationService elementDerivationService;
    private final PermissionService permissionService;

    @Autowired
    public PresetDerivationController(
            PresetRepository presetRepository,
            PresetVersionRepository presetVersionRepository,
            ElementDerivationService elementDerivationService,
            PermissionService permissionService) {
        this.presetRepository = presetRepository;
        this.presetVersionRepository = presetVersionRepository;
        this.elementDerivationService = elementDerivationService;
        this.permissionService = permissionService;
    }

    @PostMapping("/api/presets/{presetKey}/{presetVersion}/derive")
    @Operation(
            summary = "Derive Element from Preset",
            description = "Derive an element based on the specified preset and version, applying the provided element data. " +
                    "This requires the permission „" + PresetPermissionProvider.PRESET_READ + "“."
    )
    public ElementDerivationResponse derive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID presetKey,
            @PathVariable Integer presetVersion,
            @Valid @RequestBody AuthoredElementValues elementData,
            @RequestParam(value = "disableVisibilities") Optional<Boolean> disableVisibilities,
            @RequestParam(value = "disableValidation") Optional<Boolean> disableValidation
    ) throws ResponseException {
        permissionService
                .hasSystemPermission(jwt, PresetPermissionProvider.PRESET_READ);

        var preset = presetRepository
                .findById(presetKey)
                .orElseThrow(ResponseException::notFound);

        var id = new PresetVersionEntityId(presetKey, presetVersion);

        var presetVersionObject = presetVersionRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        var request = new ElementDerivationRequest(
                presetVersionObject.getRootElement(),
                elementData,
                new ElementDerivationOptions()
                        .setSkipValuesForElementIds(List.of())
                        .setSkipVisibilitiesForElementIds(disableVisibilities.orElse(false) ? List.of(ElementDerivationOptions.ALL_ELEMENTS) : List.of())
                        .setSkipErrorsForElementIds(disableValidation.orElse(false) ? List.of(ElementDerivationOptions.ALL_ELEMENTS) : List.of())
                        .setSkipOverridesForElementIds(List.of())
        );

        var logger = new ElementDerivationLogger();
        var derivedElementData = elementDerivationService
                .derive(request, new IdentityDataMap(), logger);

        return ElementDerivationResponse
                .from(derivedElementData, logger, true);
    }
}
