package de.aivot.GoverBackend.ozgCloud.services;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementState;
import de.aivot.GoverBackend.elements.models.ComputedElementStates;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudFormDataItem;
import jakarta.annotation.Nonnull;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class OZGCloudDataFormatService {
    @Nonnull
    public List<OZGCloudFormDataItem> buildFormData(
            @Nonnull BaseElement currentElement,
            @Nonnull AuthoredElementValues authoredElementValues,
            @Nonnull DerivedRuntimeElementData runtimeElementData
    ) {
        return buildFormData(
                currentElement,
                new FormattingContext(authoredElementValues, runtimeElementData)
        );
    }

    @Nonnull
    private List<OZGCloudFormDataItem> buildFormData(
            @Nonnull BaseElement currentElement,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(currentElement);

        // Check if the element is visible.
        // If not, yield an empty list.
        // If there is no explicit visibility setting for the element, we assume it is visible.
        if (!edo.getIsVisible()) {
            return List.of();
        }

        // Check if there is an override for the element.
        // If yes, we use the override instead of the current element for building the form data.
        var override = edo
                .getResolvedElement();

        return switch (override) {
            case FormLayoutElement rootElement -> buildRoot(rootElement, context);
            case GenericStepElement stepElement -> List.of(buildStep(stepElement, context));
            case GroupLayoutElement groupLayout -> buildGroupLayout(groupLayout, context);
            case ReplicatingContainerLayoutElement replicatingContainerLayout ->
                    List.of(buildReplicatingContainerLayout(replicatingContainerLayout, context));
            case CheckboxInputElement cbx -> List.of(buildCheckboxField(cbx, context));
            case DateInputElement dateField -> List.of(buildDateField(dateField, context));
            case DateTimeInputElement dateTimeField -> List.of(buildDateTimeField(dateTimeField, context));
            case DateRangeInputElement dateRangeField -> List.of(buildDateRangeField(dateRangeField, context));
            case TimeRangeInputElement timeRangeField -> List.of(buildTimeRangeField(timeRangeField, context));
            case DateTimeRangeInputElement dateTimeRangeField -> List.of(buildDateTimeRangeField(dateTimeRangeField, context));
            case MapPointInputElement mapPointField -> List.of(buildMapPointField(mapPointField, context));
            case DomainAndUserSelectInputElement domainAndUserSelectField ->
                    List.of(buildDomainAndUserSelectField(domainAndUserSelectField, context));
            case AssignmentContextInputElement assignmentContextField ->
                    List.of(buildAssignmentContextField(assignmentContextField, context));
            case RichTextInputElement richTextInputField -> List.of(buildRichTextInputField(richTextInputField, context));
            case CodeInputElement codeInputField -> List.of(buildCodeInputField(codeInputField, context));
            case NoCodeInputElement noCodeInputField -> List.of(buildNoCodeInputField(noCodeInputField, context));
            case UiDefinitionInputElement uiDefinitionInputField ->
                    List.of(buildUiDefinitionInputField(uiDefinitionInputField, context));
            case DataModelSelectInputElement dataModelSelectField ->
                    List.of(buildDataModelSelectField(dataModelSelectField, context));
            case DataObjectSelectInputElement dataObjectSelectField ->
                    List.of(buildDataObjectSelectField(dataObjectSelectField, context));
            case FileUploadInputElement fileUploadField -> List.of(buildFileUploadField(fileUploadField, context));
            case MultiCheckboxInputElement multiCheckboxField ->
                    List.of(buildMultiCheckboxField(multiCheckboxField, context));
            case NumberInputElement numberField -> List.of(buildNumberField(numberField, context));
            case SelectInputElement selectField -> List.of(buildSelectField(selectField, context));
            case RadioInputElement radioField -> List.of(buildRadioField(radioField, context));
            case TableInputElement tableField -> List.of(buildTableField(tableField, context));
            case TextInputElement textField -> List.of(buildTextField(textField, context));
            case ChipInputElement chipInputField -> List.of(buildChipInputField(chipInputField, context));
            case TimeInputElement timeField -> List.of(buildTimeField(timeField, context));
            default -> List.of();
        };
    }

    private List<OZGCloudFormDataItem> buildRoot(
            @Nonnull FormLayoutElement rootElement,
            @Nonnull FormattingContext context
    ) {
        return rootElement
                .getChildren()
                .stream()
                .flatMap(child -> buildFormData(child, context).stream())
                .toList();
    }

    private OZGCloudFormDataItem buildStep(
            @Nonnull GenericStepElement stepElement,
            @Nonnull FormattingContext context
    ) {
        var children = buildChildList(
                stepElement.getChildren(),
                context
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
            @Nonnull FormattingContext context
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
                var childFormData = buildFormData(child, context);

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
            @Nonnull FormattingContext context
    ) {
        return buildChildList(groupLayout.getChildren(), context);
    }

    private OZGCloudFormDataItem buildReplicatingContainerLayout(
            @Nonnull ReplicatingContainerLayoutElement replicatingContainerLayout,
            @Nonnull FormattingContext context
    ) {
        var childItems = new LinkedList<OZGCloudFormDataItem>();
        var rowCount = context.getReplicatingRowCount(replicatingContainerLayout);

        for (int i = 0; i < rowCount; i++) {
            var childContext = context.createRowContext(replicatingContainerLayout, i);
            var childFormData = buildChildList(
                    replicatingContainerLayout.getChildren(),
                    childContext
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
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(cbx);

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
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(cbx);
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
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(dateField);
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

    private OZGCloudFormDataItem buildDateTimeField(
            @Nonnull DateTimeInputElement dateTimeField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(dateTimeField);

        var dateTime = dateTimeField.formatValue(edo.getValue());

        var displayValue = dateTimeField.toDisplayValue(dateTime);

        return new OZGCloudFormDataItem(
                dateTimeField.getId(),
                dateTimeField.getLabel(),
                displayValue,
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildDateRangeField(
            @Nonnull DateRangeInputElement dateRangeField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(dateRangeField);

        return new OZGCloudFormDataItem(
                dateRangeField.getId(),
                dateRangeField.getLabel(),
                dateRangeField.toDisplayValue(dateRangeField.formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildTimeRangeField(
            @Nonnull TimeRangeInputElement timeRangeField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(timeRangeField);

        return new OZGCloudFormDataItem(
                timeRangeField.getId(),
                timeRangeField.getLabel(),
                timeRangeField.toDisplayValue(timeRangeField.formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildDateTimeRangeField(
            @Nonnull DateTimeRangeInputElement dateTimeRangeField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(dateTimeRangeField);

        return new OZGCloudFormDataItem(
                dateTimeRangeField.getId(),
                dateTimeRangeField.getLabel(),
                dateTimeRangeField.toDisplayValue(dateTimeRangeField.formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildMapPointField(
            @Nonnull MapPointInputElement mapPointField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(mapPointField);
        var value = mapPointField.formatValue(edo.getValue());

        return new OZGCloudFormDataItem(
                mapPointField.getId(),
                mapPointField.getLabel(),
                mapPointField.toDisplayValue(value),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildFileUploadField(
            @Nonnull FileUploadInputElement fileUploadField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(fileUploadField);

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
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(multiCheckboxField);

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
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(radioField);

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
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(selectField);

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
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(tableField);

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
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(textField);

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

    private OZGCloudFormDataItem buildChipInputField(
            @Nonnull ChipInputElement chipInputField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(chipInputField);

        var items = chipInputField.formatValue(edo.getValue());

        var displayValue = chipInputField
                .toDisplayValue(items);

        return new OZGCloudFormDataItem(
                chipInputField.getId(),
                chipInputField.getLabel(),
                displayValue,
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildDomainAndUserSelectField(
            @Nonnull DomainAndUserSelectInputElement domainAndUserSelectField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(domainAndUserSelectField);
        var value = domainAndUserSelectField.formatValue(edo.getValue());

        return new OZGCloudFormDataItem(
                domainAndUserSelectField.getId(),
                domainAndUserSelectField.getLabel(),
                domainAndUserSelectField.toDisplayValue(value),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildAssignmentContextField(
            @Nonnull AssignmentContextInputElement assignmentContextField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(assignmentContextField);
        var value = assignmentContextField.formatValue(edo.getValue());

        return new OZGCloudFormDataItem(
                assignmentContextField.getId(),
                assignmentContextField.getLabel(),
                assignmentContextField.toDisplayValue(value),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildRichTextInputField(
            @Nonnull RichTextInputElement richTextInputField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(richTextInputField);

        return new OZGCloudFormDataItem(
                richTextInputField.getId(),
                richTextInputField.getLabel(),
                richTextInputField.toDisplayValue(richTextInputField.formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildCodeInputField(
            @Nonnull CodeInputElement codeInputField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(codeInputField);

        return new OZGCloudFormDataItem(
                codeInputField.getId(),
                codeInputField.getLabel(),
                codeInputField.toDisplayValue(codeInputField.formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildNoCodeInputField(
            @Nonnull NoCodeInputElement noCodeInputField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(noCodeInputField);

        return new OZGCloudFormDataItem(
                noCodeInputField.getId(),
                noCodeInputField.getLabel(),
                noCodeInputField.toDisplayValue(noCodeInputField.formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildUiDefinitionInputField(
            @Nonnull UiDefinitionInputElement uiDefinitionInputField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(uiDefinitionInputField);

        return new OZGCloudFormDataItem(
                uiDefinitionInputField.getId(),
                uiDefinitionInputField.getLabel(),
                uiDefinitionInputField.toDisplayValue(uiDefinitionInputField.formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildDataModelSelectField(
            @Nonnull DataModelSelectInputElement dataModelSelectField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(dataModelSelectField);

        return new OZGCloudFormDataItem(
                dataModelSelectField.getId(),
                dataModelSelectField.getLabel(),
                dataModelSelectField.toDisplayValue(dataModelSelectField.formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildDataObjectSelectField(
            @Nonnull DataObjectSelectInputElement dataObjectSelectField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(dataObjectSelectField);

        return new OZGCloudFormDataItem(
                dataObjectSelectField.getId(),
                dataObjectSelectField.getLabel(),
                dataObjectSelectField.toDisplayValue(dataObjectSelectField.formatValue(edo.getValue())),
                null,
                null,
                null,
                null
        );
    }

    private OZGCloudFormDataItem buildTimeField(
            @Nonnull TimeInputElement timeField,
            @Nonnull FormattingContext context
    ) {
        var edo = context.resolve(timeField);

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

    private static final class FormattingContext {
        private final AuthoredElementValues authoredElementValues;
        private final DerivedRuntimeElementData runtimeElementData;

        private FormattingContext(@Nonnull AuthoredElementValues authoredElementValues,
                                  @Nonnull DerivedRuntimeElementData runtimeElementData) {
            this.authoredElementValues = authoredElementValues;
            this.runtimeElementData = runtimeElementData;
        }

        private ResolvedElementData resolve(@Nonnull BaseElement element) {
            return new ResolvedElementData(element, authoredElementValues, runtimeElementData);
        }

        private int getReplicatingRowCount(@Nonnull ReplicatingContainerLayoutElement element) {
            var authoredRows = ObjectMapperFactory.Utils.convertToList(
                    authoredElementValues.get(element.getId()),
                    AuthoredElementValues.class
            );
            var effectiveRows = ObjectMapperFactory.Utils.convertToList(
                    runtimeElementData.getEffectiveValues().get(element.getId()),
                    EffectiveElementValues.class
            );
            var subStates = runtimeElementData
                    .getElementStates()
                    .getOrDefault(element.getId(), ComputedElementState.create())
                    .getSubStates();

            var subStateCount = subStates != null ? subStates.size() : 0;
            return Math.max(authoredRows.size(), Math.max(effectiveRows.size(), subStateCount));
        }

        private FormattingContext createRowContext(@Nonnull ReplicatingContainerLayoutElement element, int index) {
            var authoredRows = ObjectMapperFactory.Utils.convertToList(
                    authoredElementValues.get(element.getId()),
                    AuthoredElementValues.class
            );
            var effectiveRows = ObjectMapperFactory.Utils.convertToList(
                    runtimeElementData.getEffectiveValues().get(element.getId()),
                    EffectiveElementValues.class
            );
            var subStates = runtimeElementData
                    .getElementStates()
                    .getOrDefault(element.getId(), ComputedElementState.create())
                    .getSubStates();

            var rowAuthoredValues = index < authoredRows.size()
                    ? authoredRows.get(index)
                    : new AuthoredElementValues();
            var rowEffectiveValues = index < effectiveRows.size()
                    ? effectiveRows.get(index)
                    : ObjectMapperFactory
                    .getInstance()
                    .convertValue(rowAuthoredValues, EffectiveElementValues.class);
            var rowElementStates = subStates != null && index < subStates.size()
                    ? subStates.get(index)
                    : new ComputedElementStates();

            return new FormattingContext(
                    rowAuthoredValues,
                    new DerivedRuntimeElementData(rowEffectiveValues, rowElementStates)
            );
        }
    }

    private static final class ResolvedElementData {
        private final BaseElement element;
        private final AuthoredElementValues authoredElementValues;
        private final DerivedRuntimeElementData runtimeElementData;

        private ResolvedElementData(@Nonnull BaseElement element,
                                    @Nonnull AuthoredElementValues authoredElementValues,
                                    @Nonnull DerivedRuntimeElementData runtimeElementData) {
            this.element = element;
            this.authoredElementValues = authoredElementValues;
            this.runtimeElementData = runtimeElementData;
        }

        @Nonnull
        private Boolean getIsVisible() {
            return runtimeElementData
                    .getElementStates()
                    .getOrDefault(element.getId(), ComputedElementState.create())
                    .getVisible();
        }

        @Nonnull
        private BaseElement getResolvedElement() {
            var override = runtimeElementData
                    .getElementStates()
                    .getOrDefault(element.getId(), ComputedElementState.create())
                    .getOverride();
            return override != null ? override : element;
        }

        private Object getValue() {
            var authoredValue = authoredElementValues.get(element.getId());
            if (authoredValue != null) {
                return authoredValue;
            }
            return runtimeElementData.getEffectiveValues().get(element.getId());
        }

        private <T> T getValue(@Nonnull Class<T> clazz, T defaultValue) {
            var value = getValue();
            if (value == null || !clazz.isAssignableFrom(value.getClass())) {
                return defaultValue;
            }
            return clazz.cast(value);
        }
    }
}
