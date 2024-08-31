package de.aivot.GoverBackend.models.elements.form.input;


import de.aivot.GoverBackend.models.elements.AbstractElementTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class CheckboxFieldTest extends AbstractElementTest<CheckboxField> {
    @Override
    protected Map<String, Object> getJSON() {
        return new HashMap<>();
    }

    @Override
    protected CheckboxField newItem(Map<String, Object> json) {
        return new CheckboxField(json);
    }

    @Override
    protected void testAllFieldsFilled(CheckboxField item) {

    }

    @Override
    protected void testAllFieldsNull(CheckboxField item) {

    }

    @Test
    public void testIsValidRequired() {
        var item = getItem();

        item.setRequired(true);
        // assertFalse(item.isValid((Boolean) null, null));
        // assertFalse(item.isValid(false, null));
        // assertTrue(item.isValid(true, null));
    }

    @Test
    public void testIsValidNotRequired() {
        var item = getItem();

        item.setRequired(false);
        // assertTrue(item.isValid((Boolean) null, null));
        // assertTrue(item.isValid(false, null));
        // assertTrue(item.isValid(true, null));
    }

    @Test
    public void testToPdfRows() {
        var item = getItem();

        // var rows = item.toPdfRows(, false, null);
        // assertEquals(1, rows.size());
        // assertInstanceOf(ValuePdfRowDto.class, rows.get(0));
    }
}
