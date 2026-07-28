package de.aivot.gover.backend.elements.services;

import de.aivot.gover.backend.codeLists.entities.CodeListEntity;
import de.aivot.gover.backend.codeLists.repositories.CodeListRepository;
import de.aivot.gover.backend.codeLists.repositories.VCodeListItemRepository;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.enums.OptionsSourceType;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.form.input.*;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodeListElementOptionsService {
    private static final String MISSING_CODE_LIST_SELECTION_MESSAGE = "Für die Codelisten-Optionen muss eine Codeliste ausgewählt sein.";

    private final VCodeListItemRepository vCodeListItemRepository;
    private final CodeListRepository codeListRepository;

    @Autowired
    public CodeListElementOptionsService(VCodeListItemRepository vCodeListItemRepository, CodeListRepository codeListRepository) {
        this.vCodeListItemRepository = vCodeListItemRepository;
        this.codeListRepository = codeListRepository;
    }

    @Nonnull
    public BaseElement resolve(@Nonnull BaseElement element) throws ResponseException {
        return switch (element) {
            case SelectInputElement select when select.getOptionsSource() == OptionsSourceType.CodeList -> resolveSelect(select);
            case RadioInputElement radio when radio.getOptionsSource() == OptionsSourceType.CodeList -> resolveRadio(radio);
            case MultiCheckboxInputElement multiCheckbox when multiCheckbox.getOptionsSource() == OptionsSourceType.CodeList -> resolveMultiCheckbox(multiCheckbox);
            case ChipInputElement chipInput when chipInput.getOptionsSource() == OptionsSourceType.CodeList -> resolveChipInput(chipInput);
            default -> element;
        };
    }

    @Nonnull
    private SelectInputElement resolveSelect(@Nonnull SelectInputElement element) throws ResponseException {
        var copy = copy(element, SelectInputElement.class);
        copy
                .setOptions(listAsSelectItems(requireConfiguredCodeListId(element.getCodeListKey())))
                .setDependsOnSelectFieldId(null);
        return copy;
    }

    @Nonnull
    private ChipInputElement resolveChipInput(@Nonnull ChipInputElement element) throws ResponseException {
        var copy = copy(element, ChipInputElement.class);
        copy.setSuggestions(listAsChipInputSuggestionsItems(requireConfiguredCodeListId(element.getCodeListKey())));
        return copy;
    }

    @Nonnull
    private RadioInputElement resolveRadio(@Nonnull RadioInputElement element) throws ResponseException {
        var copy = copy(element, RadioInputElement.class);
        copy.setOptions(listAsRadioItems(requireConfiguredCodeListId(element.getCodeListKey())));
        return copy;
    }

    @Nonnull
    private MultiCheckboxInputElement resolveMultiCheckbox(@Nonnull MultiCheckboxInputElement element) throws ResponseException {
        var copy = copy(element, MultiCheckboxInputElement.class);
        copy.setOptions(listAsMultiCheckboxItems(requireConfiguredCodeListId(element.getCodeListKey())));
        return copy;
    }

    @Nonnull
    public List<SelectInputElementOption> listAsSelect(@Nonnull String codeListKey) throws ResponseException {
        return listAsSelectItems(requireCodeListId(codeListKey));
    }

    @Nonnull
    private List<SelectInputElementOption> listAsSelectItems(@Nonnull Integer codeListId) {
        return vCodeListItemRepository
                .findAllByCodeListIdOrderByIdAsc(codeListId)
                .stream()
                .map(item -> SelectInputElementOption.of(item.getValue(), item.getLabel()))
                .toList();
    }

    @Nonnull
    public List<RadioInputElementOption> listAsRadio(@Nonnull String codeListKey) throws ResponseException {
        return listAsRadioItems(requireCodeListId(codeListKey));
    }

    @Nonnull
    private List<RadioInputElementOption> listAsRadioItems(@Nonnull Integer codeListId) {
        return vCodeListItemRepository
                .findAllByCodeListIdOrderByIdAsc(codeListId)
                .stream()
                .map(item -> RadioInputElementOption.of(item.getValue(), item.getLabel()))
                .toList();
    }

    @Nonnull
    public List<MultiCheckboxInputElementOption> listAsMultiCheckbox(@Nonnull String codeListKey) throws ResponseException {
        return listAsMultiCheckboxItems(requireCodeListId(codeListKey));
    }

    @Nonnull
    private List<MultiCheckboxInputElementOption> listAsMultiCheckboxItems(@Nonnull Integer codeListId) {
        return vCodeListItemRepository
                .findAllByCodeListIdOrderByIdAsc(codeListId)
                .stream()
                .map(item -> new MultiCheckboxInputElementOption()
                        .setValue(item.getValue())
                        .setLabel(item.getLabel()))
                .toList();
    }

    @Nonnull
    public List<String> listAsChipInputSuggestions(@Nonnull String codeListKey) throws ResponseException {
        return listAsChipInputSuggestionsItems(requireCodeListId(codeListKey));
    }

    @Nonnull
    private List<String> listAsChipInputSuggestionsItems(@Nonnull Integer codeListId) {
        return vCodeListItemRepository
                .findAllByCodeListIdOrderByIdAsc(codeListId)
                .stream()
                // Chip inputs store plain strings, so codelists can only provide label-based suggestions here.
                .map(item -> item.getLabel())
                .filter(label -> label != null && !label.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    @Nonnull
    private CodeListEntity requireCodeList(@Nonnull String codeListKey) throws ResponseException {
        return codeListRepository
                .findById(codeListKey)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private Integer requireCodeListId(@Nonnull String codeListKey) throws ResponseException {
        return requireInternalId(requireCodeList(codeListKey));
    }

    @Nonnull
    private Integer requireConfiguredCodeListId(String codeListKey) throws ResponseException {
        if (codeListKey == null || codeListKey.isBlank()) {
            throw ResponseException.badRequest(MISSING_CODE_LIST_SELECTION_MESSAGE);
        }

        var codeList = codeListRepository
                .findById(codeListKey)
                .orElseThrow(() -> ResponseException.badRequest(MISSING_CODE_LIST_SELECTION_MESSAGE));

        return requireInternalId(codeList);
    }

    @Nonnull
    private Integer requireInternalId(@Nonnull CodeListEntity codeList) throws ResponseException {
        var codeListId = codeList.getId();
        if (codeListId == null) {
            throw ResponseException.internalServerError("Die interne ID der Codeliste fehlt.");
        }
        return codeListId;
    }

    @Nonnull
    private <T extends BaseElement> T copy(@Nonnull T element, @Nonnull Class<T> elementType) {
        return ObjectMapperFactory
                .getInstance()
                .convertValue(element, elementType);
    }
}
