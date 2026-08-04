import {describe, expect, it, vi} from 'vitest';
import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ElementType} from '../../../data/element-type/element-type';
import type {
    ComputedElementErrors,
    DerivedRuntimeElementData,
} from '../../../models/element-data';
import {
    createDerivedRuntimeElementData,
} from '../../../models/element-data';
import {ElementDerivationContext} from './element-derivation-context';

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../../components/view-dispatcher/view-dispatcher.component', () => ({
    ViewDispatcherComponent: (props: any) => (
        <button
            type="button"
            onClick={() => props.onAuthoredElementValuesChange({field: 'valid'}, ['field'])}
        >
            Wert setzen
        </button>
    ),
}));

describe('ElementDerivationContext', () => {
    it('should not persist external computed errors when authored values change', async () => {
        const onAuthoredElementValuesChange = vi.fn();
        const onDerivedDataChange = vi.fn();
        const computedErrors: ComputedElementErrors = {
            field: {
                error: 'Der Verantwortliche Personenkreis ist ein Pflichtfeld.',
            },
        };

        render(
            <ElementDerivationContext
                element={createRootElement()}
                authoredElementValues={{field: null}}
                onAuthoredElementValuesChange={onAuthoredElementValuesChange}
                onDerivedDataChange={onDerivedDataChange}
                computedErrors={computedErrors}
                onDeriveOverride={() => Promise.resolve(createDerivedRuntimeElementData())}
            />,
        );

        await waitFor(() => expect(onDerivedDataChange).toHaveBeenCalled());
        onDerivedDataChange.mockClear();

        fireEvent.click(screen.getByRole('button', {name: 'Wert setzen'}));

        expect(onAuthoredElementValuesChange).toHaveBeenCalledWith({field: 'valid'});
        const patchedDerivedData = onDerivedDataChange.mock.calls[0][0] as DerivedRuntimeElementData;
        expect(patchedDerivedData.elementStates.field?.error).toBeUndefined();
    });
});

function createRootElement(): any {
    return {
        id: 'root',
        type: ElementType.GroupLayout,
        children: [
            {
                id: 'field',
                type: ElementType.Text,
                disabled: false,
                technical: false,
            },
        ],
    };
}
