package de.aivot.GoverBackend.utils;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.models.elements.form.content.*;
import de.aivot.GoverBackend.models.elements.form.input.*;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.models.elements.form.layout.ReplicatingContainerLayout;

import java.util.Map;

public class ElementResolver {
    public static BaseFormElement resolve(Map<String, Object> elementData) {
        Object typeObj = elementData.get("type");

        if (typeObj instanceof Integer) {
            Integer typeString = (Integer) elementData.get("type");

            ElementType type = ElementType.findElement(typeString).orElse(null);
            if (type != null) {
                switch (type) {
                    case Alert -> {
                        return new Alert(elementData);
                    }
                    case Group -> {
                        return new GroupLayout(elementData);
                    }
                    case Checkbox -> {
                        return new CheckboxField(elementData);
                    }
                    case Date -> {
                        return new DateField(elementData);
                    }
                    case Headline -> {
                        return new Headline(elementData);
                    }
                    case MultiCheckbox -> {
                        return new MultiCheckboxField(elementData);
                    }
                    case Number -> {
                        return new NumberField(elementData);
                    }
                    case ReplicatingContainer -> {
                        return new ReplicatingContainerLayout(elementData);
                    }
                    case Richtext -> {
                        return new RichText(elementData);
                    }
                    case Radio -> {
                        return new RadioField(elementData);
                    }
                    case Select -> {
                        return new SelectField(elementData);
                    }
                    case Spacer -> {
                        return new Spacer(elementData);
                    }
                    case Table -> {
                        return new TableField(elementData);
                    }
                    case Text -> {
                        return new TextField(elementData);
                    }
                    case Time -> {
                        return new TimeField(elementData);
                    }
                    case Image -> {
                        return new Image(elementData);
                    }
                    case FileUpload -> {
                        return new FileUploadField(elementData);
                    }
                }
            }
        }

        return null;
    }
}
