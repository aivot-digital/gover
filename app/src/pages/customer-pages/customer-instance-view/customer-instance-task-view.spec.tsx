import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import type {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {ProcessInstanceTaskApiService, type TaskViewEvent} from '../../../modules/process/services/process-instance-task-api-service';
import {CustomerTaskViewApiService, type TaskViewResponse} from './customer-task-view-api-service';
import {CustomerInstanceTaskView} from './customer-instance-task-view';

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(),
    elementDerivationProps: undefined as any,
    invalidateInstanceTasks: vi.fn(),
    navigate: vi.fn(),
    refreshInstanceStatus: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...await importOriginal<typeof import('react-router-dom')>(),
    useNavigate: () => mocks.navigate,
    useOutletContext: () => ({
        invalidateInstanceTasks: mocks.invalidateInstanceTasks,
        refreshInstanceStatus: mocks.refreshInstanceStatus,
    }),
    useParams: () => ({
        instanceAccessKey: 'instance-key',
        taskAccessKey: 'task-key',
    }),
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => mocks.dispatch,
}));

vi.mock('../../../utils/with-delay', () => ({
    withDelay: (promise: Promise<unknown>) => promise,
}));

vi.mock('../../../modules/elements/components/element-derivation-context', () => ({
    ElementDerivationContext: function ElementDerivationContextMock(props: any) {
        mocks.elementDerivationProps = props;

        return (
            <div>
                <button
                    type="button"
                    onClick={() => props.onAuthoredElementValuesChange({field: 'edited'})}
                >
                    Werte ändern
                </button>
                <button
                    type="button"
                    onClick={() => void props.onEvent({field: 'inline'}, 'submit')}
                >
                    Inline-Event
                </button>
                <output data-testid="computed-errors">
                    {JSON.stringify(props.computedErrors ?? {})}
                </output>
            </div>
        );
    },
}));

describe('CustomerInstanceTaskView', () => {
    beforeEach(() => {
        mocks.dispatch.mockReset();
        mocks.elementDerivationProps = undefined;
        mocks.invalidateInstanceTasks.mockReset();
        mocks.navigate.mockReset();
        mocks.refreshInstanceStatus.mockReset().mockResolvedValue(createInstanceStatus());

        vi.spyOn(CustomerTaskViewApiService.prototype, 'getTaskView')
            .mockResolvedValue(createTaskView());
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'putCustomerTaskView')
            .mockResolvedValue(createTaskView({
                data: {field: 'saved'},
                events: [createEvent('continue', 'Weiter')],
            }));
    });

    it('renders backend events and submits the latest authored values on click', async () => {
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);

        await user.click(await screen.findByRole('button', {name: 'Werte ändern'}));
        await user.click(screen.getByRole('button', {name: 'Daten einreichen'}));

        await waitFor(() => expect(ProcessInstanceTaskApiService.prototype.putCustomerTaskView).toHaveBeenCalledWith(
            'instance-key',
            'task-key',
            {field: 'edited'},
            'submit',
        ));
        expect(mocks.refreshInstanceStatus).toHaveBeenCalledOnce();
        expect(await screen.findByRole('button', {name: 'Weiter'})).toBeInTheDocument();
        expectDispatchedAction('shell/setLoadingMessage', {
            blocking: true,
            estimatedTime: 500,
            message: 'Verarbeite Aktion: Daten einreichen',
        });
    });

    it('uses the same event metadata and handler for inline events', async () => {
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);
        await user.click(await screen.findByRole('button', {name: 'Inline-Event'}));

        await waitFor(() => expect(ProcessInstanceTaskApiService.prototype.putCustomerTaskView).toHaveBeenCalledWith(
            'instance-key',
            'task-key',
            {field: 'inline'},
            'submit',
        ));
        expectDispatchedAction('shell/setLoadingMessage', expect.objectContaining({
            message: 'Verarbeite Aktion: Daten einreichen',
        }));
    });

    it('shows validation errors in the form and in a snackbar', async () => {
        vi.mocked(ProcessInstanceTaskApiService.prototype.putCustomerTaskView).mockRejectedValue({
            status: 400,
            message: 'Bitte prüfen Sie Ihre Eingaben.',
            displayableToUser: true,
            details: {
                effectiveValues: {},
                elementStates: {
                    field: {
                        error: 'Dieses Feld ist erforderlich.',
                    },
                },
            },
        });
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);
        await user.click(await screen.findByRole('button', {name: 'Daten einreichen'}));

        expect(await screen.findByTestId('computed-errors')).toHaveTextContent('Dieses Feld ist erforderlich.');
        expectSnackbar('Bitte prüfen Sie Ihre Eingaben.');
        expectActionTypeNotDispatched('shell/setErrorMessage');
        expect(mocks.refreshInstanceStatus).not.toHaveBeenCalled();
    });

    it('shows other event errors in a snackbar without replacing the page', async () => {
        vi.mocked(ProcessInstanceTaskApiService.prototype.putCustomerTaskView).mockRejectedValue(new Error('network'));
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);
        await user.click(await screen.findByRole('button', {name: 'Daten einreichen'}));

        await waitFor(() => expectSnackbar('Die Aufgabe konnte nicht verarbeitet werden.'));
        expectActionTypeNotDispatched('shell/setErrorMessage');
        expect(screen.getByRole('button', {name: 'Daten einreichen'})).toBeInTheDocument();
    });

    it('returns to the instance page when the status refresh fails after a successful event', async () => {
        mocks.refreshInstanceStatus.mockRejectedValue(new Error('status unavailable'));
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);
        await user.click(await screen.findByRole('button', {name: 'Daten einreichen'}));

        await waitFor(() => expect(mocks.navigate).toHaveBeenCalledWith('/process/instance-key', {replace: true}));
        expect(mocks.invalidateInstanceTasks).toHaveBeenCalledOnce();
        expectSnackbar('Der Aufgabenstatus konnte nach der Aktion nicht aktualisiert werden.');
    });
});

function createTaskView(overrides?: Partial<TaskViewResponse>): TaskViewResponse {
    return {
        layout: {} as GroupLayout,
        data: {field: 'initial'},
        events: [createEvent('submit', 'Daten einreichen')],
        ...overrides,
    };
}

function createEvent(event: string, label: string): TaskViewEvent {
    return {
        event,
        label,
    };
}

function createInstanceStatus() {
    return {
        title: 'Testvorgang',
        status: 'Running',
        statusOverride: '',
        tasks: [],
    };
}

function expectSnackbar(message: string): void {
    expectDispatchedAction('shell/addSnackbarMessage', expect.objectContaining({message}));
}

function expectActionTypeNotDispatched(type: string): void {
    expect(mocks.dispatch).not.toHaveBeenCalledWith(expect.objectContaining({type}));
}

function expectDispatchedAction(type: string, payload: unknown): void {
    expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({
        payload,
        type,
    }));
}
