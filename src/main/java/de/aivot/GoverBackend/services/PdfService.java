package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.core.configs.LogoSystemConfigDefinition;
import de.aivot.GoverBackend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.utils.ElementFlattenUtils;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.identity.models.IdentityData;
import de.aivot.GoverBackend.identity.repositories.IdentityProviderRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GotenbergConfig;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.payment.repositories.PaymentTransactionRepository;
import de.aivot.GoverBackend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.pdf.models.FormPdfContext;
import de.aivot.GoverBackend.services.pdf.PdfElementsGenerator;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
import de.aivot.GoverBackend.utils.MultipartUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.UUID;

@Component
public class PdfService {
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    private final GotenbergConfig gotenbergConfig;
    private final SystemConfigService systemConfigService;
    private final DepartmentRepository departmentRepository;
    private final AssetRepository assetRepository;
    private final GoverConfig goverConfig;
    private final ThemeRepository themeRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final IdentityProviderRepository identityProviderRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;

    @Autowired
    public PdfService(
            GotenbergConfig gotenbergConfig,
            SystemConfigService systemConfigService,
            DepartmentRepository departmentRepository,
            AssetRepository assetRepository,
            GoverConfig goverConfig,
            ThemeRepository themeRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            IdentityProviderRepository identityProviderRepository,
            PaymentProviderRepository paymentProviderRepository,
            PaymentProviderDefinitionsService paymentProviderDefinitionsService
    ) {
        this.gotenbergConfig = gotenbergConfig;
        this.systemConfigService = systemConfigService;
        this.departmentRepository = departmentRepository;
        this.assetRepository = assetRepository;
        this.goverConfig = goverConfig;
        this.themeRepository = themeRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.identityProviderRepository = identityProviderRepository;
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
    }

    public void testGotenbergConnection() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest
                    .newBuilder(URI.create("http://" + gotenbergConfig.getHost() + ":" + gotenbergConfig.getPort() + "/health"))
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to connect to Gotenberg. Status code: " + response.statusCode());
            }
        }
    }

    public byte[] generatePrintableForm(FormVersionWithDetailsEntity form) throws IOException, URISyntaxException, InterruptedException, ResponseException {
        var allElements = ElementFlattenUtils.flattenElements(form.getRootElement());

        var dto = new HashMap<String, Object>();
        dto.put("elements", PdfElementsGenerator.generatePdfElements(
                form.getRootElement(),
                null,
                true
        ));
        dto.put("form", form);
        dto.put("attachments", allElements.stream().filter(e -> e.getType() == ElementType.FileUpload).toList());

        return generatePdf(form, dto, FormPdfScope.Blank);
    }

    public byte[] generateCustomerSummary(FormVersionWithDetailsEntity form, Submission submission, FormPdfScope scope) throws IOException, InterruptedException, URISyntaxException, ResponseException {
        var dto = new HashMap<String, Object>();

        dto.put("elements", PdfElementsGenerator.generatePdfElements(
                form.getRootElement(),
                submission.getCustomerInput(),
                scope != FormPdfScope.Staff
        ));
        dto.put("form", form);
        dto.put("submission", submission);

        ElementDataObject authData = submission
                .getCustomerInput()
                .get(IdentityValueKey.IdCustomerInputKey);
        if (authData != null && authData.getInputValue() != null) {
            IdentityData identityData = null;
            try {
                identityData = new ObjectMapper()
                        .convertValue(authData.getInputValue(), IdentityData.class);
            } catch (IllegalArgumentException e) {
                logger.error("Failed to convert identity data to IdentityData", e);
            }

            if (identityData != null) {
                var identityProvider = identityProviderRepository
                        .findById(identityData.providerKey());

                if (identityProvider.isPresent()) {
                    dto.put("identityProvider", identityProvider.get());
                    dto.put("identityData", identityData);
                }
            }
        }

        if (submission.getPaymentTransactionKey() != null) {
            var paymentTransaction = paymentTransactionRepository
                    .findById(submission.getPaymentTransactionKey())
                    .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

            dto.put("paymentTransaction", paymentTransaction);

            var paymentProvider = paymentProviderRepository
                    .findById(paymentTransaction.getPaymentProviderKey())
                    .orElseThrow(() -> new RuntimeException("Payment provider not found"));

            dto.put("paymentProvider", paymentProvider);

            var paymentProviderDefinition = paymentProviderDefinitionsService
                    .getProviderDefinition(paymentProvider.getProviderKey())
                    .orElseThrow(() -> new RuntimeException("Payment provider definition not found"));

            dto.put("paymentProviderDefinition", paymentProviderDefinition);
        }

        return generatePdf(form, dto, scope);
    }

    private byte[] generatePdf(FormVersionWithDetailsEntity form, Map<String, Object> dto, FormPdfScope scope) throws IOException, URISyntaxException, InterruptedException, ResponseException {
        dto.put("base", createBaseContext(scope));
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

        return generateGotenbergPdf(form, dto);
    }

    private byte[] generateGotenbergPdf(FormVersionWithDetailsEntity form, Map<String, Object> dto) throws IOException, InterruptedException, URISyntaxException {
        String template = loadContentTemplate(form, dto);
        String headerTemplate = loadTemplate("pp_form_header.html", dto);
        String footerTemplate = loadTemplate("pp_form_footer.html", dto);

        var boundary = "----GotenbergBoundary";
        var uri = new URI("http://" + gotenbergConfig.getHost() + ":" + gotenbergConfig.getPort() + "/forms/chromium/convert/html");

        var multipart = new MultipartUtils.MultipartBodyPublisher(boundary)
                .addPart("files", "index.html", template)
                .addPart("files", "header.html", headerTemplate)
                .addPart("files", "footer.html", footerTemplate)
                .addPart("index", "index.html")
                .addPart("header", "header.html")
                .addPart("footer", "footer.html")
                .addPart("paperHeight", "297mm")
                .addPart("paperWidth", "210mm");

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(multipart.build())
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to generate PDF with Gotenberg. Status: " + response.statusCode());
        }

        var body = response.body();
        byte[] bytes = body.readAllBytes();
        body.close();

        return bytes;
    }

    private String loadContentTemplate(FormVersionWithDetailsEntity form, Map<String, Object> dto) {
        var template = form.getPdfTemplateKey();

        if (template != null) {
            try {
                var res = loadTemplate(form.getPdfTemplateKey().toString(), dto);
                if (StringUtils.isNotNullOrEmpty(res)) {
                    return res;
                }
                return loadTemplate("form.html", dto);
            } catch (Exception e) {
                return loadTemplate("form.html", dto);
            }
        } else {
            return loadTemplate("form.html", dto);
        }
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
    private FormPdfContext createBaseContext(FormPdfScope scope) throws ResponseException {
        var providerName = systemConfigService
                .retrieve(ProviderNameSystemConfigDefinition.KEY)
                .getValue();

        var logoAssetKey = systemConfigService
                .retrieve(LogoSystemConfigDefinition.KEY)
                .getValue();

        var logoAssetName = "";
        try {
            var assetKeyUUID = UUID.fromString(logoAssetKey);
            logoAssetName = assetRepository
                    .findById(assetKeyUUID)
                    .map(AssetEntity::getFilename)
                    .orElse("");
        } catch (Exception e) {
            // Ignore
        }

        return new FormPdfContext(providerName, logoAssetKey, logoAssetName, goverConfig, scope);
    }
}
