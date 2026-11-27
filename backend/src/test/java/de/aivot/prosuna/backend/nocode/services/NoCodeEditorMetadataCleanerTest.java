package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.core.converters.AuthoredElementValuesConverter;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.input.NoCodeAuthoredInputValue;
import de.aivot.prosuna.backend.nocode.models.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoCodeEditorMetadataCleanerTest {
    private final tools.jackson.databind.json.JsonMapper mapper = JsonMapperTestUtils.createMapper();
    private final NoCodeEditorMetadataCleaner cleaner = new NoCodeEditorMetadataCleaner(mapper);

    private NoCodeOperand expression() {
        return mapper.readValue("""
                {"type":"NoCodeExpression","operatorIdentifier":"test","operands":[
                    {"type":"NoCodeStaticValue","value":"kept","editorMetadata":{"notes":"child-secret"}},
                    {"type":"NoCodeStaticValue","value":" "}
                ],"editorMetadata":{"notes":"root-secret","testData":{"version":1,
                    "elementValues":{
                        "empty":{"text":"","type":"String","mode":"empty"},
                        "missing":{"text":"","type":"String","mode":"missing"},
                        "null":{"text":"","type":"String","mode":"null"},
                        "number":{"text":"0","type":"Number","mode":"value"},
                        "bool":{"text":"false","type":"Boolean","mode":"value"}
                    },"sourceValues":{"instanceData":{"mode":"json","json":"{unfinished-secret"}},
                    "visibility":{"field":"false"}
                }}}
                """, NoCodeOperand.class);
    }

    @Test
    void persistsExamplesAndNotesThroughTheActualConfigurationConverterAndCopiesThem() {
        var original = expression();
        var configuration = new AuthoredElementValues();
        configuration.put("rule", new NoCodeAuthoredInputValue(original));
        configuration.putLiteral("nested", List.of(Map.of("rule", original)));
        var converter = new AuthoredElementValuesConverter(mapper);
        var restored = converter.convertToEntityAttribute(converter.convertToDatabaseColumn(configuration));
        var operand = ((NoCodeAuthoredInputValue) restored.get("rule")).operand();
        assertEquals(original, operand);
        var data = operand.getEditorMetadata().testData();
        assertEquals("{unfinished-secret", data.sourceValues().get(NoCodeTestData.Source.instanceData).json());
        assertEquals("empty", data.elementValues().get("empty").mode());
        assertEquals("null", data.elementValues().get("null").mode());
        assertEquals("missing", data.elementValues().get("missing").mode());
        assertEquals("0", data.elementValues().get("number").text());
        assertEquals("false", data.elementValues().get("bool").text());
        var copy = configuration.clone();
        ((NoCodeAuthoredInputValue) copy.get("rule")).operand().setEditorMetadata(null);
        assertNotNull(original.getEditorMetadata());
        assertThrows(UnsupportedOperationException.class, () -> data.elementValues().clear());
    }

    @Test
    void cleansNestedTypedAndUntypedOperandsWithoutMutatingOriginalsOrRemovingUnrelatedNotes() {
        var original = expression();
        var configuration = new AuthoredElementValues();
        configuration.put("direct", new NoCodeAuthoredInputValue(original));
        configuration.putLiteral("layout", Map.of("visibility", Map.of("noCode", mapper.convertValue(original, Map.class))));
        configuration.putLiteral("ordinary", Map.of("notes", "keep me", "editorMetadata", "ordinary domain value"));
        var cleaned = cleaner.cleanConfiguration(configuration);
        var json = mapper.writeValueAsString(cleaned);
        var tree = mapper.readTree(json);
        assertFalse(json.contains("secret"));
        assertTrue(json.contains("kept"));
        assertEquals(" ", tree.at("/direct/operand/operands/1/value").asString());
        assertEquals(" ", tree.at("/layout/value/visibility/noCode/operands/1/value").asString());
        assertTrue(json.contains("keep me"));
        assertTrue(json.contains("ordinary domain value"));
        assertTrue(mapper.writeValueAsString(configuration).contains("root-secret"));
        assertNull(((NoCodeAuthoredInputValue) cleaned.get("direct")).operand().getEditorMetadata());
        assertNull(((NoCodeExpression) ((NoCodeAuthoredInputValue) cleaned.get("direct")).operand()).getOperands().iterator().next().getEditorMetadata());
    }

    @Test
    void legacyOperandsRemainCompatibleAndAuthoringDataDoesNotAffectEvaluation() {
        var reference = mapper.readValue("{\"type\":\"NoCodeInstanceDataReference\",\"path\":\"caseNumber\"}", NoCodeOperand.class);
        assertNull(reference.getEditorMetadata());
        assertFalse(mapper.writeValueAsString(reference).contains("editorMetadata"));
        var value = new NoCodeStaticValue("same");
        value.setEditorMetadata(expression().getEditorMetadata());
        var evaluator = new NoCodeEvaluationService(List.of());
        assertEquals("same", evaluator.evaluate(value, new de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData()).getValue());
    }
}
