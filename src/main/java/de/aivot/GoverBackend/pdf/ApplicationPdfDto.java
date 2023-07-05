package de.aivot.GoverBackend.pdf;

import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.entities.Application;

import javax.script.ScriptEngine;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ApplicationPdfDto {
    public final String title;
    public final List<BasePdfRowDto> fields;

    public ApplicationPdfDto(Application application, Map<String, Object> customerData, ScriptEngine scriptEngine) {
        title = application.getApplicationTitle();

        RootElement root = application.getRoot();
        fields = root.toPdfRows(root, customerData, null, scriptEngine);
    }
}
