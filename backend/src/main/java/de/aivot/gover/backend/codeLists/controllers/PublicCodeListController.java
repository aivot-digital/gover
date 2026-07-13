package de.aivot.gover.backend.codeLists.controllers;

import de.aivot.gover.backend.elements.models.elements.form.input.MultiCheckboxInputElementOption;
import de.aivot.gover.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.codeLists.services.CodeListService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/code-lists/{codeListId}/")
@Tag(
        name = OpenApiConstants.Tags.CodeListName,
        description = OpenApiConstants.Tags.CodeListDescription
)
public class PublicCodeListController {
    private final CodeListService codeListService;

    @Autowired
    public PublicCodeListController(CodeListService codeListService) {
        this.codeListService = codeListService;
    }

    @GetMapping("select/")
    public List<SelectInputElementOption> listAsSelect(
            @Nonnull @PathVariable Integer codeListId
    ) throws ResponseException {
        return codeListService.listAsSelect(codeListId);
    }

    @GetMapping("radio/")
    public List<RadioInputElementOption> listAsRadio(
            @Nonnull @PathVariable Integer codeListId
    ) throws ResponseException {
        return codeListService.listAsRadio(codeListId);
    }

    @GetMapping("multi-checkbox/")
    public List<MultiCheckboxInputElementOption> listAsMultiCheckbox(
            @Nonnull @PathVariable Integer codeListId
    ) throws ResponseException {
        return codeListService.listAsMultiCheckbox(codeListId);
    }
}
