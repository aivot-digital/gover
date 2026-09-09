package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.input.InputVariableReference;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputVariableResolverTest {
    private final InputVariableResolver resolver = new InputVariableResolver();

    @Test
    void shouldResolveAllSupportedSourcesAndListSegments() {
        var processData = new ProcessExecutionData()
                .addProcessData("people", List.of(Map.of("name", "Ada")))
                .addProcessMetadata(Map.of(
                        "caseNumber", "A-42",
                        "taskMetadata", Map.of("review", Map.of("finished", true))
                ));
        processData.getNodeData().put("review", Map.of("score", 7));

        assertEquals("Ada", resolve(processData, InputVariableSource.ProcessData, "people.0.name", null).value());
        assertEquals("A-42", resolve(processData, InputVariableSource.ProtectedProcessData, "caseNumber", null).value());
        assertEquals(7, resolve(processData, InputVariableSource.ElementData, "score", "review").value());
        assertEquals(true, resolve(processData, InputVariableSource.ElementMetadata, "finished", "review").value());
    }

    @Test
    void shouldDistinguishAnExplicitNullFromAMissingPath() {
        var processData = new ProcessExecutionData().addProcessData("optional", null);

        var explicitNull = resolve(processData, InputVariableSource.ProcessData, "optional", null);
        var missing = resolve(processData, InputVariableSource.ProcessData, "missing", null);

        assertTrue(explicitNull.found());
        assertNull(explicitNull.value());
        assertFalse(missing.found());
        assertNull(missing.value());
    }

    @Test
    void shouldRejectInvalidReferenceShapes() {
        assertThrows(IllegalArgumentException.class, () -> resolver.validate(reference(
                InputVariableSource.ElementData,
                "result",
                null
        )));
        assertThrows(IllegalArgumentException.class, () -> resolver.validate(reference(
                InputVariableSource.ProcessData,
                "items.*.name",
                null
        )));
        assertThrows(IllegalArgumentException.class, () -> resolver.validate(reference(
                InputVariableSource.ProcessData,
                "person.name",
                "node"
        )));
    }

    @Test
    void shouldNormalizeReferenceWhitespaceBeforeResolution() {
        var processData = new ProcessExecutionData()
                .addProcessData("people", List.of(Map.of("name", "Ada")));
        var reference = reference(InputVariableSource.ProcessData, " people . 0 . name ", null);

        assertEquals("people.0.name", reference.path());
        assertEquals("Ada", resolver.resolve(reference, processData).value());
    }

    private InputVariableResolver.Resolution resolve(ProcessExecutionData data,
                                                     InputVariableSource source,
                                                     String path,
                                                     String nodeDataKey) {
        return resolver.resolve(reference(source, path, nodeDataKey), data);
    }

    private InputVariableReference reference(InputVariableSource source, String path, String nodeDataKey) {
        return new InputVariableReference(source, path, nodeDataKey);
    }
}
