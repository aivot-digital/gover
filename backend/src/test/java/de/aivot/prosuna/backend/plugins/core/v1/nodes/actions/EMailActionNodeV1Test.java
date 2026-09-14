package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class EMailActionNodeV1Test {
    private final EMailActionNodeV1 node = new EMailActionNodeV1(
            mock(ProsunaConfig.class), mock(ProcessInstanceAttachmentService.class),
            mock(ProcessInstanceAttachmentSetService.class), mock(StorageService.class),
            mock(JavaMailSenderImpl.class), mock(AssignmentContextAssigneeResolverService.class)
    );

    @Test
    void shouldUseConfiguredDefaultsWithoutASavedDraft() throws Exception {
        var data = node.getStaffTaskViewData(context(Map.of()));

        assertEquals("Configured subject", data.getLiteral("subject"));
        assertEquals("<p>Configured body</p>", data.getLiteral("body"));
    }

    @Test
    void shouldIgnoreTopLevelRuntimeFieldsWhenNoCanonicalDraftExists() throws Exception {
        var context = context(Map.of("subject", "Legacy subject", "body", "Legacy body", "internalState", "waiting"));

        assertNull(node.getAutoSavedStaffTaskViewData(context));
        var data = node.getStaffTaskViewData(context);
        assertEquals("Configured subject", data.getLiteral("subject"));
        assertEquals("<p>Configured body</p>", data.getLiteral("body"));
        assertFalse(data.containsKey("internalState"));
    }

    @Test
    void shouldRoundTripCanonicalAutosaveIncludingAnExplicitlyClearedBody() throws Exception {
        var context = context(Map.of("internalState", "waiting"));
        var draft = new AuthoredElementValues().putLiteral("subject", "Edited subject").putLiteral("body", null);

        var result = node.onAutoSaveFromStaffTaskView(context, draft).orElseThrow();
        assertEquals(draft, result.getRuntimeData().get(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY));
        assertEquals("waiting", result.getRuntimeData().get("internalState"));

        var restored = node.getStaffTaskViewData(context(result.getRuntimeData()));
        assertEquals(draft, restored);
        assertNull(restored.getLiteral("body"));
    }

    private ProcessNodeExecutionContextUIStaff<EMailActionNodeV1.EMailActionNodeConfig> context(Map<String, Object> runtimeData) {
        var configuration = new EMailActionNodeV1.EMailActionNodeConfig();
        configuration.manualContent = new EMailActionNodeV1.EMailActionNodeConfigManualContent();
        configuration.manualContent.subject = "Configured subject";
        configuration.manualContent.content = "<p>Configured body</p>";
        var task = new ProcessInstanceTaskEntity().setRuntimeData(runtimeData).setNodeData(Map.of()).setProcessData(Map.of());
        return new ProcessNodeExecutionContextUIStaff<>(
                mock(ProcessNodeExecutionLogger.class), new ProcessNodeEntity(), new ProcessInstanceEntity(),
                task, null, new UserEntity(), configuration, new ProcessExecutionData()
        );
    }
}
