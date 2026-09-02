import {fireEvent, render, screen} from '@testing-library/react';
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
});
