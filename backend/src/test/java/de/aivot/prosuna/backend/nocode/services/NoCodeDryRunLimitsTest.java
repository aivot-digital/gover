package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoCodeDryRunLimitsTest {
    @Test
    void checksDepthBeforeRecursiveEvaluation() {
        NoCodeOperand expression = new NoCodeStaticValue("leaf");
        for (int i = 1; i < NoCodeDryRunLimits.MAX_DEPTH; i++) expression = expression(expression);
        assertTrue(NoCodeDryRunLimits.expressionFits(expression));
        assertFalse(NoCodeDryRunLimits.expressionFits(expression(expression)));
    }

    @Test
    void countsNodesAcrossAllBranchesIncludingEmptySlots() {
        var expression = new NoCodeExpression();
        expression.setOperands(Collections.nCopies(NoCodeDryRunLimits.MAX_NODES - 1, null));
        assertTrue(NoCodeDryRunLimits.expressionFits(expression));
        expression.setOperands(Collections.nCopies(NoCodeDryRunLimits.MAX_NODES, null));
        assertFalse(NoCodeDryRunLimits.expressionFits(expression));
    }

    @Test
    void countsActualUtf8JsonBytesIncludingEscapesWithoutKeepingOutput() {
        var mapper = JsonMapperTestUtils.createMapper();
        var value = List.of("Größe", "\n", "😀");
        int bytes = mapper.writeValueAsBytes(value).length;
        assertTrue(NoCodeDryRunLimits.jsonFits(mapper, value, bytes));
        assertFalse(NoCodeDryRunLimits.jsonFits(mapper, value, bytes - 1));
        assertFalse(NoCodeDryRunLimits.jsonFits(mapper, "x".repeat(2_000_000), 1024));
    }

    private NoCodeExpression expression(NoCodeOperand child) {
        var expression = new NoCodeExpression();
        expression.setOperatorIdentifier("not");
        expression.setOperands(List.of(child));
        return expression;
    }
}
