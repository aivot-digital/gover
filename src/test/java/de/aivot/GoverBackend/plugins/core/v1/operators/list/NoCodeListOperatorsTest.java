package de.aivot.GoverBackend.plugins.core.v1.operators.list;

import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static de.aivot.GoverBackend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCodeListOperatorsTest {
    @Test
    void listAvgShouldReturnAverageOrZeroForEmptyList() throws NoCodeException {
        var operator = new NoCodeListAvgOperator();
        var data = runtime();

        var result = (BigDecimal) operator.evaluate(data, List.of(1, 2, 3)).getValue();
        var emptyResult = (BigDecimal) operator.evaluate(data, List.of()).getValue();

        assertEquals(0, result.compareTo(new BigDecimal("2.00000000")));
        assertEquals(0, emptyResult.compareTo(BigDecimal.ZERO));
    }

    @Test
    void listConcatShouldMergeInOrderWithoutMutatingSourceLists() throws NoCodeException {
        var operator = new NoCodeListConcatOperator();
        var data = runtime();
        var left = Arrays.asList("a", "b");
        var right = Arrays.asList("c", "d");

        var result = operator.evaluate(data, left, right).getValue();

        assertEquals(List.of("a", "b", "c", "d"), result);
        assertEquals(List.of("a", "b"), left);
        assertEquals(List.of("c", "d"), right);
    }

    @Test
    void listSumShouldSumAllValuesAndValidateArgumentCount() throws NoCodeException {
        var operator = new NoCodeListSumOperator();
        var data = runtime();

        var sum = (BigDecimal) operator.evaluate(data, List.of(1, 2, 3)).getValue();
        var emptySum = (BigDecimal) operator.evaluate(data, List.of()).getValue();

        assertEquals(0, sum.compareTo(new BigDecimal("6.00000000")));
        assertEquals(0, emptySum.compareTo(BigDecimal.ZERO));
        assertThrows(NoCodeException.class, () -> operator.evaluate(data, List.of(1), List.of(2)));
    }

    @Test
    void listSelectShouldProjectFieldValues() throws NoCodeException {
        var operator = new NoCodeListSelectOperator();
        var data = runtime();
        var list = List.of(
                Map.of("name", "A", "age", 20),
                Map.of("name", "B", "age", 30),
                "not-an-object"
        );

        var selected = operator.evaluate(data, list, "name").getValue();

        assertEquals(Arrays.asList("A", "B", null), selected);
    }

    @Test
    void listGetShouldSupportPositiveNegativeAndOutOfRangeIndexes() throws NoCodeException {
        var operator = new NoCodeListGetOperator();
        var data = runtime();
        var list = List.of("A", "B", "C");

        assertEquals("A", operator.evaluate(data, list, 0).getValue());
        assertEquals("C", operator.evaluate(data, list, -1).getValue());
        assertEquals("B", operator.evaluate(data, list, -2).getValue());
        assertNull(operator.evaluate(data, list, 5).getValue());
    }

    @Test
    void listContainsShouldHandleCastingAndNullMembership() throws NoCodeException {
        var operator = new NoCodeListContainsOperator();
        var data = runtime();

        assertTrue(operator.evaluate(data, List.of(1, 2, 3), "2").getValueAsBoolean());
        assertFalse(operator.evaluate(data, List.of(1, 2, 3), "9").getValueAsBoolean());
        assertTrue(operator.evaluate(data, Arrays.asList("a", null), null).getValueAsBoolean());
        assertFalse(operator.evaluate(data, List.of("a", "b"), null).getValueAsBoolean());
    }

    @Test
    void listLengthShouldReturnListSize() throws NoCodeException {
        var operator = new NoCodeListLengthOperator();
        var data = runtime();

        assertEquals(3, operator.evaluate(data, List.of("a", "b", "c")).getValue());
        assertEquals(0, operator.evaluate(data, List.of()).getValue());
    }
}
