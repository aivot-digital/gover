package de.aivot.GoverBackend.models.pdf;

import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.entities.Form;

import javax.script.ScriptEngine;
import java.util.List;
import java.util.Map;

public class ApplicationPdfDto {
    public final String title;
    public final List<BasePdfRowDto> fields;

    public ApplicationPdfDto(Form form, Map<String, Object> customerData, ScriptEngine scriptEngine) {
        title = form.getApplicationTitle();

        RootElement root = form.getRoot();
        fields = root.toPdfRows(root, customerData, null, scriptEngine);
    }
}
