package de.aivot.gover.backend.elements.services;

import de.aivot.gover.backend.codeLists.services.CodeListService;
import de.aivot.gover.backend.elements.enums.OptionsSourceType;
import de.aivot.gover.backend.elements.models.elements.form.input.MultiCheckboxInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.MultiCheckboxInputElementOption;
import de.aivot.gover.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeListElementOptionsServiceTest {
    @Test
    void leavesManualElementsUnchanged() throws Exception {
        var service = new CodeListElementOptionsService(mock(CodeListService.class));
        var element = new SelectInputElement()
                .setOptionsSource(OptionsSourceType.Manual)
                .setOptions(List.of(SelectInputElementOption.of("manual", "Manual")));

        assertSame(element, service.resolve(element));
    }

    @Test
    void resolvesSelectOptionsFromCodeList() throws Exception {
        var codeListService = mock(CodeListService.class);
        var service = new CodeListElementOptionsService(codeListService);
        var element = new SelectInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListId(7)
                .setDependsOnSelectFieldId("parent")
                .setOptions(List.of(SelectInputElementOption.of("old", "Old")));

        when(codeListService.listAsSelect(7)).thenReturn(List.of(
                SelectInputElementOption.of("001", "Berlin"),
                SelectInputElementOption.of("002", "Hamburg")
        ));

        var resolved = assertInstanceOf(SelectInputElement.class, service.resolve(element));

        assertEquals(List.of("001", "002"), resolved.getOptions().stream().map(SelectInputElementOption::getValue).toList());
        assertNull(resolved.getDependsOnSelectFieldId());
        assertEquals(List.of("old"), element.getOptions().stream().map(SelectInputElementOption::getValue).toList());
    }

    @Test
    void resolvesRadioOptionsFromCodeList() throws Exception {
        var codeListService = mock(CodeListService.class);
        var service = new CodeListElementOptionsService(codeListService);
        var element = new RadioInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListId(7);

        when(codeListService.listAsRadio(7)).thenReturn(List.of(
                RadioInputElementOption.of("001", "Berlin")
        ));

        var resolved = assertInstanceOf(RadioInputElement.class, service.resolve(element));

        assertEquals("Berlin", resolved.getOptions().getFirst().getLabel());
    }

    @Test
    void resolvesMultiCheckboxOptionsFromCodeList() throws Exception {
        var codeListService = mock(CodeListService.class);
        var service = new CodeListElementOptionsService(codeListService);
        var element = new MultiCheckboxInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListId(7);

        when(codeListService.listAsMultiCheckbox(7)).thenReturn(List.of(
                new MultiCheckboxInputElementOption()
                        .setValue("001")
                        .setLabel("Berlin")
        ));

        var resolved = assertInstanceOf(MultiCheckboxInputElement.class, service.resolve(element));

        assertEquals("Berlin", resolved.getOptions().getFirst().getLabel());
    }

    @Test
    void rejectsMissingCodeListId() {
        var service = new CodeListElementOptionsService(mock(CodeListService.class));
        var element = new SelectInputElement()
                .setOptionsSource(OptionsSourceType.CodeList);

        assertThrows(ResponseException.class, () -> service.resolve(element));
    }
}
