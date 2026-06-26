package de.aivot.gover.backend.xdf.v2.controllers;

import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.xdf.v2.models.XdfStammdatenschema0102;
import de.aivot.gover.backend.xdf.v2.services.XdfTransformService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nonnull;

@RestController
@RequestMapping("/api/xdf/v2/")
public class XdfTransformController {
    private final XdfTransformService xdfTransformService;

    @Autowired
    public XdfTransformController(XdfTransformService xdfTransformService) {
        this.xdfTransformService = xdfTransformService;
    }

    @PostMapping(value = "transform/", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FormLayoutElement transform(
            @Nonnull @RequestBody @Valid XdfStammdatenschema0102 request
    ) throws ResponseException {
        return xdfTransformService
                .transformToGover(request);
    }
}
