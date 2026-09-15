package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.entities.VCodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListRepository;
import de.aivot.prosuna.backend.codeLists.repositories.VCodeListItemRepository;
import de.aivot.prosuna.backend.elements.enums.OptionsSourceType;
import de.aivot.prosuna.backend.elements.enums.SelectInputPresentation;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ChipInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MultiCheckboxInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

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
        var service = new CodeListElementOptionsService(null, null);
        var element = new SelectInputElement()
                .setOptionsSource(OptionsSourceType.Manual)
                .setOptions(List.of(SelectInputElementOption.of("manual", "Manual")));

        assertSame(element, service.resolve(element));
    }

    @Test
    void resolvesSelectOptionsFromCodeList() throws Exception {
        var codeListRepository = codeListRepository(7);
        var itemRepository = mock(VCodeListItemRepository.class);
        var service = new CodeListElementOptionsService(itemRepository, codeListRepository);
        var element = new SelectInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setPresentation(SelectInputPresentation.Combobox)
                .setCodeListKey("test")
                .setDependsOnSelectFieldId("parent")
                .setOptions(List.of(SelectInputElementOption.of("old", "Old")));

        when(itemRepository.findAllByCodeListIdOrderByIdAsc(7)).thenReturn(List.of(
                item("001", "Berlin"),
                item("002", "Hamburg")
        ));

        var resolved = assertInstanceOf(SelectInputElement.class, service.resolve(element));

        assertEquals(List.of("001", "002"), resolved.getOptions().stream().map(SelectInputElementOption::getValue).toList());
        assertEquals(SelectInputPresentation.Combobox, resolved.getPresentation());
        assertNull(resolved.getDependsOnSelectFieldId());
        assertEquals(List.of("old"), element.getOptions().stream().map(SelectInputElementOption::getValue).toList());
    }

    @Test
    void resolvesRadioOptionsFromCodeList() throws Exception {
        var codeListRepository = codeListRepository(7);
        var itemRepository = mock(VCodeListItemRepository.class);
        var service = new CodeListElementOptionsService(itemRepository, codeListRepository);
        var element = new RadioInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("test");

        when(itemRepository.findAllByCodeListIdOrderByIdAsc(7)).thenReturn(List.of(
                item("001", "Berlin")
        ));

        var resolved = assertInstanceOf(RadioInputElement.class, service.resolve(element));

        assertEquals("Berlin", resolved.getOptions().getFirst().getLabel());
    }

    @Test
    void resolvesMultiCheckboxOptionsFromCodeList() throws Exception {
        var codeListRepository = codeListRepository(7);
        var itemRepository = mock(VCodeListItemRepository.class);
        var service = new CodeListElementOptionsService(itemRepository, codeListRepository);
        var element = new MultiCheckboxInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("test");

        when(itemRepository.findAllByCodeListIdOrderByIdAsc(7)).thenReturn(List.of(
                item("001", "Berlin")
        ));

        var resolved = assertInstanceOf(MultiCheckboxInputElement.class, service.resolve(element));

        assertEquals("Berlin", resolved.getOptions().getFirst().getLabel());
    }

    @Test
    void resolvesChipInputSuggestionsFromCodeListLabels() throws Exception {
        var codeListRepository = codeListRepository(7);
        var itemRepository = mock(VCodeListItemRepository.class);
        var service = new CodeListElementOptionsService(itemRepository, codeListRepository);
        var element = new ChipInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("test")
                .setSuggestions(List.of("Old"));

        when(itemRepository.findAllByCodeListIdOrderByIdAsc(7)).thenReturn(List.of(
                item("001", "Berlin"),
                item("002", "Hamburg"),
                item("003", "Berlin")
        ));

        var resolved = assertInstanceOf(ChipInputElement.class, service.resolve(element));

        assertEquals(List.of("Berlin", "Hamburg"), resolved.getSuggestions());
        assertEquals(List.of("Old"), element.getSuggestions());
    }

    @Test
    void rejectsMissingCodeListKey() {
        var service = new CodeListElementOptionsService(null, null);
        var element = new SelectInputElement()
                .setOptionsSource(OptionsSourceType.CodeList);

        var error = assertThrows(ResponseException.class, () -> service.resolve(element));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("Für die Codelisten-Optionen muss eine Codeliste ausgewählt sein.", error.getMessage());
    }

    @Test
    void rejectsDeletedCodeListLikeMissingCodeListKey() {
        var codeListRepository = codeListRepository();
        var service = new CodeListElementOptionsService(null, codeListRepository);
        var element = new SelectInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("test");

        var error = assertThrows(ResponseException.class, () -> service.resolve(element));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("Für die Codelisten-Optionen muss eine Codeliste ausgewählt sein.", error.getMessage());
    }

    private static CodeListRepository codeListRepository(int codeListId) {
        var codeListRepository = mock(CodeListRepository.class);
        when(codeListRepository.findById("test")).thenReturn(Optional.of(
                new CodeListEntity()
                        .setKey("test")
                        .setId(codeListId)
        ));
        return codeListRepository;
    }

    private static CodeListRepository codeListRepository() {
        return mock(CodeListRepository.class);
    }

    private static VCodeListItemEntity item(String value, String label) {
        return new VCodeListItemEntity()
                .setValue(value)
                .setLabel(label);
    }
}
