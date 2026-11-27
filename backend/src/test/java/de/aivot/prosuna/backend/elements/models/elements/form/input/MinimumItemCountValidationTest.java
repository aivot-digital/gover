package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.enums.TableColumnDataType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MinimumItemCountValidationTest {
    @Test
    void treatsZeroTableBoundsAsUnrestricted() {
        var table = (TableInputElement) field(ElementType.Table, 0);
        table.setMaximumRows(0);
        assertDoesNotThrow(() -> table.validate(entries(ElementType.Table, 4)));
        assertDoesNotThrow(() -> table.validate(entries(ElementType.Table, 0)));
    }

    @ParameterizedTest
    @EnumSource(value = ElementType.class, names = {
            "ChipInput", "MultiCheckbox", "Table", "ReplicatingContainerLayout", "FileUpload",
            "ProcessInstanceAttachmentSetSelect", "DomainAndUserSelect", "AssignmentContext"
    })
    void separatesRequiredFromMinimumCount(ElementType type) {
        for (Integer minimum : new Integer[]{null, 0, 3}) {
            var field = field(type, minimum);
            for (var required : List.of(false, true)) {
                field.setRequired(required);
                for (var empty : new Object[]{null, entries(type, 0)}) {
                    if (required) {
                        assertThrows(ValidationException.class, () -> field.validate(empty));
                    } else {
                        assertDoesNotThrow(() -> field.validate(empty));
                    }
                }
                if (minimum != null && minimum > 0) {
                    assertThrows(ValidationException.class, () -> field.validate(entries(type, 1)));
                } else {
                    assertDoesNotThrow(() -> field.validate(entries(type, 1)));
                }
                assertDoesNotThrow(() -> field.validate(entries(type, 3)));
                if (type != ElementType.MultiCheckbox) {
                    assertThrows(ValidationException.class, () -> field.validate(entries(type, 4)));
                }
            }
        }
    }

    private static BaseInputElement<?> field(ElementType type, Integer minimum) {
        return switch (type) {
            case ChipInput -> new ChipInputElement().setMinItems(minimum).setMaxItems(3);
            case MultiCheckbox -> new MultiCheckboxInputElement().setMinimumRequiredOptions(minimum)
                    .setOptions(IntStream.range(0, 4).mapToObj(i -> new MultiCheckboxInputElementOption()
                            .setValue("item-" + i).setLabel("Item " + i)).toList());
            case Table -> new TableInputElement().setMinimumRequiredRows(minimum).setMaximumRows(3)
                    .setFields(List.of(new TableInputElementColumn().setKey("name").setLabel("Name").setDatatype(TableColumnDataType.String)));
            case ReplicatingContainerLayout -> new ReplicatingContainerLayoutElement().setMinimumRequiredSets(minimum).setMaximumSets(3);
            case FileUpload -> new FileUploadInputElement().setIsMultifile(true).setMinFiles(minimum).setMaxFiles(3);
            case ProcessInstanceAttachmentSetSelect -> new ProcessInstanceAttachmentSetSelectElement().setMinItems(minimum).setMaxItems(3);
            case DomainAndUserSelect -> new DomainAndUserSelectInputElement().setMinItems(minimum).setMaxItems(3);
            case AssignmentContext -> new AssignmentContextInputElement().setMinItems(minimum).setMaxItems(3);
            default -> throw new IllegalArgumentException(type.name());
        };
    }

    private static Object entries(ElementType type, int count) {
        var items = IntStream.range(0, count).mapToObj(i -> switch (type) {
            case Table -> Map.of("name", "Item " + i);
            case ReplicatingContainerLayout -> new ReplicatingContainerLayoutElementValue();
            case FileUpload -> new FileUploadInputElementItem().setName(i + ".pdf").setUri("file-" + i).setSize(1);
            case DomainAndUserSelect, AssignmentContext -> new DomainAndUserSelectInputElementValue("user", String.valueOf(i));
            default -> "item-" + i;
        }).toList();
        return type == ElementType.AssignmentContext
                ? Map.of("domainAndUserSelection", items)
                : items;
    }
}
