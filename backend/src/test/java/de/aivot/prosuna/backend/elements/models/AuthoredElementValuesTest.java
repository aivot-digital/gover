package de.aivot.prosuna.backend.elements.models;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.input.InputVariableReference;
import de.aivot.prosuna.backend.elements.models.input.LowCodeAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.NoCodeAuthoredInputValue;
import de.aivot.prosuna.backend.elements.models.input.VariableAuthoredInputValue;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeProcessDataReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeInstanceDataReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeNodeDataReference;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthoredElementValuesTest {
    @Test
    void cloneCopiesNoCodeTreesIncludingMutableStaticValuesAndNullOperands() {
        var payload = new ArrayList<>(List.of("original"));
        var nested = NoCodeExpression.of("nested", NoCodeStaticValue.of(payload));
        var expression = NoCodeExpression.of("outer", nested, null, NoCodeStaticValue.of(""));
        var values = new AuthoredElementValues();
        values.put("expression", new NoCodeAuthoredInputValue(expression));

        var copy = values.clone();
        assertEquals(values, copy);
        var copiedExpression = (NoCodeExpression) ((NoCodeAuthoredInputValue) copy.get("expression")).operand();
        var copiedOperands = copiedExpression.getOperands();
        var copiedNested = (NoCodeExpression) copiedOperands.iterator().next();
        var copiedStatic = (NoCodeStaticValue) copiedNested.getOperands().iterator().next();
        assertNotSame(expression, copiedExpression);
        assertNotSame(expression.getOperands(), copiedOperands);
        assertNotSame(nested, copiedNested);
        assertNull(new ArrayList<>(copiedOperands).get(1));

        ((List<?>) copiedStatic.getValue()).clear();
        copiedNested.setOperatorIdentifier("changed");
        copiedOperands.clear();
        assertEquals(List.of("original"), payload);
        assertEquals("nested", nested.getOperatorIdentifier());
        assertEquals(3, expression.getOperands().size());
    }

    @Test
    void cloneCopiesAllMutableNoCodeReferenceTypes() {
        var field = new NoCodeReference("field");
        var process = new NoCodeProcessDataReference("person.name");
        var instance = new NoCodeInstanceDataReference("id");
        var node = new NoCodeNodeDataReference("form", "name");
        var values = new AuthoredElementValues();
        values.put("field", new NoCodeAuthoredInputValue(field));
        values.put("process", new NoCodeAuthoredInputValue(process));
        values.put("instance", new NoCodeAuthoredInputValue(instance));
        values.put("node", new NoCodeAuthoredInputValue(node));

        var copy = values.clone();
        assertEquals(values, copy);
        ((NoCodeReference) ((NoCodeAuthoredInputValue) copy.get("field")).operand()).setElementId("changed");
        ((NoCodeProcessDataReference) ((NoCodeAuthoredInputValue) copy.get("process")).operand()).setPath("changed");
        ((NoCodeInstanceDataReference) ((NoCodeAuthoredInputValue) copy.get("instance")).operand()).setPath("changed");
        ((NoCodeNodeDataReference) ((NoCodeAuthoredInputValue) copy.get("node")).operand())
                .setNodeDataKey("changed").setPath("changed");

        assertEquals("field", field.getElementId());
        assertEquals("person.name", process.getPath());
        assertEquals("id", instance.getPath());
        assertEquals("form", node.getNodeDataKey());
        assertEquals("name", node.getPath());
    }

    @Test
    void cloneCopiesOperandsInsideLiteralPayloadsWithoutNormalizingDrafts() {
        var emptyExpression = new NoCodeExpression();
        var emptyText = NoCodeStaticValue.of("");
        var nullValue = NoCodeStaticValue.of(null);
        var values = new AuthoredElementValues().putLiteral("draft", Map.of(
                "operands", List.of(emptyExpression, emptyText, nullValue)));

        var copy = values.clone();
        assertEquals(values, copy);
        var operands = (List<?>) ((Map<?, ?>) copy.getLiteral("draft")).get("operands");
        var copiedExpression = (NoCodeExpression) operands.get(0);
        assertNull(copiedExpression.getOperands());
        copiedExpression.setOperatorIdentifier("changed");
        ((NoCodeStaticValue) operands.get(1)).setValue("changed");
        ((NoCodeStaticValue) operands.get(2)).setValue(5);
        assertNull(emptyExpression.getOperatorIdentifier());
        assertEquals(NoCodeStaticValue.of(""), emptyText);
        assertEquals(NoCodeStaticValue.of(null), nullValue);
    }

    @Test
    void clonePreservesTypedRowsAndCopiesTheirNestedAuthoredValues() {
        var inner = new ReplicatingContainerLayoutElementValue().setId("inner")
                .setValues(new AuthoredElementValues().putLiteral("name", "original"));
        var rowValues = new AuthoredElementValues().putLiteral("children", List.of(inner));
        rowValues.put("expression", new NoCodeAuthoredInputValue(NoCodeStaticValue.of(3)));
        var outer = new ReplicatingContainerLayoutElementValue().setId("outer").setValues(rowValues);
        var values = new AuthoredElementValues().putLiteral("rows", new ReplicatingContainerLayoutElementValue[]{outer});
        values.putLiteral("emptyRow", new ReplicatingContainerLayoutElementValue());

        var copy = values.clone();
        var copiedOuter = ((ReplicatingContainerLayoutElementValue[]) copy.getLiteral("rows"))[0];
        var copiedInner = (ReplicatingContainerLayoutElementValue) ((List<?>) copiedOuter.getValues().getLiteral("children")).getFirst();
        assertEquals("outer", copiedOuter.getId());
        assertEquals("inner", copiedInner.getId());
        assertNotSame(outer, copiedOuter);
        assertNotSame(inner, copiedInner);
        assertNull(((ReplicatingContainerLayoutElementValue) copy.getLiteral("emptyRow")).getValues());
        copiedInner.setId("changed").getValues().putLiteral("name", "changed");
        ((NoCodeStaticValue) ((NoCodeAuthoredInputValue) copiedOuter.getValues().get("expression")).operand()).setValue(9);

        assertEquals("inner", inner.getId());
        assertEquals("original", inner.getValues().getLiteral("name"));
        assertEquals(3, ((NoCodeStaticValue) ((NoCodeAuthoredInputValue) rowValues.get("expression")).operand()).getValue());
    }

    @Test
    void cloneSharesOnlyImmutableDynamicValues() {
        var values = new AuthoredElementValues();
        var variable = new VariableAuthoredInputValue(new InputVariableReference(InputVariableSource.ProcessData, "name", null));
        var code = new LowCodeAuthoredInputValue("return $.amount;");
        values.put("variable", variable);
        values.put("code", code);

        var copy = values.clone();
        assertSame(variable, copy.get("variable"));
        assertSame(code, copy.get("code"));
        copy.put("code", new LowCodeAuthoredInputValue("return 0;"));
        assertSame(code, values.get("code"));
    }

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
