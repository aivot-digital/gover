package de.aivot.prosuna.backend.xrepository.models;

import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XRepositoryCodeListColumnSetColumnDataTest {
    @Test
    void ignoresUnknownXRepositoryDataAttributes() throws Exception {
        var xml = """
                <Data Type="normalizedString" Lang="de"/>
                """;

        var data = new XmlMapper()
                .readValue(xml, XRepositoryCodeListColumnSetColumnData.class);

        assertEquals("normalizedString", data.getType());
    }
}
