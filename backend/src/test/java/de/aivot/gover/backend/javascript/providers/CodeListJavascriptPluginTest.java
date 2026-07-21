package de.aivot.gover.backend.javascript.providers;

import de.aivot.gover.backend.codeLists.entities.CodeListEntity;
import de.aivot.gover.backend.codeLists.entities.VCodeListItemEntity;
import de.aivot.gover.backend.codeLists.services.CodeListService;
import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.javascript.services.JavascriptEngine;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.plugins.core.v1.javascript.CodeListJavascriptV1;
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

    @BeforeEach
    void setUp() {
        codeListService = mock(CodeListService.class);
    }

    @Test
    void getItems() {
        try (var jsService = new JavascriptEngine(new CodeListJavascriptV1(codeListService))) {
            when(codeListService.retrieve(7))
                    .thenReturn(Optional.of(codeList()));
            when(codeListService.listAllItems(7))
                    .thenReturn(List.of(item(123L, List.of("11000000", "Berlin"))));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    const items = _code_lists_v1.getItems(7);
                    [
                        items[0].ags,
                        items[0].name,
                        items[0].$id,
                        items[0].$value,
                        items[0].$label,
                        items[0].$created,
                        items[0].$updated
                    ];
                    """));
            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("11000000", values.get(0));
            assertEquals("Berlin", values.get(1));
            assertEquals(123, values.get(2));
            assertEquals("11000000", values.get(3));
            assertEquals("Berlin", values.get(4));
            assertEquals(CREATED.toString(), values.get(5));
            assertEquals(UPDATED.toString(), values.get(6));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getOptions() {
        try (var jsService = new JavascriptEngine(new CodeListJavascriptV1(codeListService))) {
            when(codeListService.listAllItems(7))
                    .thenReturn(List.of(item(123L, List.of("11000000", "Berlin"))));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    const options = _code_lists_v1.getOptions(7);
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
    void returnsEmptyListsForNullCodeListId() {
        try (var jsService = new JavascriptEngine(new CodeListJavascriptV1(codeListService))) {
            var itemsResult = jsService.evaluateCode(new JavascriptCode().setCode("_code_lists_v1.getItems(null);"));
            var optionsResult = jsService.evaluateCode(new JavascriptCode().setCode("_code_lists_v1.getOptions(null);"));

            assertEquals(List.of(), itemsResult.asObject());
            assertEquals(List.of(), optionsResult.asObject());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void returnsEmptyListsForMissingCodeList() {
        try (var jsService = new JavascriptEngine(new CodeListJavascriptV1(codeListService))) {
            when(codeListService.retrieve(7))
                    .thenReturn(Optional.empty());
            when(codeListService.listAllItems(7))
                    .thenThrow(ResponseException.notFound());

            var itemsResult = jsService.evaluateCode(new JavascriptCode().setCode("_code_lists_v1.getItems(7);"));
            var optionsResult = jsService.evaluateCode(new JavascriptCode().setCode("_code_lists_v1.getOptions(7);"));

            assertEquals(List.of(), itemsResult.asObject());
            assertEquals(List.of(), optionsResult.asObject());
        } catch (Exception e) {
            fail(e);
        }
    }

    private static CodeListEntity codeList() {
        return new CodeListEntity()
                .setId(7)
                .setColumns(List.of("ags", "name"));
    }

    private static VCodeListItemEntity item(long id, List<String> columns) {
        return new VCodeListItemEntity()
                .setId(id)
                .setCodeListId(7)
                .setColumns(columns)
                .setValue(columns.getFirst())
                .setLabel(columns.getLast())
                .setCreated(CREATED)
                .setUpdated(UPDATED);
    }
}
