import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {DepartmentSelectInputView} from './department-select-input-view';
import {ElementType} from '../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {createDerivedRuntimeElementData} from '../models/element-data';

const mocks = vi.hoisted(() => ({
    department: {
        id: 17,
        name: 'Bürgerbüro',
        created: '2026-01-01T00:00:00Z',
        updated: '2026-01-01T00:00:00Z',
        depth: 0,
        children: [],
    },
    retrieve: vi.fn(),
}));

vi.mock('../modules/departments/services/v-department-shadowed-api-service', () => ({
    VDepartmentShadowedApiService: class {
        retrieve = mocks.retrieve;
    },
}));

vi.mock('../modules/departments/dialogs/select-department-dialog', () => ({
    SelectDepartmentDialog: (props: {
        open: boolean;
        title: string;
        onSelect: (department: typeof mocks.department) => void;
    }) => props.open ? (
        <div role="dialog" aria-label={props.title}>
            <button type="button" onClick={() => props.onSelect(mocks.department)}>
                Bürgerbüro auswählen
            </button>
        </div>
    ) : null,
}));

describe('DepartmentSelectInputView', () => {
    it('loads the selected department and stores only its ID', async () => {
        const user = userEvent.setup();
        const setValue = vi.fn();
        mocks.retrieve.mockResolvedValue(mocks.department);

        render(
            <DepartmentSelectInputView
                element={{
                    ...generateElementWithDefaultValues(ElementType.DepartmentSelectInput),
                    label: 'Organisationseinheit für die Signatur',
                    dialogTitle: 'Organisationseinheit für die Signatur auswählen',
                }}
                value={17}
                setValue={setValue}
                onBlur={vi.fn()}
                authoredElementValues={{}}
                onAuthoredElementValuesChange={vi.fn()}
                derivedData={createDerivedRuntimeElementData()}
                onDerive={vi.fn()}
                onEvent={vi.fn()}
                onResetErrors={vi.fn()}
                derivationTriggerIdQueue={[]}
                suppressErrors={false}
                isBusy={false}
                isDeriving={false}
            />,
        );

        expect(await screen.findByRole('button', {
            name: /Organisationseinheit für die Signatur.*Bürgerbüro/,
        })).toBeEnabled();
        expect(mocks.retrieve).toHaveBeenCalledWith(17);

        await user.click(screen.getByRole('button', {
            name: /Organisationseinheit für die Signatur.*Bürgerbüro/,
        }));
        await user.click(screen.getByRole('button', {name: 'Bürgerbüro auswählen'}));

        expect(setValue).toHaveBeenCalledWith(17);
    });

    it('keeps an unavailable selection clearable', async () => {
        const user = userEvent.setup();
        const setValue = vi.fn();
        mocks.retrieve.mockRejectedValue(new Error('Forbidden'));

        render(
            <DepartmentSelectInputView
                element={generateElementWithDefaultValues(ElementType.DepartmentSelectInput)}
                value={99}
                setValue={setValue}
                onBlur={vi.fn()}
                authoredElementValues={{}}
                onAuthoredElementValuesChange={vi.fn()}
                derivedData={createDerivedRuntimeElementData()}
                onDerive={vi.fn()}
                onEvent={vi.fn()}
                onResetErrors={vi.fn()}
                derivationTriggerIdQueue={[]}
                suppressErrors={false}
                isBusy={false}
                isDeriving={false}
            />,
        );

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'Die ausgewählte Organisationseinheit konnte nicht geladen werden.',
        );
        await waitFor(() => expect(screen.getByRole('button', {
            name: 'Organisationseinheit: Auswahl entfernen',
        })).toBeEnabled());
        await user.click(screen.getByRole('button', {name: 'Organisationseinheit: Auswahl entfernen'}));

        expect(setValue).toHaveBeenCalledWith(null);
    });
});
