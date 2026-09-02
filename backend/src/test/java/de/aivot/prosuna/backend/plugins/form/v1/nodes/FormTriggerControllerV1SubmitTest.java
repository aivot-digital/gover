package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.av.services.AVService;
import de.aivot.prosuna.backend.captcha.services.CaptchaReplayGuard;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementStates;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationLogger;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.prosuna.backend.identity.controllers.IdentityController;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.plugins.form.v1.services.FormPaymentService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.filters.ProcessTestClaimFilter;
import de.aivot.prosuna.backend.process.filters.ProcessVersionFilter;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.services.PdfService;
import de.aivot.prosuna.backend.storage.services.StorageProviderService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FormTriggerControllerV1SubmitTest {
    @Test
    void submitShouldClearIdentityCookieAfterSuccessfulSubmission() throws Exception {
        var identities = new IdentityDataMap();
        var startedProcessAccessKey = UUID.randomUUID().toString();
        var derivedRuntimeElementData = new DerivedRuntimeElementData();
        var fixture = createFixture(derivedRuntimeElementData, identities, startedProcessAccessKey);
        var response = new MockHttpServletResponse();

        var result = fixture.controller().submit(
                null,
                fixture.processSlug(),
                fixture.formSlug(),
                null,
                "identity-session-id",
                "{}",
                null,
                null,
                response
        );

        assertEquals(startedProcessAccessKey, result.startedProcessAccessKey());
        assertClearsIdentityCookie(response);
        verify(fixture.identityService()).getIdentityDataMap("identity-session-id", fixture.processNodeId());
        verify(fixture.elementDerivationService(), times(2)).derive(
                any(ElementDerivationRequest.class),
                same(identities),
                any(ElementDerivationLogger.class)
        );
        verify(fixture.elementDataTransformService()).buildPayload(
                any(FormLayoutElement.class),
                same(derivedRuntimeElementData.getEffectiveValues()),
                same(derivedRuntimeElementData.getElementStates())
        );
        var createdInstanceCaptor = ArgumentCaptor.forClass(ProcessInstanceEntity.class);
        verify(fixture.processInstanceService()).create(createdInstanceCaptor.capture());
        var updatedInstanceCaptor = ArgumentCaptor.forClass(ProcessInstanceEntity.class);
        verify(fixture.processInstanceService()).update(eq(1L), updatedInstanceCaptor.capture());
        assertEquals(
                Map.of("mapped", "payload"),
                updatedInstanceCaptor.getValue().getInitialPayload().get(FormTriggerNodeV1.DATA_KEY_PAYLOAD)
        );
        assertEquals(
                createdInstanceCaptor.getValue().getStarted(),
                updatedInstanceCaptor.getValue().getInitialPayload().get(FormTriggerNodeV1.DATA_KEY_STARTED)
        );
    }

    @Test
    void submitShouldNotClearIdentityCookieWhenDerivationFails() throws Exception {
        var identities = new IdentityDataMap();
        var fixture = createFixture(
                new DerivedRuntimeElementData().putError("field", "error"),
                identities,
                UUID.randomUUID().toString()
        );
        var response = new MockHttpServletResponse();

        assertThrows(ResponseException.class, () -> fixture.controller().submit(
                null,
                fixture.processSlug(),
                fixture.formSlug(),
                null,
                "identity-session-id",
                "{}",
                null,
                null,
                response
        ));

        assertEquals(0, response.getCookies().length);
        verify(fixture.identityService()).getIdentityDataMap("identity-session-id", fixture.processNodeId());
        verify(fixture.processInstanceService(), never()).create(any(ProcessInstanceEntity.class));
        verify(fixture.processInstanceService(), never()).update(anyLong(), any(ProcessInstanceEntity.class));
    }

    private SubmitFixture createFixture(
            DerivedRuntimeElementData derivedRuntimeElementData,
            IdentityDataMap identities,
            String startedProcessAccessKey
    ) throws ResponseException {
        var processAccessKey = UUID.randomUUID();
        var processSlug = "example-process";
        var formSlug = "example-form";

        var process = new ProcessEntity()
                .setId(100)
                .setInternalTitle("Process")
                .setDepartmentId(10)
                .setAccessKey(processAccessKey)
                .setSlug(processSlug)
                .setVersionCount(1)
                .setPublishedVersion(1);

        var processVersion = new ProcessVersionEntity()
                .setProcessId(process.getId())
                .setProcessVersion(1)
                .setStatus(ProcessVersionStatus.Published);

        var node = new ProcessNodeEntity()
                .setId(500)
                .setProcessId(process.getId())
                .setProcessVersion(processVersion.getProcessVersion())
                .setProcessNodeDefinitionKey("form.form")
                .setProcessNodeDefinitionVersion(1);

        var userService = mock(UserService.class);
        when(userService.fromJWT(isNull())).thenReturn(Optional.empty());

        var processService = mock(ProcessService.class);
        when(processService.retrieveBySlugOrHistory(processSlug)).thenReturn(Optional.of(process));

        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.retrieve(any(ProcessVersionFilter.class)))
                .thenReturn(Optional.of(processVersion));

        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.retrieve(any(ProcessNodeFilter.class)))
                .thenReturn(Optional.of(node));

        var provider = mock(FormTriggerNodeV1.class);
        when(provider.getKey()).thenReturn("form.form");

        var triggerConfig = new FormTriggerConfigV1();
        triggerConfig.formSlug = formSlug;
        triggerConfig.formLayout = new FormLayoutElement().setPublicTitle("Example form");

        when(processNodeService.deriveConfiguration(eq(node), eq(provider), isNull(), eq(true)))
                .thenReturn(new ProcessNodeService.ProcessConfigurationDetails<>(
                        triggerConfig,
                        new DerivedRuntimeElementData()
                ));

        var processNodeDefinitionService = mock(ProcessNodeDefinitionService.class);
        when(processNodeDefinitionService.getProcessNodeDefinition(eq(node), eq(FormTriggerNodeV1.class)))
                .thenReturn(Optional.of(provider));

        var identityService = mock(IdentityService.class);
        when(identityService.getIdentityDataMap("identity-session-id", node.getId())).thenReturn(identities);

        var elementDerivationService = mock(ElementDerivationService.class);
        when(elementDerivationService.derive(
                any(ElementDerivationRequest.class),
                same(identities),
                any(ElementDerivationLogger.class)
        )).thenReturn(derivedRuntimeElementData);

        var processInstanceService = mock(ProcessInstanceService.class);
        when(processInstanceService.create(any(ProcessInstanceEntity.class))).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, ProcessInstanceEntity.class);
            entity.setId(1L);
            entity.setCaseNumber("CASE-1");
            entity.setAccessKey(startedProcessAccessKey);
            return entity;
        });
        when(processInstanceService.update(eq(1L), any(ProcessInstanceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(1, ProcessInstanceEntity.class));

        var elementDataTransformService = mock(ElementDataTransformService.class);
        when(elementDataTransformService.buildPayload(
                any(FormLayoutElement.class),
                any(EffectiveElementValues.class),
                any(ComputedElementStates.class)
        )).thenReturn(Map.of("mapped", "payload"));

        var fileUploadMultipartInputService = mock(FileUploadMultipartInputService.class);
        when(fileUploadMultipartInputService.normalizeInputs(
                any(FormLayoutElement.class),
                any(AuthoredElementValues.class),
                isNull(),
                isNull(),
                eq(1L),
                isNull(),
                isNull()
        )).thenReturn(new FileUploadMultipartInputService.NormalizationResult(
                new AuthoredElementValues(),
                List.of()
        ));

        var processTestClaimService = mock(ProcessTestClaimService.class);
        when(processTestClaimService.retrieve(any(ProcessTestClaimFilter.class)))
                .thenReturn(Optional.empty());

        var controller = new FormTriggerControllerV1(
                mock(ProsunaConfig.class),
                mock(IdentityProviderService.class),
                elementDerivationService,
                mock(AssetService.class),
                mock(ThemeService.class),
                mock(VDepartmentShadowedService.class),
                mock(SystemService.class),
                userService,
                processService,
                processNodeService,
                processTestClaimService,
                processVersionService,
                processNodeDefinitionService,
                mock(SystemConfigService.class),
                mock(StorageProviderService.class),
                mock(CaptchaReplayGuard.class),
                processInstanceService,
                mock(ProcessInstanceTaskService.class),
                mock(ProcessInstanceAttachmentSetService.class),
                mock(ProcessInstanceAttachmentService.class),
                mock(StorageService.class),
                fileUploadMultipartInputService,
                elementDataTransformService,
                mock(ProcessNodeExecutionLoggerFactory.class),
                provider,
                identityService,
                mock(PaymentPayloadCreationService.class),
                mock(PaymentTransactionService.class),
                mock(PaymentProviderRepository.class),
                mock(PdfService.class),
                mock(PaymentProviderDefinitionsService.class)
        );

        return new SubmitFixture(
                controller,
                processSlug,
                formSlug,
                node.getId(),
                identityService,
                elementDerivationService,
                elementDataTransformService,
                processInstanceService
        );
    }

    private static void assertClearsIdentityCookie(MockHttpServletResponse response) {
        var cookie = Arrays
                .stream(response.getCookies())
                .filter(candidate -> IdentityController.IDENTITY_COOKIE_NAME.equals(candidate.getName()))
                .filter(candidate -> IdentityController.IDENTITY_COOKIE_PATH.equals(candidate.getPath()))
                .findFirst()
                .orElse(null);

        assertNotNull(cookie);
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
    }

    private record SubmitFixture(
            FormTriggerControllerV1 controller,
            String processSlug,
            String formSlug,
            Integer processNodeId,
            IdentityService identityService,
            ElementDerivationService elementDerivationService,
            ElementDataTransformService elementDataTransformService,
            ProcessInstanceService processInstanceService
    ) {
    }
}
