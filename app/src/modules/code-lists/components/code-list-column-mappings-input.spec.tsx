import {useState} from 'react';
import {fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {CodeListSourceType} from '../enums/code-list-source-type';
import {CodeListStatus} from '../enums/code-list-status';
import {type CodeList} from '../models/code-list';
import {CodeListColumnMappingsInput} from './code-list-column-mappings-input';

const codeList: CodeList = {
    key: 'test',
    id: 7,
    sourceType: CodeListSourceType.Asset,
    sourceRef: '00000000-0000-0000-0000-000000000003',
    name: 'Test',
    description: '',
    columns: ['id', 'label', 'extra'],
    columnMappings: [
        {key: 'id', name: 'Kennung', index: 1},
        {key: 'label', name: 'Bezeichnung', index: null, formerSourceColumn: 'name'},
        {key: 'label_2', name: 'Name', index: 2},
    ],
    keyColumnKey: 'id',
    status: CodeListStatus.Synced,
    created: '',
    updated: '',
};

function Editor({initial, onPatch = vi.fn()}: {initial: CodeList; onPatch?: (patch: Partial<CodeList>) => void}) {
    const [list, setList] = useState(initial);
    return <CodeListColumnMappingsInput codeList={list} savedColumnMappings={initial.columnMappings} onChange={(patch) => {
        onPatch(patch);
        setList({...list, ...patch});
    }}/>;
}

describe('CodeListColumnMappingsInput', () => {
    it('offers unused source columns again and preserves existing missing assignments', () => {
        const onPatch = vi.fn();
        render(<Editor initial={codeList} onPatch={onPatch}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Spalte hinzufügen'}));
        expect(onPatch).toHaveBeenCalledExactlyOnceWith({
            columnMappings: [...codeList.columnMappings, {key: 'extra', name: 'extra', index: 3}],
            keyColumnKey: 'id',
        });
        expect(screen.getByRole('button', {name: 'Spalte hinzufügen'})).toBeDisabled();
    });

    it.each([CodeListSourceType.Asset, CodeListSourceType.XRepository])('repairs a missing %s column immediately while preserving its key and label', async (sourceType) => {
        const onPatch = vi.fn();
        render(<Editor initial={{...codeList, sourceType}} onPatch={onPatch}/>);
        const select = screen.getByRole('combobox', {name: 'Quellspalte für Bezeichnung'});
        expect(select).toHaveTextContent('„name“ fehlt in der Quelle');
        expect(select).toHaveAccessibleDescription(/Wählen Sie eine andere Quellspalte oder löschen Sie die Zuordnung/);
        const user = userEvent.setup();
        select.focus();
        await user.keyboard('{ArrowDown}');
        const choices = await screen.findByRole('listbox');
        expect(within(choices).getAllByRole('option')).toHaveLength(3);
        await user.keyboard('{End}{Enter}');
        expect(onPatch).toHaveBeenCalledExactlyOnceWith({
            columnMappings: [codeList.columnMappings[0], {key: 'label', name: 'Bezeichnung', index: 3, formerSourceColumn: null}, codeList.columnMappings[2]],
            keyColumnKey: 'id',
        });
        expect(select).toHaveTextContent('3 – extra');
        expect(screen.queryByText('„name“ fehlt in der Quelle')).not.toBeInTheDocument();
    });

    it('allows a temporary duplicate while changing two assignments and keeps the configured order', async () => {
        render(<Editor initial={codeList}/>);
        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Quellspalte für Kennung'}));
        fireEvent.click(await screen.findByRole('option', {name: '2 – label'}));
        expect(screen.getByText(/Die Spalte „2 – label“ ist mehrfach zugeordnet/)).toBeInTheDocument();
        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Quellspalte für Name'}));
        fireEvent.click(await screen.findByRole('option', {name: '1 – id'}));
        expect(screen.queryByText(/ist mehrfach zugeordnet/)).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('columnheader', {name: /Anzeigename/}));
        expect(within(screen.getAllByRole('row')[1]).getByRole('combobox')).toHaveAccessibleName('Quellspalte für Kennung');
        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Schlüsselspalte'}));
        const options = within(await screen.findByRole('listbox')).getAllByRole('option');
        expect(options).toHaveLength(2);
        expect(options[0]).toHaveTextContent('Kennung');
        expect(options[1]).toHaveTextContent('Name');
    });

    it('shows a neutral status for an intentionally unbound legacy column', () => {
        const legacy = {key: 'internal', name: 'Intern', index: null};
        render(<Editor initial={{...codeList, columnMappings: [...codeList.columnMappings, legacy]}}/>);
        const select = screen.getByRole('combobox', {name: 'Quellspalte für Intern'});
        expect(select).toHaveTextContent('Keine Quellspalte zugeordnet');
        expect(select).not.toHaveAccessibleDescription();
    });

    it('automatically assigns a safe storage index to a new manual column without showing a source control', () => {
        const onPatch = vi.fn();
        render(<Editor initial={{...codeList, sourceType: CodeListSourceType.Manual}} onPatch={onPatch}/>);
        expect(screen.queryByRole('columnheader', {name: /Quellspalte|Position/})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Spalte hinzufügen'}));
        expect(onPatch.mock.lastCall![0].columnMappings.at(-1)).toEqual({key: '', name: '', index: 4});
    });

    it('keeps saved manual cell indexes and closes gaps between new columns before saving', async () => {
        const onPatch = vi.fn();
        const list = {
            ...codeList,
            sourceType: CodeListSourceType.Manual,
            columns: ['deleted', 'id'],
            columnMappings: [{key: 'id', name: 'Kennung', index: 2}],
        };
        render(<Editor initial={list} onPatch={onPatch}/>);
        const add = screen.getByRole('button', {name: 'Spalte hinzufügen'});
        fireEvent.click(add);
        fireEvent.click(add);
        const rows = within(screen.getByRole('grid')).getAllByRole('row');
        fireEvent.click(within(rows[2]).getByRole('checkbox'));
        fireEvent.click(screen.getByRole('button', {name: 'Ausgewählte Einträge löschen'}));
        fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', {name: 'Löschen'}));
        fireEvent.click(add);
        expect(onPatch.mock.lastCall![0].columnMappings).toEqual([
            ...list.columnMappings,
            {key: '', name: '', index: 3},
            {key: '', name: '', index: 4},
        ]);
    });
});
