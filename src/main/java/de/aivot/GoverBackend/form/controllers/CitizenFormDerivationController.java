package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.ElementDerivationOptions;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionWithDetailsRepository;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.FormVersionWithDetailsService;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import java.util.List;

@RestController
public class CitizenFormDerivationController {
    private final ElementDerivationService elementDerivationService;
    private final FormVersionWithDetailsRepository formVersionWithDetailsRepository;

    @Autowired
    public CitizenFormDerivationController(ElementDerivationService elementDerivationService,
                                           FormVersionWithDetailsRepository formVersionWithDetailsRepository) {
        this.elementDerivationService = elementDerivationService;
        this.formVersionWithDetailsRepository = formVersionWithDetailsRepository;
    }

    @PostMapping("/api/public/forms/{formId}/{formVersion}/derive")
    public ElementData derive(
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion,
            @Nonnull @Valid @RequestBody ElementData elementData,
            @Nonnull @RequestParam(defaultValue = ElementDerivationOptions.ALL_ELEMENTS, value = "skipErrorsFor") List<String> skipErrorsFor,
            @Nonnull @RequestParam(defaultValue = ElementDerivationOptions.ALL_ELEMENTS, value = "skipVisibilitiesFor") List<String> skipVisibilitiesFor,
            @Nonnull @RequestParam(defaultValue = ElementDerivationOptions.ALL_ELEMENTS, value = "skipValuesFor") List<String> skipValuesFor,
            @Nonnull @RequestParam(defaultValue = ElementDerivationOptions.ALL_ELEMENTS, value = "skipOverridesFor") List<String> skipOverridesFor
    ) throws ResponseException {
        var id = FormVersionWithDetailsEntityId
                .of(formId, formVersion);

        var form = formVersionWithDetailsRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        var options = new ElementDerivationOptions()
                .setSkipValuesForElementIds(skipValuesFor)
                .setSkipOverridesForElementIds(skipOverridesFor)
                .setSkipErrorsForElementIds(skipErrorsFor)
                .setSkipVisibilitiesForElementIds(skipVisibilitiesFor);

        var request = new ElementDerivationRequest()
                .setElement(form.getRootElement())
                .setElementData(elementData)
                .setOptions(options);

        var derivedElementData = elementDerivationService
                .derive(request);

        var inputIdValue = elementData
                .getOrDefault(IdentityValueKey.IdCustomerInputKey, new ElementDataObject(ElementType.SubmittedStep))
                .setComputedErrors(null); // Clear any previous computed errors

        derivedElementData.put(IdentityValueKey.IdCustomerInputKey, inputIdValue);

        if (options.notContainsSkipErrors(form.getRootElement().getIntroductionStep().getId())) {
            if (form.getIdentityVerificationRequired() && inputIdValue.isEmpty()) {
                inputIdValue.setComputedErrors(List.of("Bitte melden Sie sich mit einem der Nutzerkonten an."));
            }
        }

        return derivedElementData;
    }
}
