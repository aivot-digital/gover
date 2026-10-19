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
    columns: ['Zusatz', 'Anzeigename', 'Schlüssel'],
    labelColumnIndex: 1,
    valueColumnIndex: 2,
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
    it('marks only the configured label and value columns as required', () => {
        renderDialog();

        expect(screen.getByRole('textbox', {name: 'Anzeigename'})).toBeRequired();
        expect(screen.getByRole('textbox', {name: 'Schlüssel'})).toBeRequired();
        expect(screen.getByRole('textbox', {name: 'Zusatz – optional'})).not.toBeRequired();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('blocks empty entries and associates each error with its field', () => {
        const {props} = renderDialog();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(props.onSave).not.toHaveBeenCalled();
        expect(screen.getByRole('textbox', {name: 'Anzeigename'})).toBeInvalid();
        expect(screen.getByRole('textbox', {name: 'Anzeigename'}))
            .toHaveAccessibleDescription('Bitte geben Sie für „Anzeigename“ einen Wert ein.');
        expect(screen.getByRole('textbox', {name: 'Schlüssel'}))
            .toHaveAccessibleDescription('Bitte geben Sie für „Schlüssel“ einen Wert ein.');
        expect(screen.getByRole('textbox', {name: 'Zusatz – optional'})).not.toBeInvalid();
    });

    it.each(['Anzeigename', 'Schlüssel'])('rejects whitespace in %s even when the other required field is filled', (blankField) => {
        const {props} = renderDialog();
        for (const name of ['Anzeigename', 'Schlüssel']) {
            fireEvent.change(screen.getByRole('textbox', {name}), {target: {value: name === blankField ? '   ' : 'Berlin'}});
        }

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(props.onSave).not.toHaveBeenCalled();
        expect(screen.getByRole('textbox', {name: blankField})).toBeInvalid();
        expect(screen.getAllByRole('alert')).toHaveLength(1);
    });

    it('clears corrected errors and saves with an empty optional column', () => {
        const {props} = renderDialog();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        fireEvent.change(screen.getByRole('textbox', {name: 'Anzeigename'}), {target: {value: 'Berlin'}});
        expect(screen.getByRole('textbox', {name: 'Anzeigename'})).not.toBeInvalid();
        expect(screen.getByRole('textbox', {name: 'Schlüssel'})).toBeInvalid();
        fireEvent.change(screen.getByRole('textbox', {name: 'Schlüssel'}), {target: {value: 'BE'}});
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(props.onSave).toHaveBeenCalledExactlyOnceWith(['', 'Berlin', 'BE']);
    });

    it('requires only one field when label and value use the same column', () => {
        const {props} = renderDialog({codeList: {...codeList, labelColumnIndex: 2}});
        expect(screen.getByRole('textbox', {name: 'Anzeigename – optional'})).not.toBeRequired();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        expect(props.onSave).not.toHaveBeenCalled();
        expect(screen.getAllByRole('alert')).toHaveLength(1);

        fireEvent.change(screen.getByRole('textbox', {name: 'Schlüssel'}), {target: {value: 'BE'}});
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(props.onSave).toHaveBeenCalledExactlyOnceWith(['', '', 'BE']);
    });

    it('validates edited entries and resets errors when the dialog is reopened', () => {
        const item: CodeListItem = {
            id: 10, codeListId: 7, columns: ['', 'Berlin', 'BE'],
            label: 'Berlin', value: 'BE', created: '', updated: '',
        };
        const {props, rerender} = renderDialog({item});
        fireEvent.change(screen.getByRole('textbox', {name: 'Schlüssel'}), {target: {value: ''}});
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));
        expect(props.onSave).not.toHaveBeenCalled();
        expect(screen.getByRole('textbox', {name: 'Schlüssel'})).toBeInvalid();

        rerender(<CodeListItemDialog {...props} open={false}/>);
        rerender(<CodeListItemDialog {...props} item={null}/>);

        expect(screen.getByRole('textbox', {name: 'Schlüssel'})).toHaveValue('');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
});
