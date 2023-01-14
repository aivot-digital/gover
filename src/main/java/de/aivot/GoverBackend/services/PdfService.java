package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.dtos.ApplicationDto;
import de.aivot.GoverBackend.models.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Component
public class PdfService {
    private final BlobService blobService;

    @Autowired
    public PdfService(BlobService blobService) {
        this.blobService = blobService;
    }


    public String generatePdf(Application application, ApplicationDto applicationDto) throws IOException, InterruptedException {
        String uuid = UUID.randomUUID().toString();
        Path pathHtml = blobService.getPrintHtmlPath(uuid);
        Path pathPdf = blobService.getPrintPdfPath(uuid);

        String template = loadTemplate(applicationDto);
        Files.writeString(pathHtml, template);

        String pdfTitle = (String) application.getRoot().getOrDefault("headline", application.getRoot().getOrDefault("title", application.getSlug()));
        pdfTitle = pdfTitle.replaceAll("\\r?\\n", " ");

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
                "--header-left",
                pdfTitle,
                "--footer-left",
                "[date] [time] Uhr",
                "--footer-right",
                "[page]/[toPage]",
                pathHtml.toString(),
                pathPdf.toString()
        ).start();
        generateToPdf.waitFor();

        Files.delete(pathHtml);

        return uuid;
    }

    private String loadTemplate(ApplicationDto applicationDto) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setTemplateMode(TemplateMode.HTML);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariable("data", applicationDto);

        return templateEngine.process("application.html", context);
    }
}
