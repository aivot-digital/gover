package de.aivot.prosuna.backend.services;

import de.aivot.prosuna.backend.asset.entities.AssetEntity;
import de.aivot.prosuna.backend.asset.repositories.AssetRepository;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.prosuna.backend.core.exceptions.HttpConnectionException;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.services.ElementDerivationLogger;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.utils.ElementFlattenUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.GotenbergConfig;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.repositories.PaymentTransactionRepository;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.pdf.models.FormPdfContext;
import de.aivot.prosuna.backend.pdf.models.PrintableFormPdfData;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerConfigV1;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.services.pdf.PdfElementsGenerator;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.utils.MultipartUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfService {
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    private final GotenbergConfig gotenbergConfig;
    private final SystemConfigService systemConfigService;
    private final AssetRepository assetRepository;
    private final ProsunaConfig prosunaConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final IdentityProviderRepository identityProviderRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private final HttpService httpService;
    private final ElementDerivationService elementDerivationService;
    private final VDepartmentShadowedRepository vDepartmentShadowedRepository;
    private final ProcessRepository processRepository;
    private final ThemeService themeService;

    @Autowired
    public PdfService(GotenbergConfig gotenbergConfig,
                      SystemConfigService systemConfigService,
                      VDepartmentShadowedRepository vDepartmentShadowedRepository,
                      ProcessRepository processRepository,
                      AssetRepository assetRepository,
                      ProsunaConfig prosunaConfig,
                      PaymentTransactionRepository paymentTransactionRepository,
                      IdentityProviderRepository identityProviderRepository,
                      PaymentProviderRepository paymentProviderRepository,
                      PaymentProviderDefinitionsService paymentProviderDefinitionsService,
                      HttpService httpService,
                      ElementDerivationService elementDerivationService, ThemeService themeService) {
        this.gotenbergConfig = gotenbergConfig;
        this.systemConfigService = systemConfigService;
        this.assetRepository = assetRepository;
        this.prosunaConfig = prosunaConfig;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.identityProviderRepository = identityProviderRepository;
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
        this.httpService = httpService;
        this.elementDerivationService = elementDerivationService;
        this.vDepartmentShadowedRepository = vDepartmentShadowedRepository;
        this.processRepository = processRepository;
        this.themeService = themeService;
    }

    public void testGotenbergConnection() throws IOException {
        var healthUri = URI.create("http://" + gotenbergConfig.getHost() + ":" + gotenbergConfig.getPort() + "/health");

        HttpResponse<String> response;
        try {
            response = httpService.get(healthUri);
        } catch (HttpConnectionException e) {
            throw new IOException("Failed to connect to Gotenberg.", e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("Failed to connect to Gotenberg. Status code: " + response.statusCode());
        }
    }

    public byte[] generatePrintableForm(@Nonnull PrintableFormPdfData form,
                                        @Nonnull ThemeEntity theme,
                                        @Nonnull VDepartmentShadowedEntity department,
                                        @Nullable VDepartmentShadowedEntity responsibleDepartment,
                                        @Nullable VDepartmentShadowedEntity managingDepartment) throws IOException, URISyntaxException, InterruptedException, ResponseException {
        var rootElement = form.getRootElement();
        if (rootElement == null) {
            throw new IllegalArgumentException("Printable form root element cannot be null.");
        }

        var allElements = ElementFlattenUtils.flattenElements(rootElement);
        var derivedRuntimeElementData = deriveBlankPrintableElementData(rootElement);

        var dto = new HashMap<String, Object>();
        dto.put("elements", PdfElementsGenerator.generatePdfElements(
                rootElement,
                derivedRuntimeElementData,
                true,
                true
        ));
        dto.put("form", form);
        dto.put("attachments", allElements.stream().filter(e -> e.getType() == ElementType.FileUpload).toList());
        dto.put("base", createBaseContext(theme, FormPdfScope.Blank));
        dto.put("department", department);
        dto.put("responsibleDepartment", responsibleDepartment);
        dto.put("managingDepartment", managingDepartment);
        dto.put("theme", theme);

        return generateGotenbergPdf(form.getPdfTemplateKey(), dto);
    }

    public byte[] generateCustomerSummary(FormLayoutElement formLayoutElement,
                                          AuthoredElementValues submission,
                                          FormPdfScope scope,
                                          ProcessInstanceEntity processInstance,
                                          FormTriggerConfigV1 formTriggerConfig,
                                          ProcessNodeEntity processNode) throws IOException, InterruptedException, URISyntaxException, ResponseException {
        var dto = new HashMap<String, Object>();
        var derivedRuntimeElementData = elementDerivationService
                .derive(
                        new ElementDerivationRequest(
                                formLayoutElement,
                                submission,
                                new ElementDerivationOptions()
                                        .setSkipErrorsForElementIds(java.util.List.of(ElementDerivationOptions.ALL_ELEMENTS))
                        ),
                        new IdentityDataMap(), // TODO: Load identities for this
                        new ElementDerivationLogger()
                );


        dto.put("elements", PdfElementsGenerator.generatePdfElements(
                formLayoutElement,
                derivedRuntimeElementData,
                scope != FormPdfScope.Staff,
                false
        ));
        dto.put("form", new PrintableFormPdfData()
                .setSlug(formTriggerConfig.formSlug)
                .setInternalTitle(processNode.getName())
                .setVersion(processInstance.getInitialProcessVersion())
                .setPublicTitle(formLayoutElement.getPublicTitle())
                .setRootElement(formLayoutElement)
                .setPdfTemplateKey(formLayoutElement.getPdfTemplateKey())
        );
        dto.put("submission", Map.of(
                "created", processInstance.getStarted()
        ));

        /* TODO: Resolve Identity
        var authData = submission
                .getCustomerInput()
                .get(IdentityValueKey.IdCustomerInputKey);
        if (authData != null) {
            IdentityData identityData = null;
            try {
                identityData = new ObjectMapper()
                        .convertValue(authData, IdentityData.class);
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
         */

        /* TODO: Resolve Payment
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
                    .getProviderDefinition(
                            paymentProvider.getPaymentProviderDefinitionKey(),
                            paymentProvider.getPaymentProviderDefinitionVersion()
                    )
                    .orElseThrow(() -> new RuntimeException("Payment provider definition not found"));

            dto.put("paymentProviderDefinition", paymentProviderDefinition);
        }
         */

        return generatePdf(formLayoutElement, dto, scope, processInstance.getProcessId());
    }

    public byte[] generatePaymentConfirmation(@Nonnull PaymentTransactionEntity transaction,
                                              @Nonnull String caseNumber,
                                              @Nullable String logoUrl,
                                              @Nonnull VDepartmentShadowedEntity department) throws IOException, InterruptedException, URISyntaxException {
        var dto = new HashMap<String, Object>();
        dto.put("transaction", transaction);
        dto.put("caseNumber", caseNumber);
        dto.put("logoUrl", logoUrl);
        dto.put("department", department);
        dto.put("generatedAt", Instant.now().toString());

        var contentHtml = loadTemplate("payment-confirmation/form-trigger-payment-confirmation.html", dto);
        var footerHtml = loadTemplate("payment-confirmation/form-trigger-payment-confirmation-footer.html", dto);

        return generatePdfFromHtml(contentHtml, null, footerHtml);
    }

    private DerivedRuntimeElementData deriveBlankPrintableElementData(FormLayoutElement form) {
        return elementDerivationService
                .derive(
                        new ElementDerivationRequest(
                                form,
                                createBlankPrintableElementValues(form),
                                new ElementDerivationOptions()
                                        .setSkipErrorsForElementIds(java.util.List.of(ElementDerivationOptions.ALL_ELEMENTS))
                        ),
                        new IdentityDataMap(),
                        new ElementDerivationLogger()
                );
    }

    private AuthoredElementValues createBlankPrintableElementValues(BaseElement element) {
        var values = new AuthoredElementValues();
        collectBlankPrintableElementValues(element, values);
        return values;
    }

    private AuthoredElementValues createBlankPrintableElementValues(Collection<? extends BaseElement> elements) {
        var values = new AuthoredElementValues();
        for (var element : elements) {
            collectBlankPrintableElementValues(element, values);
        }
        return values;
    }

    private void collectBlankPrintableElementValues(BaseElement element, AuthoredElementValues values) {
        if (element instanceof ReplicatingContainerLayoutElement replicatingContainer) {
            if (replicatingContainer.getId() != null) {
                var rows = new ArrayList<ReplicatingContainerLayoutElementValue>();
                var rowCount = PdfElementsGenerator.getBlankPrintPlaceholderCount(replicatingContainer);
                for (var i = 0; i < rowCount; i++) {
                    rows.add(new ReplicatingContainerLayoutElementValue()
                            .setValues(createBlankPrintableElementValues(replicatingContainer.getChildren())));
                }
                values.put(replicatingContainer.getId(), rows);
            }
            return;
        }

        if (element instanceof LayoutElement<?> layoutElement) {
            for (var child : layoutElement.getChildren()) {
                collectBlankPrintableElementValues(child, values);
            }
        }
    }

    private byte[] generatePdf(FormLayoutElement form, Map<String, Object> dto, FormPdfScope scope, @Nullable Integer processId) throws IOException, URISyntaxException, InterruptedException, ResponseException {
        var formTheme = themeService
                .getFormThemesInOrderOfImportance(form)
                .getFirst();

        dto.put("base", createBaseContext(formTheme, scope));
        dto.put("department", resolvePdfDepartment(form, processId));
        dto.put("responsibleDepartment", findDepartment(form.getResponsibleDepartmentId()));
        dto.put("managingDepartment", findDepartment(form.getManagingDepartmentId()));
        dto.put("theme", formTheme);

        return generateGotenbergPdf(form.getPdfTemplateKey(), dto);
    }

    @Nonnull
    private VDepartmentShadowedEntity resolvePdfDepartment(@Nonnull FormLayoutElement form,
                                                           @Nullable Integer processId) {
        return findDepartmentById(form.getRelevantDepartmentId())
                .or(() -> findProcessOwningDepartment(processId))
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }

    @Nonnull
    private Optional<VDepartmentShadowedEntity> findDepartmentById(@Nullable Integer departmentId) {
        if (departmentId == null) {
            return Optional.empty();
        }

        return vDepartmentShadowedRepository.findById(departmentId);
    }

    @Nonnull
    private Optional<VDepartmentShadowedEntity> findProcessOwningDepartment(@Nullable Integer processId) {
        if (processId == null) {
            return Optional.empty();
        }

        return processRepository
                .findById(processId)
                .map(ProcessEntity::getDepartmentId)
                .flatMap(vDepartmentShadowedRepository::findById);
    }

    @Nullable
    private VDepartmentShadowedEntity findDepartment(@Nullable Integer departmentId) {
        if (departmentId == null) {
            return null;
        }

        return vDepartmentShadowedRepository
                .findById(departmentId)
                .orElse(null);
    }

    private byte[] generateGotenbergPdf(@Nullable UUID pdfTemplateKey, Map<String, Object> dto) throws IOException, InterruptedException, URISyntaxException {
        String template = loadContentTemplate(pdfTemplateKey, dto);
        String headerTemplate = loadTemplate("pp_form_header.html", dto);
        String footerTemplate = loadTemplate("pp_form_footer.html", dto);

        return generatePdfFromHtml(
                template,
                headerTemplate,
                footerTemplate
        );
    }

    private static final String GOTENBERG_ARG_FILE = "files";
    private static final String GOTENBERG_ARG_INDEX = "index";
    private static final String GOTENBERG_ARG_HEADER = "header";
    private static final String GOTENBERG_ARG_FOOTER = "footer";
    private static final String GOTENBERG_ARG_PAPER_HEIGHT = "paperHeight";
    private static final String GOTENBERG_ARG_PAPER_WIDTH = "paperWidth";
    private static final String GOTENBERG_ARG_MARGIN_TOP = "marginTop";
    private static final String GOTENBERG_ARG_MARGIN_BOTTOM = "marginBottom";
    private static final String GOTENBERG_ARG_MARGIN_LEFT = "marginLeft";
    private static final String GOTENBERG_ARG_MARGIN_RIGHT = "marginRight";

    private static final String GOTENBERG_VAL_INDEX = "index.html";
    private static final String GOTENBERG_VAL_HEADER = "header.html";
    private static final String GOTENBERG_VAL_FOOTER = "footer.html";
    private static final String GOTENBERG_VAL_PAPER_HEIGHT = "29.7cm";
    private static final String GOTENBERG_VAL_PAPER_WIDTH = "21.0cm";
    private static final String GOTENBERG_VAL_MARGIN_TOP = "2.5cm";
    private static final String GOTENBERG_VAL_MARGIN_BOTTOM = "2.5cm";
    private static final String GOTENBERG_VAL_MARGIN_RIGHT = "2.0cm";
    private static final String GOTENBERG_VAL_MARGIN_LEFT = "2.5cm";
    private static final Pattern HTML_HEAD_TAG_PATTERN = Pattern.compile("<head\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_BASE_TAG_PATTERN = Pattern.compile("<base\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    public byte[] generatePdfFromHtml(@Nonnull String contentHtml,
                                      @Nullable String headerHtml,
                                      @Nullable String footerHtml) throws IOException, InterruptedException, URISyntaxException {
        contentHtml = injectBaseUrlIntoHTML(contentHtml);
        headerHtml = injectBaseUrlIntoHTML(headerHtml);
        footerHtml = injectBaseUrlIntoHTML(footerHtml);

        if (contentHtml == null) {
            throw new IllegalArgumentException("Content HTML cannot be null");
        }

        var multipart = new MultipartUtils.MultipartBodyPublisher()
                .addPart(GOTENBERG_ARG_FILE, GOTENBERG_VAL_INDEX, contentHtml)
                .addPart(GOTENBERG_ARG_INDEX, GOTENBERG_VAL_INDEX)
                .addPart(GOTENBERG_ARG_HEADER, GOTENBERG_VAL_HEADER)
                .addPart(GOTENBERG_ARG_FOOTER, GOTENBERG_VAL_FOOTER)
                .addPart(GOTENBERG_ARG_PAPER_HEIGHT, GOTENBERG_VAL_PAPER_HEIGHT)
                .addPart(GOTENBERG_ARG_PAPER_WIDTH, GOTENBERG_VAL_PAPER_WIDTH)
                .addPart(GOTENBERG_ARG_MARGIN_TOP, GOTENBERG_VAL_MARGIN_TOP)
                .addPart(GOTENBERG_ARG_MARGIN_BOTTOM, GOTENBERG_VAL_MARGIN_BOTTOM)
                .addPart(GOTENBERG_ARG_MARGIN_RIGHT, GOTENBERG_VAL_MARGIN_RIGHT)
                .addPart(GOTENBERG_ARG_MARGIN_LEFT, GOTENBERG_VAL_MARGIN_LEFT);

        if (StringUtils.isNotNullOrEmpty(headerHtml)) {
            multipart.addPart(GOTENBERG_ARG_FILE, GOTENBERG_VAL_HEADER, headerHtml);
        }

        if (StringUtils.isNotNullOrEmpty(footerHtml)) {
            multipart.addPart(GOTENBERG_ARG_FILE, GOTENBERG_VAL_FOOTER, footerHtml);
        }

        var convertUri = new URI("http://" + gotenbergConfig.getHost() + ":" + gotenbergConfig.getPort() + "/forms/chromium/convert/html");

        HttpResponse<byte[]> response;
        try {
            response = httpService.postMultipart(convertUri, multipart);
        } catch (HttpConnectionException e) {
            throw new IOException("Failed to generate PDF with Gotenberg.", e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("Failed to generate PDF with Gotenberg. Status: " + response.statusCode());
        }

        return response.body();
    }

    private String loadContentTemplate(@Nullable UUID pdfTemplateKey, Map<String, Object> dto) {
        if (pdfTemplateKey != null) {
            try {
                var res = loadTemplate(pdfTemplateKey.toString(), dto);
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
    private FormPdfContext createBaseContext(ThemeEntity theme, FormPdfScope scope) throws ResponseException {
        var providerName = systemConfigService
                .retrieve(ProviderNameSystemConfigDefinition.KEY)
                .getValue();

        var logoAssetKey = theme.getLogoKey();
        var logoAssetName = "";
        try {
            if (logoAssetKey != null) {
                logoAssetName = assetRepository
                        .findById(logoAssetKey)
                        .map(AssetEntity::getKey)
                        .map(UUID::toString)
                        .orElse("");
            }
        } catch (Exception e) {
            // Ignore
        }

        return new FormPdfContext(providerName, logoAssetKey != null ? logoAssetKey.toString() : "", logoAssetName, prosunaConfig, scope);
    }

    /**
     * Injects the HTML-Tag {@code <base href="{PROSUNA_HOSTNAME}"/>} into the Head-Tag of the given HTML.
     *
     * @param originalHTML The original HTML content.
     * @return The injected HTML content.
     */
    @Nullable
    private String injectBaseUrlIntoHTML(@Nullable String originalHTML) {
        if (StringUtils.isNullOrEmpty(originalHTML)) {
            return originalHTML;
        }

        var baseUrl = URI.create(prosunaConfig.getProsunaHostname()).toString();
        var baseTag = "<base href=\"" + HtmlUtils.htmlEscape(baseUrl) + "\"/>";
        var existingBaseTagMatcher = HTML_BASE_TAG_PATTERN.matcher(originalHTML);

        if (existingBaseTagMatcher.find()) {
            return existingBaseTagMatcher.replaceFirst(Matcher.quoteReplacement(baseTag));
        }

        var headTagMatcher = HTML_HEAD_TAG_PATTERN.matcher(originalHTML);
        if (!headTagMatcher.find()) {
            return originalHTML;
        }

        return new StringBuilder(originalHTML)
                .insert(headTagMatcher.end(), baseTag)
                .toString();
    }
}
