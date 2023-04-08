package de.aivot.GoverBackend.pdf;

import de.aivot.GoverBackend.models.entities.Application;

import javax.script.ScriptEngine;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ApplicationPdfDto {
    public final String title;
    public final List<BasePdfRowDto> fields;
    public final LocalDate now = LocalDate.now();

    public ApplicationPdfDto(Application application, Map<String, Object> customerData, ScriptEngine scriptEngine) {
        title = application.getApplicationTitle();
        fields = application.getRoot().toPdfRows(customerData, null, scriptEngine);
    }

    /*
    private List<BasePdfRowDto> processElement(StepElement pElement, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
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
            case Group -> {
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

    private List<BasePdfRowDto> processSegment(String id, Map<String, Object> segmentElement) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        List<BasePdfRowDto> fields = new LinkedList<>();
        List<Map<String, Object>> children = (List<Map<String, Object>>) segmentElement.get("children");

        fields.add(new HeadlinePdfRowDto((String) segmentElement.get("title"), 2));

        for (Map<String, Object> childElement : children) {
            fields.addAll(processElement(childElement, null));
        }

        return fields;
    }
     */

    /*
    private List<BasePdfRowDto> processGroup(String id, Map<String, Object> groupElement, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        List<BasePdfRowDto> fields = new LinkedList<>();
        List<Map<String, Object>> children = (List<Map<String, Object>>) groupElement.get("children");

        for (Map<String, Object> childElement : children) {
            fields.addAll(processElement(childElement, idPrefix));
        }

        return fields;
    }

    private List<BasePdfRowDto> processReplicatingContainer(String id, Map<String, Object> containerElement, @Nullable String idPrefix) throws ScriptRequiredException, ScriptException, JsonProcessingException {
        // TODO: Check processing of replicating containers in replicating containers

        List<BasePdfRowDto> fields = new LinkedList<>();
        List<Map<String, Object>> children = (List<Map<String, Object>>) containerElement.get("children");

        List<String> childIds = (List<String>) customerData.get(id);

        if (childIds != null && !childIds.isEmpty()) {
            fields.add(new HeadlinePdfRowDto((String) containerElement.get("label"), 3));

            for (int i = 0; i < childIds.size(); i++) {
                String childId = childIds.get(i);
                String headlineTemplate = (String) containerElement.get("headlineTemplate");
                headlineTemplate = headlineTemplate.replace("#", "" + (i + 1));
                fields.add(new HeadlinePdfRowDto(headlineTemplate, 4));

                for (Map<String, Object> childElement : children) {
                    fields.addAll(processElement(childElement, id + "_" + childId));
                }
            }
        }

        return fields;
    }

    private List<BasePdfRowDto> initCheckbox(Map<String, Object> fieldData, Object value) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                (String) fieldData.get("label"),
                Boolean.TRUE.equals(value) ? "Ja" : "Nein"
        ));

        return fields;
    }

    private List<BasePdfRowDto> initDate(Map<String, Object> fieldData, Object value) {
        List<BasePdfRowDto> fields = new LinkedList<>();

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

        fields.add(new ValuePdfRowDto(
                (String) fieldData.get("label"),
                displayValue
        ));

        return fields;
    }

    private List<BasePdfRowDto> initMultiCheckbox(Map<String, Object> fieldData, Object value) {
        List<BasePdfRowDto> fields = new LinkedList<>();
        List<String> options = (List<String>) value;

        if (options == null || options.isEmpty()) {
            fields.add(new ValuePdfRowDto(
                    (String) fieldData.get("label"),
                    "Keine Angaben"
            ));
        } else {
            fields.add(new ValuePdfRowDto(
                    (String) fieldData.get("label"),
                    options.get(0)
            ));
            for (int i = 1; i < options.size(); i++) {
                fields.add(new ValuePdfRowDto(
                        "",
                        options.get(i)
                ));
            }
        }

        return fields;
    }

    private List<BasePdfRowDto> initNumber(Map<String, Object> fieldData, Object value) {
        List<BasePdfRowDto> fields = new LinkedList<>();

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

        fields.add(new ValuePdfRowDto(
                (String) fieldData.get("label"),
                displayValue
        ));

        return fields;
    }

    private List<BasePdfRowDto> initRadio(Map<String, Object> fieldData, Object value) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                (String) fieldData.get("label"),
                value != null ? (String) value : "Keine Angaben"
        ));

        return fields;
    }

    private List<BasePdfRowDto> initSelect(Map<String, Object> fieldData, Object value) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                (String) fieldData.get("label"),
                value != null ? (String) value : "Keine Angaben"
        ));

        return fields;
    }

    private List<BasePdfRowDto> initTable(Map<String, Object> fieldData, Object value) {
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

        List<BasePdfRowDto> fields = new LinkedList<>();
        fields.add(new TablePdfRowDto((String) fieldData.get("label"), columnHeaders, columnValues));
        return fields;
    }

    private List<BasePdfRowDto> initText(Map<String, Object> fieldData, Object value) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                (String) fieldData.get("label"),
                value != null ? (String) value : "Keine Angaben"
        ));

        return fields;
    }

    private List<BasePdfRowDto> initTime(Map<String, Object> fieldData, Object value) {
        List<BasePdfRowDto> fields = new LinkedList<>();
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

        fields.add(new ValuePdfRowDto(
                label,
                valueText
        ));

        return fields;
    }

    private List<BasePdfRowDto> initFileUpload(Map<String, Object> element, Object value) {
        List<BasePdfRowDto> fields = new LinkedList<>();
        String label = (String) element.get("label");


        if (value instanceof List<?>) {
            List<Map<String, String>> items = (List<Map<String, String>>) value;

            fields.add(new ValuePdfRowDto(
                    label,
                    items.get(0).get("name")
            ));

            for (int i=1; i<items.size(); i++) {
                fields.add(new ValuePdfRowDto(
                        "",
                        items.get(i).get("name")
                ));
            }
        } else {
            fields.add(new ValuePdfRowDto(
                    label,
                    "Keine Anlagen hinzugefügt"
            ));
        }

        return fields;
    }
    */
}
