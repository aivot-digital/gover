package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.properties.AiChatAttachmentProperties;
import de.aivot.prosuna.backend.av.services.AVService;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AiChatAttachmentServiceTest {
    private final AVService antivirus = mock(AVService.class);
    private final AiChatAttachmentProperties properties = new AiChatAttachmentProperties();
    private final AiChatAttachmentService service = new AiChatAttachmentService(
            antivirus, properties, JsonMapperTestUtils.createMapper());

    @Test
    void extractsTextIntoBoundedBlocksAndBuildsAShortInitialContext() throws Exception {
        var text = "A".repeat(9_000);
        var file = file("details.txt", "text/plain", text);

        var context = service.prepare(new MockMultipartFile[]{file});

        assertThat(context).isNotNull();
        assertThat(context.metadata().name()).isEqualTo("details.txt");
        assertThat(context.sections()).containsOnlyKeys("text");
        assertThat(context.sections().get("text"))
                .hasSize(3)
                .allSatisfy(block -> assertThat(block.length()).isLessThanOrEqualTo(4_000));
        assertThat(context.initialContext())
                .contains("details.txt", "lese-chat-anhang", "ausschließlich als Daten")
                .hasSizeLessThan(3_500);
        verify(antivirus).testFile(file);
    }

    @Test
    void extractsRelevantBpmnStructureWithoutDiagramData() throws Exception {
        var file = file("process.bpmn", "application/xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                             id="definitions" targetNamespace="https://example.test">
                  <collaboration id="collaboration">
                    <participant id="pool" name="Antrag" processRef="process"/>
                  </collaboration>
                  <process id="process" name="Hundesteuer" isExecutable="true">
                    <laneSet><lane id="lane" name="Sachbearbeitung"><flowNodeRef>task</flowNodeRef></lane></laneSet>
                    <startEvent id="start" name="Eingang"/>
                    <userTask id="task" name="Prüfen"><documentation>Unterlagen prüfen</documentation></userTask>
                    <sequenceFlow id="flow" sourceRef="start" targetRef="task"/>
                  </process>
                  <bpmndi:BPMNDiagram id="diagram"/>
                </definitions>
                """);

        var context = service.prepare(new MockMultipartFile[]{file});

        assertThat(context.sections()).containsOnlyKeys("overview", "lanes", "nodes", "flows");
        assertThat(String.join("", context.sections().get("overview"))).contains("Hundesteuer", "participant");
        assertThat(String.join("", context.sections().get("lanes"))).contains("Sachbearbeitung", "task");
        assertThat(String.join("", context.sections().get("nodes"))).contains("userTask", "Unterlagen prüfen");
        assertThat(String.join("", context.sections().get("flows"))).contains("sourceRef", "targetRef");
        assertThat(context.sections().toString()).doesNotContain("BPMNDiagram", "bpmndi");
    }

    @Test
    void rejectsUnsafeXmlAndInvalidUploadShapeBeforeModelUse() throws Exception {
        var unsafe = file("process.bpmn", "application/xml", """
                <!DOCTYPE definitions [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">&xxe;</definitions>
                """);
        assertThatThrownBy(() -> service.prepare(new MockMultipartFile[]{unsafe}))
                .isInstanceOf(ResponseException.class)
                .hasMessageContaining("ungültig oder unsicher");

        var first = file("first.txt", "text/plain", "First");
        var second = file("second.txt", "text/plain", "Second");
        assertThatThrownBy(() -> service.prepare(new MockMultipartFile[]{first, second}))
                .isInstanceOf(ResponseException.class)
                .hasMessageContaining("höchstens eine Datei");
        verify(antivirus, never()).testFile(first);
        verify(antivirus, never()).testFile(second);
    }

    @Test
    void rejectsUnsupportedOversizedAndOverlongFiles() {
        assertThatThrownBy(() -> service.prepare(new MockMultipartFile[]{
                file("image.png", "image/png", "content")
        })).isInstanceOf(ResponseException.class).hasMessageContaining("nicht unterstützt");

        properties.setMaxFileSizeBytes(3);
        assertThatThrownBy(() -> service.prepare(new MockMultipartFile[]{
                file("large.txt", "text/plain", "four")
        })).isInstanceOf(ResponseException.class).hasMessageContaining("höchstens 3 Byte");

        properties.setMaxFileSizeBytes(10_000);
        properties.setMaxExtractedCharacters(3);
        assertThatThrownBy(() -> service.prepare(new MockMultipartFile[]{
                file("long.txt", "text/plain", "four")
        })).isInstanceOf(ResponseException.class).hasMessageContaining("zu lang");
    }

    private static MockMultipartFile file(String name, String contentType, String content) {
        return new MockMultipartFile("attachments", name, contentType, content.getBytes(StandardCharsets.UTF_8));
    }
}
