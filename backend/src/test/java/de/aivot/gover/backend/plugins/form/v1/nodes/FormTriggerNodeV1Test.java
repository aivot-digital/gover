package de.aivot.gover.backend.plugins.form.v1.nodes;

import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.plugins.form.v1.nodes.FormTriggerConfigV1;
import de.aivot.gover.backend.plugins.form.v1.nodes.FormTriggerNodeV1;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.enums.ProcessVersionStatus;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import de.aivot.gover.backend.process.services.PublicUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        node = new FormTriggerNodeV1(mock(PublicUrlService.class), processNodeRepository);
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
    void getOutputs_ShouldExposeStartedTimestamp() {
        var output = node
                .getOutputs()
                .stream()
                .filter(candidate -> FormTriggerNodeV1.DATA_KEY_STARTED.equals(candidate.key()))
                .findFirst()
                .orElse(null);

        assertNotNull(output);
        assertEquals("Eingangszeitstempel", output.label());
        assertEquals("Der Zeitstempel des Dateneingangs an den Auslöser", output.description());
    }

    @Test
    void getConfigurationLayout_ShouldExposeCopyableSlugUrlTemplate() throws Exception {
        var publicUrlService = new PublicUrlService(goverConfig());
        var node = new FormTriggerNodeV1(publicUrlService, processNodeRepository);

        var layout = node.getConfigurationLayout(configurationLayoutContext());
        var slugField = layout
                .findChild(FormTriggerConfigV1.FORM_SLUG, TextInputElement.class)
                .orElseThrow();

        assertEquals(true, slugField.getCopyable());
        assertEquals("https://example.test/form/antrag-prozess/{value}/", slugField.getCopyValueTemplate());
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
                List.of("Das URL-Segment des Formulars wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet."),
                errors.get(FormTriggerConfigV1.FORM_SLUG)
        );
    }

    @Test
    void validateConfiguration_ShouldReturnMultipleSlugErrors() throws Exception {
        when(processNodeRepository.exists(anySpecification())).thenReturn(true);

        var errors = node.validateConfiguration(
                processNode(),
                configuration("Antrag Online", validFormLayout())
        );

        assertNotNull(errors);
        assertEquals(
                List.of(
                        "Das URL-Segment des Formulars darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.",
                        "Das URL-Segment des Formulars wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet."
                ),
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

    private static ProcessNodeDefinitionConfigurationLayoutContext configurationLayoutContext() {
        return new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                process(),
                processVersion(),
                processNode()
        );
    }

    private static ProcessEntity process() {
        return new ProcessEntity()
                .setId(PROCESS_ID)
                .setInternalTitle("Antrag")
                .setDepartmentId(1)
                .setAccessKey(UUID.randomUUID())
                .setSlug("antrag-prozess")
                .setVersionCount(PROCESS_VERSION)
                .setDraftedVersion(PROCESS_VERSION);
    }

    private static ProcessVersionEntity processVersion() {
        return new ProcessVersionEntity()
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessVersionStatus.Drafted)
                .setPublicTitle("Antrag");
    }

    private static GoverConfig goverConfig() {
        var config = new GoverConfig();
        config.setGoverHostname("https://example.test/");
        return config;
    }

    private static Specification<ProcessNodeEntity> anySpecification() {
        return any();
    }
}
