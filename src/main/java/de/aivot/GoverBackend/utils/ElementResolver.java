package de.aivot.GoverBackend.utils;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.models.elements.form.content.*;
import de.aivot.GoverBackend.models.elements.form.input.*;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.models.elements.form.layout.ReplicatingContainerLayout;

import java.util.Map;

public class ElementResolver {
    public static FormElement resolve(BaseElement parent, Map<String, Object> elementData) {
        Object typeObj = elementData.get("type");

        if (typeObj instanceof Integer) {
            Integer typeString = (Integer) elementData.get("type");

            ElementType type = ElementType.findElement(typeString).orElse(null);
            if (type != null) {
                switch (type) {
                    case Alert -> {
                        return new Alert(parent, elementData);
                    }
                    case Group -> {
                        return new GroupLayout(parent, elementData);
                    }
                    case Checkbox -> {
                        return new CheckboxField(parent, elementData);
                    }
                    case Date -> {
                        return new DateField(parent, elementData);
                    }
                    case Headline -> {
                        return new Headline(parent, elementData);
                    }
                    case MultiCheckbox -> {
                        return new MultiCheckboxField(parent, elementData);
                    }
                    case Number -> {
                        return new NumberField(parent, elementData);
                    }
                    case ReplicatingContainer -> {
                        return new ReplicatingContainerLayout(parent, elementData);
                    }
                    case Richtext -> {
                        return new RichText(parent, elementData);
                    }
                    case Radio -> {
                        return new RadioField(parent, elementData);
                    }
                    case Select -> {
                        return new SelectField(parent, elementData);
                    }
                    case Spacer -> {
                        return new Spacer(parent, elementData);
                    }
                    case Table -> {
                        return new TableField(parent, elementData);
                    }
                    case Text -> {
                        return new TextField(parent, elementData);
                    }
                    case Time -> {
                        return new TimeField(parent, elementData);
                    }
                    case Image -> {
                        return new Image(parent, elementData);
                    }
                    case FileUpload -> {
                        return new FileUploadField(parent, elementData);
                    }
                }
            }
        }

        return null;
    }
}
