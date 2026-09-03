package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessIdentityIdInputElement;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunicationMessageActionNodeV1Test {
    @Test
    void metadataExposesAutomaticExecutionAndTypedOutputs() {
        var node = createNode(mock(TemplateRenderService.class));

        assertArrayEquals(
                new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic},
                node.getExecutionTypes()
        );
        assertFalse(node.getAbstract().isBlank());
        assertEquals(
                java.util.List.of(
                        "string",
                        "number | null",
                        "string",
                        "string",
                        "Array<string>",
                        "string",
                        "Record<string, unknown>"
                ),
                node.getOutputs().stream().map(output -> output.typeDefinition()).toList()
        );
    }

    @Test
    void configurationUsesProcessIdentityIdInputElement() throws Exception {
        var node = createNode(mock(TemplateRenderService.class));

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
    void initReturnsCommunicationRequestForConfiguredIdentity() throws Exception {
        var templateRenderService = mock(TemplateRenderService.class);
        var node = createNode(templateRenderService);
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

        var result = node.init(context);

        var communicationRequest = result.getCommunicationRequest();
        assertNotNull(communicationRequest);
        assertEquals("applicant", communicationRequest.recipientIdentityId());
        assertEquals("sendResult", communicationRequest.nodeDataOutputKey());
        assertEquals("Subject 123", communicationRequest.message().subject());
        assertEquals("Hello", communicationRequest.message().body());
        assertEquals("Hello", communicationRequest.message().htmlBody());
        assertEquals(result.getNodeData().get("sentAt"), communicationRequest.message().timestamp());
        assertEquals(5, result.getNodeData().get("communicationProviderBindingId"));
    }

    private static CommunicationMessageActionNodeV1 createNode(TemplateRenderService templateRenderService) {
        return new CommunicationMessageActionNodeV1(
                templateRenderService,
                mock(ProcessInstanceAttachmentSetService.class),
                mock(ProcessInstanceAttachmentService.class),
                mock(StorageService.class)
        );
    }
}
