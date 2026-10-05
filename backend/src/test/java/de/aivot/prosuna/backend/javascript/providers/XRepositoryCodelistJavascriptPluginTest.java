package de.aivot.prosuna.backend.javascript.providers;

import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.plugins.core.v1.javascript.XRepositoryCodelistJavascriptV1;
import de.aivot.prosuna.backend.xrepository.services.XRepositoryCodeListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XRepositoryCodelistJavascriptPluginTest {
    private XRepositoryCodeListService codeListService;

    @BeforeEach
    void setUp() {
        codeListService = mock(XRepositoryCodeListService.class);
    }

    @Test
    void getValues() {
        try (var jsService = new JavascriptEngine(new XRepositoryCodelistJavascriptV1(codeListService))) {
            when(codeListService.getReducedCodeList("urn:test"))
                    .thenReturn(List.of(Map.of(
                            "code", "001",
                            "name", "Berlin"
                    )));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_xrp_codelists_v1.getValues('urn:test');"));
            var values = assertInstanceOf(List.class, result.asObject());
            var value = assertInstanceOf(Map.class, values.getFirst());

            assertEquals("001", value.get("code"));
            assertEquals("Berlin", value.get("name"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getOptions() {
        try (var jsService = new JavascriptEngine(new XRepositoryCodelistJavascriptV1(codeListService))) {
            when(codeListService.getRadioFieldOptionCodeList("urn:test"))
                    .thenReturn(List.of(RadioInputElementOption.of("001", "Berlin")));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    const options = _xrp_codelists_v1.getOptions('urn:test');
                    [options[0].value, options[0].label];
                    """));
            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("001", values.get(0));
            assertEquals("Berlin", values.get(1));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void returnsEmptyListsForNullUrn() {
        try (var jsService = new JavascriptEngine(new XRepositoryCodelistJavascriptV1(codeListService))) {
            var valuesResult = jsService.evaluateCode(new JavascriptCode().setCode("_xrp_codelists_v1.getValues(null);"));
            var optionsResult = jsService.evaluateCode(new JavascriptCode().setCode("_xrp_codelists_v1.getOptions(null);"));

            assertEquals(List.of(), valuesResult.asObject());
            assertEquals(List.of(), optionsResult.asObject());
        } catch (Exception e) {
            fail(e);
        }
    }
}
