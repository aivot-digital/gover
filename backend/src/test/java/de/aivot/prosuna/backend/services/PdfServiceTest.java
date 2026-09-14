package de.aivot.prosuna.backend.services;

import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.GotenbergConfig;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.repositories.PaymentTransactionRepository;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.pdf.enums.FormPdfScope;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerConfigV1;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import de.aivot.prosuna.backend.services.pdf.PdfLogoService;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.utils.MultipartUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfServiceTest {
    private PdfService pdfService;
    private VDepartmentShadowedRepository vDepartmentShadowedRepository;
    private ProcessRepository processRepository;
    private ProcessVersionRepository processVersionRepository;
    private ElementDerivationService elementDerivationService;
    private ThemeService themeService;
    private GotenbergConfig gotenbergConfig;
    private HttpService httpService;
    private PdfLogoService pdfLogoService;
    private Method injectBaseUrlIntoHTMLMethod;
    private Method resolvePdfDepartmentMethod;

    @BeforeEach
    void setUp() throws Exception {
        var prosunaConfig = new ProsunaConfig();
        prosunaConfig.setProsunaHostname("https://prosuna.example/");
        vDepartmentShadowedRepository = mock(VDepartmentShadowedRepository.class);
        processRepository = mock(ProcessRepository.class);
        processVersionRepository = mock(ProcessVersionRepository.class);
        elementDerivationService = mock(ElementDerivationService.class);
        themeService = mock(ThemeService.class);
        gotenbergConfig = mock(GotenbergConfig.class);
        httpService = mock(HttpService.class);
        pdfLogoService = mock(PdfLogoService.class);

        pdfService = new PdfService(
                gotenbergConfig,
                mock(SystemConfigService.class),
                vDepartmentShadowedRepository,
                processRepository,
                processVersionRepository,
                pdfLogoService,
                prosunaConfig,
                mock(PaymentTransactionRepository.class),
                mock(IdentityProviderRepository.class),
                mock(PaymentProviderRepository.class),
                mock(PaymentProviderDefinitionsService.class),
                httpService,
                elementDerivationService,
                themeService
        );

        injectBaseUrlIntoHTMLMethod = PdfService.class
                .getDeclaredMethod("injectBaseUrlIntoHTML", String.class);
        injectBaseUrlIntoHTMLMethod.setAccessible(true);

        resolvePdfDepartmentMethod = PdfService.class
                .getDeclaredMethod("resolvePdfDepartment", FormLayoutElement.class, Integer.class);
        resolvePdfDepartmentMethod.setAccessible(true);
    }

    @Test
    void generatePaymentConfirmationEmbedsResolvedLogoInSubmittedHtml() throws Exception {
        var logoAssetKey = UUID.randomUUID();
        var logoDataUrl = "data:image/png;base64,bG9nbw==";
        var expectedPdf = "%PDF".getBytes(StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(expectedPdf);
        when(gotenbergConfig.getHost()).thenReturn("gotenberg");
        when(gotenbergConfig.getPort()).thenReturn("3000");
        when(pdfLogoService.resolveDataUrl(logoAssetKey)).thenReturn(Optional.of(logoDataUrl));
        when(httpService.postMultipart(any(), any())).thenReturn(response);

        var paymentRequest = new XBezahldienstePaymentRequest();
        paymentRequest.setGrosAmount(BigDecimal.TEN);
        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setTransactionId("transaction-1");
        paymentInformation.setTransactionTimestamp("2026-09-14T10:00:00Z");
        var transaction = new PaymentTransactionEntity()
                .setPaymentRequest(paymentRequest)
                .setPaymentInformation(paymentInformation);
        var department = new VDepartmentShadowedEntity()
                .setName("Example Department")
                .setPostalAddress("Example Street 1");

        var result = pdfService.generatePaymentConfirmation(
                transaction,
                "CASE-1",
                logoAssetKey,
                department
        );

        assertArrayEquals(expectedPdf, result);
        verify(pdfLogoService).resolveDataUrl(logoAssetKey);

        var multipartCaptor = ArgumentCaptor.forClass(MultipartUtils.MultipartBodyPublisher.class);
        verify(httpService).postMultipart(any(), multipartCaptor.capture());
        @SuppressWarnings("unchecked")
        var parts = (MultiValueMap<String, Object>) multipartCaptor.getValue().build();
        var indexHtml = parts.get("files")
                .stream()
                .filter(ByteArrayResource.class::isInstance)
                .map(ByteArrayResource.class::cast)
                .filter(part -> "index.html".equals(part.getFilename()))
                .findFirst()
                .map(part -> new String(part.getByteArray(), StandardCharsets.UTF_8))
                .orElseThrow();

        assertTrue(indexHtml.contains("src=\"" + logoDataUrl + "\""));
        assertFalse(indexHtml.contains("/api/public/assets/"));
    }

    @Test
    void generateCustomerSummary_UsesTheProcessNodesExactVersionForThemeResolution() throws Exception {
        var form = new FormLayoutElement().setPublicTitle("Citizen Form");
        var processVersion = new ProcessVersionEntity()
                .setProcessId(42)
                .setProcessVersion(7)
                .setThemeId(11);
        var processNode = new ProcessNodeEntity()
                .setProcessId(42)
                .setProcessVersion(7)
                .setName("Form");
        var processInstance = new ProcessInstanceEntity()
                .setProcessId(42)
                .setInitialProcessVersion(7)
                .setStarted(Instant.now());
        var config = new FormTriggerConfigV1();
        config.formSlug = "citizen-form";
        var expected = new IllegalStateException("Stop after resolving the theme.");

        when(elementDerivationService.derive(any(), any(), any())).thenReturn(DerivedRuntimeElementData.empty());
        when(processVersionRepository.findById(ProcessVersionEntityId.of(42, 7)))
                .thenReturn(Optional.of(processVersion));
        when(processRepository.findById(42)).thenReturn(Optional.of(
                new ProcessEntity().setId(42).setDepartmentId(20)
        ));
        when(themeService.resolveFormTheme(processVersion, form, 20)).thenThrow(expected);

        var result = assertThrows(IllegalStateException.class, () -> pdfService.generateCustomerSummary(
                form,
                new AuthoredElementValues(),
                FormPdfScope.Customer,
                processInstance,
                config,
                processNode
        ));

        assertSame(expected, result);
        verify(processVersionRepository).findById(ProcessVersionEntityId.of(42, 7));
        verify(themeService).resolveFormTheme(processVersion, form, 20);
    }

    @Test
    void injectBaseUrlIntoHTML_InsertsBaseTagIntoHead() throws Exception {
        var html = "<html><head><title>PDF</title></head><body><img src=\"/assets/logo.png\"/></body></html>";

        assertEquals(
                "<html><head><base href=\"https://prosuna.example/\"/><title>PDF</title></head><body><img src=\"/assets/logo.png\"/></body></html>",
                invokeInjectBaseUrlIntoHTML(html)
        );
    }

    @Test
    void injectBaseUrlIntoHTML_ReplacesExistingBaseTag() throws Exception {
        var html = "<html><head><base href=\"https://old.example/\"><title>PDF</title></head><body></body></html>";

        assertEquals(
                "<html><head><base href=\"https://prosuna.example/\"/><title>PDF</title></head><body></body></html>",
                invokeInjectBaseUrlIntoHTML(html)
        );
    }

    @Test
    void injectBaseUrlIntoHTML_ReturnsOriginalHtmlWhenHeadIsMissing() throws Exception {
        var html = "<div><img src=\"/assets/logo.png\"/></div>";

        assertEquals(html, invokeInjectBaseUrlIntoHTML(html));
    }

    @Test
    void injectBaseUrlIntoHTML_ReturnsNullForNullHtml() throws Exception {
        assertNull(invokeInjectBaseUrlIntoHTML(null));
    }

    @Test
    void resolvePdfDepartment_UsesFormDepartmentFirst() throws Exception {
        var formDepartment = new VDepartmentShadowedEntity().setId(10);
        var form = new FormLayoutElement().setManagingDepartmentId(10);

        when(vDepartmentShadowedRepository.findById(10)).thenReturn(Optional.of(formDepartment));

        assertSame(formDepartment, invokeResolvePdfDepartment(form, 42));
        verify(processRepository, never()).findById(anyInt());
    }

    @Test
    void resolvePdfDepartment_FallsBackToOwningProcessDepartment() throws Exception {
        var processDepartment = new VDepartmentShadowedEntity().setId(20);
        var process = new ProcessEntity()
                .setId(42)
                .setDepartmentId(20);
        var form = new FormLayoutElement();

        when(processRepository.findById(42)).thenReturn(Optional.of(process));
        when(vDepartmentShadowedRepository.findById(20)).thenReturn(Optional.of(processDepartment));

        assertSame(processDepartment, invokeResolvePdfDepartment(form, 42));
    }

    private String invokeInjectBaseUrlIntoHTML(String html) throws Exception {
        return (String) injectBaseUrlIntoHTMLMethod.invoke(pdfService, new Object[]{html});
    }

    private VDepartmentShadowedEntity invokeResolvePdfDepartment(FormLayoutElement form,
                                                                 Integer processId) throws Exception {
        return (VDepartmentShadowedEntity) resolvePdfDepartmentMethod.invoke(pdfService, form, processId);
    }
}
