package de.aivot.GoverBackend.javascript.providers;

import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.javascript.services.JavascriptEngine;
import de.aivot.GoverBackend.plugins.core.v1.javascript.DateJavascriptV1;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DateJavascriptPluginTest {
    @Test
    void compareAndFormatDates() {
        try (var jsService = new JavascriptEngine(new DateJavascriptV1())) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    [
                        _date_v1.formatDate(_date_v1.createDate('2024-05-30'), 'yyyy-MM-dd'),
                        _date_v1.isSameDay('2024-05-30', '30.05.2024'),
                        _date_v1.isBefore('2024-05-29', '2024-05-30'),
                        _date_v1.isBeforeOrSameDay('2024-05-30', '2024-05-30'),
                        _date_v1.isAfter('2024-06-01', '2024-05-30'),
                        _date_v1.isAfterOrSameDay('2024-05-30', '2024-05-30')
                    ];
                    """));

            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("2024-05-30", values.get(0));
            assertEquals(true, values.get(1));
            assertEquals(true, values.get(2));
            assertEquals(true, values.get(3));
            assertEquals(true, values.get(4));
            assertEquals(true, values.get(5));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void addSubtractAndDiffDates() {
        try (var jsService = new JavascriptEngine(new DateJavascriptV1())) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    [
                        _date_v1.formatDate(_date_v1.addDays('2024-02-28', 1), 'yyyy-MM-dd'),
                        _date_v1.formatDate(_date_v1.addWeeks('2024-02-28', 1), 'yyyy-MM-dd'),
                        _date_v1.formatDate(_date_v1.addMonths('2024-01-31', 1), 'yyyy-MM-dd'),
                        _date_v1.formatDate(_date_v1.addYears('2024-02-29', 1), 'yyyy-MM-dd'),
                        _date_v1.formatDate(_date_v1.subtractDays('2024-03-01', 1), 'yyyy-MM-dd'),
                        _date_v1.diff('2024-05-01', '2024-05-15', 'days'),
                        _date_v1.diff('2024-05-01', '2024-05-15', 'weeks'),
                        _date_v1.diff('2024-01-01', '2024-04-01', 'months'),
                        _date_v1.diff('2022-01-01', '2024-01-01', 'years')
                    ];
                    """));

            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("2024-02-29", values.get(0));
            assertEquals("2024-03-06", values.get(1));
            assertEquals("2024-02-29", values.get(2));
            assertEquals("2025-02-28", values.get(3));
            assertEquals("2024-02-29", values.get(4));
            assertEquals(14, values.get(5));
            assertEquals(2.0, assertInstanceOf(Number.class, values.get(6)).doubleValue(), 0.001);
            assertEquals(3, values.get(7));
            assertEquals(2, values.get(8));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void formatDateReturnsNullForInvalidInput() {
        try (var jsService = new JavascriptEngine(new DateJavascriptV1())) {
            var result = jsService.evaluateCode(new JavascriptCode().setCode("_date_v1.formatDate('not-a-date', 'yyyy-MM-dd');"));

            assertTrue(result.isNull());
        } catch (Exception e) {
            fail(e);
        }
    }
}
