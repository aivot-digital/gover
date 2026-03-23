package de.aivot.GoverBackend.preset.controllers;

import de.aivot.GoverBackend.elements.dtos.ElementDerivationResponse;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.preset.entities.PresetVersionEntityId;
import de.aivot.GoverBackend.preset.repositories.PresetRepository;
import de.aivot.GoverBackend.preset.repositories.PresetVersionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(
        name = "Preset Derivation",
        description = "Endpoints for deriving elements from presets"
)
public class PresetDerivationController {
    private final PresetRepository presetRepository;
    private final PresetVersionRepository presetVersionRepository;
    private final ElementDerivationService elementDerivationService;

    @Autowired
    public PresetDerivationController(
            PresetRepository presetRepository,
            PresetVersionRepository presetVersionRepository,
            ElementDerivationService elementDerivationService) {
        this.presetRepository = presetRepository;
        this.presetVersionRepository = presetVersionRepository;
        this.elementDerivationService = elementDerivationService;
    }

    @PostMapping("/api/presets/{presetKey}/{presetVersion}/derive")
    @Operation(
            summary = "Derive Element from Preset",
            description = "Derive an element based on the specified preset and version, applying the provided element data."
    )
    public ElementDerivationResponse derive(
            @PathVariable UUID presetKey,
            @PathVariable Integer presetVersion,
            @Valid @RequestBody ElementData elementData,
            @RequestParam(value = "disableVisibilities") Optional<Boolean> disableVisibilities,
            @RequestParam(value = "disableValidation") Optional<Boolean> disableValidation
    ) throws ResponseException {
        var preset = presetRepository
                .findById(presetKey)
                .orElseThrow(ResponseException::notFound);

        var id = new PresetVersionEntityId(presetKey, presetVersion);

        var presetVersionObject = presetVersionRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        var request = new ElementDerivationRequest()
                .setElement(presetVersionObject.getRootElement())
                .setElementData(elementData)
                .setOptions(
                        new ElementDerivationOptions()
                                .setSkipValuesForElementIds(List.of())
                                .setSkipVisibilitiesForElementIds(disableVisibilities.orElse(false) ? List.of(ElementDerivationOptions.ALL_ELEMENTS) : List.of())
                                .setSkipErrorsForElementIds(disableValidation.orElse(false) ? List.of(ElementDerivationOptions.ALL_ELEMENTS) : List.of())
                                .setSkipOverridesForElementIds(List.of())
                );

        var logger = new ElementDerivationLogger();
        var derivedElementData = elementDerivationService
                .derive(request, logger);

        return ElementDerivationResponse
                .from(derivedElementData, logger, true);
    }
}
