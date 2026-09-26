import {renderHook, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {BaseApiService} from '../../../services/base-api-service';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {useCodeListElementOptions} from './use-code-list-element-options';

describe('useCodeListElementOptions', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it.each([
        [ElementType.Select, 'select'],
        [ElementType.Radio, 'radio'],
        [ElementType.MultiCheckbox, 'multi-checkbox'],
    ] as const)('loads %s options from the matching public endpoint', async (type, endpoint) => {
        const get = vi.spyOn(BaseApiService.prototype, 'get').mockResolvedValue([
            {label: 'Berlin', value: 'BE'},
            {label: 'Hamburg', value: 'HH'},
        ]);
        const element = {
            ...generateElementWithDefaultValues(type),
            id: 'location',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'federal/states',
        };

        const {result} = renderHook(() => useCodeListElementOptions([element]));

        await waitFor(() => expect(result.current.get('location')).toEqual([
            {label: 'Berlin', value: 'BE'},
            {label: 'Hamburg', value: 'HH'},
        ]));
        expect(get).toHaveBeenCalledWith(
            `/api/public/code-lists/federal%2Fstates/${endpoint}/`,
            {skipAuthCheck: true},
        );
    });

    it('uses chip labels as both suggestion labels and values', async () => {
        vi.spyOn(BaseApiService.prototype, 'get').mockResolvedValue(['Berlin', 'Hamburg']);
        const element = {
            ...generateElementWithDefaultValues(ElementType.ChipInput),
            id: 'locations',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'cities',
        };

        const {result} = renderHook(() => useCodeListElementOptions([element]));

        await waitFor(() => expect(result.current.get('locations')).toEqual([
            {label: 'Berlin', value: 'Berlin'},
            {label: 'Hamburg', value: 'Hamburg'},
        ]));
    });

    it('deduplicates concurrent requests for elements using the same codelist', async () => {
        const get = vi.spyOn(BaseApiService.prototype, 'get').mockResolvedValue([
            {label: 'Berlin', value: 'BE'},
        ]);
        const firstElement = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'birth-state',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'states',
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
        expect(get).toHaveBeenCalledOnce();
    });

    it('keeps options from successful requests when another codelist cannot be loaded', async () => {
        vi.spyOn(BaseApiService.prototype, 'get').mockImplementation(async (path) => {
            if (path.includes('/radio/')) {
                throw new Error('Unavailable');
            }
            return [{label: 'Berlin', value: 'BE'}];
        });
        const selectElement = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'state',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'states',
        };
        const radioElement = {
            ...generateElementWithDefaultValues(ElementType.Radio),
            id: 'category',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'categories',
        };

        const {result} = renderHook(() => useCodeListElementOptions([selectElement, radioElement]));

        await waitFor(() => expect(result.current.get('state')).toEqual([
            {label: 'Berlin', value: 'BE'},
        ]));
        expect(result.current.has('category')).toBe(false);
    });

    it('does not load options for manually configured elements', () => {
        const get = vi.spyOn(BaseApiService.prototype, 'get');
        const element = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'location',
            optionsSource: OptionsSourceType.Manual,
            codeListKey: 'states',
        };

        const {result} = renderHook(() => useCodeListElementOptions([element]));

        expect(result.current.size).toBe(0);
        expect(get).not.toHaveBeenCalled();
    });
});
