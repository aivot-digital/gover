package de.aivot.gover.backend.codeLists.controllers;

import de.aivot.gover.backend.elements.models.elements.form.input.MultiCheckboxInputElementOption;
import de.aivot.gover.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.elements.services.CodeListElementOptionsService;
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
@RequestMapping("/api/public/code-lists/{codeListKey}/")
@Tag(
        name = OpenApiConstants.Tags.CodeListName,
        description = OpenApiConstants.Tags.CodeListDescription
)
public class PublicCodeListController {
    private final CodeListElementOptionsService codeListElementOptionsService;

    @Autowired
    public PublicCodeListController(CodeListElementOptionsService codeListElementOptionsService) {
        this.codeListElementOptionsService = codeListElementOptionsService;
    }

    @GetMapping("select/")
    public List<SelectInputElementOption> listAsSelect(
            @Nonnull @PathVariable String codeListKey
    ) throws ResponseException {
        return codeListElementOptionsService.listAsSelect(codeListKey);
    }

    @GetMapping("radio/")
    public List<RadioInputElementOption> listAsRadio(
            @Nonnull @PathVariable String codeListKey
    ) throws ResponseException {
        return codeListElementOptionsService.listAsRadio(codeListKey);
    }

    @GetMapping("multi-checkbox/")
    public List<MultiCheckboxInputElementOption> listAsMultiCheckbox(
            @Nonnull @PathVariable String codeListKey
    ) throws ResponseException {
        return codeListElementOptionsService.listAsMultiCheckbox(codeListKey);
    }
}
