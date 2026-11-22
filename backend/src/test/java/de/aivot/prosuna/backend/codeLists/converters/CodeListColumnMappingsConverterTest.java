package de.aivot.prosuna.backend.codeLists.converters;

import de.aivot.prosuna.backend.codeLists.models.CodeListColumnMapping;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeListColumnMappingsConverterTest {
    private final CodeListColumnMappingsConverter converter = new CodeListColumnMappingsConverter(JsonMapper.builder().build());

    @Test
    void readsMappingsWrittenByTheMigration() {
        var mappings = converter.convertToEntityAttribute("[{\"key\": \"code\", \"name\": \"Code\", \"index\": 1}]");

        assertEquals(List.of(new CodeListColumnMapping("code", "Code", 1)), mappings);
    }

    @Test
    void keepsTheFormerSourceColumnOfAMissingColumn() {
        var mappings = List.of(
                new CodeListColumnMapping("code", "Code", 1),
                new CodeListColumnMapping("label", "Bezeichnung", null, "name")
        );

        assertEquals(mappings, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(mappings)));
    }
}
