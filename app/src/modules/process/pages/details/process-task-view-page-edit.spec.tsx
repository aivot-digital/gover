import React, {type ComponentProps} from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {createMemoryRouter, Link, RouterProvider} from 'react-router-dom';
import {ElementType} from '../../../../data/element-type/element-type';
import {
    type AuthoredElementValues,
    literalAuthoredValue,
} from '../../../../models/element-data';
import {generateElementWithDefaultValues} from '../../../../utils/generate-element-with-default-values';
import type {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import {
    ProcessInstanceTaskApiService,
    type TaskView,
} from '../../services/process-instance-task-api-service';
import {ProcessTaskViewPageEdit} from './process-task-view-page-edit';

const testState = vi.hoisted(() => ({
    dispatch: vi.fn(),
    nextValues: {} as AuthoredElementValues,
    item: {
        task: {
            id: 20,
            processInstanceId: 10,
        },
        instance: null,
        process: null,
        node: null,
        provider: null,
    },
}));

vi.mock('../../../../components/generic-details-page/generic-details-page-context', () => ({
    useGenericDetailsPageContext: () => ({item: testState.item}),
}));

vi.mock('../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => testState.dispatch,
}));

vi.mock('../../../../utils/with-delay', () => ({
    withDelay: <T,>(promise: Promise<T>) => promise,
}));

vi.mock('../../../elements/components/element-derivation-context', () => ({
    ElementDerivationContext: (props: ComponentProps<typeof ElementDerivationContext>) => (
        <>
            <button
                type="button"
                onClick={() => props.onAuthoredElementValuesChange(testState.nextValues)}
            >
                Werte ändern
            </button>
            <button
                type="button"
                onClick={() => props.onAuthoredElementValuesChange(props.authoredElementValues)}
            >
                Unveränderte Werte melden
            </button>
        </>
    ),
}));

const layout = generateElementWithDefaultValues(ElementType.GroupLayout);

function createTaskView(data: AuthoredElementValues): TaskView {
    return {
        layout,
        data,
        events: [],
    };
}

async function renderPage(initialValues: AuthoredElementValues) {
    const getTaskView = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'getStaffTaskView')
        .mockResolvedValue(createTaskView(initialValues));
    const putTaskView = vi.spyOn(ProcessInstanceTaskApiService.prototype, 'putStaffTaskView')
        .mockResolvedValue(createTaskView(initialValues));
    const router = createMemoryRouter([
        {
            path: '/tasks/10/20/edit',
            element: (
                <>
                    <ProcessTaskViewPageEdit/>
                    <Link to="/target">Aufgabe verlassen</Link>
                </>
            ),
        },
        {
            path: '/target',
            element: <div>Zielseite</div>,
        },
    ], {
        initialEntries: ['/tasks/10/20/edit'],
    });

    render(<RouterProvider router={router}/>);
    await screen.findByRole('button', {name: 'Werte ändern'});

    expect(getTaskView).toHaveBeenCalledWith(10, 20);
    return {putTaskView};
}

describe('ProcessTaskViewPageEdit autosave', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        testState.dispatch.mockReset();
        testState.nextValues = {};
        vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it.each([
        {
            description: 'the first row of a replicating list is removed',
            initialValues: {
                rows: literalAuthoredValue([
                    {id: 'row-1', values: {name: literalAuthoredValue('First')}},
                    {id: 'row-2', values: {name: literalAuthoredValue('Second')}},
                ]),
            },
            nextValues: {
                rows: literalAuthoredValue([
                    {id: 'row-2', values: {name: literalAuthoredValue('Second')}},
                ]),
            },
        },
        {
            description: 'the second row of a replicating list is removed',
            initialValues: {
                rows: literalAuthoredValue([
                    {id: 'row-1', values: {name: literalAuthoredValue('First')}},
                    {id: 'row-2', values: {name: literalAuthoredValue('Second')}},
                ]),
            },
            nextValues: {
                rows: literalAuthoredValue([
                    {id: 'row-1', values: {name: literalAuthoredValue('First')}},
                ]),
            },
        },
        {
            description: 'an existing subject is cleared',
            initialValues: {
                subject: literalAuthoredValue('Existing subject'),
            },
            nextValues: {
                subject: literalAuthoredValue(null),
            },
        },
    ])('saves the first actual change when $description', async ({initialValues, nextValues}) => {
        const {putTaskView} = await renderPage(initialValues);
        testState.nextValues = nextValues;
        vi.useFakeTimers();

        fireEvent.click(screen.getByRole('button', {name: 'Werte ändern'}));

        expect(screen.getByText('Ungespeicherte Eingaben vorhanden')).toBeInTheDocument();
        expect(putTaskView).not.toHaveBeenCalled();

        await act(async () => {
            await vi.advanceTimersByTimeAsync(2000);
        });

        expect(putTaskView).toHaveBeenCalledOnce();
        expect(putTaskView).toHaveBeenCalledWith(10, 20, nextValues);
        expect(screen.getByText('Eingaben wurden zwischengespeichert')).toBeInTheDocument();
    });

    it('ignores an unchanged value notification', async () => {
        const initialValues = {
            subject: literalAuthoredValue('Existing subject'),
        };
        const {putTaskView} = await renderPage(initialValues);
        vi.useFakeTimers();

        fireEvent.click(screen.getByRole('button', {name: 'Unveränderte Werte melden'}));
        await act(async () => {
            await vi.advanceTimersByTimeAsync(2000);
        });

        expect(putTaskView).not.toHaveBeenCalled();
        expect(screen.getByText('Eingaben wurden zwischengespeichert')).toBeInTheDocument();
    });

    it('flushes the first change before navigating away', async () => {
        const initialValues = {
            subject: literalAuthoredValue('Existing subject'),
        };
        const nextValues = {
            subject: literalAuthoredValue(null),
        };
        const {putTaskView} = await renderPage(initialValues);
        testState.nextValues = nextValues;
        vi.useFakeTimers();

        fireEvent.click(screen.getByRole('button', {name: 'Werte ändern'}));
        fireEvent.click(screen.getByRole('link', {name: 'Aufgabe verlassen'}));

        await act(async () => {
            await Promise.resolve();
        });

        expect(putTaskView).toHaveBeenCalledOnce();
        expect(putTaskView).toHaveBeenCalledWith(10, 20, nextValues);
        expect(screen.getByText('Zielseite')).toBeInTheDocument();
        expect(screen.queryByText('Ungespeicherte Eingaben')).not.toBeInTheDocument();
    });
});
