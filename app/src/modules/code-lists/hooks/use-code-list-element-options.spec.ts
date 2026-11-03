import {renderHook, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {CodeListsApiService} from '../code-lists-api-service';
import {useCodeListElementOptions, useCodeListOptions} from './use-code-list-element-options';

describe('useCodeListElementOptions', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('does not request text suggestions that reference editors do not use', () => {
        const listOptions = vi.spyOn(CodeListsApiService.prototype, 'listOptions');
        const element = {
            ...generateElementWithDefaultValues(ElementType.Text),
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'cities',
            codeListLabelColumnKey: 'name',
        };
        const {result} = renderHook(() => useCodeListElementOptions([element]));
        expect(listOptions).not.toHaveBeenCalled();
        expect(result.current.size).toBe(0);
    });

    it.each([
        ElementType.Select,
        ElementType.MultiCheckbox,
    ] as const)('loads %s options from the chosen value and label columns', async (type) => {
        const listOptions = vi.spyOn(CodeListsApiService.prototype, 'listOptions').mockResolvedValue([
            {label: 'Berlin', value: 'BE', group: 'east'},
            {label: 'Hamburg', value: 'HH', group: 'north'},
        ]);
        const element = {
            ...generateElementWithDefaultValues(type),
            id: 'location',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'federal/states',
            codeListValueColumnKey: 'code',
            codeListLabelColumnKey: 'name',
        };

        const {result} = renderHook(() => useCodeListElementOptions([element]));

        await waitFor(() => expect(result.current.get('location')).toEqual([
            {label: 'Berlin', value: 'BE'},
            {label: 'Hamburg', value: 'HH'},
        ]));
        expect(listOptions).toHaveBeenCalledWith('federal/states', 'code', 'name');
    });

    it('uses the suggestion column of chip inputs as labels and values', async () => {
        const listOptions = vi.spyOn(CodeListsApiService.prototype, 'listOptions').mockResolvedValue([
            {label: 'Berlin', value: 'Berlin'},
        ]);
        const element = {
            ...generateElementWithDefaultValues(ElementType.ChipInput),
            id: 'locations',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'cities',
            codeListLabelColumnKey: 'name',
        };

        const {result} = renderHook(() => useCodeListElementOptions([element]));

        await waitFor(() => expect(result.current.get('locations')).toEqual([
            {label: 'Berlin', value: 'Berlin'},
        ]));
        expect(listOptions).toHaveBeenCalledWith('cities', 'name', 'name');
    });

    it('deduplicates concurrent requests for elements using the same codelist columns', async () => {
        const listOptions = vi.spyOn(CodeListsApiService.prototype, 'listOptions').mockResolvedValue([
            {label: 'Berlin', value: 'BE'},
        ]);
        const firstElement = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'birth-state',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'states',
            codeListValueColumnKey: 'code',
            codeListLabelColumnKey: 'name',
        };
        const secondElement = {
            ...firstElement,
            id: 'residence-state',
        };

        const {result} = renderHook(() => useCodeListElementOptions([firstElement, secondElement]));

        await waitFor(() => {
            expect(result.current.get('birth-state')).toHaveLength(1);
            expect(result.current.get('residence-state')).toHaveLength(1);
        });
        expect(listOptions).toHaveBeenCalledOnce();
    });

    it('keeps options from successful requests when another codelist cannot be loaded', async () => {
        vi.spyOn(CodeListsApiService.prototype, 'listOptions').mockImplementation(async (codeListKey) => {
            if (codeListKey === 'categories') {
                throw new Error('Unavailable');
            }
            return [{label: 'Berlin', value: 'BE'}];
        });
        const selectElement = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'state',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'states',
            codeListValueColumnKey: 'code',
            codeListLabelColumnKey: 'name',
        };
        const radioElement = {
            ...selectElement,
            id: 'category',
            codeListKey: 'categories',
        };

        const {result} = renderHook(() => useCodeListElementOptions([selectElement, radioElement]));

        await waitFor(() => expect(result.current.get('state')).toEqual([
            {label: 'Berlin', value: 'BE'},
        ]));
        expect(result.current.has('category')).toBe(false);
    });

    it('does not load options for manual elements or elements without columns', () => {
        const listOptions = vi.spyOn(CodeListsApiService.prototype, 'listOptions');
        const manualElement = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'location',
            optionsSource: OptionsSourceType.Manual,
            codeListKey: 'states',
            codeListValueColumnKey: 'code',
            codeListLabelColumnKey: 'name',
        };
        const unconfiguredElement = {
            ...manualElement,
            id: 'unconfigured',
            optionsSource: OptionsSourceType.CodeList,
            codeListLabelColumnKey: null,
        };

        const {result} = renderHook(() => useCodeListElementOptions([manualElement, unconfiguredElement]));

        expect(result.current.size).toBe(0);
        expect(listOptions).not.toHaveBeenCalled();
    });
});

describe('useCodeListOptions', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('loads the options of the given columns', async () => {
        const listOptions = vi.spyOn(CodeListsApiService.prototype, 'listOptions').mockResolvedValue([
            {label: 'Berlin', value: 'BE'},
        ]);

        const {result} = renderHook(() => useCodeListOptions([{
            id: 'person.state',
            codeListKey: ' federal/states ',
            valueColumnKey: 'code',
            labelColumnKey: 'name',
        }]));

        await waitFor(() => expect(result.current.get('person.state')).toEqual([
            {label: 'Berlin', value: 'BE'},
        ]));
        expect(listOptions).toHaveBeenCalledWith('federal/states', 'code', 'name');
    });

    it('deduplicates requests for sources with the same codelist columns', async () => {
        const listOptions = vi.spyOn(CodeListsApiService.prototype, 'listOptions').mockResolvedValue([
            {label: 'Berlin', value: 'BE'},
        ]);

        const {result} = renderHook(() => useCodeListOptions([
            {id: 'birthState', codeListKey: 'states', valueColumnKey: 'code', labelColumnKey: 'name'},
            {id: 'residenceState', codeListKey: 'states', valueColumnKey: 'code', labelColumnKey: 'name'},
        ]));

        await waitFor(() => {
            expect(result.current.get('birthState')).toHaveLength(1);
            expect(result.current.get('residenceState')).toHaveLength(1);
        });
        expect(listOptions).toHaveBeenCalledOnce();
    });

    it('does not load options without a codelist key or columns', () => {
        const listOptions = vi.spyOn(CodeListsApiService.prototype, 'listOptions');

        const {result} = renderHook(() => useCodeListOptions([
            {id: 'blank', codeListKey: '  ', valueColumnKey: 'code', labelColumnKey: 'name'},
            {id: 'missing', codeListKey: null, valueColumnKey: 'code', labelColumnKey: 'name'},
            {id: 'no-columns', codeListKey: 'states', valueColumnKey: null, labelColumnKey: undefined},
        ]));

        expect(result.current.size).toBe(0);
        expect(listOptions).not.toHaveBeenCalled();
    });
});
