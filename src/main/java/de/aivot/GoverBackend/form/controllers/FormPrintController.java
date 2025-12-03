package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.services.VFormVersionWithDetailsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenAPIConfiguration;
import de.aivot.GoverBackend.services.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;


@RestController
@RequestMapping("/api/forms/{formId}/{formVersion}/print/")
@Tag(
        name = "Forms",
        description = "Forms are built for collecting data from users. " +
                      "They can be designed with various elements and configurations to suit different data collection needs. " +
                      "Forms can be published, managed, and analyzed within the system."
)
@SecurityRequirement(name = OpenAPIConfiguration.Name)
public class FormPrintController {
    private final PdfService pdfService;
    private final VFormVersionWithDetailsService formVersionWithDetailsService;

    /**
     * Constructs a new FormPrintController with required services.
     *
     * @param pdfService                    Service for generating PDFs from form data.
     * @param formVersionWithDetailsService Service for retrieving form version details.
     */
    @Autowired
    public FormPrintController(PdfService pdfService,
                               VFormVersionWithDetailsService formVersionWithDetailsService) {
        this.pdfService = pdfService;
        this.formVersionWithDetailsService = formVersionWithDetailsService;
    }

    /**
     * Generates and returns a printable PDF for the specified form and version.
     *
     * @param formId      The ID of the form to print.
     * @param formVersion The version of the form to print.
     * @return ResponseEntity containing the PDF as a resource.
     * @throws ResponseException If the form is not found or PDF generation fails.
     */
    @GetMapping("")
    @Operation(
            summary = "Print Form as PDF",
            description = "Generate and retrieve a printable PDF version of the specified form and version."
    )
    public ResponseEntity<Resource> print(
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion
    ) throws ResponseException {
        var form = formVersionWithDetailsService
                .retrieve(new VFormVersionWithDetailsEntityId(formId, formVersion))
                .orElseThrow(ResponseException::notFound);

        byte[] bytes;
        try {
            bytes = pdfService.generatePrintableForm(form);
        } catch (IOException | URISyntaxException | InterruptedException | TemplateProcessingException e) {
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
                                .filename(form.getSlug() + "-" + form.getVersion() + ".pdf", StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(resource);
    }
}
