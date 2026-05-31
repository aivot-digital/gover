package de.aivot.GoverBackend.plugins.form.v1.nodes;

import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormTriggerNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;

    private ProcessNodeRepository processNodeRepository;
    private FormTriggerNodeV1 node;

    @BeforeEach
    void setUp() {
        processNodeRepository = mock(ProcessNodeRepository.class);
        node = new FormTriggerNodeV1(mock(GoverConfig.class), processNodeRepository);
    }

    @Test
    void validateConfiguration_ShouldAllowValidLayoutAndUniqueSlug() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(false);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("antrag-online", validFormLayout())
        );

        assertNull(errors);
    }

    @Test
    void validateConfiguration_ShouldReportLegacyLayoutFieldsMissingFromFormLayout() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(false);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("antrag-online", new FormLayoutElement())
        );

        assertNotNull(errors);
        assertEquals(1, errors.size());

        var layoutError = errors.get(FormTriggerConfigV1.FORM_LAYOUT);
        assertNotNull(layoutError);
        assertTrue(layoutError.contains("Der öffentliche Titel muss hinterlegt sein."));
        assertTrue(layoutError.contains("Der fachliche Support muss eingerichtet sein."));
        assertTrue(layoutError.contains("Der technische Support muss eingerichtet sein."));
        assertTrue(layoutError.contains("Das Impressum muss eingerichtet sein."));
        assertTrue(layoutError.contains("Die Datenschutzerklärung muss eingerichtet sein."));
        assertTrue(layoutError.contains("Die Barrierefreiheitserklärung muss eingerichtet sein."));
    }

    @Test
    void validateConfiguration_ShouldRejectDuplicateSlug() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(true);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("antrag-online", validFormLayout())
        );

        assertNotNull(errors);
        assertEquals(
                "Die Formular-URL wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet.",
                errors.get(FormTriggerConfigV1.FORM_SLUG)
        );
    }

    @Test
    void validateConfiguration_ShouldReturnSlugAndLayoutErrorsTogether() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(true);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("antrag-online", new FormLayoutElement())
        );

        assertNotNull(errors);
        assertEquals(2, errors.size());
        assertTrue(errors.containsKey(FormTriggerConfigV1.FORM_SLUG));
        assertTrue(errors.containsKey(FormTriggerConfigV1.FORM_LAYOUT));
    }

    private static FormTriggerConfigV1 configuration(String formSlug, FormLayoutElement formLayout) {
        var configuration = new FormTriggerConfigV1();
        configuration.formSlug = formSlug;
        configuration.formLayout = formLayout;
        return configuration;
    }

    private static FormLayoutElement validFormLayout() {
        return new FormLayoutElement()
                .setPublicTitle("Antrag auf Leistung")
                .setLegalSupportDepartmentId(1)
                .setTechnicalSupportDepartmentId(2)
                .setImprintDepartmentId(3)
                .setPrivacyDepartmentId(4)
                .setAccessibilityDepartmentId(5);
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Formular")
                .setDataKey("formNode")
                .setProcessNodeDefinitionKey(FormTriggerNodeV1.NODE_KEY)
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());
    }

    private static Specification<ProcessNodeEntity> anySpecification() {
        return any();
    }
}
