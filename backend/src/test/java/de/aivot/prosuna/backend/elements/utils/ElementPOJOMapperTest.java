package de.aivot.prosuna.backend.elements.utils;

import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.layout.EffectiveReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.enums.ElementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ElementPOJOMapperTest {
    private EffectiveElementValues testElementData;

    @BeforeEach
    void setUp() {
        testElementData = new EffectiveElementValues();
        testElementData.put("text_field", "Test Value");
        testElementData.put("switch_field", true);

        var replicatingChildData = new EffectiveElementValues();
        replicatingChildData.put("text_field", "Replicating Value 1");
        testElementData.put("replicating_container", List.of(
                new EffectiveReplicatingContainerLayoutElementValue().setValues(replicatingChildData)
        ));
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
        var textField = layout.findChild("text_field", TextInputElement.class).orElse(null);

        assertNotNull(layout);
        assertNotNull(textField);
        assertEquals(Boolean.TRUE, textField.getCopyable());
        assertNotNull(layout.findChild("switch_field", CheckboxInputElement.class).orElse(null));
        assertNotNull(layout.findChild("replicating_container", ReplicatingContainerLayoutElement.class).orElse(null));
        assertNotNull(layout.findChild("replicating_container", ReplicatingContainerLayoutElement.class).get().findChild("text_field"));
    }

    @Test
    void createFromPOJO_ShouldMapInputModePolicy() throws ElementDataConversionException {
        var layout = ElementPOJOMapper.createFromPOJO(DynamicBindingClass.class);
        var input = assertInstanceOf(
                BaseInputElement.class,
                layout.findChild("dynamic_field").orElseThrow()
        );

        assertEquals(List.of(InputMode.Literal, InputMode.Variable), input.getInputModePolicy().allowedModes());
        assertEquals(InputMode.Variable, input.getInputModePolicy().defaultMode());
        assertEquals(List.of(InputVariableSource.ProcessData), input.getInputModePolicy().allowedVariableSources());
    }

    @Test
    void createFromPOJO_ShouldAllowAllVariableSourcesByDefault() throws ElementDataConversionException {
        var layout = ElementPOJOMapper.createFromPOJO(DefaultVariableSourcesBindingClass.class);
        var input = assertInstanceOf(
                BaseInputElement.class,
                layout.findChild("dynamic_field").orElseThrow()
        );

        assertEquals(List.of(InputVariableSource.values()), input.getInputModePolicy().allowedVariableSources());
    }

    @Test
    void createFromPOJO_ShouldIgnoreDefaultVariableSourcesWithoutVariableMode() throws ElementDataConversionException {
        var layout = ElementPOJOMapper.createFromPOJO(NoVariableModeBindingClass.class);
        var input = assertInstanceOf(
                BaseInputElement.class,
                layout.findChild("dynamic_field").orElseThrow()
        );

        assertEquals(List.of(InputMode.Literal, InputMode.LowCode), input.getInputModePolicy().allowedModes());
        assertEquals(List.of(), input.getInputModePolicy().allowedVariableSources());
    }

    @Test
    void createFromPOJO_ShouldRejectPrimitiveDynamicFields() {
        assertThrows(
                ElementDataConversionException.class,
                () -> ElementPOJOMapper.createFromPOJO(PrimitiveDynamicBindingClass.class)
        );
    }

    @Test
    void createFromPOJO_ShouldMapDynamicTextPolicies() throws ElementDataConversionException {
        var layout = ElementPOJOMapper.createFromPOJO(DynamicTextBindingClass.class);
        var text = layout.findChild("text", TextInputElement.class).orElseThrow();
        var richText = layout.findChild("rich_text", RichTextInputElement.class).orElseThrow();

        assertEquals(
                List.of(InputVariableSource.ProcessData),
                text.getDynamicTextPolicy().variableSuggestionSources()
        );
        assertEquals(
                List.of(InputVariableSource.values()),
                richText.getDynamicTextPolicy().variableSuggestionSources()
        );
    }

    @Test
    void createFromPOJO_ShouldRejectDynamicTextOnUnsupportedElements() {
        assertThrows(
                ElementDataConversionException.class,
                () -> ElementPOJOMapper.createFromPOJO(UnsupportedDynamicTextBindingClass.class)
        );
    }

    @LayoutElementPOJOBinding(id = "root", type = ElementType.ConfigLayout)
    public static class ExampleBindingClass {
        @InputElementPOJOBinding(id = "text_field", type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Example Text Field"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "maxCharacters", intValue = 255),
                @ElementPOJOBindingProperty(key = "copyable", boolValue = true),
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

    @LayoutElementPOJOBinding(id = "sub_element", type = ElementType.GroupLayout)
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

    @LayoutElementPOJOBinding(id = "dynamic_root", type = ElementType.ConfigLayout)
    public static class DynamicBindingClass {
        @InputElementPOJOBinding(
                id = "dynamic_field",
                type = ElementType.Text,
                allowedInputModes = {InputMode.Literal, InputMode.Variable},
                defaultInputMode = InputMode.Variable,
                allowedVariableSources = {InputVariableSource.ProcessData}
        )
        private String dynamicField;

        public DynamicBindingClass() {
        }

        public DynamicBindingClass setDynamicField(String dynamicField) {
            this.dynamicField = dynamicField;
            return this;
        }
    }

    @LayoutElementPOJOBinding(id = "default_variable_sources_root", type = ElementType.ConfigLayout)
    public static class DefaultVariableSourcesBindingClass {
        @InputElementPOJOBinding(
                id = "dynamic_field",
                type = ElementType.Text,
                allowedInputModes = {InputMode.Literal, InputMode.Variable}
        )
        private String dynamicField;

        public DefaultVariableSourcesBindingClass() {
        }

        public DefaultVariableSourcesBindingClass setDynamicField(String dynamicField) {
            this.dynamicField = dynamicField;
            return this;
        }
    }

    @LayoutElementPOJOBinding(id = "no_variable_mode_root", type = ElementType.ConfigLayout)
    public static class NoVariableModeBindingClass {
        @InputElementPOJOBinding(
                id = "dynamic_field",
                type = ElementType.Text,
                allowedInputModes = {InputMode.Literal, InputMode.LowCode}
        )
        private String dynamicField;

        public NoVariableModeBindingClass() {
        }

        public NoVariableModeBindingClass setDynamicField(String dynamicField) {
            this.dynamicField = dynamicField;
            return this;
        }
    }

    @LayoutElementPOJOBinding(id = "primitive_dynamic_root", type = ElementType.ConfigLayout)
    public static class PrimitiveDynamicBindingClass {
        @InputElementPOJOBinding(
                id = "dynamic_field",
                type = ElementType.Number,
                allowedInputModes = {InputMode.Literal, InputMode.Variable},
                allowedVariableSources = {InputVariableSource.ProcessData}
        )
        private int dynamicField;

        public PrimitiveDynamicBindingClass() {
        }

        public PrimitiveDynamicBindingClass setDynamicField(int dynamicField) {
            this.dynamicField = dynamicField;
            return this;
        }
    }

    @LayoutElementPOJOBinding(id = "dynamic_text_root", type = ElementType.ConfigLayout)
    public static class DynamicTextBindingClass {
        @InputElementPOJOBinding(
                id = "text",
                type = ElementType.Text,
                dynamicText = true,
                dynamicTextVariableSuggestionSources = {InputVariableSource.ProcessData}
        )
        private String text;

        @InputElementPOJOBinding(id = "rich_text", type = ElementType.RichTextInput, dynamicText = true)
        private String richText;
    }

    @LayoutElementPOJOBinding(id = "unsupported_dynamic_text_root", type = ElementType.ConfigLayout)
    public static class UnsupportedDynamicTextBindingClass {
        @InputElementPOJOBinding(id = "number", type = ElementType.Number, dynamicText = true)
        private Double number;
    }
}
