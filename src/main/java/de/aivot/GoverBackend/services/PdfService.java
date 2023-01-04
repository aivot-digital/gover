package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.dtos.ApplicationDto;
import de.aivot.GoverBackend.models.Application;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Component
public class PdfService {
    @Autowired
    BlobService blobService;

    public String generatePdf(Application application, ApplicationDto applicationDto) throws IOException, InterruptedException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String template = loadTemplate(application, applicationDto);

        String uuid = UUID.randomUUID().toString();
        Path pathHtml = Paths.get("/tmp/" + uuid + ".html");
        Path pathPdf = Paths.get("/tmp/" + uuid + ".pdf");

        Files.writeString(pathHtml, template);

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
                (String) application.getRoot().get("title"),
                "--footer-left",
                "[date] [time] Uhr",
                "--footer-right",
                "[page]/[toPage]",
                pathHtml.toString(),
                pathPdf.toString()
        ).start();
        generateToPdf.waitFor();

        String blobUrl = blobService.storeData("prints", uuid + ".pdf", pathPdf.toString());

        Files.delete(pathHtml);
        Files.delete(pathPdf);

        return blobUrl;
    }

    private String loadTemplate(Application application, ApplicationDto applicationDto) {
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
