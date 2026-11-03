import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {CodeListSourceType} from '../enums/code-list-source-type';
import {CodeListStatus} from '../enums/code-list-status';
import {CodeList} from '../models/code-list';
import {CodeListItem} from '../models/code-list-item';
import {CodeListItemDialog} from './code-list-item-dialog';

const codeList: CodeList = {
    key: 'test',
    id: 7,
    sourceType: CodeListSourceType.Manual,
    sourceRef: '',
    name: 'Test',
    description: '',
    columns: ['zusatz', 'name', 'removed', 'code'],
    columnMappings: [
        {key: 'code', name: 'Schlüssel', index: 4},
        {key: 'name', name: 'Anzeigename', index: 2},
        {key: 'zusatz', name: 'Zusatz', index: 1},
        {key: 'gone', name: 'Entfernt', index: null},
    ],
    keyColumnKey: 'code',
    status: CodeListStatus.Synced,
    created: '',
    updated: '',
};

function renderDialog(overrides: {codeList?: CodeList; item?: CodeListItem; isBusy?: boolean} = {}) {
    const props = {
        open: true,
        codeList,
        item: null,
        isBusy: false,
        onClose: vi.fn(),
        onSave: vi.fn(),
        ...overrides,
    };
    return {props, ...render(<CodeListItemDialog {...props}/>)};
}

describe('CodeListItemDialog', () => {
    it('shows the mapped columns by name and requires only the key column', () => {
        renderDialog();

        expect(screen.getAllByRole('textbox')).toHaveLength(3);
        expect(screen.getByRole('textbox', {name: 'Schlüssel'})).toBeRequired();
        expect(screen.getByRole('textbox', {name: 'Schlüssel'}))
            .toHaveAccessibleDescription('Die Werte der Schlüsselspalte identifizieren die Einträge der Codeliste.');
        expect(screen.getByRole('textbox', {name: 'Anzeigename – optional'})).not.toBeRequired();
        expect(screen.getByRole('textbox', {name: 'Zusatz – optional'})).not.toBeRequired();
        expect(screen.queryByRole('textbox', {name: /Entfernt/})).not.toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it.each(['', '   '])('blocks entries without a key value (%j)', (keyValue) => {
        const {props} = renderDialog();
        fireEvent.change(screen.getByRole('textbox', {name: 'Schlüssel'}), {target: {value: keyValue}});

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(props.onSave).not.toHaveBeenCalled();
        expect(screen.getByRole('textbox', {name: 'Schlüssel'})).toBeInvalid();
        expect(screen.getByRole('textbox', {name: 'Schlüssel'}))
            .toHaveAccessibleDescription('Bitte geben Sie für „Schlüssel“ einen Wert ein.');
        expect(screen.getByRole('textbox', {name: 'Anzeigename – optional'})).not.toBeInvalid();
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });

    it('clears a corrected error and saves the cells in the stored column order', () => {
        const {props} = renderDialog();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        fireEvent.change(screen.getByRole('textbox', {name: 'Anzeigename – optional'}), {target: {value: 'Berlin'}});
        fireEvent.change(screen.getByRole('textbox', {name: 'Schlüssel'}), {target: {value: 'BE'}});
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(props.onSave).toHaveBeenCalledExactlyOnceWith(['', 'Berlin', '', 'BE']);
    });

    it('keeps the cells of unmapped columns and resets errors when the dialog is reopened', () => {
        const item: CodeListItem = {
            id: 10, codeListId: 7, columns: ['', 'Berlin', 'alt', 'BE'], created: '', updated: '',
        };
        const {props, rerender} = renderDialog({item});
        fireEvent.change(screen.getByRole('textbox', {name: 'Schlüssel'}), {target: {value: ''}});
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        expect(props.onSave).not.toHaveBeenCalled();
        expect(screen.getByRole('textbox', {name: 'Schlüssel'})).toBeInvalid();

        fireEvent.change(screen.getByRole('textbox', {name: 'Schlüssel'}), {target: {value: 'B'}});
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        expect(props.onSave).toHaveBeenCalledExactlyOnceWith(['', 'Berlin', 'alt', 'B']);

        rerender(<CodeListItemDialog {...props} open={false}/>);
        rerender(<CodeListItemDialog {...props} item={null}/>);

        expect(screen.getByRole('textbox', {name: 'Schlüssel'})).toHaveValue('');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
});
