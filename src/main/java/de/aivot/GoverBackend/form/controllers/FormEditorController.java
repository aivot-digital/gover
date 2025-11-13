package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.FormEditorEntity;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/form-editors/")
public class FormEditorController {

    private final FormRepository formRepository;

    @Autowired
    public FormEditorController(FormRepository formRepository) {
        this.formRepository = formRepository;
    }

    @GetMapping("")
    public List<FormEditorEntity> listFormEditorsForForms(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestParam List<Integer> formIds
    ) {
        return formRepository
                .findAllByFormIdIn(formIds);
    }

    @GetMapping("{formId}/")
    public List<FormEditorEntity> listFormEditorsForVersions(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) {
        return formRepository
                .findAllByFormId(formId);
    }
}
