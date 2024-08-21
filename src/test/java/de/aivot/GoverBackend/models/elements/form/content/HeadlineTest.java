package de.aivot.GoverBackend.models.elements.form.content;

import de.aivot.GoverBackend.models.elements.AbstractElementTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeadlineTest extends AbstractElementTest<Headline> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>() {{
            put("content", "content");
            put("small", true);
        }};
    }

    @Override
    protected Headline newItem(Map<String, Object> json) {
        return new Headline(json);
    }

    @Override
    protected void testAllFieldsFilled(Headline item) {
        assertEquals("content", item.getContent());
        assertEquals(true, item.getSmall());
    }

    @Override
    protected void testAllFieldsNull(Headline item) {
        assertEquals("", item.getContent());
        assertEquals(false, item.getSmall());
    }

    @Test
    void toPdfRows() {
        var item = getItem();

        // var rows = item.toPdfRows(new HashMap<>(), null, );
        // assertEquals(1, rows.size());
        // assertInstanceOf(HeadlinePdfRowDto.class, rows.get(0));
        // assertEquals("content", ((HeadlinePdfRowDto) rows.get(0)).text);
        // assertEquals(Headline.HEADLINE_SIZE_SMALL, ((HeadlinePdfRowDto) rows.get(0)).size);
    }
}
