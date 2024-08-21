package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.PuppetPdfConfig;
import de.aivot.GoverBackend.models.entities.Asset;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.entities.SystemConfig;
import de.aivot.GoverBackend.repositories.AssetRepository;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.repositories.ThemeRepository;
import de.aivot.GoverBackend.services.pdf.PdfContext;
import de.aivot.GoverBackend.services.pdf.PdfElementsGenerator;
import de.aivot.GoverBackend.utils.ElementUtils;
import de.aivot.GoverBackend.utils.StringUtils;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class PdfService {
    private final PuppetPdfConfig puppetPdfConfig;
    private final SystemConfigRepository systemConfigRepository;
    private final DepartmentRepository departmentRepository;
    private final AssetRepository assetRepository;
    private final GoverConfig goverConfig;
    private final ThemeRepository themeRepository;

    @Autowired
    public PdfService(
            PuppetPdfConfig puppetPdfConfig,
            SystemConfigRepository systemConfigRepository,
            DepartmentRepository departmentRepository,
            AssetRepository assetRepository,
            GoverConfig goverConfig,
            ThemeRepository themeRepository
    ) {
        this.puppetPdfConfig = puppetPdfConfig;
        this.systemConfigRepository = systemConfigRepository;
        this.departmentRepository = departmentRepository;
        this.assetRepository = assetRepository;
        this.goverConfig = goverConfig;
        this.themeRepository = themeRepository;
    }

    public void testPuppetPdfConnection() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest
                    .newBuilder(URI.create("http://" + puppetPdfConfig.getHost() + ":" + puppetPdfConfig.getPort() + "/health"))
                    .GET()
                    .build();
            var response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to connect to Puppet PDF with status code: " + response.statusCode());
            }
        }
    }

    public byte[] generatePrintableForm(Form form) throws IOException, URISyntaxException, InterruptedException {
        var allElements = ElementUtils.flattenElementsWithContext(form.getRoot(), 0);

        var dto = new HashMap<String, Object>();
        dto.put("elements", PdfElementsGenerator.generatePdfElements(form.getRoot(), Optional.empty()));
        dto.put("form", form);
        dto.put("attachments", allElements.stream().filter(e -> e.element().getType() == ElementType.FileUpload).toList());

        return generatePdf(form, dto, false);
    }

    public byte[] generateCustomerSummary(Form form, Submission submission) throws IOException, InterruptedException, URISyntaxException {
        var dto = new HashMap<String, Object>();

        dto.put("elements", PdfElementsGenerator.generatePdfElements(form.getRoot(), Optional.of(submission.getCustomerInput())));
        dto.put("form", form);
        dto.put("submission", submission);

        return generatePdf(form, dto, true);
    }

    private byte[] generatePdf(Form form, Map<String, Object> dto, Boolean isSummary) throws IOException, URISyntaxException, InterruptedException {
        dto.put("base", createBaseContext(isSummary));
        dto.put("department",
                departmentRepository
                        .findById(form.getRelevantDepartmentId())
                        .orElseThrow(() -> new RuntimeException("Department not found"))
        );
        dto.put("theme",
                form.getThemeId() != null ?
                        themeRepository
                                .findById(form.getThemeId())
                                .orElse(null)
                        : null
        );

        return generatePuppetPdf(form, dto);
    }

    private byte[] generatePuppetPdf(Form form, Map<String, Object> dto) throws IOException, InterruptedException, URISyntaxException {
        String template = loadContentTemplate(form, dto);
        String headerTemplate = loadTemplate("pp_form_header.html", dto);
        String footerTemplate = loadTemplate("pp_form_footer.html", dto);

        var uri = new URI("http://" + puppetPdfConfig.getHost() + ":" + puppetPdfConfig.getPort() + "/print");

        JSONObject json = new JSONObject();
        json.put("html", template);
        json.put("headerTemplate", headerTemplate);
        json.put("footerTemplate", footerTemplate);

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

        var body = response.body();
        var bytes = body.readAllBytes();
        body.close();

        client.close();

        return bytes;
    }

    private String loadContentTemplate(Form form, Map<String, Object> dto) {
        var template = form.getPdfBodyTemplateKey();

        if (template != null) {
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
    private PdfContext createBaseContext(Boolean isSummary) {
        var providerName = systemConfigRepository
                .findById(SystemConfigKey.PROVIDER__NAME.getKey())
                .map(SystemConfig::getValue)
                .orElse("");

        var logoAssetKey = systemConfigRepository
                .findById(SystemConfigKey.SYSTEM__LOGO.getKey())
                .map(SystemConfig::getValue)
                .orElse("");

        var logoAssetName = "";
        try {
            var assetKeyUUID = UUID.fromString(logoAssetKey);
            logoAssetName = assetRepository
                    .findById(assetKeyUUID.toString())
                    .map(Asset::getFilename)
                    .orElse("");
        } catch (Exception e) {
            // Ignore
        }

        return new PdfContext(providerName, logoAssetKey, logoAssetName, goverConfig, isSummary);
    }
}
