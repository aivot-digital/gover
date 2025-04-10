package de.aivot.GoverBackend.preset.controllers;

import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.form.services.FormDerivationService;
import de.aivot.GoverBackend.form.services.FormDerivationServiceFactory;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.preset.repositories.PresetRepository;
import de.aivot.GoverBackend.preset.repositories.PresetVersionRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// TODO: Move to dedicated preset module
@RestController
public class PresetDerivationController {
    private final PresetRepository presetRepository;
    private final PresetVersionRepository presetVersionRepository;
    private final FormDerivationServiceFactory formDerivationServiceFactory;

    @Autowired
    public PresetDerivationController(
            PresetRepository presetRepository,
            PresetVersionRepository presetVersionRepository,
            FormDerivationServiceFactory formDerivationServiceFactory
    ) {
        this.presetRepository = presetRepository;
        this.presetVersionRepository = presetVersionRepository;
        this.formDerivationServiceFactory = formDerivationServiceFactory;
    }

    /**
     * Derive the state of a preset based on the given customer input.
     *
     * @param presetKey     The id of the preset to derive.
     * @param presetVersion The version of the preset to derive.
     * @param customerInput The customer input to derive the preset state from.
     * @return The result of the derivation as the new state of the preset.
     */
    @PostMapping("/api/presets/{presetKey}/{presetVersion}/derive")
    public FormState derive(
            @PathVariable String presetKey,
            @PathVariable String presetVersion,
            @Valid @RequestBody Map<String, Object> customerInput,
            @RequestParam(value = "disableVisibilities") Optional<Boolean> disableVisibilities,
            @RequestParam(value = "disableValidation") Optional<Boolean> disableValidation
    ) throws ResponseException {
        var preset = presetRepository
                .findById(presetKey)
                .orElseThrow(ResponseException::notFound);

        var presetVersionObject = presetVersionRepository
                .getByPresetAndVersion(preset.getKey(), presetVersion)
                .orElseThrow(ResponseException::notFound);

        var dummyForm = new Form();

        var root = new RootElement(Map.of());
        dummyForm.setRoot(root);

        var step = new StepElement(Map.of());
        root.setChildren(List.of(step));

        step.setChildren(List.of(presetVersionObject.getRoot()));

        return formDerivationServiceFactory
                .create(
                        dummyForm,
                        List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER),
                        List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER),
                        List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER),
                        List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER)
                )
                .derive(root, customerInput)
                .getFormState();
    }
}
