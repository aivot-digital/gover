package de.aivot.prosuna.backend.xrepository.models;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

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
