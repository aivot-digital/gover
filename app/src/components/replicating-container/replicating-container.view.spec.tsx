import {act, createEvent, fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {
    createDerivedRuntimeElementData,
    literalAuthoredValue,
    type ReplicatingContainerElementValues,
} from '../../models/element-data';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {type BaseViewProps} from '../../views/base-view';
import {DragAndDropProvider} from '../../providers/drag-and-drop-provider';
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

function renderView(
    overrides: Partial<BaseViewProps<ReplicatingContainerLayout, ReplicatingContainerElementValues>> = {},
    wrapper?: typeof DragAndDropProvider,
) {
    const props: BaseViewProps<ReplicatingContainerLayout, ReplicatingContainerElementValues> = {
        element,
        value: [{id: 'address-1', values: {street: literalAuthoredValue('Musterstraße 1')}}],
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

    const {rerender} = render(<ReplicatingContainerView {...props}/>, {wrapper});
    return {props, rerender};
}

const threeAddresses: ReplicatingContainerElementValues = [
    {id: 'address-1', values: {street: literalAuthoredValue('Musterstraße 1')}},
    {id: 'address-2', values: {street: literalAuthoredValue('Musterstraße 2')}},
    {id: 'address-3', values: {street: literalAuthoredValue('Musterstraße 3')}},
];

const dragEvent = {dataTransfer: {types: [], setData: vi.fn()}, clientX: 0, clientY: 1};

describe('ReplicatingContainerView', () => {
    it.each([undefined, null, false])('keeps the order fixed when sortable is %s', sortable => {
        // Rendering without a drag-and-drop provider also proves that unsortable containers do not need one.
        renderView({element: {...element, sortable}, value: threeAddresses});

        expect(screen.queryByRole('button', {name: /verschieben$/})).not.toBeInTheDocument();
    });

    it('offers no reordering while the collection is disabled', () => {
        renderView({element: {...element, sortable: true, disabled: true}, value: threeAddresses}, DragAndDropProvider);

        expect(screen.queryByRole('button', {name: /verschieben$/})).not.toBeInTheDocument();
    });

    it('places each handle directly after its row label when a second data set is added', () => {
        const {props, rerender} = renderView({element: {...element, sortable: true}}, DragAndDropProvider);
        expect(screen.queryByRole('button', {name: 'Adresse 1 verschieben'})).not.toBeInTheDocument();

        rerender(<ReplicatingContainerView {...props} value={threeAddresses.slice(0, 2)}/>);

        for (const label of ['Adresse 1', 'Adresse 2']) {
            const row = screen.getByRole('group', {name: label});
            const handle = within(row).getByRole('button', {name: `${label} verschieben`});
            expect(handle.previousElementSibling).toHaveTextContent(label);
        }
    });

    it('moves a data set by one position through the menu of its handle', async () => {
        const user = userEvent.setup();
        const {props} = renderView({element: {...element, sortable: true}, value: threeAddresses}, DragAndDropProvider);

        await user.click(screen.getByRole('button', {name: 'Adresse 1 verschieben'}));
        const menu = screen.getByRole('menu', {name: 'Adresse 1 verschieben'});
        expect(within(menu).getByRole('menuitem', {name: 'Nach oben verschieben'})).toHaveAttribute('aria-disabled', 'true');
        await user.click(within(menu).getByRole('menuitem', {name: 'Nach unten verschieben'}));

        expect(props.setValue).toHaveBeenCalledWith(
            [threeAddresses[1], threeAddresses[0], threeAddresses[2]],
            ['addresses', 'street'],
        );
    });

    it('moves a dragged data set to the position of the data set it is dropped on', async () => {
        const {props} = renderView({element: {...element, sortable: true}, value: threeAddresses}, DragAndDropProvider);
        const handle = screen.getByRole('button', {name: 'Adresse 1 verschieben'});
        const target = screen.getByRole('group', {name: 'Adresse 3'});

        expect(handle).toHaveAttribute('draggable', 'true');
        fireEvent.dragStart(handle, dragEvent);
        fireEvent.dragEnter(target, dragEvent);
        fireEvent.dragOver(target, dragEvent);
        fireEvent.drop(target, dragEvent);
        fireEvent.dragEnd(handle, dragEvent);
        await act(async () => {});

        expect(props.setValue).toHaveBeenCalledOnce();
        expect(props.setValue).toHaveBeenCalledWith(
            [threeAddresses[1], threeAddresses[2], threeAddresses[0]],
            ['addresses', 'street'],
        );
    });

    it('scrolls the surrounding scroll container while a data set is dragged towards its edge', () => {
        const frames: FrameRequestCallback[] = [];
        vi.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => frames.push(callback));
        vi.spyOn(window, 'cancelAnimationFrame').mockImplementation(() => undefined);

        renderView({element: {...element, sortable: true}, value: threeAddresses}, ({children}) => (
            <DragAndDropProvider>
                <div data-testid="scroll-container" style={{overflowY: 'auto'}}>{children}</div>
            </DragAndDropProvider>
        ));
        const scrollContainer = screen.getByTestId('scroll-container');
        Object.defineProperty(scrollContainer, 'scrollHeight', {value: 1200});
        Object.defineProperty(scrollContainer, 'clientHeight', {value: 300});
        vi.spyOn(scrollContainer, 'getBoundingClientRect').mockReturnValue({top: 0, bottom: 300, left: 0, right: 400} as DOMRect);

        const handle = screen.getByRole('button', {name: 'Adresse 1 verschieben'});
        const target = screen.getByRole('group', {name: 'Adresse 2'});
        fireEvent.dragStart(handle, dragEvent);
        // jsdom has no DragEvent and drops pointer coordinates from the init, so they are set on the event itself.
        const dragOverNearBottom = createEvent.dragOver(target, dragEvent);
        Object.defineProperties(dragOverNearBottom, {clientX: {value: 200}, clientY: {value: 295}});
        fireEvent(target, dragOverNearBottom);
        act(() => frames.shift()?.(0));

        expect(scrollContainer.scrollTop).toBeGreaterThan(0);

        fireEvent.dragEnd(handle, dragEvent);
        vi.restoreAllMocks();
    });

    it('allows clearing an optional collection below its minimum', async () => {
        const user = userEvent.setup();
        const {props} = renderView({element: {...element, minimumRequiredSets: 3}});
        await user.click(screen.getByRole('button', {name: 'Datensatz löschen'}));
        expect(props.setValue).not.toHaveBeenCalled();
        await user.click(within(screen.getByRole('dialog')).getByRole('button', {name: 'Löschen'}));
        expect(props.setValue).toHaveBeenCalledWith(null, ['addresses', 'street']);
    });

    it('allows removing the first entry of a required collection while another entry remains', async () => {
        const user = userEvent.setup();
        const second = {id: 'address-2', values: {street: literalAuthoredValue('Musterweg 2')}};
        const {props} = renderView({
            element: {...element, required: true},
            value: [{id: 'address-1', values: {street: literalAuthoredValue('Musterstraße 1')}}, second],
        });

        expect(within(screen.getByRole('group', {name: 'Adresse 2'})).getByRole('button', {name: 'Datensatz löschen'})).toBeEnabled();
        await user.click(within(screen.getByRole('group', {name: 'Adresse 1'})).getByRole('button', {name: 'Datensatz löschen'}));
        await user.click(within(screen.getByRole('dialog')).getByRole('button', {name: 'Löschen'}));
        expect(props.setValue).toHaveBeenCalledWith([second], ['addresses', 'street']);
    });

    it.each([
        {minimumRequiredSets: null, entries: 1, removable: false},
        {minimumRequiredSets: 2, entries: 2, removable: false},
        {minimumRequiredSets: 2, entries: 3, removable: true},
    ])('offers removal for every entry of a required collection only above its minimum ($entries entries, minimum $minimumRequiredSets)', ({minimumRequiredSets, entries, removable}) => {
        renderView({
            element: {...element, required: true, minimumRequiredSets},
            value: Array.from({length: entries}, (_, i) => ({id: `address-${i + 1}`, values: {}})),
        });

        expect(screen.queryAllByRole('button', {name: 'Datensatz löschen'})).toHaveLength(removable ? entries : 0);
    });

    it('explains that an optional minimum applies only when entries are provided', () => {
        renderView({element: {...element, minimumRequiredSets: 3}, value: null});
        expect(screen.getByText(/Bei Eingabe mindestens 3 Datensätze erforderlich/)).toBeVisible();
        expect(screen.getByRole('group', {name: 'Adressen – optional'})).not.toHaveAttribute('aria-invalid', 'true');
    });
    it('forwards external actions from the input-mode context without disabling them', () => {
        renderView({
            inputModeLiteralContext: {
                fieldProps: {
                    label: 'Adressen',
                    disabled: true,
                    externalAction: <button>Adressen ansehen</button>,
                },
            },
        });
        const action = screen.getByRole('button', {name: 'Adressen ansehen'});
        expect(action).toBeEnabled();
        expect(screen.getByRole('group', {name: 'Adressen – optional'})).not.toContainElement(action);
        expect(screen.getByRole('button', {name: 'Datensatz hinzufügen'})).toBeDisabled();
    });

    it('exposes the collection and every data set as labelled groups', () => {
        renderView();

        const collection = screen.getByRole('group', {name: 'Adressen – optional'});
        expect(collection).toHaveAccessibleDescription('Erfassen Sie alle relevanten Adressen.');
        expect(screen.getByRole('group', {name: 'Adresse 1'})).toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Straße'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Datensatz löschen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Datensatz hinzufügen'})).toBeInTheDocument();
        expect(screen.getByText('Adresse 1').closest('.MuiChip-root')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Datensatz löschen'})).toHaveClass('MuiButton-colorError');
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
        expect(screen.getByText(/Mindestens 2 Datensätze erforderlich/)).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Datensatz hinzufügen'}));

        expect(setValue).toHaveBeenCalledWith([
            expect.objectContaining({
                id: expect.any(String),
                values: {},
            }),
        ], ['addresses', 'street']);
    });
});
