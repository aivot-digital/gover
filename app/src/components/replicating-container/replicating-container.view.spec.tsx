import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {
    createDerivedRuntimeElementData,
    type ReplicatingContainerElementValues,
} from '../../models/element-data';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {type BaseViewProps} from '../../views/base-view';
import {ReplicatingContainerView} from './replicating-container.view';

vi.mock('../view-dispatcher/view-dispatcher.component', () => ({
    ViewDispatcherComponent: (props: {
        element: {label?: string | null};
        isBusy: boolean;
    }) => (
        <input
            aria-label={props.element.label ?? 'Unterfeld'}
            disabled={props.isBusy}
        />
    ),
}));

const element = {
    type: ElementType.ReplicatingContainer,
    id: 'addresses',
    name: null,
    testProtocolSet: null,
    visibility: null,
    override: null,
    metadata: null,
    weight: 12,
    label: 'Adressen',
    hint: 'Erfassen Sie alle relevanten Adressen.',
    required: false,
    disabled: false,
    technical: false,
    destinationKey: 'addresses',
    validation: null,
    value: null,
    minimumRequiredSets: null,
    maximumSets: 3,
    headlineTemplate: 'Adresse #',
    addLabel: null,
    removeLabel: null,
    children: [{id: 'street', label: 'Straße'}],
} as ReplicatingContainerLayout;

function renderView(overrides: Partial<BaseViewProps<ReplicatingContainerLayout, ReplicatingContainerElementValues>> = {}) {
    const props: BaseViewProps<ReplicatingContainerLayout, ReplicatingContainerElementValues> = {
        element,
        value: [{id: 'address-1', values: {street: 'Musterstraße 1'}}],
        setValue: vi.fn(),
        onBlur: vi.fn(),
        isBusy: false,
        isDeriving: false,
        errors: undefined,
        errorDetails: undefined,
        authoredElementValues: {},
        onAuthoredElementValuesChange: vi.fn(),
        onElementBlur: vi.fn(),
        derivedData: createDerivedRuntimeElementData(),
        onDerive: vi.fn(async () => createDerivedRuntimeElementData()),
        onEvent: vi.fn(async () => undefined),
        onResetErrors: vi.fn(),
        suppressErrors: false,
        derivationTriggerIdQueue: [],
        ...overrides,
    };

    render(<ReplicatingContainerView {...props}/>);
    return props;
}

describe('ReplicatingContainerView', () => {
    it('exposes the collection and every data set as labelled groups', () => {
        renderView();

        const collection = screen.getByRole('group', {name: 'Adressen – optional'});
        expect(collection).toHaveAccessibleDescription('Erfassen Sie alle relevanten Adressen.');
        expect(screen.getByRole('group', {name: 'Adresse 1'})).toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Straße'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Datensatz löschen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Datensatz hinzufügen'})).toBeInTheDocument();
        expect(screen.getByText('Adresse 1').closest('.MuiChip-root')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Datensatz löschen'})).toHaveClass('MuiButton-colorInherit');
        const listSurface = collection.querySelector('[data-replicating-container-list]');
        expect(listSurface).toContainElement(screen.getByRole('group', {name: 'Adresse 1'}));
        expect(listSurface).toContainElement(screen.getByRole('button', {name: 'Datensatz hinzufügen'}));
    });

    it('associates one collection error and supports adding another data set', async () => {
        const user = userEvent.setup();
        const setValue = vi.fn();

        renderView({
            element: {
                ...element,
                required: true,
                minimumRequiredSets: 2,
            },
            value: null,
            errors: ['Mindestens zwei Adressen sind erforderlich.'],
            setValue,
        });

        const collection = screen.getByRole('group', {name: 'Adressen'});
        expect(collection).toHaveAttribute('aria-invalid', 'true');
        expect(collection).toHaveAccessibleDescription('Mindestens zwei Adressen sind erforderlich.');
        expect(screen.getByRole('alert')).toHaveTextContent('Mindestens zwei Adressen sind erforderlich.');
        expect(screen.getByText(/Mindestens 2 Datensätze sind erforderlich/)).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Datensatz hinzufügen'}));

        expect(setValue).toHaveBeenCalledWith([
            expect.objectContaining({
                id: expect.any(String),
                values: {},
            }),
        ], ['addresses', 'street']);
    });
});
