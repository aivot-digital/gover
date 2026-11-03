package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.models.CodeListColumnMapping;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListItemRepository;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListRepository;
import de.aivot.prosuna.backend.elements.enums.OptionsSourceType;
import de.aivot.prosuna.backend.elements.enums.SelectInputPresentation;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ChipInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MultiCheckboxInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.MultiCheckboxInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeListElementOptionsServiceTest {
    @Test
    void resolvesSelectOptionsFromTheChosenColumns() throws Exception {
        var service = groupsService();
        var element = groupsSelect("id", "name")
                .setPresentation(SelectInputPresentation.Combobox)
                .setOptions(List.of(SelectInputElementOption.of("old", "Old")));

        var resolved = assertInstanceOf(SelectInputElement.class, service.resolve(element));

        assertEquals(List.of(
                SelectInputElementOption.of("1", "Group 1"),
                SelectInputElementOption.of("2", "Group 2"),
                SelectInputElementOption.of("3", "Group 3"),
                SelectInputElementOption.of("4", "Group 4")
        ), resolved.getOptions());
        assertEquals(SelectInputPresentation.Combobox, resolved.getPresentation());
        assertEquals(List.of(SelectInputElementOption.of("old", "Old")), element.getOptions());
    }

    @Test
    void mergesRowsThatShareAValueAndJoinsTheirLabels() throws Exception {
        var service = groupsService();

        var resolved = assertInstanceOf(SelectInputElement.class, service.resolve(groupsSelect("alt_grouping", "name")));

        assertEquals(List.of(
                SelectInputElementOption.of("1", "Group 1, Group 2"),
                SelectInputElementOption.of("2", "Group 3"),
                SelectInputElementOption.of("3", "Group 4")
        ), resolved.getOptions());
    }

    @Test
    void dependentSelectKeepsItsDependencyAndReceivesGroupValues() throws Exception {
        var service = itemsService();
        var element = itemsSelect("group_alt_id").setDependsOnSelectFieldId("groups");

        var resolved = assertInstanceOf(SelectInputElement.class, service.resolve(element));

        assertEquals("groups", resolved.getDependsOnSelectFieldId());
        assertEquals(List.of(
                SelectInputElementOption.of("a", "Item A", "1"),
                SelectInputElementOption.of("b", "Item B", "2"),
                SelectInputElementOption.of("c", "Item C", "1"),
                SelectInputElementOption.of("d", "Item D", "3")
        ), resolved.getOptions());
        assertEquals(List.of("d"), valuesOfGroup(resolved, "3"));
    }

    @Test
    void selectWithoutDependencyIgnoresTheGroupColumn() throws Exception {
        var service = itemsService();

        var resolved = assertInstanceOf(SelectInputElement.class, service.resolve(itemsSelect("group_id")));

        assertTrue(resolved.getOptions().stream().allMatch(option -> option.getGroup() == null));
    }

    @Test
    void skipsRowsWithoutValueOrGroupAndUsesTheValueForAMissingLabel() throws Exception {
        var itemRepository = mock(CodeListItemRepository.class);
        var service = new CodeListElementOptionsService(itemRepository, codeListRepository(itemsCodeList()));
        when(itemRepository.findAllByCodeListIdOrderByIdAsc(8)).thenReturn(List.of(
                item("a", "Item A", "1"),
                item(" ", "Ohne Wert", "1"),
                item("c", "Ohne Gruppe", " "),
                item(" d ", "", " 1 ")
        ));

        var resolved = assertInstanceOf(SelectInputElement.class, service.resolve(itemsSelect("group_id").setDependsOnSelectFieldId("groups")));

        assertEquals(List.of(
                SelectInputElementOption.of("a", "Item A", "1"),
                SelectInputElementOption.of("d", "d", "1")
        ), resolved.getOptions());
    }

    @Test
    void rejectsDependentSelectWithoutGroupColumn() {
        var service = itemsService();
        var element = itemsSelect(null).setDependsOnSelectFieldId("groups");

        var error = assertThrows(ResponseException.class, () -> service.resolve(element));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("Für das abhängige Auswahlfeld muss eine Gruppenwert-Spalte ausgewählt sein.", error.getMessage());
    }

    @Test
    void rejectsSelectWithoutValueOrLabelColumn() {
        var service = groupsService();

        var missingValue = assertThrows(ResponseException.class, () -> service.resolve(groupsSelect(null, "name")));
        var missingLabel = assertThrows(ResponseException.class, () -> service.resolve(groupsSelect("id", " ")));

        assertEquals("Für die Codelisten-Optionen müssen eine Wert-Spalte und eine Beschriftungs-Spalte ausgewählt sein.", missingValue.getMessage());
        assertEquals(missingValue.getMessage(), missingLabel.getMessage());
    }

    @Test
    void rejectsRenamedOrRemovedColumns() {
        var service = groupsService();

        var renamed = assertThrows(ResponseException.class, () -> service.resolve(groupsSelect("identifier", "name")));
        var removed = assertThrows(ResponseException.class, () -> service.resolve(groupsSelect("id", "removed")));

        assertEquals(HttpStatus.BAD_REQUEST, renamed.getStatus());
        assertEquals("Die Spalte „identifier“ ist in der Codeliste „Groups“ nicht vorhanden.", renamed.getMessage());
        assertEquals("Die Spalte „removed“ ist in der Codeliste „Groups“ nicht vorhanden.", removed.getMessage());
    }

    @Test
    void resolvesMultiCheckboxOptionsFromTheChosenColumns() throws Exception {
        var service = groupsService();
        var element = new MultiCheckboxInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("groups")
                .setCodeListValueColumnKey("alt_grouping")
                .setCodeListLabelColumnKey("name");

        var resolved = assertInstanceOf(MultiCheckboxInputElement.class, service.resolve(element));

        assertEquals(List.of("Group 1, Group 2", "Group 3", "Group 4"),
                resolved.getOptions().stream().map(MultiCheckboxInputElementOption::getLabel).toList());
        assertEquals(List.of("1", "2", "3"),
                resolved.getOptions().stream().map(MultiCheckboxInputElementOption::getValue).toList());
    }

    @Test
    void resolvesChipInputSuggestionsFromItsColumn() throws Exception {
        var service = groupsService();
        var element = new ChipInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("groups")
                .setCodeListLabelColumnKey("color")
                .setSuggestions(List.of("Old"));

        var resolved = assertInstanceOf(ChipInputElement.class, service.resolve(element));

        assertEquals(List.of("red", "blue", "green"), resolved.getSuggestions());
        assertEquals(List.of("Old"), element.getSuggestions());
    }

    @Test
    void resolvesTextSuggestionsWithoutRestrictingFreeInput() throws Exception {
        var service = groupsService();
        var original = new TextInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("groups")
                .setCodeListLabelColumnKey("name");

        var resolved = assertInstanceOf(TextInputElement.class, service.resolve(original));

        assertEquals(List.of("Group 1", "Group 2", "Group 3", "Group 4"), resolved.getSuggestions());
        resolved.validate("Group 5");
    }

    @Test
    void rejectsSuggestionsWithoutColumn() {
        var service = groupsService();
        var element = new ChipInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("groups");

        var error = assertThrows(ResponseException.class, () -> service.resolve(element));

        assertEquals("Für die Codelisten-Vorschläge muss eine Vorschlagsspalte ausgewählt sein.", error.getMessage());
    }

    @Test
    void retainsInactiveSuggestionsForMultilineTextWithoutLoadingTheCodeList() throws Exception {
        var service = new CodeListElementOptionsService(null, null);
        var element = new TextInputElement().setOptionsSource(OptionsSourceType.CodeList).setCodeListKey("missing");
        element.setIsMultiline(true);
        assertSame(element, service.resolve(element));
    }

    @Test
    void leavesManualElementsUnchanged() throws Exception {
        var service = new CodeListElementOptionsService(null, null);
        var element = new SelectInputElement()
                .setOptionsSource(OptionsSourceType.Manual)
                .setOptions(List.of(SelectInputElementOption.of("manual", "Manual")));

        assertSame(element, service.resolve(element));
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
        var service = new CodeListElementOptionsService(null, mock(CodeListRepository.class));
        var element = groupsSelect("id", "name");

        var error = assertThrows(ResponseException.class, () -> service.resolve(element));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("Für die Codelisten-Optionen muss eine Codeliste ausgewählt sein.", error.getMessage());
    }

    @Test
    void listsOptionsWithGroupsForEditors() throws Exception {
        var service = itemsService();

        var options = service.listOptions("items", "id", "name", " group_id ");

        assertEquals(SelectInputElementOption.of("a", "Item A", "1"), options.getFirst());
        assertEquals(List.of("1", "1", "2", "3"), options.stream().map(SelectInputElementOption::getGroup).toList());
    }

    @Test
    void listsNoOptionsForAnUnknownCodeList() {
        var service = new CodeListElementOptionsService(null, mock(CodeListRepository.class));

        var error = assertThrows(ResponseException.class, () -> service.listOptions("unknown", "id", "name", null));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    private static List<String> valuesOfGroup(SelectInputElement select, String group) {
        return select
                .getOptions()
                .stream()
                .filter(option -> group.equals(option.getGroup()))
                .map(SelectInputElementOption::getValue)
                .toList();
    }

    /** The updated "Groups" list of OP#1265: the color column was added in front of the name column. */
    private static CodeListElementOptionsService groupsService() {
        var itemRepository = mock(CodeListItemRepository.class);
        when(itemRepository.findAllByCodeListIdOrderByIdAsc(7)).thenReturn(List.of(
                item("1", "red", "Group 1", "1"),
                item("2", "blue", "Group 2", "1"),
                item("3", "green", "Group 3", "2"),
                item("4", "", "Group 4", "3")
        ));
        return new CodeListElementOptionsService(itemRepository, codeListRepository(new CodeListEntity()
                .setKey("groups")
                .setId(7)
                .setName("Groups")
                .setColumns(List.of("id", "color", "name", "alt_grouping"))
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("id", "id", 1),
                        new CodeListColumnMapping("name", "name", 3),
                        new CodeListColumnMapping("alt_grouping", "alt_grouping", 4),
                        new CodeListColumnMapping("color", "color", 2),
                        new CodeListColumnMapping("removed", "removed", null)
                ))
                .setKeyColumnKey("id")));
    }

    private static CodeListElementOptionsService itemsService() {
        var itemRepository = mock(CodeListItemRepository.class);
        when(itemRepository.findAllByCodeListIdOrderByIdAsc(8)).thenReturn(List.of(
                item("a", "Item A", "1", "1"),
                item("b", "Item B", "1", "2"),
                item("c", "Item C", "2", "1"),
                item("d", "Item D", "3", "3")
        ));
        return new CodeListElementOptionsService(itemRepository, codeListRepository(itemsCodeList()));
    }

    private static CodeListEntity itemsCodeList() {
        return new CodeListEntity()
                .setKey("items")
                .setId(8)
                .setName("Items")
                .setColumns(List.of("id", "name", "group_id", "group_alt_id"))
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("id", "id", 1),
                        new CodeListColumnMapping("name", "name", 2),
                        new CodeListColumnMapping("group_id", "group_id", 3),
                        new CodeListColumnMapping("group_alt_id", "group_alt_id", 4)
                ))
                .setKeyColumnKey("id");
    }

    private static SelectInputElement groupsSelect(String valueColumnKey, String labelColumnKey) {
        return new SelectInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("groups")
                .setCodeListValueColumnKey(valueColumnKey)
                .setCodeListLabelColumnKey(labelColumnKey);
    }

    private static SelectInputElement itemsSelect(String groupColumnKey) {
        return new SelectInputElement()
                .setOptionsSource(OptionsSourceType.CodeList)
                .setCodeListKey("items")
                .setCodeListValueColumnKey("id")
                .setCodeListLabelColumnKey("name")
                .setCodeListGroupColumnKey(groupColumnKey);
    }

    private static CodeListRepository codeListRepository(CodeListEntity codeList) {
        var codeListRepository = mock(CodeListRepository.class);
        when(codeListRepository.findById(codeList.getKey())).thenReturn(Optional.of(codeList));
        return codeListRepository;
    }

    private static CodeListItemEntity item(String... cells) {
        return new CodeListItemEntity().setColumns(List.of(cells));
    }
}
