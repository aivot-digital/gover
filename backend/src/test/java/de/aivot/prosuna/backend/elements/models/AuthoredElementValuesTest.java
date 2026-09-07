package de.aivot.prosuna.backend.elements.models;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.input.InputVariableReference;
import de.aivot.prosuna.backend.elements.models.input.LowCodeAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.NoCodeAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.VariableAuthoredInputValue;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthoredElementValuesTest {
    @Test
    void literalValueJsonPathPlacesEnvelopeBeforeNestedDomainProperties() {
        assertEquals(
                List.of("formSlug", "value"),
                AuthoredElementValues.literalValueJsonPath("formSlug")
        );
        assertEquals(
                List.of("formLayout", "value", "publicTitle"),
                AuthoredElementValues.literalValueJsonPath("formLayout", "publicTitle")
        );
        assertThrows(IllegalArgumentException.class, AuthoredElementValues::literalValueJsonPath);
    }

    @Test
    void javaSerializationSupportsEveryInputModeForHibernateDirtyChecking() throws Exception {
        var values = new AuthoredElementValues();
        values.putLiteral("literal", Map.of("nested", List.of(1, 2)));
        values.put("variable", new VariableAuthoredInputValue(
                new InputVariableReference(InputVariableSource.ProcessData, "person.name", null)
        ));
        values.put("noCode", new NoCodeAuthoredInputValue(NoCodeStaticValue.of(3)));
        values.put("lowCode", new LowCodeAuthoredInputValue("$.amount * 2"));

        byte[] serialized;
        try (var bytes = new ByteArrayOutputStream(); var output = new ObjectOutputStream(bytes)) {
            output.writeObject(values);
            serialized = bytes.toByteArray();
        }

        try (var input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            assertEquals(values, input.readObject());
        }
    }

    @Test
    void cloneCreatesDeepCopyForNestedContainers() {
        var nestedValues = new AuthoredElementValues();
        nestedValues.putLiteral("name", "original");

        var listItem = new AuthoredElementValues();
        listItem.putLiteral("value", "list-original");

        var mapValue = new LinkedHashMap<String, Object>();
        mapValue.put("nested", nestedValues);

        var setValue = new LinkedHashSet<Object>();
        setValue.add(listItem);

        var values = new AuthoredElementValues();
        values.putLiteral("nested", nestedValues);
        values.putLiteral("list", new ArrayList<>(List.of(listItem)));
        values.putLiteral("map", mapValue);
        values.putLiteral("set", setValue);

        var clone = values.clone();

        assertEquals(values, clone);
        assertNotSame(values, clone);
        assertNotSame(values.getLiteral("nested"), clone.getLiteral("nested"));
        assertNotSame(values.getLiteral("list"), clone.getLiteral("list"));
        assertNotSame(values.getLiteral("map"), clone.getLiteral("map"));
        assertNotSame(values.getLiteral("set"), clone.getLiteral("set"));

        ((AuthoredElementValues) clone.getLiteral("nested")).putLiteral("name", "changed");
        ((AuthoredElementValues) ((List<?>) clone.getLiteral("list")).getFirst()).putLiteral("value", "list-changed");
        ((AuthoredElementValues) ((Map<?, ?>) clone.getLiteral("map")).get("nested")).putLiteral("name", "map-changed");

        assertEquals("original", nestedValues.getLiteral("name"));
        assertEquals("list-original", listItem.getLiteral("value"));
    }

    @Test
    void cloneCreatesDeepCopyForArrays() {
        var nestedValues = new AuthoredElementValues();
        nestedValues.putLiteral("name", "original");

        var values = new AuthoredElementValues();
        values.putLiteral("objects", new Object[]{nestedValues});
        values.putLiteral("numbers", new int[]{1, 2, 3});

        var clone = values.clone();

        assertArrayEquals((Object[]) values.getLiteral("objects"), (Object[]) clone.getLiteral("objects"));
        assertArrayEquals((int[]) values.getLiteral("numbers"), (int[]) clone.getLiteral("numbers"));
        assertNotSame(values.getLiteral("objects"), clone.getLiteral("objects"));
        assertNotSame(values.getLiteral("numbers"), clone.getLiteral("numbers"));
        assertNotSame(((Object[]) values.getLiteral("objects"))[0], ((Object[]) clone.getLiteral("objects"))[0]);

        ((AuthoredElementValues) ((Object[]) clone.getLiteral("objects"))[0]).putLiteral("name", "changed");

        assertEquals("original", nestedValues.getLiteral("name"));
    }
}
