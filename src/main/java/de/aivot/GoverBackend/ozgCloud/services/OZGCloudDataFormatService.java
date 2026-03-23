package de.aivot.GoverBackend.ozgCloud.services;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.StepElement;
import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudFormDataItem;
import jakarta.annotation.Nonnull;

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
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(currentElement);

        // Check if the element is visible.
        // If not, yield an empty list.
        // If there is no explicit visibility setting for the element, we assume it is visible.
        if (!edo.getIsVisible()) {
            return List.of();
        }

        // Check if there is an override for the element.
        // If yes, we use the override instead of the current element for building the form data.
        var override = edo
                .getComputedOverrideOrDefault(currentElement);

        return switch (override) {
            case FormLayoutElement rootElement -> buildRoot(rootElement, elementData);
            case StepElement stepElement -> List.of(buildStep(stepElement, elementData));
            case GroupLayoutElement groupLayout -> buildGroupLayout(groupLayout, elementData);
            case ReplicatingContainerLayoutElement replicatingContainerLayout ->
                    List.of(buildReplicatingContainerLayout(replicatingContainerLayout, elementData));
            case CheckboxInputElement cbx -> List.of(buildCheckboxField(cbx, elementData));
            case DateInputElement dateField -> List.of(buildDateField(dateField, elementData));
            case FileUploadInputElement fileUploadField -> List.of(buildFileUploadField(fileUploadField, elementData));
            case MultiCheckboxInputElement multiCheckboxField ->
                    List.of(buildMultiCheckboxField(multiCheckboxField, elementData));
            case NumberInputElement numberField -> List.of(buildNumberField(numberField, elementData));
            case SelectInputElement selectField -> List.of(buildSelectField(selectField, elementData));
            case RadioInputElement radioField -> List.of(buildRadioField(radioField, elementData));
            case TableInputElement tableField -> List.of(buildTableField(tableField, elementData));
            case TextInputElement textField -> List.of(buildTextField(textField, elementData));
            case TimeInputElement timeField -> List.of(buildTimeField(timeField, elementData));
            default -> List.of();
        };
    }

    private List<OZGCloudFormDataItem> buildRoot(
            @Nonnull FormLayoutElement rootElement,
            @Nonnull ElementData elementData
    ) {
        return rootElement
                .getChildren()
                .stream()
                .flatMap(child -> buildFormData(child, elementData).stream())
                .toList();
    }

    private OZGCloudFormDataItem buildStep(
            @Nonnull StepElement stepElement,
            @Nonnull ElementData elementData
    ) {
        var children = buildChildList(
                stepElement.getChildren(),
                elementData
        );

        return new OZGCloudFormDataItem(
                stepElement.getId(),
                stepElement.getResolvedTitle(),
                null,
                null,
                null,
                null,
                children
        );
    }

    private List<OZGCloudFormDataItem> buildChildList(
            @Nonnull Collection<? extends BaseElement> childList,
            @Nonnull ElementData elementData
    ) {
        var children = new LinkedList<OZGCloudFormDataItem>();

        HeadlineContentElement currentLargeHeadline = null;
        List<OZGCloudFormDataItem> largeHeadlineBuffer = null;

        HeadlineContentElement currentSmallHeadline = null;
        List<OZGCloudFormDataItem> smallHeadlineBuffer = null;

        // Iterate over all children in the list
        for (var child : childList) {

            // If the child is a headline, we need to start a new section for it. If there is already an active
            //  headline with the same size, we need to flush the buffer into the children list before starting the
            //  new section.
            if (child instanceof HeadlineContentElement hdl) {
                // If it's a small headline, we need to check if there is already an active small headline.
                //  If yes, we need to check if there is an active large headline. If there is, we need to flush the
                //  small headline as an OZGCloudFormDataItem with its buffered children into the large headline
                //  buffer, because the small headline is a subsection of the large headline. If there is no active
                //  large headline, we can flush the small headline directly into the children list. After flushing
                //  the existing small headline, we can start the new small headline section.
                if (Boolean.TRUE.equals(hdl.getSmall())) {
                    if (currentSmallHeadline != null) {
                        var smallHeadlineItem = new OZGCloudFormDataItem(
                                currentSmallHeadline.getId(),
                                currentSmallHeadline.getResolvedContent(),
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
                                    currentSmallHeadline.getId(),
                                    currentSmallHeadline.getResolvedContent(),
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
                                currentLargeHeadline.getId(),
                                currentLargeHeadline.getResolvedContent(),
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
                var childFormData = buildFormData(child, elementData);

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
                        currentSmallHeadline.getId(),
                        currentSmallHeadline.getResolvedContent(),
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
                    currentLargeHeadline.getId(),
                    currentLargeHeadline.getResolvedContent(),
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
            @Nonnull GroupLayoutElement groupLayout,
            @Nonnull ElementData elementData
    ) {
        return buildChildList(groupLayout.getChildren(), elementData);
    }

    private OZGCloudFormDataItem buildReplicatingContainerLayout(
            @Nonnull ReplicatingContainerLayoutElement replicatingContainerLayout,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData
                .mustGet(replicatingContainerLayout);

        var childElementDataObject = edo.getValue();
        List<ElementData> childElementData = new LinkedList<>();

        if (childElementDataObject instanceof List<?> list) {
            for (var itemObj : list) {
                if (itemObj instanceof ElementData str) {
                    childElementData.add(str);
                }
            }
        }

        var childItems = new LinkedList<OZGCloudFormDataItem>();

        for (int i = 0; i < childElementData.size(); i++) {
            var childED = childElementData.get(i);
            var childFormData = buildChildList(
                    replicatingContainerLayout.getChildren(),
                    childED
            );

            var childItem = new OZGCloudFormDataItem(
                    replicatingContainerLayout.getId() + "_" + i,
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
                replicatingContainerLayout.getId(),
                replicatingContainerLayout.getLabel(),
                null,
                null,
                null,
                null,
                childItems
        );
    }

    private OZGCloudFormDataItem buildCheckboxField(
            @Nonnull CheckboxInputElement cbx,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(cbx);

        return new OZGCloudFormDataItem(
                cbx.getId(),
                cbx.getLabel(),
                null,
                null,
                null,
                edo.getValue(Boolean.class, false),
                null
        );
    }

    private OZGCloudFormDataItem buildNumberField(
            @Nonnull NumberInputElement cbx,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(cbx);
        var val = edo.getValue(Number.class, null);

        return new OZGCloudFormDataItem(
                cbx.getId(),
                cbx.getLabel(),
                val == null ? "Keine Angabe" : null,
                val != null ? val : null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildDateField(
            @Nonnull DateInputElement dateField,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(dateField);
        var date = dateField
                .formatValue(edo.getValue());

        if (date == null) {
            return new OZGCloudFormDataItem(
                    dateField.getId(),
                    dateField.getLabel(),
                    "Keine Angabe",
                    null,
                    null,
                    null,
                    null
            );
        }


        var displayValue = date
                .format(
                        DateTimeFormatter
                                .ofPattern("yyyy-MM-dd")
                                .withZone(DateInputElement.zoneId)
                );


        return new OZGCloudFormDataItem(
                dateField.getId(),
                dateField.getLabel(),
                null,
                null,
                displayValue,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildFileUploadField(
            @Nonnull FileUploadInputElement fileUploadField,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(fileUploadField);

        var items = fileUploadField.formatValue(edo.getValue());

        var displayValue = items != null ? items
                .stream()
                .map(FileUploadInputElementItem::getName)
                .collect(Collectors.joining(", ")) : "";

        if (displayValue.isEmpty()) {
            displayValue = "Keine Anlagen hochgeladen";
        }

        return new OZGCloudFormDataItem(
                fileUploadField.getId(),
                fileUploadField.getLabel(),
                displayValue,
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildMultiCheckboxField(
            @Nonnull MultiCheckboxInputElement multiCheckboxField,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(multiCheckboxField);

        var items = multiCheckboxField.formatValue(edo.getValue());

        var displayValue = multiCheckboxField
                .toDisplayValue(items);

        return new OZGCloudFormDataItem(
                multiCheckboxField.getId(),
                multiCheckboxField.getLabel(),
                displayValue,
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildRadioField(
            @Nonnull RadioInputElement radioField,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(radioField);

        return new OZGCloudFormDataItem(
                radioField.getId(),
                radioField.getLabel(),
                radioField.toDisplayValue(edo.getValue(String.class, null)),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildSelectField(
            @Nonnull SelectInputElement selectField,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(selectField);

        return new OZGCloudFormDataItem(
                selectField.getId(),
                selectField.getLabel(),
                selectField.toDisplayValue(edo.getValue(String.class, null)),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildTableField(
            @Nonnull TableInputElement tableField,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(tableField);

        var rows = tableField.formatValue(edo.getValue());
        if (rows == null || rows.isEmpty()) {
            return new OZGCloudFormDataItem(
                    tableField.getId(),
                    tableField.getLabel(),
                    "Keine Angabe",
                    null,
                    null,
                    null,
                    null
            );
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
                        tableField.getId() + "_" + i + "_" + column.getLabel().replaceAll("\\W+", "_"),
                        column.getLabel(),
                        stringValue,
                        numberValue,
                        null,
                        null,
                        null
                ));
            }

            rowItems.add(new OZGCloudFormDataItem(
                    tableField.getId() + "_" + i,
                    "Zeile " + (i + 1),
                    null,
                    null,
                    null,
                    null,
                    cellItems
            ));
        }

        return new OZGCloudFormDataItem(
                tableField.getId(),
                tableField.getLabel(),
                null,
                null,
                null,
                null,
                rowItems
        );
    }

    private OZGCloudFormDataItem buildTextField(
            @Nonnull TextInputElement textField,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(textField);

        return new OZGCloudFormDataItem(
                textField.getId(),
                textField.getLabel(),
                textField.toDisplayValue(edo.getValue(String.class, null)),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildTimeField(
            @Nonnull TimeInputElement timeField,
            @Nonnull ElementData elementData
    ) {
        var edo = elementData.mustGet(timeField);

        return new OZGCloudFormDataItem(
                timeField.getId(),
                timeField.getLabel(),
                timeField.toDisplayValue(DateInputElement._formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }
}
