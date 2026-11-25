package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoCodeExpressionDeserializationTest {
    @Test
    void shouldDeserializeNestedExpressionsWithoutOperands() {
        // Choosing a function stores "operands": null until its parameters are filled in; other stored
        // expressions omit the property entirely.
        var withNullOperands = new HashMap<String, Object>();
        withNullOperands.put("type", NoCodeExpression.TYPE_ID);
        withNullOperands.put("operatorIdentifier", "not");
        withNullOperands.put("operands", null);
        var withoutOperands = Map.of(
                "type", NoCodeExpression.TYPE_ID,
                "operatorIdentifier", "not"
        );
        var source = Map.of(
                "type", NoCodeExpression.TYPE_ID,
                "operatorIdentifier", "and",
                "operands", List.of(withNullOperands, withoutOperands)
        );

        var expression = JsonMapperFactory
                .getInstance()
                .convertValue(source, NoCodeOperand.class);

        var root = assertInstanceOf(NoCodeExpression.class, expression);
        assertEquals("and", root.getOperatorIdentifier());
        assertEquals(2, root.getOperands().size());
        for (var operand : root.getOperands()) {
            var nested = assertInstanceOf(NoCodeExpression.class, operand);
            assertEquals("not", nested.getOperatorIdentifier());
            assertNull(nested.getOperands());
        }
    }
}
