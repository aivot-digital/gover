package de.aivot.gover.backend.javascript.providers;

import de.aivot.gover.backend.javascript.exceptions.JavascriptException;
import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.javascript.services.JavascriptEngine;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.plugins.core.v1.javascript.ProcessAttachmentSetJavascriptV1;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.gover.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentSetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessAttachmentSetJavascriptPluginTest {
    private static final Long PROCESS_INSTANCE_ID = 42L;
    private static final Long PROCESS_INSTANCE_TASK_ID = 9L;

    private ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private ProcessInstanceAttachmentService processInstanceAttachmentService;
    private ProcessInstanceRepository processInstanceRepository;
    private ProcessInstanceTaskRepository processInstanceTaskRepository;
    private ProcessAttachmentSetJavascriptV1 provider;

    @BeforeEach
    void setUp() {
        processInstanceAttachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
        processInstanceAttachmentService = mock(ProcessInstanceAttachmentService.class);
        processInstanceRepository = mock(ProcessInstanceRepository.class);
        processInstanceTaskRepository = mock(ProcessInstanceTaskRepository.class);
        provider = new ProcessAttachmentSetJavascriptV1(
                processInstanceAttachmentSetService,
                processInstanceAttachmentService,
                processInstanceRepository,
                processInstanceTaskRepository
        );
    }

    @Test
    void create_CreatesAttachmentSetForRunningProcessTask() throws Exception {
        arrangeRunningProcessTask(PROCESS_INSTANCE_ID, PROCESS_INSTANCE_TASK_ID);
        when(processInstanceAttachmentSetService.create(any(ProcessInstanceAttachmentSetEntity.class)))
                .thenAnswer(invocation -> invocation
                        .getArgument(0, ProcessInstanceAttachmentSetEntity.class)
                        .setId(321));

        try (var jsService = new JavascriptEngine(provider)) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    _attachments_v1.create(' generated.documents ', ' Generated documents ', 42, 9);
                    """));

            assertEquals(321, result.asNumber().intValue());
        }

        var attachmentSetCaptor = ArgumentCaptor.forClass(ProcessInstanceAttachmentSetEntity.class);
        verify(processInstanceAttachmentSetService).create(attachmentSetCaptor.capture());
        var attachmentSet = attachmentSetCaptor.getValue();
        assertEquals("generated.documents", attachmentSet.getDataKey());
        assertEquals("Generated documents", attachmentSet.getName());
        assertEquals(PROCESS_INSTANCE_ID, attachmentSet.getProcessInstanceId());
        assertEquals(PROCESS_INSTANCE_TASK_ID, attachmentSet.getProcessInstanceTaskId());
        assertEquals(List.of(
                "create(dataKey: string, name: string, processInstanceId: number, processInstanceTaskId: number): number;",
                "addAttachmentBase64(attachmentSetId: number, fileName: string, base64Content: string): { key: string; filename: string; originalFilename: string; position: number; attachmentSetId: number; processInstanceId: number; processInstanceTaskId: number | null; storageProviderId: number; storagePathFromRoot: string; };",
                "addAttachmentString(attachmentSetId: number, fileName: string, content: string): { key: string; filename: string; originalFilename: string; position: number; attachmentSetId: number; processInstanceId: number; processInstanceTaskId: number | null; storageProviderId: number; storagePathFromRoot: string; };"
        ), List.of(provider.getMethodTypeDefinitions()));
    }

    @Test
    void addAttachmentBase64_DecodesBase64AndUsesNextPosition() throws Exception {
        var attachmentKey = UUID.fromString("3891538b-9058-4c3f-bb5b-0e318c77c70f");
        arrangeAttachmentCreation(attachmentKey);

        try (var jsService = new JavascriptEngine(provider)) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    const attachment = _attachments_v1.addAttachmentBase64(321, 'report.txt', 'SGVsbG8=');
                    [
                        attachment.key,
                        attachment.filename,
                        attachment.originalFilename,
                        attachment.position,
                        attachment.attachmentSetId,
                        attachment.processInstanceId,
                        attachment.processInstanceTaskId,
                        attachment.storageProviderId,
                        attachment.storagePathFromRoot
                    ];
                    """));
            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals(attachmentKey.toString(), values.get(0));
            assertEquals("report.txt", values.get(1));
            assertEquals("report.txt", values.get(2));
            assertEquals(4, ((Number) values.get(3)).intValue());
            assertEquals(321, ((Number) values.get(4)).intValue());
            assertEquals(42, ((Number) values.get(5)).intValue());
            assertEquals(9, ((Number) values.get(6)).intValue());
            assertEquals(5, ((Number) values.get(7)).intValue());
            assertEquals("/proc/attachments/file.txt", values.get(8));
        }

        var attachmentCaptor = ArgumentCaptor.forClass(ProcessInstanceAttachmentEntity.class);
        verify(processInstanceAttachmentService).create(attachmentCaptor.capture());
        var attachment = attachmentCaptor.getValue();
        assertEquals("report.txt", attachment.getFileName());
        assertEquals("report.txt", attachment.getOriginalFileName());
        assertEquals(4, attachment.getPosition());
        assertEquals(321, attachment.getAttachmentSetId());
        assertEquals(PROCESS_INSTANCE_ID, attachment.getProcessInstanceId());
        assertEquals(PROCESS_INSTANCE_TASK_ID, attachment.getProcessInstanceTaskId());
        assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_8), attachment.getFileBytes());
    }

    @Test
    void addAttachmentString_UsesUtf8ContentAndNextPosition() throws Exception {
        var attachmentKey = UUID.fromString("71bc1d08-1e4e-4a74-93c3-e8c2ec3212bf");
        arrangeAttachmentCreation(attachmentKey);

        try (var jsService = new JavascriptEngine(provider)) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    const attachment = _attachments_v1.addAttachmentString(321, 'report.txt', ' Gr\\u00fc\\u00dfe ');
                    [
                        attachment.key,
                        attachment.filename,
                        attachment.originalFilename,
                        attachment.position
                    ];
                    """));
            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals(attachmentKey.toString(), values.get(0));
            assertEquals("report.txt", values.get(1));
            assertEquals("report.txt", values.get(2));
            assertEquals(4, ((Number) values.get(3)).intValue());
        }

        var attachmentCaptor = ArgumentCaptor.forClass(ProcessInstanceAttachmentEntity.class);
        verify(processInstanceAttachmentService).create(attachmentCaptor.capture());
        var attachment = attachmentCaptor.getValue();
        assertEquals("report.txt", attachment.getFileName());
        assertEquals("report.txt", attachment.getOriginalFileName());
        assertEquals(4, attachment.getPosition());
        assertArrayEquals(" Gr\u00fc\u00dfe ".getBytes(StandardCharsets.UTF_8), attachment.getFileBytes());
    }

    @Test
    void create_FailsWhenTaskDoesNotBelongToProcessInstance() {
        when(processInstanceRepository.findById(PROCESS_INSTANCE_ID))
                .thenReturn(Optional.of(processInstance(ProcessInstanceStatus.Running)));
        when(processInstanceTaskRepository.findById(PROCESS_INSTANCE_TASK_ID))
                .thenReturn(Optional.of(task(100L, ProcessTaskStatus.Running)));

        try (var jsService = new JavascriptEngine(provider)) {
            assertThrows(
                    JavascriptException.class,
                    () -> jsService.evaluateCode(new JavascriptCode().setCode("_attachments_v1.create('docs', 'Docs', 42, 9);"))
            );
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void arrangeAttachmentCreation(UUID attachmentKey) throws ResponseException {
        arrangeRunningProcessTask(PROCESS_INSTANCE_ID, PROCESS_INSTANCE_TASK_ID);
        when(processInstanceAttachmentSetService.retrieve(321))
                .thenReturn(Optional.of(new ProcessInstanceAttachmentSetEntity()
                        .setId(321)
                        .setDataKey("generated.documents")
                        .setName("Generated documents")
                        .setProcessInstanceId(PROCESS_INSTANCE_ID)
                        .setProcessInstanceTaskId(PROCESS_INSTANCE_TASK_ID)));
        when(processInstanceAttachmentService.findAllByAttachmentSetId(321))
                .thenReturn(List.of(
                        new ProcessInstanceAttachmentEntity().setPosition(1),
                        new ProcessInstanceAttachmentEntity().setPosition(3)
                ));
        when(processInstanceAttachmentService.create(any(ProcessInstanceAttachmentEntity.class)))
                .thenAnswer(invocation -> invocation
                        .getArgument(0, ProcessInstanceAttachmentEntity.class)
                        .setKey(attachmentKey)
                        .setStorageProviderId(5)
                        .setStoragePathFromRoot("/proc/attachments/file.txt"));
    }

    private void arrangeRunningProcessTask(Long processInstanceId,
                                           Long processInstanceTaskId) {
        when(processInstanceRepository.findById(processInstanceId))
                .thenReturn(Optional.of(processInstance(ProcessInstanceStatus.Running)));
        when(processInstanceTaskRepository.findById(processInstanceTaskId))
                .thenReturn(Optional.of(task(processInstanceId, ProcessTaskStatus.Running)));
    }

    private static ProcessInstanceEntity processInstance(ProcessInstanceStatus status) {
        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setStatus(status);
    }

    private static ProcessInstanceTaskEntity task(Long processInstanceId,
                                                  ProcessTaskStatus status) {
        return new ProcessInstanceTaskEntity()
                .setId(PROCESS_INSTANCE_TASK_ID)
                .setProcessInstanceId(processInstanceId)
                .setStatus(status);
    }
}
