package de.aivot.prosuna.backend.javascript.providers;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.models.CodeListColumnMapping;
import de.aivot.prosuna.backend.codeLists.services.CodeListService;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.v1.javascript.CodeListJavascriptV1;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeListJavascriptPluginTest {
    private static final Instant CREATED = Instant.parse("2024-05-01T10:15:30Z");
    private static final Instant UPDATED = Instant.parse("2024-05-02T11:16:31Z");

    private CodeListService codeListService;
    private CodeListElementOptionsService codeListElementOptionsService;

    @BeforeEach
    void setUp() {
        codeListService = mock(CodeListService.class);
        codeListElementOptionsService = mock(CodeListElementOptionsService.class);
    }

    @Test
    void getItemsReadsCellsByColumnKey() {
        try (var jsService = new JavascriptEngine(new CodeListJavascriptV1(codeListService, codeListElementOptionsService))) {
            when(codeListService.retrieve("test"))
                    .thenReturn(Optional.of(codeList()));
            // The source moved the name column in front of the key column.
            when(codeListService.listAllItems("test"))
                    .thenReturn(List.of(item(123L, List.of("Berlin", "11000000"))));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    const items = _code_lists_v1.getItems('test');
                    [
                        items[0].ags,
                        items[0].name,
                        items[0].$id,
                        items[0].$value,
                        items[0].$created,
                        items[0].$updated,
                        'removed' in items[0]
                    ];
                    """));
            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("11000000", values.get(0));
            assertEquals("Berlin", values.get(1));
            assertEquals(123, values.get(2));
            assertEquals("11000000", values.get(3));
            assertEquals(IsoTimestampUtils.toOffsetString(CREATED), values.get(4));
            assertEquals(IsoTimestampUtils.toOffsetString(UPDATED), values.get(5));
            assertEquals(false, values.get(6));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getOptionsUsesTheGivenColumns() {
        try (var jsService = new JavascriptEngine(new CodeListJavascriptV1(codeListService, codeListElementOptionsService))) {
            when(codeListService.retrieve("test"))
                    .thenReturn(Optional.of(codeList()));
            when(codeListElementOptionsService.listOptions("test", "ags", "name", null))
                    .thenReturn(List.of(SelectInputElementOption.of("11000000", "Berlin")));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    const options = _code_lists_v1.getOptions('test', 'ags', 'name');
                    [options[0].value, options[0].label];
                    """));
            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("11000000", values.get(0));
            assertEquals("Berlin", values.get(1));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getOptionsDefaultsToTheKeyColumn() {
        try (var jsService = new JavascriptEngine(new CodeListJavascriptV1(codeListService, codeListElementOptionsService))) {
            when(codeListService.retrieve("test"))
                    .thenReturn(Optional.of(codeList()));
            when(codeListElementOptionsService.listOptions("test", "ags", "ags", null))
                    .thenReturn(List.of(SelectInputElementOption.of("11000000", "11000000")));
            when(codeListElementOptionsService.listOptions("test", "name", "name", null))
                    .thenReturn(List.of(SelectInputElementOption.of("Berlin", "Berlin")));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    [_code_lists_v1.getOptions('test')[0].label, _code_lists_v1.getOptions('test', 'name')[0].label];
                    """));
            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("11000000", values.get(0));
            assertEquals("Berlin", values.get(1));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void returnsEmptyListsForNullCodeListKey() {
        try (var jsService = new JavascriptEngine(new CodeListJavascriptV1(codeListService, codeListElementOptionsService))) {
            var itemsResult = jsService.evaluateCode(new JavascriptCode().setCode("_code_lists_v1.getItems(null);"));
            var optionsResult = jsService.evaluateCode(new JavascriptCode().setCode("_code_lists_v1.getOptions(null);"));

            assertEquals(List.of(), itemsResult.asObject());
            assertEquals(List.of(), optionsResult.asObject());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void returnsEmptyListsForMissingCodeListOrColumn() {
        try (var jsService = new JavascriptEngine(new CodeListJavascriptV1(codeListService, codeListElementOptionsService))) {
            when(codeListService.retrieve("missing"))
                    .thenReturn(Optional.empty());
            when(codeListService.retrieve("test"))
                    .thenReturn(Optional.of(codeList()));
            when(codeListElementOptionsService.listOptions("test", "unknown", "unknown", null))
                    .thenThrow(ResponseException.badRequest("Die Spalte ist nicht vorhanden."));

            var itemsResult = jsService.evaluateCode(new JavascriptCode().setCode("_code_lists_v1.getItems('missing');"));
            var optionsResult = jsService.evaluateCode(new JavascriptCode().setCode("_code_lists_v1.getOptions('missing');"));
            var unknownColumnResult = jsService.evaluateCode(new JavascriptCode().setCode("_code_lists_v1.getOptions('test', 'unknown');"));

            assertEquals(List.of(), itemsResult.asObject());
            assertEquals(List.of(), optionsResult.asObject());
            assertEquals(List.of(), unknownColumnResult.asObject());
        } catch (Exception e) {
            fail(e);
        }
    }

    private static CodeListEntity codeList() {
        return new CodeListEntity()
                .setKey("test")
                .setId(7)
                .setColumns(List.of("Name", "AGS"))
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("ags", "AGS", 2),
                        new CodeListColumnMapping("name", "Name", 1),
                        new CodeListColumnMapping("removed", "Entfernt", null)
                ))
                .setKeyColumnKey("ags");
    }

    private static CodeListItemEntity item(long id, List<String> columns) {
        return new CodeListItemEntity()
                .setId(id)
                .setCodeListId(7)
                .setColumns(columns)
                .setCreated(CREATED)
                .setUpdated(UPDATED);
    }
}
