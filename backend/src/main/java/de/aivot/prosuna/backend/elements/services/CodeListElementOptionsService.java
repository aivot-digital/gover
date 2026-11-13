package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.models.CodeListColumnMapping;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListItemRepository;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListRepository;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.enums.OptionsSourceType;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Resolves the options and suggestions of input elements whose entries come from a code list. Every element chooses
 * the columns it reads by their keys, so a source that reorders its columns does not change the entries.
 */
@Service
public class CodeListElementOptionsService {
    private static final String MISSING_CODE_LIST_SELECTION_MESSAGE = "Für die Codelisten-Optionen muss eine Codeliste ausgewählt sein.";
    private static final String MISSING_OPTION_COLUMNS_MESSAGE = "Für die Codelisten-Optionen müssen eine Wert-Spalte und eine Beschriftungs-Spalte ausgewählt sein.";
    private static final String MISSING_SUGGESTION_COLUMN_MESSAGE = "Für die Codelisten-Vorschläge muss eine Vorschlagsspalte ausgewählt sein.";
    private static final String MISSING_GROUP_COLUMN_MESSAGE = "Für das abhängige Auswahlfeld muss eine Gruppenwert-Spalte ausgewählt sein.";

    private final CodeListItemRepository codeListItemRepository;
    private final CodeListRepository codeListRepository;

    @Autowired
    public CodeListElementOptionsService(CodeListItemRepository codeListItemRepository, CodeListRepository codeListRepository) {
        this.codeListItemRepository = codeListItemRepository;
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

    /**
     * Lists the options for the given columns, as an input element would receive them. Rows that share a value form
     * one option per group, and every option of that value lists the labels of all these rows.
     */
    @Nonnull
    public List<SelectInputElementOption> listOptions(@Nonnull String codeListKey,
                                                      @Nonnull String valueColumnKey,
                                                      @Nonnull String labelColumnKey,
                                                      @Nullable String groupColumnKey) throws ResponseException {
        var codeList = codeListRepository
                .findById(codeListKey)
                .orElseThrow(ResponseException::notFound);
        return listOptions(codeList, valueColumnKey, labelColumnKey, StringUtils.toNullableTrimmedString(groupColumnKey));
    }

    @Nonnull
    private SelectInputElement resolveSelect(@Nonnull SelectInputElement element) throws ResponseException {
        var codeList = requireConfiguredCodeList(element.getCodeListKey());
        // Only a dependent select needs group values; its options are then filtered by the referenced select.
        var groupColumnKey = StringUtils.isNullOrEmpty(element.getDependsOnSelectFieldId())
                ? null
                : requireColumnKey(element.getCodeListGroupColumnKey(), MISSING_GROUP_COLUMN_MESSAGE);
        var options = listOptions(
                codeList,
                requireColumnKey(element.getCodeListValueColumnKey(), MISSING_OPTION_COLUMNS_MESSAGE),
                requireColumnKey(element.getCodeListLabelColumnKey(), MISSING_OPTION_COLUMNS_MESSAGE),
                groupColumnKey
        );

        var copy = copy(element, SelectInputElement.class);
        copy.setOptions(options);
        return copy;
    }

    @Nonnull
    private MultiCheckboxInputElement resolveMultiCheckbox(@Nonnull MultiCheckboxInputElement element) throws ResponseException {
        var options = listOptions(
                requireConfiguredCodeList(element.getCodeListKey()),
                requireColumnKey(element.getCodeListValueColumnKey(), MISSING_OPTION_COLUMNS_MESSAGE),
                requireColumnKey(element.getCodeListLabelColumnKey(), MISSING_OPTION_COLUMNS_MESSAGE),
                null
        );

        var copy = copy(element, MultiCheckboxInputElement.class);
        copy.setOptions(options
                .stream()
                .map(option -> new MultiCheckboxInputElementOption()
                        .setValue(option.getValue())
                        .setLabel(option.getLabel()))
                .toList());
        return copy;
    }

    @Nonnull
    private TextInputElement resolveText(@Nonnull TextInputElement element) throws ResponseException {
        var suggestions = listSuggestions(
                requireConfiguredCodeList(element.getCodeListKey()),
                requireColumnKey(element.getCodeListLabelColumnKey(), MISSING_SUGGESTION_COLUMN_MESSAGE)
        );

        var copy = copy(element, TextInputElement.class);
        copy.setSuggestions(suggestions);
        return copy;
    }

    @Nonnull
    private ChipInputElement resolveChipInput(@Nonnull ChipInputElement element) throws ResponseException {
        var suggestions = listSuggestions(
                requireConfiguredCodeList(element.getCodeListKey()),
                requireColumnKey(element.getCodeListLabelColumnKey(), MISSING_SUGGESTION_COLUMN_MESSAGE)
        );

        var copy = copy(element, ChipInputElement.class);
        copy.setSuggestions(suggestions);
        return copy;
    }

    /**
     * Rows without a value cannot be selected and are skipped. A dependent select only shows rows of the selected
     * group, so rows without a group value are skipped as well. Values are trimmed like submitted values.
     * <p>
     * The label of a value does not depend on its group, since summaries and exports look labels up by the value
     * alone and would otherwise show the label of another group.
     */
    @Nonnull
    private List<SelectInputElementOption> listOptions(@Nonnull CodeListEntity codeList,
                                                       @Nonnull String valueColumnKey,
                                                       @Nonnull String labelColumnKey,
                                                       @Nullable String groupColumnKey) throws ResponseException {
        var valueIndex = requireColumnIndex(codeList, valueColumnKey);
        var labelIndex = requireColumnIndex(codeList, labelColumnKey);
        var groupIndex = groupColumnKey == null ? null : requireColumnIndex(codeList, groupColumnKey);

        var optionKeys = new LinkedHashSet<OptionKey>();
        var labelsByValue = new HashMap<String, LinkedHashSet<String>>();
        for (var item : listItems(codeList)) {
            var value = item.cellAt(valueIndex).trim();
            var group = groupIndex == null ? null : item.cellAt(groupIndex).trim();
            if (value.isEmpty() || (group != null && group.isEmpty())) {
                continue;
            }

            optionKeys.add(new OptionKey(group, value));
            var labels = labelsByValue.computeIfAbsent(value, key -> new LinkedHashSet<>());
            var label = item.cellAt(labelIndex).trim();
            if (!label.isEmpty()) {
                labels.add(label);
            }
        }

        return optionKeys
                .stream()
                .map(optionKey -> {
                    var labels = labelsByValue.get(optionKey.value());
                    return SelectInputElementOption.of(
                            optionKey.value(),
                            labels.isEmpty() ? optionKey.value() : String.join(", ", labels),
                            optionKey.group()
                    );
                })
                .toList();
    }

    /** Free-text inputs store the suggestion text itself, so suggestions are the distinct cells of one column. */
    @Nonnull
    private List<String> listSuggestions(@Nonnull CodeListEntity codeList, @Nonnull String columnKey) throws ResponseException {
        var index = requireColumnIndex(codeList, columnKey);
        return listItems(codeList)
                .stream()
                .map(item -> item.cellAt(index).trim())
                .filter(suggestion -> !suggestion.isEmpty())
                .distinct()
                .toList();
    }

    @Nonnull
    private List<CodeListItemEntity> listItems(@Nonnull CodeListEntity codeList) throws ResponseException {
        var codeListId = codeList.getId();
        if (codeListId == null) {
            throw ResponseException.internalServerError("Die interne ID der Codeliste fehlt.");
        }
        // The rows come with the columns of their list, so the mappings of codeList read the right cells.
        return codeListItemRepository
                .findSnapshotsByCodeListId(codeListId)
                .stream()
                .map(row -> row.toItem(codeList.getColumns()))
                .toList();
    }

    @Nonnull
    private CodeListEntity requireConfiguredCodeList(@Nullable String codeListKey) throws ResponseException {
        if (codeListKey == null || codeListKey.isBlank()) {
            throw ResponseException.badRequest(MISSING_CODE_LIST_SELECTION_MESSAGE);
        }

        return codeListRepository
                .findById(codeListKey)
                .orElseThrow(() -> ResponseException.badRequest(MISSING_CODE_LIST_SELECTION_MESSAGE));
    }

    @Nonnull
    private static String requireColumnKey(@Nullable String columnKey, @Nonnull String message) throws ResponseException {
        var trimmedColumnKey = StringUtils.toNullableTrimmedString(columnKey);
        if (trimmedColumnKey == null) {
            throw ResponseException.badRequest(message);
        }
        return trimmedColumnKey;
    }

    // Renamed or removed columns are not rewritten in the elements that reference them, so they fail visibly here.
    private static int requireColumnIndex(@Nonnull CodeListEntity codeList, @Nonnull String columnKey) throws ResponseException {
        return codeList
                .findColumnMapping(columnKey)
                .map(CodeListColumnMapping::index)
                .orElseThrow(() -> ResponseException.badRequest(
                        "Die Spalte %s ist in der Codeliste %s nicht vorhanden.",
                        StringUtils.quote(columnKey),
                        StringUtils.quote(codeList.getName())
                ));
    }

    @Nonnull
    private <T extends BaseElement> T copy(@Nonnull T element, @Nonnull Class<T> elementType) {
        return JsonMapperFactory
                .getInstance()
                .convertValue(element, elementType);
    }

    private record OptionKey(@Nullable String group, @Nonnull String value) {
    }
}
