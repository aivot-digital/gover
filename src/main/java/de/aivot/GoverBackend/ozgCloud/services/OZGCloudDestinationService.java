package de.aivot.GoverBackend.ozgCloud.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.input.*;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudControlData;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudFormDataItem;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudPayload;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OZGCloudDestinationService {
    private static final String FORM_FIELD_PAYLOAD = "formData";
    private static final String FORM_FIELD_REPRESENTATION = "representation";
    private static final String FORM_FIELD_ATTACHMENT = "attachment";

    private final RestClient httpClient;

    public OZGCloudDestinationService() {
        this.httpClient = RestClient
                .builder()
                .build();
    }

    public void send(
            @Nonnull String destinationUrl,
            @Nonnull OZGCloudControlData controlData,
            @Nonnull BaseElement rootElement,
            @Nonnull Map<String, Object> elementData,
            @Nonnull Resource representation,
            @Nonnull List<Resource> attachments
    ) {
        var destinationUri = URI.create(destinationUrl);

        var formData = buildFormData(rootElement, elementData, null);

        var payload = new OZGCloudPayload(
                controlData,
                formData
        );

        try {
            submit(destinationUri, payload, representation, attachments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private List<OZGCloudFormDataItem> buildFormData(
            @Nonnull BaseElement currentElement,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        return switch (currentElement) {
            case RootElement rootElement -> buildRoot(rootElement, elementData, idPrefix);
            case StepElement stepElement -> List.of(buildStep(stepElement, elementData, idPrefix));
            case GroupLayout groupLayout -> buildGroupLayout(groupLayout, elementData, idPrefix);
            case ReplicatingContainerLayout replicatingContainerLayout ->
                    List.of(buildReplicatingContainerLayout(replicatingContainerLayout, elementData, idPrefix));
            case CheckboxField cbx -> List.of(buildCheckboxField(cbx, elementData, idPrefix));
            case DateField dateField -> List.of(buildDateField(dateField, elementData, idPrefix));
            case FileUploadField fileUploadField ->
                    List.of(buildFileUploadField(fileUploadField, elementData, idPrefix));
            case MultiCheckboxField multiCheckboxField ->
                    List.of(buildMultiCheckboxField(multiCheckboxField, elementData, idPrefix));
            case SelectField selectField -> List.of(buildSelectField(selectField, elementData, idPrefix));
            case RadioField radioField -> List.of(buildRadioField(radioField, elementData, idPrefix));
            case TableField tableField -> List.of(buildTableField(tableField, elementData, idPrefix));
            case TextField textField -> List.of(buildTextField(textField, elementData, idPrefix));
            case TimeField timeField -> List.of(buildTimeField(timeField, elementData, idPrefix));
            default -> List.of();
        };
    }

    private List<OZGCloudFormDataItem> buildRoot(
            @Nonnull RootElement rootElement,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        return rootElement
                .getChildren()
                .stream()
                .flatMap(child -> buildFormData(child, elementData, idPrefix).stream())
                .toList();
    }

    private OZGCloudFormDataItem buildStep(
            @Nonnull StepElement stepElement,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = stepElement.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                stepElement.getTitle(),
                null,
                null,
                null,
                null,
                stepElement
                        .getChildren()
                        .stream()
                        .flatMap(child -> buildFormData(child, elementData, idPrefix).stream())
                        .toList()
        );
    }

    private List<OZGCloudFormDataItem> buildGroupLayout(
            @Nonnull GroupLayout groupLayout,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        return groupLayout
                .getChildren()
                .stream()
                .flatMap(child -> buildFormData(child, elementData, idPrefix).stream())
                .toList();
    }

    private OZGCloudFormDataItem buildReplicatingContainerLayout(
            @Nonnull ReplicatingContainerLayout replicatingContainerLayout,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = replicatingContainerLayout.getResolvedId(idPrefix);

        var childIds = new LinkedList<String>();

        var childIdsObj = elementData.getOrDefault(resolvedId, List.of());
        if (childIdsObj instanceof List<?> list) {
            for (var itemObj : list) {
                if (itemObj instanceof String str) {
                    childIds.add(str);
                }
            }
        }

        var childItems = new LinkedList<OZGCloudFormDataItem>();

        for (int i = 0; i < childIds.size(); i++) {
            var childId = childIds.get(i);
            var childFormData = new LinkedList<OZGCloudFormDataItem>();
            for (var child : replicatingContainerLayout.getChildren()) {
                childFormData.addAll(buildFormData(child, elementData, resolvedId + "_" + childId));
            }
            var childItem = new OZGCloudFormDataItem(
                    resolvedId + "_" + childId,
                    replicatingContainerLayout.getResolvedHeadline(i),
                    null,
                    null,
                    null,
                    null,
                    childFormData
            );
            childItems.add(childItem);
        }

        return new OZGCloudFormDataItem(
                resolvedId,
                replicatingContainerLayout.getLabel(),
                null,
                null,
                null,
                null,
                childItems
        );
    }

    private OZGCloudFormDataItem buildCheckboxField(
            @Nonnull CheckboxField cbx,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = cbx.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                cbx.getLabel(),
                null,
                null,
                null,
                (Boolean) elementData.getOrDefault(resolvedId, false),
                null
        );
    }

    private OZGCloudFormDataItem buildDateField(
            @Nonnull DateField dateField,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = dateField.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                dateField.getLabel(),
                null,
                null,
                (String) elementData.getOrDefault(resolvedId, null),
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildFileUploadField(
            @Nonnull FileUploadField fileUploadField,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = fileUploadField.getResolvedId(idPrefix);

        var items = new LinkedList<FileUploadFieldItem>();

        var valObj = elementData.getOrDefault(resolvedId, List.of());
        if (valObj instanceof List<?> list) {
            for (var itemObj : list) {
                if (itemObj instanceof FileUploadFieldItem fileItem) {
                    items.add(fileItem);
                }
            }
        }

        return new OZGCloudFormDataItem(
                resolvedId,
                fileUploadField.getLabel(),
                items
                        .stream()
                        .map(FileUploadFieldItem::getName)
                        .collect(Collectors.joining(", ")),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildMultiCheckboxField(
            @Nonnull MultiCheckboxField multiCheckboxField,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = multiCheckboxField.getResolvedId(idPrefix);

        var values = new LinkedList<String>();

        var valObj = elementData.getOrDefault(resolvedId, List.of());
        if (valObj instanceof List<?> list) {
            for (var itemObj : list) {
                if (itemObj instanceof String str) {
                    values.add(str);
                }
            }
        }

        return new OZGCloudFormDataItem(
                resolvedId,
                multiCheckboxField.getLabel(),
                String.join(", ", values),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildRadioField(
            @Nonnull RadioField radioField,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = radioField.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                radioField.getLabel(),
                (String) elementData.getOrDefault(resolvedId, null),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildSelectField(
            @Nonnull SelectField selectField,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = selectField.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                selectField.getLabel(),
                (String) elementData.getOrDefault(resolvedId, null),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildTableField(
            @Nonnull TableField tableField,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = tableField.getResolvedId(idPrefix);

        var rows = new LinkedList<Map<String, Object>>();

        var rowsObject = elementData.getOrDefault(resolvedId, List.of());
        if (rowsObject instanceof List<?> rowsList) {
            for (var rowObj : rowsList) {
                if (rowObj instanceof Map<?, ?> rowMap) {
                    var typedRowMap = rowMap
                            .entrySet()
                            .stream()
                            .filter(e -> e.getKey() instanceof String)
                            .collect(Collectors.toMap(
                                    e -> (String) e.getKey(),
                                    e -> (Object) e.getValue()
                            ));
                    rows.add(typedRowMap);
                }
            }
        }

        var rowItems = new LinkedList<OZGCloudFormDataItem>();

        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            var cellItems = new LinkedList<OZGCloudFormDataItem>();
            for (var column : tableField.getFields()) {
                String stringValue = null;
                Number numberValue = null;

                if (column.getDatatype() == TableColumnDataType.String) {
                    stringValue = (String) row.getOrDefault(column.getLabel(), null);
                } else {
                    var numObj = row.getOrDefault(column.getLabel(), null);
                    if (numObj instanceof Number num) {
                        numberValue = num;
                    } else if (numObj instanceof String numStr) {
                        try {
                            numberValue = Double.parseDouble(numStr);
                        } catch (NumberFormatException e) {
                            // Ignore parsing errors and keep numberValue as null
                        }
                    }
                }

                cellItems.add(new OZGCloudFormDataItem(
                        resolvedId + "[" + i + "]" + "." + column.getLabel().replaceAll("\\W+", "_"),
                        column.getLabel(),
                        stringValue,
                        numberValue,
                        null,
                        null,
                        null
                ));
            }

            rowItems.add(new OZGCloudFormDataItem(
                    resolvedId + "[" + i + "]",
                    "Zeile " + (i + 1),
                    null,
                    null,
                    null,
                    null,
                    cellItems
            ));
        }

        return new OZGCloudFormDataItem(
                resolvedId,
                tableField.getLabel(),
                null,
                null,
                null,
                null,
                rowItems
        );
    }

    private OZGCloudFormDataItem buildTextField(
            @Nonnull TextField textField,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = textField.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                textField.getLabel(),
                (String) elementData.getOrDefault(resolvedId, null),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildTimeField(
            @Nonnull TimeField timeField,
            @Nonnull Map<String, Object> elementData,
            @Nullable String idPrefix
    ) {
        var resolvedId = timeField.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                timeField.getLabel(),
                (String) elementData.getOrDefault(resolvedId, null),
                null,
                null,
                null,
                null
        );
    }

    private void submit(
            @Nonnull URI destinationUri,
            @Nonnull OZGCloudPayload payload,
            @Nonnull Resource representation,
            @Nonnull List<Resource> attachments
    ) throws IOException {
        var payloadJSON = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(payload);

        var body = new MultipartBodyPublisher()
                .addPart(FORM_FIELD_PAYLOAD, payloadJSON)
                .addPart(FORM_FIELD_REPRESENTATION, representation);

        for (var attachment : attachments) {
            body.addPart(FORM_FIELD_ATTACHMENT, attachment);
        }

        var responseBody = httpClient
                .post()
                .uri(destinationUri)
                .body(body.build())
                .retrieve()
                .body(byte[].class);

        if (responseBody == null) {
            throw new RuntimeException("Received null response body from OZGCloud destination");
        }
    }

    public static class MultipartBodyPublisher {
        private final MultiValueMap<String, Resource> parts = new LinkedMultiValueMap<>();

        public MultipartBodyPublisher addPart(String name, String value) {
            var res = new ByteArrayResource(value.getBytes(StandardCharsets.UTF_8));
            parts.add(name, res);
            return this;
        }

        public MultipartBodyPublisher addPart(String name, Resource resource) {
            parts.add(name, resource);
            return this;
        }

        public Object build() {
            return parts;
        }
    }
}
