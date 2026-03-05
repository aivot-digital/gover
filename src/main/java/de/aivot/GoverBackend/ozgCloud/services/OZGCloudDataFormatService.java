package de.aivot.GoverBackend.ozgCloud.services;

import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.content.Headline;
import de.aivot.GoverBackend.elements.models.form.input.*;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudFormDataItem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OZGCloudDataFormatService {
    @Nonnull
    public List<OZGCloudFormDataItem> buildFormData(
            @Nonnull BaseElement currentElement,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = currentElement.getResolvedId(idPrefix);

        // Check if the element is visible.
        // If not, yield an empty list.
        // If there is no explicit visibility setting for the element, we assume it is visible.
        var isVisible = elementData
                .visibilities()
                .getOrDefault(resolvedId, true);
        if (!isVisible) {
            return List.of();
        }

        // Check if there is an override for the element.
        // If yes, we use the override instead of the current element for building the form data.
        var override = elementData
                .overrides()
                .getOrDefault(resolvedId, currentElement);
        // If the override was null, we need to set it to the current element to avoid null pointer exceptions in the switch statement.
        if (override == null) {
            override = currentElement;
        }

        return switch (override) {
            case RootElement rootElement -> buildRoot(rootElement, customerInput, idPrefix, elementData);
            case StepElement stepElement -> List.of(buildStep(stepElement, customerInput, idPrefix, elementData));
            case GroupLayout groupLayout -> buildGroupLayout(groupLayout, customerInput, idPrefix, elementData);
            case ReplicatingContainerLayout replicatingContainerLayout ->
                    List.of(buildReplicatingContainerLayout(replicatingContainerLayout, customerInput, idPrefix, elementData));
            case CheckboxField cbx -> List.of(buildCheckboxField(cbx, customerInput, idPrefix, elementData));
            case DateField dateField -> List.of(buildDateField(dateField, customerInput, idPrefix, elementData));
            case FileUploadField fileUploadField ->
                    List.of(buildFileUploadField(fileUploadField, customerInput, idPrefix, elementData));
            case MultiCheckboxField multiCheckboxField ->
                    List.of(buildMultiCheckboxField(multiCheckboxField, customerInput, idPrefix, elementData));
            case NumberField numberField -> List.of(buildNumberField(numberField, customerInput, idPrefix, elementData));
            case SelectField selectField -> List.of(buildSelectField(selectField, customerInput, idPrefix, elementData));
            case RadioField radioField -> List.of(buildRadioField(radioField, customerInput, idPrefix, elementData));
            case TableField tableField -> List.of(buildTableField(tableField, customerInput, idPrefix, elementData));
            case TextField textField -> List.of(buildTextField(textField, customerInput, idPrefix, elementData));
            case TimeField timeField -> List.of(buildTimeField(timeField, customerInput, idPrefix, elementData));
            default -> List.of();
        };
    }

    private List<OZGCloudFormDataItem> buildRoot(
            @Nonnull RootElement rootElement,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        return rootElement
                .getChildren()
                .stream()
                .flatMap(child -> buildFormData(child, customerInput, idPrefix,elementData).stream())
                .toList();
    }

    private OZGCloudFormDataItem buildStep(
            @Nonnull StepElement stepElement,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = stepElement.getResolvedId(idPrefix);

        var children = buildChildList(
                stepElement.getChildren(),
                customerInput,
                idPrefix,
                elementData
        );

        return new OZGCloudFormDataItem(
                resolvedId,
                stepElement.getTitle(),
                null,
                null,
                null,
                null,
                children
        );
    }

    private List<OZGCloudFormDataItem> buildChildList(
            @Nonnull Collection<? extends BaseElement> childList,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var children = new LinkedList<OZGCloudFormDataItem>();

        Headline currentLargeHeadline = null;
        List<OZGCloudFormDataItem> largeHeadlineBuffer = null;

        Headline currentSmallHeadline = null;
        List<OZGCloudFormDataItem> smallHeadlineBuffer = null;

        // Iterate over all children in the list
        for (var child : childList) {

            // If the child is a headline, we need to start a new section for it. If there is already an active
            //  headline with the same size, we need to flush the buffer into the children list before starting the
            //  new section.
            if (child instanceof Headline hdl) {
                // If it's a small headline, we need to check if there is already an active small headline.
                //  If yes, we need to check if there is an active large headline. If there is, we need to flush the
                //  small headline as an OZGCloudFormDataItem with its buffered children into the large headline
                //  buffer, because the small headline is a subsection of the large headline. If there is no active
                //  large headline, we can flush the small headline directly into the children list. After flushing
                //  the existing small headline, we can start the new small headline section.
                if (hdl.getSmall()) {
                    if (currentSmallHeadline != null) {
                        var smallHeadlineItem = new OZGCloudFormDataItem(
                                currentSmallHeadline.getResolvedId(idPrefix),
                                currentSmallHeadline.getContent(),
                                null,
                                null,
                                null,
                                null,
                                smallHeadlineBuffer
                        );

                        if (currentLargeHeadline != null) {
                            largeHeadlineBuffer.add(smallHeadlineItem);
                        } else {
                            children.add(smallHeadlineItem);
                        }
                    }

                    currentSmallHeadline = hdl;
                    smallHeadlineBuffer = new LinkedList<>();
                }

                // If it's a large headline, we need to check if there is already an active large headline. If yes,
                //  we need to flush the buffer into the children list before starting the new section. Also, if there
                //  is an active small headline, we need to flush its buffer into large buffer list before flushing the
                //  large headline into the children list, because the small headline is a subsection of the large
                //  headline. After flushing the existing large headline, we can start the new large headline section.
                else {
                    if (currentLargeHeadline != null) {
                        // If there is an active small headline, we need to flush it into the large headline buffer before flushing the large headline
                        if (currentSmallHeadline != null) {
                            var smallHeadlineItem = new OZGCloudFormDataItem(
                                    currentSmallHeadline.getResolvedId(idPrefix),
                                    currentSmallHeadline.getContent(),
                                    null,
                                    null,
                                    null,
                                    null,
                                    smallHeadlineBuffer
                            );
                            largeHeadlineBuffer.add(smallHeadlineItem);
                            currentSmallHeadline = null;
                            smallHeadlineBuffer = null;
                        }

                        var headlineItem = new OZGCloudFormDataItem(
                                currentLargeHeadline.getResolvedId(idPrefix),
                                currentLargeHeadline.getContent(),
                                null,
                                null,
                                null,
                                null,
                                largeHeadlineBuffer
                        );
                        children.add(headlineItem);
                    }

                    currentLargeHeadline = hdl;
                    largeHeadlineBuffer = new LinkedList<>();
                }
            }
            // If it's not a headline we might append the form data of the child to the current headline section
            //  or add it directly to the children list.
            else {
                var childFormData = buildFormData(child, customerInput, idPrefix, elementData);

                if (currentSmallHeadline != null) {
                    smallHeadlineBuffer.addAll(childFormData);
                } else if (currentLargeHeadline != null) {
                    largeHeadlineBuffer.addAll(childFormData);
                } else {
                    children.addAll(childFormData);
                }
            }
        }

        // If there is an active large headline at the end of the list, we need to flush its buffer into the children list.
        if (currentLargeHeadline != null) {
            // Flush the small headline buffer if there is an active small headline
            if (currentSmallHeadline != null) {
                var smallHeadlineItem = new OZGCloudFormDataItem(
                        currentSmallHeadline.getResolvedId(idPrefix),
                        currentSmallHeadline.getContent(),
                        null,
                        null,
                        null,
                        null,
                        smallHeadlineBuffer
                );
                largeHeadlineBuffer.add(smallHeadlineItem);
                currentSmallHeadline = null;
                smallHeadlineBuffer = null;
            }

            var headlineItem = new OZGCloudFormDataItem(
                    currentLargeHeadline.getResolvedId(idPrefix),
                    currentLargeHeadline.getContent(),
                    null,
                    null,
                    null,
                    null,
                    largeHeadlineBuffer
            );
            children.add(headlineItem);
        }

        return children;
    }

    private List<OZGCloudFormDataItem> buildGroupLayout(
            @Nonnull GroupLayout groupLayout,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        return buildChildList(groupLayout.getChildren(), customerInput, idPrefix, elementData);
    }

    private OZGCloudFormDataItem buildReplicatingContainerLayout(
            @Nonnull ReplicatingContainerLayout replicatingContainerLayout,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = replicatingContainerLayout.getResolvedId(idPrefix);

        var childIds = new LinkedList<String>();

        var childIdsObj = elementData
                .values()
                .getOrDefault(resolvedId, List.of());

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
            var childFormData = buildChildList(
                    replicatingContainerLayout.getChildren(),
                    customerInput,
                    resolvedId + "_" + childId,
                    elementData
            );

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
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = cbx.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                cbx.getLabel(),
                null,
                null,
                null,
                (Boolean) elementData.values().getOrDefault(resolvedId, false),
                null
        );
    }

    private OZGCloudFormDataItem buildNumberField(
            @Nonnull NumberField cbx,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = cbx.getResolvedId(idPrefix);

        var val = elementData.values().getOrDefault(resolvedId, null);

        return new OZGCloudFormDataItem(
                resolvedId,
                cbx.getLabel(),
                val == null ? "Keine Angabe" : null,
                val != null ? (Number) val : null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildDateField(
            @Nonnull DateField dateField,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = dateField.getResolvedId(idPrefix);

        var valObj = elementData.values().getOrDefault(resolvedId, null);
        if (valObj == null) {
            return new OZGCloudFormDataItem(
                    resolvedId,
                    dateField.getLabel(),
                    "Keine Angabe",
                    null,
                    null,
                    null,
                    null
            );
        }

        var date = dateField
                .getDate((String) valObj);

        var displayValue = date
                .format(
                        DateTimeFormatter
                                .ofPattern("yyyy-MM-dd")
                                .withZone(DateField.zoneId)
                );


        return new OZGCloudFormDataItem(
                resolvedId,
                dateField.getLabel(),
                null,
                null,
                displayValue,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildFileUploadField(
            @Nonnull FileUploadField fileUploadField,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = fileUploadField.getResolvedId(idPrefix);

        var items = new LinkedList<FileUploadFieldItem>();

        var valObj = elementData.values().getOrDefault(resolvedId, List.of());
        if (valObj instanceof List<?> list) {
            for (var itemObj : list) {
                if (itemObj instanceof FileUploadFieldItem fileItem) {
                    items.add(fileItem);
                } else if (itemObj instanceof Map<?, ?> itemMap) {
                    items.add(new FileUploadFieldItem((Map<String, Object>) itemMap));
                }
            }
        }

        var displayValue = items
                .stream()
                .map(FileUploadFieldItem::getName)
                .collect(Collectors.joining(", "));

        if (displayValue.isEmpty()) {
            displayValue = "Keine Anlagen hochgeladen";
        }

        return new OZGCloudFormDataItem(
                resolvedId,
                fileUploadField.getLabel(),
                displayValue,
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildMultiCheckboxField(
            @Nonnull MultiCheckboxField multiCheckboxField,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = multiCheckboxField.getResolvedId(idPrefix);

        var displayValue = multiCheckboxField
                .toDisplayValue(elementData.values().getOrDefault(resolvedId, List.of()));

        return new OZGCloudFormDataItem(
                resolvedId,
                multiCheckboxField.getLabel(),
                displayValue,
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildRadioField(
            @Nonnull RadioField radioField,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = radioField.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                radioField.getLabel(),
                radioField.toDisplayValue(elementData.values().getOrDefault(resolvedId, null)),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildSelectField(
            @Nonnull SelectField selectField,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = selectField.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                selectField.getLabel(),
                selectField.toDisplayValue(elementData.values().getOrDefault(resolvedId, null)),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildTableField(
            @Nonnull TableField tableField,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = tableField.getResolvedId(idPrefix);

        var rows = new LinkedList<Map<String, Object>>();

        var rowsObject = elementData.values().getOrDefault(resolvedId, List.of());
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
                        resolvedId + "_" + i + "_" + column.getLabel().replaceAll("\\W+", "_"),
                        column.getLabel(),
                        stringValue,
                        numberValue,
                        null,
                        null,
                        null
                ));
            }

            rowItems.add(new OZGCloudFormDataItem(
                    resolvedId + "_" + i,
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
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = textField.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                textField.getLabel(),
                textField.toDisplayValue(elementData.values().getOrDefault(resolvedId, null)),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildTimeField(
            @Nonnull TimeField timeField,
            @Nonnull Map<String, Object> customerInput,
            @Nullable String idPrefix,
            @Nonnull FormState elementData
    ) {
        var resolvedId = timeField.getResolvedId(idPrefix);

        return new OZGCloudFormDataItem(
                resolvedId,
                timeField.getLabel(),
                timeField.toDisplayValue(elementData.values().getOrDefault(resolvedId, null)),
                null,
                null,
                null,
                null
        );
    }
}
