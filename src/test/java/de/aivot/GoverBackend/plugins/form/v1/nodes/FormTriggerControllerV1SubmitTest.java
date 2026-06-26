package de.aivot.GoverBackend.plugins.form.v1.nodes;

import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.av.services.AVService;
import de.aivot.GoverBackend.captcha.services.CaptchaReplayGuard;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.ElementDerivationRequest;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.form.services.FormPaymentService;
import de.aivot.GoverBackend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.GoverBackend.identity.controllers.IdentityController;
import de.aivot.GoverBackend.identity.models.IdentityDataMap;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.identity.services.IdentityService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.process.services.*;
import de.aivot.GoverBackend.storage.services.StorageProviderService;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import de.aivot.GoverBackend.system.services.SystemService;
import de.aivot.GoverBackend.theme.services.ThemeService;
import de.aivot.GoverBackend.user.services.UserService;
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
        var startedProcessAccessKey = UUID.randomUUID();
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
                UUID.randomUUID()
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
            UUID startedProcessAccessKey
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
        when(processVersionService.retrieve(any(de.aivot.GoverBackend.process.filters.ProcessVersionFilter.class)))
                .thenReturn(Optional.of(processVersion));

        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.retrieve(any(de.aivot.GoverBackend.process.filters.ProcessNodeFilter.class)))
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
        when(processTestClaimService.retrieve(any(de.aivot.GoverBackend.process.filters.ProcessTestClaimFilter.class)))
                .thenReturn(Optional.empty());

        var controller = new FormTriggerControllerV1(
                mock(GoverConfig.class),
                mock(FormPaymentService.class),
                mock(de.aivot.GoverBackend.payment.services.PaymentProviderService.class),
                mock(DestinationService.class),
                mock(IdentityProviderService.class),
                mock(IdentityCacheRepository.class),
                elementDerivationService,
                mock(AssetService.class),
                mock(ThemeService.class),
                mock(de.aivot.GoverBackend.department.services.VDepartmentShadowedService.class),
                mock(SystemService.class),
                userService,
                processService,
                processNodeService,
                processTestClaimService,
                processVersionService,
                processNodeDefinitionService,
                mock(SystemConfigService.class),
                mock(StorageProviderService.class),
                mock(AVService.class),
                mock(CaptchaReplayGuard.class),
                processInstanceService,
                mock(ProcessInstanceAttachmentService.class),
                fileUploadMultipartInputService,
                elementDataTransformService,
                mock(ProcessNodeExecutionLoggerFactory.class),
                provider,
                identityService
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
