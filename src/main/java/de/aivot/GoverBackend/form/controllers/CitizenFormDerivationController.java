package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.services.FormDerivationServiceFactory;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class CitizenFormDerivationController {
    private final FormRepository formRepository;
    private final FormDerivationServiceFactory formDerivationServiceFactory;

    @Autowired
    public CitizenFormDerivationController(
            FormRepository formRepository,
            FormDerivationServiceFactory formDerivationServiceFactory
    ) {
        this.formRepository = formRepository;
        this.formDerivationServiceFactory = formDerivationServiceFactory;
    }

    /**
     * Derive the state of a form based on the given customer input.
     *
     * @param formId            The id of the form to derive.
     * @param customerInput     The customer input to derive the form state from.
     * @param disableValidation Disable the validation during this derivation
     * @param limitToStepId     Limit the override and validation to the step with the given id
     * @return The result of the derivation as the new state of the form.
     */
    @PostMapping("/api/public/forms/{formId}/derive")
    public FormState derive(
            @Nonnull @PathVariable Integer formId,
            @Nonnull @Valid @RequestBody Map<String, Object> customerInput,
            @Nonnull @RequestParam(defaultValue = "ALL", value = "stepsToValidate") List<String> stepsToValidate,
            @Nonnull @RequestParam(defaultValue = "ALL", value = "stepsToCalculateVisibilities") List<String> stepsToCalculateVisibilities,
            @Nonnull @RequestParam(defaultValue = "ALL", value = "stepsToCalculateValues") List<String> stepsToCalculateValues,
            @Nonnull @RequestParam(defaultValue = "ALL", value = "stepsToCalculateOverrides") List<String> stepsToCalculateOverrides
    ) throws ResponseException {
        var form = formRepository
                .findById(formId)
                .orElseThrow(ResponseException::notFound);

        return formDerivationServiceFactory
                .create(
                        form,
                        stepsToValidate,
                        stepsToCalculateVisibilities,
                        stepsToCalculateValues,
                        stepsToCalculateOverrides
                )
                .derive(form.getRoot(), customerInput)
                .getFormState();
    }
}
