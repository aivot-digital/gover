import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {type AnyElement} from '../../../models/elements/any-element';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {type ElementConfigurationErrors, ElementsApiService} from '../elements-api-service';
import {
    ELEMENT_CONFIGURATION_VALIDATION_DELAY_MS,
    useElementConfigurationErrors,
} from './use-element-configuration-errors';

const errors: ElementConfigurationErrors = {
    properties: {},
    children: {
        text: {
            properties: {minCharacters: ['Sie fordern mehr Zeichen, als Sie maximal zulassen.']},
            children: {},
        },
    },
};

describe('useElementConfigurationErrors', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    it('validates only the latest tree after edits pause', async () => {
        const validate = vi.spyOn(ElementsApiService.prototype, 'validateConfiguration').mockResolvedValue(errors);
        const first = createGroup('first');
        const latest = createGroup('latest');

        const {result, rerender} = renderHook(
            ({element}) => useElementConfigurationErrors(element, true),
            {initialProps: {element: first}},
        );
        rerender({element: latest});
        await act(async () => {
            await vi.advanceTimersByTimeAsync(ELEMENT_CONFIGURATION_VALIDATION_DELAY_MS);
        });

        expect(validate).toHaveBeenCalledTimes(1);
        expect(validate).toHaveBeenCalledWith(latest, expect.any(AbortSignal));
        expect(result.current).toEqual(errors);
    });

    it('does not validate while disabled', async () => {
        const validate = vi.spyOn(ElementsApiService.prototype, 'validateConfiguration').mockResolvedValue(errors);

        const {result} = renderHook(() => useElementConfigurationErrors(createGroup('group'), false));
        await act(async () => {
            await vi.advanceTimersByTimeAsync(ELEMENT_CONFIGURATION_VALIDATION_DELAY_MS);
        });

        expect(validate).not.toHaveBeenCalled();
        expect(result.current).toBeNull();
    });

    it('ignores a response for a tree that has been replaced in the meantime', async () => {
        const responses: Array<(value: ElementConfigurationErrors) => void> = [];
        vi.spyOn(ElementsApiService.prototype, 'validateConfiguration').mockImplementation(() => new Promise((resolve) => {
            responses.push(resolve);
        }));
        const latestErrors: ElementConfigurationErrors = {properties: {}, children: {}};

        const {result, rerender} = renderHook(
            ({element}) => useElementConfigurationErrors(element, true),
            {initialProps: {element: createGroup('first')}},
        );
        await act(async () => {
            await vi.advanceTimersByTimeAsync(ELEMENT_CONFIGURATION_VALIDATION_DELAY_MS);
        });
        rerender({element: createGroup('latest')});
        await act(async () => {
            responses[0](errors);
            await vi.advanceTimersByTimeAsync(ELEMENT_CONFIGURATION_VALIDATION_DELAY_MS);
        });

        expect(result.current).toBeNull();

        await act(async () => {
            responses[1](latestErrors);
        });

        expect(result.current).toEqual(latestErrors);
    });
});

function createGroup(id: string): AnyElement {
    return {...generateElementWithDefaultValues(ElementType.GroupLayout), id};
}
