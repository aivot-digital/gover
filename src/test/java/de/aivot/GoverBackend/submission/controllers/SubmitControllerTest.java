package de.aivot.GoverBackend.submission.controllers;

import de.aivot.GoverBackend.av.services.AVService;
import de.aivot.GoverBackend.captcha.services.RedisCaptchaReplayGuard;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.SubmitStepElement;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.repositories.VFormVersionWithDetailsRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.ProcessInstanceService;
import de.aivot.GoverBackend.process.services.ProcessNodeService;
import de.aivot.GoverBackend.process.services.ProcessTestClaimService;
import de.aivot.GoverBackend.process.services.ProcessVersionService;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubmitControllerTest {
    @Mock
    private AVService avService;

    @Mock
    private ElementDerivationService elementDerivationService;

    @Mock
    private VFormVersionWithDetailsRepository formVersionWithDetailsRepository;

    @Mock
    private ProcessNodeService processNodeService;

    @Mock
    private ProcessInstanceService processInstanceService;

    @Mock
    private ProcessInstanceAttachmentService processInstanceAttachmentService;

    @Mock
    private ProcessVersionService processVersionService;

    @Mock
    private ProcessTestClaimService processTestClaimService;

    @Mock
    private ElementDataTransformService elementDataTransformService;

    @Mock
    private RedisCaptchaReplayGuard captchaReplayGuard;

    private SubmitController submitController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        submitController = new SubmitController(
                avService,
                elementDerivationService,
                formVersionWithDetailsRepository,
                processNodeService,
                processInstanceService,
                processInstanceAttachmentService,
                processVersionService,
                processTestClaimService,
                elementDataTransformService,
                captchaReplayGuard
        );
    }

    @Test
    void extractCaptchaPayloadShouldReturnNullWhenMissing() {
        var rootElement = new FormLayoutElement();
        var submitStepElement = new SubmitStepElement();
        submitStepElement.setId("submit-step");
        rootElement.setChildren(List.of(submitStepElement));

        assertNull(SubmitController.extractCaptchaPayload(rootElement, new EffectiveElementValues()));
    }

    @Test
    void testCaptchaReplayProtectionShouldRejectReusedPayloads() throws Exception {
        var submitStepElement = new SubmitStepElement();
        submitStepElement.setId("submit-step");
        var rootElement = new FormLayoutElement()
                .setChildren(List.of(submitStepElement));
        var form = new VFormVersionWithDetailsEntity()
                .setRootElement(rootElement);
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("submit-step", Map.of("payload", "payload"));

        when(captchaReplayGuard.isUsed("payload")).thenReturn(true);

        var exception = assertThrows(ResponseException.class, () ->
                submitController.testCaptchaReplayProtection(form, effectiveValues)
        );

        assertEquals("Die Captcha-Bestätigung wurde bereits verwendet. Bitte erneut bestätigen.", exception.getTitle());
    }

    @Test
    void testCaptchaReplayProtectionShouldSkipMissingPayloads() throws Exception {
        var form = new VFormVersionWithDetailsEntity()
                .setRootElement(new FormLayoutElement());

        submitController.testCaptchaReplayProtection(form, new EffectiveElementValues());

        verifyNoInteractions(captchaReplayGuard);
    }
}
