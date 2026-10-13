package de.aivot.prosuna.backend.process.dtos;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessNodePortRequestDTOTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            {}                                                        | false | false
            {"forImport": false}                                      | false | false
            {"forImport": true}                                       | true  | false
            {"useInitialConfiguration": false}                        | false | false
            {"useInitialConfiguration": true}                         | false | true
            {"forImport": null, "useInitialConfiguration": null}       | false | false
            {"forImport": null, "useInitialConfiguration": true}       | false | true
            {"forImport": true, "useInitialConfiguration": null}       | true  | false
            {"forImport": false, "useInitialConfiguration": false}     | false | false
            {"forImport": false, "useInitialConfiguration": true}      | false | true
            {"forImport": true, "useInitialConfiguration": false}      | true  | false
            {"forImport": true, "useInitialConfiguration": true}       | true  | true
            """)
    void deserializesOptionalFlagsWithFalseDefaults(String options, boolean forImport, boolean useInitialConfiguration) {
        var mapper = JsonMapperTestUtils.createMapper();
        var payload = mapper.readValue(options, ObjectNode.class);
        payload.set("node", mapper.readTree("""
                {
                    "id": 0,
                    "processId": 1,
                    "processVersion": 1,
                    "processNodeDefinitionKey": "test.node",
                    "processNodeDefinitionVersion": 1,
                    "dataKey": "test",
                    "configuration": {},
                    "outputMappings": {},
                    "deadlines": {},
                    "savedWithErrors": false,
                    "created": "",
                    "updated": ""
                }
                """));

        var request = mapper.readValue(payload.toString(), ProcessNodePortRequestDTO.class);

        assertEquals(forImport, request.forImport());
        assertEquals(useInitialConfiguration, request.useInitialConfiguration());
        assertEquals("test", request.node().getDataKey());
    }
}
