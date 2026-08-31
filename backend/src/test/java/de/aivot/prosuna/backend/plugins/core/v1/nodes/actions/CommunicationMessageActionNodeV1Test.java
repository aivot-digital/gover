package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessIdentityIdInputElement;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunicationMessageActionNodeV1Test {
    @Test
    void configurationUsesProcessIdentityIdInputElement() throws Exception {
        var node = createNode(mock(CommunicationService.class), mock(TemplateRenderService.class));

        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                mock(ProcessEntity.class),
                mock(ProcessVersionEntity.class),
                mock(ProcessNodeEntity.class)
        ));

        assertTrue(layout.getChildren().stream()
                .anyMatch(ProcessIdentityIdInputElement.class::isInstance));
    }

    @Test
    void initSendsMessageToConfiguredIdentity() throws Exception {
        var communicationService = mock(CommunicationService.class);
        var templateRenderService = mock(TemplateRenderService.class);
        var node = createNode(communicationService, templateRenderService);
        var configuration = new CommunicationMessageActionNodeV1.Configuration();
        configuration.identityId = "applicant";
        configuration.subject = "Subject {{ $.caseNumber }}";
        configuration.body = "Hello";

        var identity = new IdentityData(
                "session", "applicant", IdentityType.IdentityProvider, UUID.randomUUID(), "metadata", null,
                Map.of(), 5, Map.of()
        );
        var identities = new IdentityDataMap();
        identities.put("applicant", identity);
        var processInstance = mock(ProcessInstanceEntity.class);
        when(processInstance.getIdentities()).thenReturn(identities);

        var executionData = new ProcessExecutionData();
        var context = mock(ProcessNodeExecutionInitContext.class);
        when(context.getConfigurationOfExecutingNode()).thenReturn(configuration);
        when(context.getThisProcessInstance()).thenReturn(processInstance);
        when(context.getCurrentProcessExecutionData()).thenReturn(executionData);
        when(templateRenderService.interpolate(eq(executionData), eq(configuration.subject))).thenReturn("Subject 123");
        when(templateRenderService.interpolate(eq(executionData), eq(configuration.body))).thenReturn("Hello");

        node.init(context);

        verify(communicationService).sendMessage(eq(identity), any(CommunicationMessage.class));
    }

    private static CommunicationMessageActionNodeV1 createNode(CommunicationService communicationService,
                                                               TemplateRenderService templateRenderService) {
        return new CommunicationMessageActionNodeV1(
                communicationService,
                templateRenderService,
                mock(ProcessInstanceAttachmentSetService.class),
                mock(ProcessInstanceAttachmentService.class),
                mock(StorageService.class)
        );
    }
}
