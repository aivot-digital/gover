import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {
    ElementTreeContextProvider,
    type ElementTreeContextType,
} from '../components/element-tree-2/element-tree-context';
import {ElementType} from '../data/element-type/element-type';
import type {SelectFieldElement} from '../models/elements/form/input/select-field-element';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';
import {SelectFieldPresentation} from '../models/elements/form/input/select-field-presentation';
import {SelectFieldEditor} from './select-field-editor';

vi.mock('../modules/code-lists/code-lists-api-service', () => ({
    CodeListsApiService: class {
        async listAllOrdered() {
            return {content: [{key: 'items', name: 'Items', description: ''}]};
        }

        async retrieve() {
            return {
                key: 'items',
                columns: ['id', 'name', 'group_id'],
                columnMappings: [
                    {key: 'id', name: 'ID', index: 1},
                    {key: 'name', name: 'Name', index: 2},
                    {key: 'group_id', name: 'Gruppe', index: 3},
                    {key: 'removed', name: 'Entfernt', index: null},
                ],
                keyColumnKey: 'id',
            };
        }
    },
}));

const groupsSelect = {
    id: 'groups',
    type: ElementType.Select,
    label: 'Gruppe',
    optionsSource: OptionsSourceType.Manual,
    options: [{value: '1', label: 'Group 1'}],
} as SelectFieldElement;

function renderEditor(element: SelectFieldElement, onPatch = vi.fn()) {
    render(
        <ElementTreeContextProvider value={{
            allElements: [{element: groupsSelect, parents: []}, {element, parents: []}],
        } as unknown as ElementTreeContextType}>
            <SelectFieldEditor
                element={element}
                onPatch={onPatch}
                editable
                hasSummaryLayoutParent={false}
                scope="data_modelling"
            />
        </ElementTreeContextProvider>,
    );
    return onPatch;
}

function chooseOption(combobox: HTMLElement, optionText: string) {
    fireEvent.mouseDown(combobox);
    const option = Array.from(document.querySelectorAll('[role="option"]'))
        .find((candidate) => candidate.textContent?.includes(optionText));
    expect(option).toBeDefined();
    fireEvent.click(option!);
}

describe('SelectFieldEditor', () => {
    it('uses the dropdown default and persists the selection presentation', () => {
        const onPatch = vi.fn();
        const element = {
            id: 'category',
            type: ElementType.Select,
            label: 'Kategorie',
            presentation: undefined,
            optionsSource: OptionsSourceType.Manual,
            options: [{value: 'request', label: 'Antrag'}],
        } as SelectFieldElement;

        const {container} = render(
            <ElementTreeContextProvider value={{allElements: []} as unknown as ElementTreeContextType}>
                <SelectFieldEditor
                    element={element}
                    onPatch={onPatch}
                    editable
                    hasSummaryLayoutParent={false}
                    scope="data_modelling"
                />
            </ElementTreeContextProvider>,
        );

        const presentationControl = container.querySelector('[role="combobox"]');
        expect(presentationControl).toHaveTextContent('Auswahlmenü');
        fireEvent.mouseDown(presentationControl!);

        const comboboxOption = Array.from(document.querySelectorAll('[role="option"]'))
            .find((candidate) => candidate.textContent?.includes('Durchsuchbare Auswahl'));
        expect(comboboxOption).toBeDefined();
        fireEvent.click(comboboxOption!);

        expect(onPatch).toHaveBeenCalledWith({
            presentation: SelectFieldPresentation.Combobox,
        });
    });

    it('configures value, label and group columns of a dependent codelist select by their keys', async () => {
        const onPatch = renderEditor({
            id: 'items',
            type: ElementType.Select,
            label: 'Item',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'items',
            codeListValueColumnKey: 'id',
            codeListLabelColumnKey: 'renamed',
            dependsOnSelectFieldId: 'groups',
        } as SelectFieldElement);

        await waitFor(() => expect(screen.getByRole('combobox', {name: 'Beschriftungs-Spalte'}))
            .toHaveAccessibleDescription(expect.stringContaining('Die Spalte „renamed“ ist in der Codeliste nicht vorhanden.')));
        expect(screen.getByRole('combobox', {name: 'Wert-Spalte'})).toHaveTextContent('ID');
        expect(screen.getByRole('combobox', {name: /^Abhängiges Auswahlfeld/})).toHaveTextContent('Gruppe');

        chooseOption(screen.getByRole('combobox', {name: 'Gruppenwert-Spalte'}), 'Gruppe');

        expect(onPatch).toHaveBeenLastCalledWith({codeListGroupColumnKey: 'group_id'});
        expect(Array.from(document.querySelectorAll('[role="option"]')).some((option) => option.textContent?.includes('Entfernt')))
            .toBe(false);
    });

    it('asks for a group column only for dependent codelist selects', async () => {
        renderEditor({
            id: 'items',
            type: ElementType.Select,
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'items',
        } as SelectFieldElement);

        expect(await screen.findByRole('combobox', {name: 'Wert-Spalte'})).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Gruppenwert-Spalte'})).not.toBeInTheDocument();
    });

    it('keeps the dependency when the options switch to a codelist', () => {
        const onPatch = renderEditor({
            id: 'items',
            type: ElementType.Select,
            optionsSource: OptionsSourceType.Manual,
            options: [{value: 'a', label: 'Item A', group: '1'}],
            dependsOnSelectFieldId: 'groups',
        } as SelectFieldElement);

        chooseOption(screen.getByRole('combobox', {name: 'Optionen definieren über'}), 'System-Codeliste');

        expect(onPatch).toHaveBeenLastCalledWith(expect.objectContaining({optionsSource: OptionsSourceType.CodeList}));
        expect(onPatch.mock.lastCall?.[0]).not.toHaveProperty('dependsOnSelectFieldId');
    });
});
