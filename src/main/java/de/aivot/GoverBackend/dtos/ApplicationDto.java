package de.aivot.GoverBackend.dtos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ScriptRequiredException;
import de.aivot.GoverBackend.models.Application;
import de.aivot.GoverBackend.services.ScriptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ApplicationDto {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationDto.class);

    public final String title;
    public final List<FieldDto> fields;
    public final LocalDate now;

    private final Application application;
    private final ScriptService scriptService;
    private final Map<String, Object> customerData;

    public ApplicationDto(Application application, Map<String, Object> customerData, ScriptService scriptService) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        Map<String, Object> root = application.getRoot();

        this.application = application;
        this.customerData = customerData;
        this.scriptService = scriptService;

        this.title = (String) root.getOrDefault("headline", root.getOrDefault("title", application.getSlug()));

        this.now = LocalDate.now();
        this.fields = new LinkedList<>();

        Collection<Map<String, Object>> segments = (Collection<Map<String, Object>>) root.get("children");
        for (Map<String, Object> segment : segments) {
            this.fields.addAll(processElement(segment, null));
        }
    }

    private List<FieldDto> processElement(Map<String, Object> pElement, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        boolean isVisible = scriptService.isElementVisible(application, pElement, customerData, idPrefix);
        if (!isVisible) {
            return new LinkedList<>();
        }

        Map<String, Object> element = scriptService.patchElement(application, pElement, customerData, idPrefix);

        Optional<ElementType> type = ElementType.findElement(element.get("type"));
        if (type.isEmpty()) {
            return new LinkedList<>();
        }

        String id = ScriptService.getPrefixedId(element, idPrefix);

        Object value = ScriptService.getElementValue(customerData, element, idPrefix);

        switch (type.get()) {
            case Step -> {
                return processSegment(id, element);
            }
            case Container -> {
                return processGroup(id, element, idPrefix);
            }
            case ReplicatingContainer -> {
                return processReplicatingContainer(id, element, idPrefix);
            }
            case Checkbox -> {
                return initCheckbox(element, value);
            }
            case Date -> {
                return initDate(element, value);
            }
            case MultiCheckbox -> {
                return initMultiCheckbox(element, value);
            }
            case Number -> {
                return initNumber(element, value);
            }
            case Radio -> {
                return initRadio(element, value);
            }
            case Select -> {
                return initSelect(element, value);
            }
            case Table -> {
                return initTable(element, value);
            }
            case Text -> {
                return initText(element, value);
            }
            case Time -> {
                return initTime(element, value);
            }
            case FileUpload -> {
                return initFileUpload(element, value);
            }
            default -> {
                return new LinkedList<>();
            }
        }
    }

    private List<FieldDto> processSegment(String id, Map<String, Object> segmentElement) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        List<FieldDto> fields = new LinkedList<>();
        List<Map<String, Object>> children = (List<Map<String, Object>>) segmentElement.get("children");

        fields.add(new HeadlineFieldDto((String) segmentElement.get("title"), 2));

        for (Map<String, Object> childElement : children) {
            fields.addAll(processElement(childElement, null));
        }

        return fields;
    }

    private List<FieldDto> processGroup(String id, Map<String, Object> groupElement, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        List<FieldDto> fields = new LinkedList<>();
        List<Map<String, Object>> children = (List<Map<String, Object>>) groupElement.get("children");

        for (Map<String, Object> childElement : children) {
            fields.addAll(processElement(childElement, idPrefix));
        }

        return fields;
    }

    private List<FieldDto> processReplicatingContainer(String id, Map<String, Object> containerElement, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        // TODO: Check processing of replicating containers in replicating containers

        List<FieldDto> fields = new LinkedList<>();
        List<Map<String, Object>> children = (List<Map<String, Object>>) containerElement.get("children");

        List<String> childIds = (List<String>) customerData.get(id);

        if (childIds != null && !childIds.isEmpty()) {
            fields.add(new HeadlineFieldDto((String) containerElement.get("label"), 3));

            for (int i = 0; i < childIds.size(); i++) {
                String childId = childIds.get(i);
                String headlineTemplate = (String) containerElement.get("headlineTemplate");
                headlineTemplate = headlineTemplate.replace("#", "" + (i + 1));
                fields.add(new HeadlineFieldDto(headlineTemplate, 4));

                for (Map<String, Object> childElement : children) {
                    fields.addAll(processElement(childElement, id + "_" + childId));
                }
            }
        }

        return fields;
    }

    private List<FieldDto> initCheckbox(Map<String, Object> fieldData, Object value) {
        List<FieldDto> fields = new LinkedList<>();

        fields.add(new ValueFieldDto(
                (String) fieldData.get("label"),
                Boolean.TRUE.equals(value) ? "Ja" : "Nein"
        ));

        return fields;
    }

    private List<FieldDto> initDate(Map<String, Object> fieldData, Object value) {
        List<FieldDto> fields = new LinkedList<>();

        String displayValue = "Keine Angaben";

        if (value != null) {
            String mode = (String) fieldData.get("mode");
            String displayPattern = "dd.MM.yyyy";

            if (mode != null) {
                switch (mode) {
                    case "year" -> displayPattern = "yyyy";
                    case "month" -> displayPattern = "mm.yyyy";
                }
            }

            try {
                ZonedDateTime date = ZonedDateTime.parse((String) value);
                displayValue = date.format(DateTimeFormatter
                        .ofPattern(displayPattern)
                        .withZone(ZoneId.of("Europe/Paris")));
            } catch (Exception e) {
                logger.warn("Failed to parse date for field " + fieldData.get("id"));
            }
        }

        fields.add(new ValueFieldDto(
                (String) fieldData.get("label"),
                displayValue
        ));

        return fields;
    }

    private List<FieldDto> initMultiCheckbox(Map<String, Object> fieldData, Object value) {
        List<FieldDto> fields = new LinkedList<>();
        List<String> options = (List<String>) value;

        if (options == null || options.isEmpty()) {
            fields.add(new ValueFieldDto(
                    (String) fieldData.get("label"),
                    "Keine Angaben"
            ));
        } else {
            fields.add(new ValueFieldDto(
                    (String) fieldData.get("label"),
                    options.get(0)
            ));
            for (int i = 1; i < options.size(); i++) {
                fields.add(new ValueFieldDto(
                        "",
                        options.get(i)
                ));
            }
        }

        return fields;
    }

    private List<FieldDto> initNumber(Map<String, Object> fieldData, Object value) {
        List<FieldDto> fields = new LinkedList<>();

        String displayValue = "Keine Angaben";

        if (value != null) {
            Integer decimalPlaces = (Integer) fieldData.get("decimalPlaces");
            decimalPlaces = decimalPlaces != null ? decimalPlaces : 0;

            if (value instanceof Integer) {
                displayValue = ((Integer) value).toString();
            } else if (value instanceof Float) {
                displayValue = String.format("%." + decimalPlaces + "f", value);
            } else if (value instanceof Double) {
                displayValue = String.format("%." + decimalPlaces + "f", value);
            }

            String suffix = (String) fieldData.get("suffix");
            if (suffix != null) {
                displayValue += " " + suffix;
            }
        }

        fields.add(new ValueFieldDto(
                (String) fieldData.get("label"),
                displayValue
        ));

        return fields;
    }

    private List<FieldDto> initRadio(Map<String, Object> fieldData, Object value) {
        List<FieldDto> fields = new LinkedList<>();

        fields.add(new ValueFieldDto(
                (String) fieldData.get("label"),
                value != null ? (String) value : "Keine Angaben"
        ));

        return fields;
    }

    private List<FieldDto> initSelect(Map<String, Object> fieldData, Object value) {
        List<FieldDto> fields = new LinkedList<>();

        fields.add(new ValueFieldDto(
                (String) fieldData.get("label"),
                value != null ? (String) value : "Keine Angaben"
        ));

        return fields;
    }

    private List<FieldDto> initTable(Map<String, Object> fieldData, Object value) {
        List<String> columnHeaders = new LinkedList<>();
        Collection<Map<String, Object>> tableColumns = (Collection<Map<String, Object>>) fieldData.get("fields");
        for (Map<String, Object> col : tableColumns) {
            columnHeaders.add((String) col.get("label"));
        }

        List<List<String>> columnValues = new LinkedList<>();
        Collection<Map<String, Object>> tableRows = (Collection<Map<String, Object>>) value;
        if (tableRows != null) {
            for (Map<String, Object> row : tableRows) {
                List<String> fields = new LinkedList<>();
                for (Map<String, Object> col : tableColumns) {
                    String datatype = (String) col.get("datatype");
                    Object cellValue = row.get((String) col.get("label"));

                    if ("string".equals(datatype)) {
                        fields.add((String) cellValue);
                    } else if ("number".equals(datatype)) {
                        Integer decimalPlaces = (Integer) col.get("decimalPlaces");
                        if (decimalPlaces == null) {
                            decimalPlaces = 2;
                        }
                        String decimalFormat = "%." + decimalPlaces + "f";

                        if (cellValue instanceof String) {
                            try {
                                Double val = Double.parseDouble((String) cellValue);
                                fields.add(String.format(decimalFormat, val));
                            } catch (Exception e) {
                                fields.add((String) cellValue);
                            }
                        } else if (cellValue instanceof Integer) {
                            fields.add("" + cellValue);
                        } else if (cellValue instanceof Double) {
                            fields.add(String.format(decimalFormat, cellValue));
                        } else if (cellValue instanceof Float) {
                            fields.add(String.format(decimalFormat, cellValue));
                        } else {
                            fields.add("");
                        }
                    }
                }
                columnValues.add(fields);
            }
        }

        List<FieldDto> fields = new LinkedList<>();
        fields.add(new TableFieldDto((String) fieldData.get("label"), columnHeaders, columnValues));
        return fields;
    }

    private List<FieldDto> initText(Map<String, Object> fieldData, Object value) {
        List<FieldDto> fields = new LinkedList<>();

        fields.add(new ValueFieldDto(
                (String) fieldData.get("label"),
                value != null ? (String) value : "Keine Angaben"
        ));

        return fields;
    }

    private List<FieldDto> initTime(Map<String, Object> fieldData, Object value) {
        List<FieldDto> fields = new LinkedList<>();
        String label = (String) fieldData.get("label");

        String valueText = "Keine Angaben";
        try {
            ZonedDateTime time = ZonedDateTime.parse((String) value, DateTimeFormatter.ISO_DATE_TIME);
            valueText = time.format(DateTimeFormatter
                    .ofPattern("hh:mm")
                    .withZone(ZoneId.of("Europe/Paris"))) + " Uhr";
        } catch (Exception e) {
            logger.warn("Failed to parse time for field " + fieldData.get("id"));
            e.printStackTrace();
        }

        fields.add(new ValueFieldDto(
                label,
                valueText
        ));

        return fields;
    }

    private List<FieldDto> initFileUpload(Map<String, Object> element, Object value) {
        List<FieldDto> fields = new LinkedList<>();
        String label = (String) element.get("label");


        if (value instanceof List<?>) {
            List<Map<String, String>> items = (List<Map<String, String>>) value;

            fields.add(new ValueFieldDto(
                    label,
                    items.get(0).get("name")
            ));

            for (int i=1; i<items.size(); i++) {
                fields.add(new ValueFieldDto(
                        "",
                        items.get(i).get("name")
                ));
            }
        } else {
            fields.add(new ValueFieldDto(
                    label,
                    "Keine Anlagen hinzugefügt"
            ));
        }

        return fields;
    }
}
