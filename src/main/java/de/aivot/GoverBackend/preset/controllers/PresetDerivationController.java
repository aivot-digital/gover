package de.aivot.GoverBackend.preset.controllers;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.preset.repositories.PresetRepository;
import de.aivot.GoverBackend.preset.repositories.PresetVersionRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// TODO: Move to dedicated preset module
@RestController
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
    public ElementData derive(
            @PathVariable String presetKey,
            @PathVariable String presetVersion,
            @Valid @RequestBody ElementData elementData,
            @RequestParam(value = "disableVisibilities") Optional<Boolean> disableVisibilities,
            @RequestParam(value = "disableValidation") Optional<Boolean> disableValidation
    ) throws ResponseException {
        var preset = presetRepository
                .findById(presetKey)
                .orElseThrow(ResponseException::notFound);

        var presetVersionObject = presetVersionRepository
                .getByPresetAndVersion(preset.getKey(), presetVersion)
                .orElseThrow(ResponseException::notFound);

        var request = new ElementDerivationRequest()
                .setElement(presetVersionObject.getRoot())
                .setElementData(elementData)
                .setOptions(
                        new ElementDerivationOptions()
                                .setSkipValuesForElementIds(List.of())
                                .setSkipVisibilitiesForElementIds(disableVisibilities.orElse(false) ? List.of(ElementDerivationOptions.ALL_ELEMENTS) : List.of())
                                .setSkipErrorsForElementIds(disableValidation.orElse(false) ? List.of(ElementDerivationOptions.ALL_ELEMENTS) : List.of())
                                .setSkipOverridesForElementIds(List.of())
                );

        return elementDerivationService
                .derive(request);
    }
}
