import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import type {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {getLiteralElementValue, literalAuthoredValue, type ReplicatingContainerElementValues} from '../../../models/element-data';
import type {AuthoredInputValue} from '../../../models/input-mode';
import type {DataObjectItem} from '../models/data-object-item';
import {fromDataObjectItemDraft, toDataObjectItemDraft} from './data-object-editor-values';

const schema = {
    id: 'root', type: ElementType.GroupLayout,
    children: [{
        id: 'group', type: ElementType.GroupLayout,
        children: [{
            id: 'rows', type: ElementType.ReplicatingContainer,
            children: [{id: 'name', type: ElementType.Text}, {
                id: 'details', type: ElementType.ReplicatingContainer,
                children: [{id: 'note', type: ElementType.Text}],
            }],
        }],
    }],
} as GroupLayout;

function item(data: Record<string, unknown>): DataObjectItem {
    return {id: '1', schemaKey: 'contacts', created: '', updated: '', data};
}

describe('Data-object editor boundary', () => {
    it('round-trips nested plain values and row IDs without mutating the API model', () => {
        const original = item({
            $id: '1',
            rows: [{id: 'outer', values: {
                name: 'Ada', details: [{id: 'inner', values: {note: 'Original'}}],
            }}],
        });
        const snapshot = structuredClone(original);
        const draft = toDataObjectItemDraft(original, schema);
        expect(fromDataObjectItemDraft(draft, schema)).toEqual(original);

        const rows = getLiteralElementValue<ReplicatingContainerElementValues>(draft.data, 'rows')!;
        const details = getLiteralElementValue<ReplicatingContainerElementValues>(rows[0].values!, 'details')!;
        details[0].values!.note = literalAuthoredValue('Edited');
        const request = fromDataObjectItemDraft(draft, schema);
        expect(request.data.rows).toEqual([{id: 'outer', values: {
            name: 'Ada', details: [{id: 'inner', values: {note: 'Edited'}}],
        }}]);
        expect(original).toEqual(snapshot);
        expect(toDataObjectItemDraft(request, schema)).toEqual(draft);
    });

    it.each([{rows: null}, {rows: []}, {rows: [{id: 'empty', values: {}}]}])('preserves empty container values %j', ({rows}) => {
        const original = item({rows});
        expect(fromDataObjectItemDraft(toDataObjectItemDraft(original, schema), schema)).toEqual(original);
    });

    it('preserves absent fields and treats ordinary object/list values as opaque payloads', () => {
        const original = item({
            object: {type: 'LowCode', code: 'Business data', value: 0},
            objects: [{id: 'not-a-row', values: {type: 'Literal', value: false}}],
            nullable: null, zero: 0, checked: false,
        });
        const draft = toDataObjectItemDraft(original, schema);
        expect(draft.data).not.toHaveProperty('rows');
        expect(fromDataObjectItemDraft(draft, schema)).toEqual(original);
    });

    it.each<AuthoredInputValue<unknown>>([
        {type: 'Variable', reference: {source: 'ProcessData', path: 'name'}},
        {type: 'LowCode', code: 'return 1;'},
        {type: 'NoCode', operand: {type: 'NoCodeStaticValue', value: '1'}},
    ])('rejects mode $type at both top-level and nested element positions', (value) => {
        const top = toDataObjectItemDraft(item({}), schema);
        top.data.name = value;
        expect(() => fromDataObjectItemDraft(top, schema)).toThrow(/dynamic authored value/);

        const nested = toDataObjectItemDraft(item({rows: [{id: 'row', values: {}}]}), schema);
        const rows = getLiteralElementValue<ReplicatingContainerElementValues>(nested.data, 'rows')!;
        rows[0].values!.name = value;
        expect(() => fromDataObjectItemDraft(nested, schema)).toThrow(/dynamic authored value/);
    });
});
