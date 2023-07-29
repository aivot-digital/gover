package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.pdf.ApplicationPdfDto;
import de.aivot.GoverBackend.models.entities.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class PdfService {
    private final SubmissionStorageService submissionStorageService;

    @Autowired
    public PdfService(
            SubmissionStorageService submissionStorageService
    ) {
        this.submissionStorageService = submissionStorageService;
    }


    public void generatePdf(
            ApplicationPdfDto applicationDto,
            String uuid
    ) throws IOException, InterruptedException {
        submissionStorageService.initSubmission(uuid);

        Path pathHtml = submissionStorageService.getSubmissionHtmlPath(uuid);
        String template = loadContentTemplate(applicationDto);
        Files.writeString(pathHtml, template);

        Path pathHeaderHtml = submissionStorageService.getSubmissionHeaderHtmlPath(uuid);
        String headerTemplate = loadHeaderTemplate(applicationDto);
        Files.writeString(pathHeaderHtml, headerTemplate);

        Path pathFooterHtml = submissionStorageService.getSubmissionFooterHtmlPath(uuid);
        String footerTemplate = loadFooterTemplate(applicationDto);
        Files.writeString(pathFooterHtml, footerTemplate);

        Path pathPdf = submissionStorageService.getSubmissionPdfPath(uuid);
        Process generateToPdf = new ProcessBuilder(
                "wkhtmltopdf",
                "--encoding",
                "UTF-8",
                "--margin-top",
                "20mm",
                "--header-spacing",
                "5",
                "--margin-bottom",
                "20mm",
                "--footer-spacing",
                "5",
                "--header-html",
                pathHeaderHtml.toString(),
                "--footer-html",
                pathFooterHtml.toString(),
                pathHtml.toString(),
                pathPdf.toString()
        ).start();
        generateToPdf.waitFor();

        Files.delete(pathHtml);
        Files.delete(pathHeaderHtml);
        Files.delete(pathFooterHtml);
    }

    private String loadContentTemplate(ApplicationPdfDto applicationDto) {
        return loadTemplate("application.html", applicationDto);
    }

    private String loadHeaderTemplate(ApplicationPdfDto applicationDto) {
        return loadTemplate("application_header.html", applicationDto);
    }

    private String loadFooterTemplate(ApplicationPdfDto applicationDto) {
        return loadTemplate("application_footer.html", applicationDto);
    }

    private String loadTemplate(String templateName, ApplicationPdfDto applicationDto) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setTemplateMode(TemplateMode.HTML);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariable("data", applicationDto);

        return templateEngine.process(templateName, context);
    }
}
