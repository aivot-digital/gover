package de.aivot.prosuna.backend.elements.models.elements;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.input.DynamicTextPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DynamicTextElementTest {
    @ParameterizedTest
    @MethodSource("textElements")
    void preservesPolicyThroughPolymorphicJsonRoundTrip(BaseInputElement<String> element) {
        var mapper = JsonMapperTestUtils.createMapper();
        var policy = new DynamicTextPolicy(List.of(InputVariableSource.ProcessData));
        ((DynamicTextElement) element).setDynamicTextPolicy(policy);

        var json = mapper.writeValueAsString(element);
        assertEquals(mapper.valueToTree(policy), mapper.readTree(json).get("dynamicTextPolicy"));
        var restored = mapper.readValue(json, BaseElement.class);

        assertEquals(policy, assertInstanceOf(DynamicTextElement.class, restored).getDynamicTextPolicy());
        assertEquals(element, restored);
        assertEquals(element.hashCode(), restored.hashCode());
    }

    @ParameterizedTest
    @MethodSource("textElements")
    void includesPolicyInEquality(BaseInputElement<String> element) {
        var mapper = JsonMapperTestUtils.createMapper();
        var copy = mapper.readValue(mapper.writeValueAsString(element), BaseElement.class);
        var dynamicText = (DynamicTextElement) element;
        assertNull(dynamicText.getDynamicTextPolicy());
        assertEquals(element, copy);

        dynamicText.setDynamicTextPolicy(new DynamicTextPolicy());
        assertNotEquals(element, copy);
        ((DynamicTextElement) copy).setDynamicTextPolicy(new DynamicTextPolicy());
        assertEquals(element, copy);
        assertEquals(element.hashCode(), copy.hashCode());

        dynamicText.setDynamicTextPolicy(new DynamicTextPolicy(List.of(InputVariableSource.ProcessData)));
        assertNotEquals(element, copy);
        dynamicText.setDynamicTextPolicy(null);
        ((DynamicTextElement) copy).setDynamicTextPolicy(null);
        assertEquals(element, copy);
    }

    @Test
    void unsupportedInputsDoNotExposeOrDeserializePolicy() {
        var mapper = JsonMapperTestUtils.createMapper();
        for (var element : List.<BaseElement>of(new NumberInputElement(), new SelectInputElement())) {
            var data = mapper.convertValue(element, new TypeReference<Map<String, Object>>() {});
            assertFalse(data.containsKey("dynamicTextPolicy"));
            data.put("dynamicTextPolicy", Map.of("variableSuggestionSources", List.of("ProcessData")));

            var restored = mapper.convertValue(data, BaseElement.class);
            assertFalse(restored instanceof DynamicTextElement);
            assertFalse(mapper.valueToTree(restored).has("dynamicTextPolicy"));
        }
    }

    private static Stream<BaseInputElement<String>> textElements() {
        // Make the required default explicit so JSON round trips do not turn a backing null into false.
        return Stream.<BaseInputElement<String>>of(new TextInputElement(), new RichTextInputElement())
                .peek(element -> element.setRequired(false));
    }
}
