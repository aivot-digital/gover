package de.aivot.prosuna.backend.services;

import de.aivot.prosuna.backend.asset.repositories.AssetRepository;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.models.config.GotenbergConfig;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.repositories.PaymentTransactionRepository;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfServiceTest {
    private PdfService pdfService;
    private VDepartmentShadowedRepository vDepartmentShadowedRepository;
    private ProcessRepository processRepository;
    private Method injectBaseUrlIntoHTMLMethod;
    private Method resolvePdfDepartmentMethod;

    @BeforeEach
    void setUp() throws Exception {
        var prosunaConfig = new ProsunaConfig();
        prosunaConfig.setProsunaHostname("https://prosuna.example/");
        vDepartmentShadowedRepository = mock(VDepartmentShadowedRepository.class);
        processRepository = mock(ProcessRepository.class);

        pdfService = new PdfService(
                mock(GotenbergConfig.class),
                mock(SystemConfigService.class),
                vDepartmentShadowedRepository,
                processRepository,
                mock(AssetRepository.class),
                prosunaConfig,
                mock(PaymentTransactionRepository.class),
                mock(IdentityProviderRepository.class),
                mock(PaymentProviderRepository.class),
                mock(PaymentProviderDefinitionsService.class),
                mock(HttpService.class),
                mock(ElementDerivationService.class),
                mock(ThemeService.class)
        );

        injectBaseUrlIntoHTMLMethod = PdfService.class
                .getDeclaredMethod("injectBaseUrlIntoHTML", String.class);
        injectBaseUrlIntoHTMLMethod.setAccessible(true);

        resolvePdfDepartmentMethod = PdfService.class
                .getDeclaredMethod("resolvePdfDepartment", FormLayoutElement.class, Integer.class);
        resolvePdfDepartmentMethod.setAccessible(true);
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
