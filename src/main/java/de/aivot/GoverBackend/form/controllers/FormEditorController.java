package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.projections.FormEditorProjection;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.openApi.OpenAPIConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/form-editors/")
@Tag(
        name = "Forms",
        description = "Forms are built for collecting data from users. " +
                      "They can be designed with various elements and configurations to suit different data collection needs. " +
                      "Forms can be published, managed, and analyzed within the system."
)
@SecurityRequirement(name = OpenAPIConfiguration.Name)
public class FormEditorController {

    private final FormRepository formRepository;

    @Autowired
    public FormEditorController(FormRepository formRepository) {
        this.formRepository = formRepository;
    }

    @GetMapping("")
    @Operation(
            summary = "List Form Editors for Forms",
            description = "Retrieve a list of form editors associated with the specified form IDs."
    )
    public List<FormEditorProjection> listFormEditorsForForms(
            @Nonnull @RequestParam List<Integer> formIds
    ) {
        return formRepository
                .findAllByFormIdIn(formIds);
    }

    @GetMapping("{formId}/")
    @Operation(
            summary = "List Form Editors for a Form's Versions",
            description = "Retrieve a list of form editors associated with all versions of the specified form ID."
    )
    public List<FormEditorProjection> listFormEditorsForVersions(
            @Nonnull @PathVariable Integer formId
    ) {
        return formRepository
                .findAllByFormId(formId);
    }
}
