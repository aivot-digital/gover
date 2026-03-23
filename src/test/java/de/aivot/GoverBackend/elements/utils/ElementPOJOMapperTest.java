package de.aivot.GoverBackend.elements.utils;

import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.enums.ElementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ElementPOJOMapperTest {
    private EffectiveElementValues testElementData;

    @BeforeEach
    void setUp() {
        testElementData = new EffectiveElementValues();
        testElementData.put("text_field", "Test Value");
        testElementData.put("switch_field", true);

        var replicatingChildData = new EffectiveElementValues();
        replicatingChildData.put("text_field", "Replicating Value 1");
        testElementData.put("replicating_container", List.of(replicatingChildData));
    }

    @Test
    void mapToPOJO() throws ElementDataConversionException {
        var example = ElementPOJOMapper
                .mapToPOJO(testElementData, ExampleBindingClass.class);

        assertNotNull(example);
        assertNotNull(example.exampleField);
        assertNotNull(example.subGroup);
        assertNotNull(example.replicatingContainer);

        assertEquals("Test Value", example.exampleField);
        assertEquals(true, example.subGroup.exampleSwitch);
        assertEquals(1, example.replicatingContainer.size());
        assertEquals("Replicating Value 1", example.replicatingContainer.getFirst().replicatingTextField);
    }

    @Test
    void createFromPOJO() throws ElementDataConversionException {
        var layout = ElementPOJOMapper
                .createFromPOJO(ExampleBindingClass.class);

        assertNotNull(layout);
        assertNotNull(layout.findChild("text_field", TextInputElement.class).orElse(null));
        assertNotNull(layout.findChild("switch_field", CheckboxInputElement.class).orElse(null));
        assertNotNull(layout.findChild("replicating_container", ReplicatingContainerLayoutElement.class).orElse(null));
        assertNotNull(layout.findChild("replicating_container", ReplicatingContainerLayoutElement.class).get().findChild("text_field"));
    }

    @LayoutElementPOJOBinding(id = "root", type = ElementType.ConfigLayout)
    public static class ExampleBindingClass {
        @InputElementPOJOBinding(id = "text_field", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Example Text Field"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "maxCharacters", intValue = 255),
        })
        private String exampleField;

        private ExampleBindingSubClass subGroup;

        private List<ExampleBindingReplicatingClass> replicatingContainer;

        public ExampleBindingClass() {
        }

        public String getExampleField() {
            return exampleField;
        }

        public ExampleBindingClass setExampleField(String exampleField) {
            this.exampleField = exampleField;
            return this;
        }

        public ExampleBindingSubClass getSubGroup() {
            return subGroup;
        }

        public ExampleBindingClass setSubGroup(ExampleBindingSubClass subGroup) {
            this.subGroup = subGroup;
            return this;
        }

        public List<ExampleBindingReplicatingClass> getReplicatingContainer() {
            return replicatingContainer;
        }

        public ExampleBindingClass setReplicatingContainer(List<ExampleBindingReplicatingClass> replicatingContainer) {
            this.replicatingContainer = replicatingContainer;
            return this;
        }
    }

    @LayoutElementPOJOBinding(id = "sub_element", type = ElementType.Group)
    public static class ExampleBindingSubClass {
        @InputElementPOJOBinding(id = "switch_field", type = ElementType.Checkbox, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Example Switch"),
                @ElementPOJOBindingProperty(key = "required", boolValue = false),
                @ElementPOJOBindingProperty(key = "variant", strValue = "switch"),
        })
        private Boolean exampleSwitch;

        public ExampleBindingSubClass() {
        }

        public Boolean getExampleSwitch() {
            return exampleSwitch;
        }

        public ExampleBindingSubClass setExampleSwitch(Boolean exampleSwitch) {
            this.exampleSwitch = exampleSwitch;
            return this;
        }
    }

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = "replicating_container")
    public static class ExampleBindingReplicatingClass {
        @InputElementPOJOBinding(id = "text_field", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Replicating Text Field"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        private String replicatingTextField;

        public ExampleBindingReplicatingClass() {
        }

        public String getReplicatingTextField() {
            return replicatingTextField;
        }

        public ExampleBindingReplicatingClass setReplicatingTextField(String replicatingTextField) {
            this.replicatingTextField = replicatingTextField;
            return this;
        }

    }
}
