package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.core.configs.LogoSystemConfigDefinition;
import de.aivot.GoverBackend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.services.FormDerivationService;
import de.aivot.GoverBackend.form.services.FormDerivationServiceFactory;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.identity.models.IdentityValue;
import de.aivot.GoverBackend.identity.repositories.IdentityProviderRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.PuppetPdfConfig;
import de.aivot.GoverBackend.payment.repositories.PaymentTransactionRepository;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.pdf.enums.FormPdfScope;
import de.aivot.GoverBackend.pdf.models.FormPdfContext;
import de.aivot.GoverBackend.services.pdf.PdfElementsGenerator;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
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
import java.util.*;

@Component
public class PdfService {
    private final PuppetPdfConfig puppetPdfConfig;
    private final SystemConfigService systemConfigService;
    private final DepartmentRepository departmentRepository;
    private final AssetRepository assetRepository;
    private final GoverConfig goverConfig;
    private final ThemeRepository themeRepository;
    private final FormDerivationServiceFactory formDerivationServiceFactory;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentProviderService paymentProviderService;
    private final IdentityProviderRepository identityProviderRepository;

    @Autowired
    public PdfService(
            PuppetPdfConfig puppetPdfConfig,
            SystemConfigService systemConfigService,
            DepartmentRepository departmentRepository,
            AssetRepository assetRepository,
            GoverConfig goverConfig,
            ThemeRepository themeRepository,
            FormDerivationServiceFactory formDerivationServiceFactory,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentProviderService paymentProviderService,
            IdentityProviderRepository identityProviderRepository) {
        this.puppetPdfConfig = puppetPdfConfig;
        this.systemConfigService = systemConfigService;
        this.departmentRepository = departmentRepository;
        this.assetRepository = assetRepository;
        this.goverConfig = goverConfig;
        this.themeRepository = themeRepository;
        this.formDerivationServiceFactory = formDerivationServiceFactory;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentProviderService = paymentProviderService;
        this.identityProviderRepository = identityProviderRepository;
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

    public byte[] generatePrintableForm(Form form) throws IOException, URISyntaxException, InterruptedException, ResponseException {
        var allElements = ElementUtils.flattenElements(form.getRoot());

        var derivationContext = formDerivationServiceFactory
                .create(form, List.of(), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER))
                .derive(form.getRoot(), Map.of());

        var dto = new HashMap<String, Object>();
        dto.put("elements", PdfElementsGenerator.generatePdfElements(form.getRoot(), Optional.empty(), derivationContext.getFormState()));
        dto.put("form", form);
        dto.put("attachments", allElements.stream().filter(e -> e.getType() == ElementType.FileUpload).toList());

        return generatePdf(form, dto, FormPdfScope.Blank);
    }

    public byte[] generateCustomerSummary(Form form, Submission submission, FormPdfScope scope) throws IOException, InterruptedException, URISyntaxException, ResponseException {
        var dto = new HashMap<String, Object>();

        var derivationContext = formDerivationServiceFactory
                .create(form, List.of(), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER), List.of(FormDerivationService.FORM_STEP_LIMIT_ALL_IDENTIFIER))
                .derive(form.getRoot(), submission.getCustomerInput());

        dto.put("elements", PdfElementsGenerator.generatePdfElements(form.getRoot(), Optional.of(submission.getCustomerInput()), derivationContext.getFormState()));
        dto.put("form", form);
        dto.put("submission", submission);

        var authData = submission
                .getCustomerInput()
                .get(IdentityValueKey.IdCustomerInputKey);

        if (authData instanceof Map<?, ?> mAuthData) {
            var identityData = IdentityValue
                    .fromMap(mAuthData);

            var identityProvider = identityProviderRepository
                    .findById(identityData.identityProviderKey());

            if (identityProvider.isPresent()) {
                dto.put("identityProvider", identityProvider.get());
                dto.put("identityData", identityData);
            }
        }

        if (submission.getPaymentTransactionKey() != null) {
            var paymentTransaction = paymentTransactionRepository
                    .findById(submission.getPaymentTransactionKey())
                    .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

            dto.put("paymentTransaction", paymentTransaction);

            var paymentProvider = paymentProviderService
                    .retrieve(paymentTransaction.getPaymentProviderKey())
                    .orElseThrow(() -> new RuntimeException("Payment provider not found"));

            dto.put("paymentProvider", paymentProvider);

            var paymentProviderDefinition = paymentProviderService
                    .getProviderDefinition(paymentProvider.getProviderKey())
                    .orElseThrow(() -> new RuntimeException("Payment provider definition not found"));

            dto.put("paymentProviderDefinition", paymentProviderDefinition);
        }

        return generatePdf(form, dto, scope);
    }

    private byte[] generatePdf(Form form, Map<String, Object> dto, FormPdfScope scope) throws IOException, URISyntaxException, InterruptedException, ResponseException {
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

        return generatePuppetPdf(form, dto);
    }

    private byte[] generatePuppetPdf(Form form, Map<String, Object> dto) throws IOException, InterruptedException, URISyntaxException {
        String template = loadContentTemplate(form, dto).replaceAll("(?m)^[ \\t]*\\r?\\n", "");
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
                    .findById(assetKeyUUID.toString())
                    .map(AssetEntity::getFilename)
                    .orElse("");
        } catch (Exception e) {
            // Ignore
        }

        return new FormPdfContext(providerName, logoAssetKey, logoAssetName, goverConfig, scope);
    }
}
