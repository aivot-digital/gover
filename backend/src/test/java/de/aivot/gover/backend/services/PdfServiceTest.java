package de.aivot.gover.backend.services;

import de.aivot.gover.backend.asset.repositories.AssetRepository;
import de.aivot.gover.backend.config.services.SystemConfigService;
import de.aivot.gover.backend.core.services.HttpService;
import de.aivot.gover.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.form.services.FormVersionService;
import de.aivot.gover.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.gover.backend.models.config.GotenbergConfig;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.gover.backend.payment.repositories.PaymentTransactionRepository;
import de.aivot.gover.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.gover.backend.services.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class PdfServiceTest {
    private PdfService pdfService;
    private Method injectBaseUrlIntoHTMLMethod;

    @BeforeEach
    void setUp() throws Exception {
        var goverConfig = new GoverConfig();
        goverConfig.setGoverHostname("https://gover.example/");

        pdfService = new PdfService(
                mock(GotenbergConfig.class),
                mock(SystemConfigService.class),
                mock(VDepartmentShadowedRepository.class),
                mock(AssetRepository.class),
                goverConfig,
                mock(PaymentTransactionRepository.class),
                mock(IdentityProviderRepository.class),
                mock(PaymentProviderRepository.class),
                mock(PaymentProviderDefinitionsService.class),
                mock(FormVersionService.class),
                mock(HttpService.class),
                mock(ElementDerivationService.class)
        );

        injectBaseUrlIntoHTMLMethod = PdfService.class
                .getDeclaredMethod("injectBaseUrlIntoHTML", String.class);
        injectBaseUrlIntoHTMLMethod.setAccessible(true);
    }

    @Test
    void injectBaseUrlIntoHTML_InsertsBaseTagIntoHead() throws Exception {
        var html = "<html><head><title>PDF</title></head><body><img src=\"/assets/logo.png\"/></body></html>";

        assertEquals(
                "<html><head><base href=\"https://gover.example/\"/><title>PDF</title></head><body><img src=\"/assets/logo.png\"/></body></html>",
                invokeInjectBaseUrlIntoHTML(html)
        );
    }

    @Test
    void injectBaseUrlIntoHTML_ReplacesExistingBaseTag() throws Exception {
        var html = "<html><head><base href=\"https://old.example/\"><title>PDF</title></head><body></body></html>";

        assertEquals(
                "<html><head><base href=\"https://gover.example/\"/><title>PDF</title></head><body></body></html>",
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

    private String invokeInjectBaseUrlIntoHTML(String html) throws Exception {
        return (String) injectBaseUrlIntoHTMLMethod.invoke(pdfService, new Object[]{html});
    }
}
