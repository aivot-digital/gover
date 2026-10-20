import {describe, expect, it} from 'vitest';
import {ElementType} from '../../../../data/element-type/element-type';
import {DurationLargestUnit} from '../../../../models/elements/form/input/duration-field-element';
import {type GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {type DataObjectSchema} from '../../models/data-object-schema';
import {type DataObjectItem} from '../../models/data-object-item';
import {dataObjectSchemaExtractDisplayFields} from './data-object-item-list-page';

function displayValue(type: ElementType, value: unknown, properties = {}) {
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
        [undefined, '27 Stunden 30 Minuten'],
        [DurationLargestUnit.Day, '1 Tag 3 Stunden 30 Minuten'],
    ])('keeps duration values numeric for sorting with largest unit %s', (largestUnit, display) => {
        const schema = {
            key: 'work', displayFields: ['duration'],
            schema: {
                type: ElementType.GroupLayout, id: 'root',
                children: [{type: ElementType.Duration, id: 'duration', label: 'Aufwand', largestUnit}],
            },
        } as DataObjectSchema;
        const column = dataObjectSchemaExtractDisplayFields(schema)[0];
        const row: DataObjectItem = {
            id: '1', schemaKey: 'work', data: {duration: 99000}, created: '', updated: '',
        };
        const value = column.valueGetter!(undefined as never, row, column, undefined as never);
        expect(column.type).toBe('number');
        expect(value).toBe(99000);
        expect(column.valueFormatter!(value as never, row, column, undefined as never)).toBe(display);
    });

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
    ])('formats plain values for element type %s', (type, value, expected) => {
        expect(displayValue(type as ElementType, value)).toEqual(expected);
    });

    it.each([ElementType.Select, ElementType.Radio, ElementType.MultiCheckbox])('resolves option labels for type %s', (type) => {
        const value = type === ElementType.MultiCheckbox ? ['kita-1'] : 'kita-1';
        expect(displayValue(type, value, {
            options: [{value: 'kita-1', label: 'Kita Sonnenschein'}],
        })).toBe('Kita Sonnenschein');
    });

    it.each([ElementType.Text, ElementType.Date, ElementType.MultiCheckbox])('keeps missing and null values empty for type %s', (type) => {
        expect(displayValue(type, undefined)).toBeNull();
        expect(displayValue(type, null)).toBeNull();
    });

});
