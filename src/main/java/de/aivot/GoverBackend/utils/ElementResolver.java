package de.aivot.GoverBackend.utils;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.elements.models.form.BaseFormElement;
import de.aivot.GoverBackend.elements.models.form.content.*;
import de.aivot.GoverBackend.elements.models.form.input.*;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;

import java.util.Map;

public class ElementResolver {
    public static BaseFormElement resolve(Map<String, Object> elementData) {
        Object typeObj = elementData.get("type");

        if (typeObj == null) {
            return null;
        }

        ElementType type = switch (typeObj) {
            case ElementType elementType -> elementType;
            case Integer iTypeObj -> ElementType.findElement(iTypeObj).orElse(null);
            case String sTypeObj -> ElementType.findElement(Integer.parseInt(sTypeObj)).orElse(null);
            default -> null;
        };

        if (type == null) {
            return null;
        }

        return switch (type) {
            case Alert -> new Alert(elementData);
            case Group -> new GroupLayout(elementData);
            case Checkbox -> new CheckboxField(elementData);
            case Date -> new DateField(elementData);
            case Headline -> new Headline(elementData);
            case MultiCheckbox -> new MultiCheckboxField(elementData);
            case Number -> new NumberField(elementData);
            case ReplicatingContainer -> new ReplicatingContainerLayout(elementData);
            case Richtext -> new RichText(elementData);
            case Radio -> new RadioField(elementData);
            case Select -> new SelectField(elementData);
            case Spacer -> new Spacer(elementData);
            case Table -> new TableField(elementData);
            case Text -> new TextField(elementData);
            case Time -> new TimeField(elementData);
            case Image -> new Image(elementData);
            case FileUpload -> new FileUploadField(elementData);
            default -> null;
        };
    }
}
