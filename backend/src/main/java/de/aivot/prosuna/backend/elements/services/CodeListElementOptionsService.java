package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListRepository;
import de.aivot.prosuna.backend.codeLists.repositories.VCodeListItemRepository;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.enums.OptionsSourceType;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

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
            case MultiCheckboxInputElement multiCheckbox when multiCheckbox.getOptionsSource() == OptionsSourceType.CodeList -> resolveMultiCheckbox(multiCheckbox);
            case TextInputElement text when text.getOptionsSource() == OptionsSourceType.CodeList
                    && !Boolean.TRUE.equals(text.getIsMultiline()) && text.getDynamicTextPolicy() == null -> resolveText(text);
            case ChipInputElement chipInput when chipInput.getOptionsSource() == OptionsSourceType.CodeList -> resolveChipInput(chipInput);
            default -> element;
        };
    }

    @Nonnull
    private SelectInputElement resolveSelect(@Nonnull SelectInputElement element) throws ResponseException {
        var copy = copy(element, SelectInputElement.class);
        copy
                .setOptions(listConfigured(element.getCodeListKey(), this::listAsSelectItems))
                .setDependsOnSelectFieldId(null);
        return copy;
    }

    @Nonnull
    private TextInputElement resolveText(@Nonnull TextInputElement element) throws ResponseException {
        var copy = copy(element, TextInputElement.class);
        copy.setSuggestions(listConfigured(element.getCodeListKey(), this::listAsChipInputSuggestionsItems));
        return copy;
    }

    @Nonnull
    private ChipInputElement resolveChipInput(@Nonnull ChipInputElement element) throws ResponseException {
        var copy = copy(element, ChipInputElement.class);
        copy.setSuggestions(listConfigured(element.getCodeListKey(), this::listAsChipInputSuggestionsItems));
        return copy;
    }

    @Nonnull
    private MultiCheckboxInputElement resolveMultiCheckbox(@Nonnull MultiCheckboxInputElement element) throws ResponseException {
        var copy = copy(element, MultiCheckboxInputElement.class);
        copy.setOptions(listConfigured(element.getCodeListKey(), this::listAsMultiCheckboxItems));
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
                // Free-text inputs store the suggestion text, not the technical codelist value.
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
    private <T> List<T> listConfigured(@Nullable String codeListKey, @Nonnull Function<Integer, List<T>> list) throws ResponseException {
        // The configuration validation reports a missing selection at the element, so it is derived without options
        // instead of showing the error at the field and in the error summary of the form.
        if (codeListKey == null || codeListKey.isBlank()) {
            return List.of();
        }
        return list.apply(requireConfiguredCodeListId(codeListKey));
    }

    @Nonnull
    private Integer requireConfiguredCodeListId(@Nonnull String codeListKey) throws ResponseException {
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
        return JsonMapperFactory
                .getInstance()
                .convertValue(element, elementType);
    }
}
