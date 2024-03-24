package de.aivot.GoverBackend.services;

import com.oracle.truffle.regex.tregex.util.json.JsonObject;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.PuppetPdfConfig;
import de.aivot.GoverBackend.models.entities.Asset;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.SystemConfig;
import de.aivot.GoverBackend.pdf.ApplicationPdfDto;
import de.aivot.GoverBackend.repositories.AssetRepository;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import org.graalvm.shadowed.org.jcodings.util.Hash;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class PdfService {
    private final SubmissionStorageService submissionStorageService;
    private final PuppetPdfConfig puppetPdfConfig;
    private final SystemConfigRepository systemConfigRepository;
    private final DepartmentRepository departmentRepository;
    private final AssetRepository assetRepository;
    private final GoverConfig goverConfig;

    @Autowired
    public PdfService(
            SubmissionStorageService submissionStorageService,
            PuppetPdfConfig puppetPdfConfig,
            SystemConfigRepository systemConfigRepository,
            DepartmentRepository departmentRepository,
            AssetRepository assetRepository,
            GoverConfig goverConfig
    ) {
        this.submissionStorageService = submissionStorageService;
        this.puppetPdfConfig = puppetPdfConfig;
        this.systemConfigRepository = systemConfigRepository;
        this.departmentRepository = departmentRepository;
        this.assetRepository = assetRepository;
        this.goverConfig = goverConfig;
    }

    public void generatePdf(
            Form form,
            ApplicationPdfDto applicationDto,
            String uuid
    ) throws IOException, InterruptedException, URISyntaxException {
        var dto = new HashMap<String, Object>();
        dto.put("base", createBaseContext());
        dto.put("data", applicationDto);
        dto.put("form", form);

        Integer departmentId;
        if (form.getManagingDepartmentId() != null) {
            departmentId = form.getManagingDepartmentId();
        } else if (form.getResponsibleDepartmentId() != null) {
            departmentId = form.getResponsibleDepartmentId();
        } else {
            departmentId = form.getDevelopingDepartmentId();
        }

        departmentRepository
                .findById(departmentId)
                .ifPresent(department -> dto.put("department", department));

        if (puppetPdfConfig.isEnabled()) {
            generatePuppetPdf(form, dto, uuid);
        } else {
            generateWkHtmlToPdf(form, dto, uuid);
        }
    }

    private void generatePuppetPdf(Form form, Map<String, Object> dto, String uuid) throws IOException, InterruptedException, URISyntaxException {
        submissionStorageService.initSubmission(uuid);
        var pdfPath = submissionStorageService.getSubmissionPdfPath(uuid);

        String template = loadContentTemplate(form, dto);

        var uri = new URI("http://" + puppetPdfConfig.getHost() + ":" + puppetPdfConfig.getPort() + "/print");

        JSONObject json = new JSONObject();
        json.put("html", template);

        var client = HttpClient.newHttpClient();
        var request = HttpRequest
                .newBuilder(uri)
                .headers("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to generate PDF with status code: " + response.statusCode());
        }

        Files.write(pdfPath, response.body().readAllBytes());
    }

    private void generateWkHtmlToPdf(Form form, Map<String, Object> dto, String uuid) throws IOException, InterruptedException {
        submissionStorageService.initSubmission(uuid);

        Path pathHtml = submissionStorageService.getSubmissionHtmlPath(uuid);
        String template = loadContentTemplate(form, dto);
        Files.writeString(pathHtml, template);

        Path pathHeaderHtml = submissionStorageService.getSubmissionHeaderHtmlPath(uuid);
        String headerTemplate = loadHeaderTemplate(dto);
        Files.writeString(pathHeaderHtml, headerTemplate);

        Path pathFooterHtml = submissionStorageService.getSubmissionFooterHtmlPath(uuid);
        String footerTemplate = loadFooterTemplate(dto);
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
        generateToPdf.waitFor(30, TimeUnit.SECONDS);

        Files.delete(pathHtml);
        Files.delete(pathHeaderHtml);
        Files.delete(pathFooterHtml);
    }

    private String loadContentTemplate(Form form, Map<String, Object> dto) {
        var template = form.getPdfBodyTemplateKey();
        var pdfTemplatesEnabled = systemConfigRepository
                .existsByKeyAndValue(
                        SystemConfigKey.EXPERIMENTAL_FEATURES__PDF_TEMPLATES.getKey(),
                        SystemConfigKey.SYSTEM_CONFIG_TRUE
                );
        if (template != null && pdfTemplatesEnabled) {
            try {
                var res = loadTemplate(form.getPdfBodyTemplateKey(), dto);
                if (StringUtils.isNotNullOrEmpty(res)) {
                    return res;
                }
                return loadTemplate("application.html", dto);
            } catch (Exception e) {
                return loadTemplate("application.html", dto);
            }
        } else {
            return loadTemplate("application.html", dto);
        }
    }

    private String loadHeaderTemplate(Map<String, Object> dto) {
        return loadTemplate("application_header.html", dto);
    }

    private String loadFooterTemplate(Map<String, Object> dto) {
        return loadTemplate("application_footer.html", dto);
    }

    private String loadTemplate(String templateName, Map<String, Object> data) {
        return new TemplateLoaderService()
                .processTemplate(
                        templateName,
                        data,
                        TemplateMode.HTML
                );
    }

    // TODO: This is a copy from the MailService. Needs unification!
    private HashMap<String, Object> createBaseContext() {
        var context = new HashMap<String, Object>();

        context.put("providerName", systemConfigRepository
                .findById(SystemConfigKey.PROVIDER__NAME.getKey())
                .map(SystemConfig::getValue)
                .orElse("")
        );

        context.put("logoAssetKey", systemConfigRepository
                .findById(SystemConfigKey.SYSTEM__LOGO.getKey())
                .map(SystemConfig::getValue)
                .orElse("")
        );

        context.put("logoAssetName", assetRepository
                .findById(context.get("logoAssetKey").toString())
                .map(Asset::getFilename)
                .orElse("")
        );

        context.put("config", goverConfig);

        return context;
    }
}
