import {describe, expect, it, vi} from 'vitest';
import {literalAuthoredValue} from '../../models/element-data';
import {listAllPages} from '../../utils/page-utils';
import {loadDataObjectSelectOptions} from './data-object-select-options';

vi.mock('../../utils/page-utils', () => ({listAllPages: vi.fn()}));

describe('Data-object option labels', () => {
    it.each([
        ['Kita Sonnenschein', 'Kita Sonnenschein'],
        [0, '0'],
        [false, 'false'],
        [['A', 'B'], 'A, B'],
        [null, 'object-1'],
        ['', 'object-1'],
        [{name: 'Structured value'}, 'object-1'],
    ])('reads the configured label from the literal payload %j', async (value, expected) => {
        vi.mocked(listAllPages).mockResolvedValue([{
            id: 'object-1', schemaKey: 'kitas', created: '', updated: '',
            data: {name: literalAuthoredValue(value)},
        }]);
        const options = await loadDataObjectSelectOptions('kitas', 'name', true);
        expect(options).toEqual([expect.objectContaining({
            value: 'object-1', label: expected, subLabel: 'kitas · object-1',
        })]);
    });

    it('keeps the id fallback for absent or unconfigured label attributes', async () => {
        vi.mocked(listAllPages).mockResolvedValue([{
            id: 'object-1', schemaKey: 'kitas', created: '', updated: '', data: {},
        }]);
        expect((await loadDataObjectSelectOptions('kitas', 'name', true))[0].label).toBe('object-1');
        expect((await loadDataObjectSelectOptions('kitas', undefined, true))[0].label).toBe('object-1');
    });
});
