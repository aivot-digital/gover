import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {
    type StoragePathSelectorInputElement,
    StoragePathSelectorMode,
} from '../models/elements/form/input/storage-path-selector-input-element';
import {StoragePathSelectorInputFieldEditor} from './storage-path-selector-input-field-editor';

vi.mock('../components/select-field/select-field-component', () => ({
    SelectFieldComponent: (props: {
        label: string;
        value?: string | null;
        options: {label: string; value: string}[];
        onChange: (value: string | null) => void;
    }) => (
        <select
            aria-label={props.label}
            value={props.value ?? ''}
            onChange={(event) => props.onChange(event.target.value)}
        >
            {props.options.map((option) => (
                <option
                    key={option.value}
                    value={option.value}
                >
                    {option.label}
                </option>
            ))}
        </select>
    ),
}));

vi.mock('../components/text-field/text-field-component', () => ({
    TextFieldComponent: () => null,
}));

vi.mock('../components/checkbox-field/checkbox-field-component', () => ({
    CheckboxFieldComponent: () => null,
}));

vi.mock('../components/multi-checkbox-field/multi-checkbox-component', () => ({
    MultiCheckboxComponent: () => null,
}));

describe('StoragePathSelectorInputFieldEditor', () => {
    it('should update the built-in placeholder when switching mode', () => {
        const onPatch = vi.fn();

        renderEditor(createElement(), onPatch);
        fireEvent.change(screen.getByLabelText('Auswahlmodus'), {
            target: {value: StoragePathSelectorMode.File},
        });

        expect(onPatch).toHaveBeenCalledWith({
            mode: StoragePathSelectorMode.File,
            placeholder: 'Datei auswählen',
        });
    });

    it('should preserve a custom placeholder when switching mode', () => {
        const onPatch = vi.fn();

        renderEditor(createElement({placeholder: 'Zertifikat wählen'}), onPatch);
        fireEvent.change(screen.getByLabelText('Auswahlmodus'), {
            target: {value: StoragePathSelectorMode.File},
        });

        expect(onPatch).toHaveBeenCalledWith({
            mode: StoragePathSelectorMode.File,
            placeholder: 'Zertifikat wählen',
        });
    });
});

function renderEditor(
    element: StoragePathSelectorInputElement,
    onPatch: (patch: Partial<StoragePathSelectorInputElement>) => void,
) {
    return render(
        <StoragePathSelectorInputFieldEditor
            element={element}
            onPatch={onPatch}
            editable={true}
            hasSummaryLayoutParent={false}
            scope="application"
        />,
    );
}

function createElement(overrides?: Partial<StoragePathSelectorInputElement>): StoragePathSelectorInputElement {
    return {
        id: 'ps_test',
        type: ElementType.StoragePathSelector,
        weight: 12,
        label: 'Speicherpfad',
        hint: undefined,
        required: undefined,
        disabled: undefined,
        technical: undefined,
        destinationKey: undefined,
        validation: undefined,
        value: undefined,
        mode: StoragePathSelectorMode.Folder,
        placeholder: 'Ordner auswählen',
        storageProviderSelectHint: undefined,
        allowedStorageProviderTypes: undefined,
        allowReadOnlyStorageProviders: false,
        metadata: undefined,
        name: undefined,
        override: undefined,
        testProtocolSet: undefined,
        visibility: undefined,
        ...overrides,
    };
}
