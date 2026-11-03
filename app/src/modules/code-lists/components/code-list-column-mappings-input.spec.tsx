import {fireEvent, render, screen} from '@testing-library/react';
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
    columns: ['id', 'label'],
    columnMappings: [
        {key: 'id', name: 'id', index: 1},
        {key: 'label', name: 'Bezeichnung', index: null, formerSourceColumn: 'name'},
        {key: 'label_2', name: 'label', index: 2},
    ],
    keyColumnKey: 'id',
    status: CodeListStatus.Synced,
    created: '',
    updated: '',
};

describe('CodeListColumnMappingsInput', () => {
    it('keeps the former source column of a missing column when the mappings change', () => {
        const onChange = vi.fn();
        render(
            <CodeListColumnMappingsInput
                codeList={codeList}
                savedColumnMappings={codeList.columnMappings}
                onChange={onChange}
            />,
        );

        fireEvent.click(screen.getByRole('button', {name: 'Spalte hinzufügen'}));

        expect(onChange).toHaveBeenCalledExactlyOnceWith({
            columnMappings: [
                {key: 'id', name: 'id', index: 1, formerSourceColumn: null},
                {key: 'label', name: 'Bezeichnung', index: null, formerSourceColumn: 'name'},
                {key: 'label_2', name: 'label', index: 2, formerSourceColumn: null},
                {key: '', name: '', index: null, formerSourceColumn: null},
            ],
            keyColumnKey: 'id',
        });
    });
});
