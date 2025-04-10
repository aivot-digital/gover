package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.services.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.exceptions.TemplateProcessingException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/forms/{formId}/print/")
public class FormPrintController {
    private final PdfService pdfService;
    private final FormService formService;

    @Autowired
    public FormPrintController(
            PdfService pdfService,
            FormService formService
    ) {
        this.pdfService = pdfService;
        this.formService = formService;
    }

    @GetMapping("")
    public ResponseEntity<Resource> print(
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        byte[] bytes;
        try {
            bytes = pdfService.generatePrintableForm(form);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw ResponseException.internalServerError("Fehler beim Erzeugen der PDF-Datei. Bitte versuchen Sie es später erneut.", e);
        } catch (TemplateProcessingException e) {
            throw ResponseException.internalServerError("Fehler beim Erzeugen der PDF-Datei. Bitte versuchen Sie es später erneut.", e);
        }

        Resource resource;
        try {
            resource = new ByteArrayResource(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(form.getSlug() + "-" + form.getVersion() + ".pdf")
                                .build()
                                .toString()
                )
                .body(resource);
    }
}
