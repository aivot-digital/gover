import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../../data/element-type/element-type';
import {literalAuthoredValue} from '../../../../models/element-data';
import {type AuthoredInputValue} from '../../../../models/input-mode';
import {type GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {type DataObjectSchema} from '../../models/data-object-schema';
import {type DataObjectItem} from '../../models/data-object-item';
import {dataObjectSchemaExtractDisplayFields} from './data-object-item-list-page';

function displayValue(type: ElementType, value: AuthoredInputValue<unknown> | undefined, properties = {}) {
    const schema: DataObjectSchema = {
        key: 'children', name: 'Kinder', description: '', idGen: '__SERIAL__',
        created: '', updated: '', displayFields: ['field'],
        schema: {
            type: ElementType.GroupLayout,
            id: 'root',
            children: [{id: 'field', type, label: 'Angabe', ...properties}],
        } as GroupLayout,
    };
    const row: DataObjectItem = {id: '1', schemaKey: schema.key, data: {field: value}, created: '', updated: ''};
    const column = dataObjectSchemaExtractDisplayFields(schema)[0];
    return column.valueGetter!(undefined as never, row, column, undefined as never);
}

describe('Data-object display columns', () => {
    it.each([
        [ElementType.Text, 'Erika Musterfrau', 'Erika Musterfrau'],
        [ElementType.Date, '2021-05-17', '17.05.2021'],
        [ElementType.DataObjectSelect, 'kita-1', 'kita-1'],
        [ElementType.DataModelSelect, 'kitas', 'kitas'],
        [ElementType.Number, 0, 0],
        [ElementType.Checkbox, false, false],
        [ElementType.ChipInput, ['A', 'B'], 'A, B'],
        [ElementType.DateRange, {start: '2021-05-17', end: '2021-05-20'}, '17.05.2021 bis 20.05.2021'],
        [ElementType.MapPoint, {address: 'Musterstraße 1'}, 'Musterstraße 1'],
    ])('formats literal values for element type %s', (type, value, expected) => {
        expect(displayValue(type as ElementType, literalAuthoredValue(value))).toEqual(expected);
    });

    it.each([ElementType.Select, ElementType.Radio, ElementType.MultiCheckbox])('resolves option labels after unwrapping type %s', (type) => {
        const value = type === ElementType.MultiCheckbox ? ['kita-1'] : 'kita-1';
        expect(displayValue(type, literalAuthoredValue(value), {
            options: [{value: 'kita-1', label: 'Kita Sonnenschein'}],
        })).toBe('Kita Sonnenschein');
    });

    it.each([ElementType.Text, ElementType.Date, ElementType.MultiCheckbox])('keeps missing and null values empty for type %s', (type) => {
        expect(displayValue(type, undefined)).toBeNull();
        expect(displayValue(type, literalAuthoredValue(null))).toBeNull();
    });

    it('does not expose an unexpected dynamic wrapper as a display value', () => {
        expect(displayValue(ElementType.Text, {type: 'LowCode', code: 'return 1;'})).toBeNull();
    });
});
