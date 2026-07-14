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
            default -> element;
        };
    }

    @Nonnull
    private SelectInputElement resolveSelect(@Nonnull SelectInputElement element) throws ResponseException {
        var copy = copy(element, SelectInputElement.class);
        copy
                .setOptions(listAsSelect(requireCodeListId(element.getCodeListId())))
                .setDependsOnSelectFieldId(null);
        return copy;
    }

    @Nonnull
    private RadioInputElement resolveRadio(@Nonnull RadioInputElement element) throws ResponseException {
        var copy = copy(element, RadioInputElement.class);
        copy.setOptions(listAsRadio(requireCodeListId(element.getCodeListId())));
        return copy;
    }

    @Nonnull
    private MultiCheckboxInputElement resolveMultiCheckbox(@Nonnull MultiCheckboxInputElement element) throws ResponseException {
        var copy = copy(element, MultiCheckboxInputElement.class);
        copy.setOptions(listAsMultiCheckbox(requireCodeListId(element.getCodeListId())));
        return copy;
    }

    @Nonnull
    public List<SelectInputElementOption> listAsSelect(@Nonnull Integer codeListId) throws ResponseException {
        requireCodeList(codeListId);
        return vCodeListItemRepository
                .findAllByCodeListIdOrderByIdAsc(codeListId)
                .stream()
                .map(item -> SelectInputElementOption.of(item.getValue(), item.getLabel()))
                .toList();
    }

    @Nonnull
    public List<RadioInputElementOption> listAsRadio(@Nonnull Integer codeListId) throws ResponseException {
        requireCodeList(codeListId);
        return vCodeListItemRepository
                .findAllByCodeListIdOrderByIdAsc(codeListId)
                .stream()
                .map(item -> RadioInputElementOption.of(item.getValue(), item.getLabel()))
                .toList();
    }

    @Nonnull
    public List<MultiCheckboxInputElementOption> listAsMultiCheckbox(@Nonnull Integer codeListId) throws ResponseException {
        requireCodeList(codeListId);
        return vCodeListItemRepository
                .findAllByCodeListIdOrderByIdAsc(codeListId)
                .stream()
                .map(item -> new MultiCheckboxInputElementOption()
                        .setValue(item.getValue())
                        .setLabel(item.getLabel()))
                .toList();
    }

    @Nonnull
    private CodeListEntity requireCodeList(@Nonnull Integer codeListId) throws ResponseException {
        return codeListRepository
                .findById(codeListId)
                .orElseThrow(ResponseException::notFound);
    }

    @Nonnull
    private Integer requireCodeListId(Integer codeListId) throws ResponseException {
        if (codeListId == null || codeListId <= 0) {
            throw ResponseException.badRequest("Für die Code-Listen-Optionen muss eine Code-Liste ausgewählt sein.");
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
