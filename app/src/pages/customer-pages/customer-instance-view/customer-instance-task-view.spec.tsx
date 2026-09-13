import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createDerivedRuntimeElementData} from '../../../models/element-data';
import type {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {IdentityProviderType} from '../../../modules/identity/enums/identity-provider-type';
import type {IdentitySlot} from '../../../modules/identity/models/identity-slot';
import type {ReadyCustomerTaskView} from '../../../modules/process/models/customer-task-view';
import {
    ProcessInstanceTaskApiService,
    type TaskViewEvent,
} from '../../../modules/process/services/process-instance-task-api-service';
import {CustomerInstanceTaskView} from './customer-instance-task-view';
import {CustomerTaskViewApiService, type TaskViewResponse} from './customer-task-view-api-service';

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(),
    elementDerivationProps: undefined as any,
    invalidateInstanceTasks: vi.fn(),
    navigate: vi.fn(),
    refreshInstanceStatus: vi.fn(),
    taskIsActive: true,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...await importOriginal<typeof import('react-router-dom')>(),
    useNavigate: () => mocks.navigate,
    useOutletContext: () => ({
        invalidateInstanceTasks: mocks.invalidateInstanceTasks,
        refreshInstanceStatus: mocks.refreshInstanceStatus,
        taskIsActive: mocks.taskIsActive,
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
                <button
                    type="button"
                    onClick={() => void props.onDeriveOverride({field: 'derived'}, [])}
                >
                    Ableiten
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
        vi.restoreAllMocks();
        window.history.replaceState({}, '', '/process/instance-key/tasks/task-key');
        mocks.dispatch.mockReset();
        mocks.elementDerivationProps = undefined;
        mocks.invalidateInstanceTasks.mockReset();
        mocks.navigate.mockReset();
        mocks.refreshInstanceStatus.mockReset().mockResolvedValue(createInstanceStatus());
        mocks.taskIsActive = true;

        vi.spyOn(CustomerTaskViewApiService.prototype, 'getTaskView')
            .mockResolvedValue(createTaskView());
        vi.spyOn(ProcessInstanceTaskApiService.prototype, 'putCustomerTaskView')
            .mockResolvedValue(createTaskView({
                data: {field: 'saved'},
                events: [createEvent('continue', 'Weiter')],
            }));
    });

    it('shows the configured provider login for an existing identity', async () => {
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValue(createBlockedTaskView({
                existingIdentitySlot: createExistingIdentitySlot(),
            }));

        render(<CustomerInstanceTaskView/>);

        expect(await screen.findByText('Empfängeridentität')).toBeInTheDocument();
        expect(screen.getByText(/erneute Anmeldung erforderlich/)).toBeInTheDocument();
        const loginLink = screen.getByRole('link', {name: /Mit „BundID“ anmelden/});
        expect(loginLink).toHaveAttribute(
            'href',
            expect.stringContaining('/api/public/processes/instance-key/tasks/task-key/identity/start/'),
        );
        expect(screen.getByRole('button', {name: 'Mit Aufgabe fortfahren'})).toBeDisabled();
        expectActionTypeNotDispatched('shell/setErrorMessage');
    });

    it('keeps reauthentication available and explains a wrong provider account', async () => {
        window.history.replaceState({}, '', '/process/instance-key/tasks/task-key?identity-state=0');
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValue(createBlockedTaskView({
                existingIdentitySlot: createExistingIdentitySlot({
                    identityProvider: createIdentityProvider(true),
                }),
            }));

        render(<CustomerInstanceTaskView/>);

        expect(await screen.findByText('Falsches Nutzerkonto')).toBeInTheDocument();
        expect(screen.getByText(/gehört nicht zur Empfängeridentität/)).toBeInTheDocument();
        expect(screen.getByRole('link', {name: /Mit „BundID“ anmelden/})).toBeInTheDocument();
        await waitFor(() => expect(window.location.search).toBe(''));
    });

    it('shows an authenticated existing identity until the customer explicitly continues', async () => {
        const existingIdentitySlot = createExistingIdentitySlot({
            isReady: true,
            identityProvider: createIdentityProvider(true),
        });
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValueOnce(createTaskView({existingIdentitySlot}))
            .mockResolvedValueOnce(createTaskView({existingIdentitySlot}));
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);

        expect(await screen.findByRole('status')).toHaveTextContent('Mit „BundID“ angemeldet');
        expect(screen.queryByRole('button', {name: 'Daten einreichen'})).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Mit Aufgabe fortfahren'}));

        expect(await screen.findByRole('button', {name: 'Daten einreichen'})).toBeInTheDocument();
        expect(CustomerTaskViewApiService.prototype.getTaskView).toHaveBeenCalledTimes(2);
    });

    it('stores a new email identity through the task endpoint before opening the task', async () => {
        const initialSlot = createIdentitySlot();
        const savedSlot = createIdentitySlot({
            identityType: 'Email',
            emailAddress: 'customer@example.test',
            isReady: true,
        });
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValueOnce(createBlockedTaskView({newIdentitySlot: initialSlot}))
            .mockResolvedValueOnce(createTaskView({newIdentitySlot: savedSlot}));
        const setEmail = vi.spyOn(CustomerTaskViewApiService.prototype, 'setNewIdentityEmail')
            .mockResolvedValue(savedSlot);
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);

        expect(await screen.findByText('Antragsteller:in')).toBeInTheDocument();
        const continueButton = screen.getByRole('button', {name: 'Mit Aufgabe fortfahren'});
        expect(continueButton).toBeDisabled();
        await user.type(screen.getByRole('textbox', {name: /E-Mail-Adresse/}), 'customer@example.test');
        await waitFor(() => expect(continueButton).toBeEnabled());
        await user.click(continueButton);

        await waitFor(() => expect(setEmail).toHaveBeenCalledWith(
            'instance-key',
            'task-key',
            'applicant',
            'customer@example.test',
        ));
        expect(await screen.findByRole('button', {name: 'Daten einreichen'})).toBeInTheDocument();
    });

    it('lets the customer skip an untouched optional new identity', async () => {
        const optionalSlot = createIdentitySlot({isOptional: true, isRequired: false});
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValue(createTaskView({newIdentitySlot: optionalSlot}));
        const setEmail = vi.spyOn(CustomerTaskViewApiService.prototype, 'setNewIdentityEmail');
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);

        await user.click(await screen.findByRole('button', {name: 'Ohne Anmeldung fortfahren'}));

        expect(setEmail).not.toHaveBeenCalled();
        expect(await screen.findByRole('button', {name: 'Daten einreichen'})).toBeInTheDocument();
    });

    it('stores a new provider communication choice through the task endpoints', async () => {
        const initialCommunication = {
            required: true,
            ready: false,
            selectedBindingId: null,
            choices: [
                {id: 10, name: 'E-Mail', description: 'Versand per E-Mail'},
                {id: 20, name: 'Postfach', description: 'Digitales Postfach'},
            ],
            customerLayout: null,
            customerData: {},
            derivedData: createDerivedRuntimeElementData(),
        };
        const previewCommunication = {...initialCommunication, selectedBindingId: 20};
        const savedCommunication = {...previewCommunication, ready: true};
        const initialSlot = createIdentitySlot({
            allowsEmail: false,
            identityType: 'IdentityProvider',
            availableIdentityProviders: [createIdentityProvider(true)],
            communication: initialCommunication,
        });
        const savedSlot = createIdentitySlot({
            allowsEmail: false,
            identityType: 'IdentityProvider',
            isReady: true,
            availableIdentityProviders: [createIdentityProvider(true)],
            communication: savedCommunication,
        });
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValueOnce(createBlockedTaskView({newIdentitySlot: initialSlot}))
            .mockResolvedValueOnce(createTaskView({newIdentitySlot: savedSlot}));
        const deriveCommunication = vi.spyOn(
            CustomerTaskViewApiService.prototype,
            'deriveNewIdentityCommunication',
        ).mockResolvedValue(previewCommunication);
        const selectCommunication = vi.spyOn(
            CustomerTaskViewApiService.prototype,
            'selectNewIdentityCommunication',
        ).mockResolvedValue(savedCommunication);
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);

        const continueButton = await screen.findByRole('button', {name: 'Mit Aufgabe fortfahren'});
        expect(continueButton).toBeDisabled();
        await user.click(screen.getByRole('radio', {name: /Postfach/}));
        await waitFor(() => expect(deriveCommunication).toHaveBeenCalledWith(
            'instance-key',
            'task-key',
            'applicant',
            20,
            {},
            ['ALL'],
        ));
        await waitFor(() => expect(continueButton).toBeEnabled());
        await user.click(continueButton);

        await waitFor(() => expect(selectCommunication).toHaveBeenCalledWith(
            'instance-key',
            'task-key',
            'applicant',
            20,
            {},
        ));
        expect(await screen.findByRole('button', {name: 'Daten einreichen'})).toBeInTheDocument();
    });

    it('persists a new identity communication draft before reauthenticating the existing identity', async () => {
        const initialCommunication = {
            required: true,
            ready: false,
            selectedBindingId: null,
            choices: [
                {id: 10, name: 'E-Mail', description: 'Versand per E-Mail'},
                {id: 20, name: 'Postfach', description: 'Digitales Postfach'},
            ],
            customerLayout: null,
            customerData: {},
            derivedData: createDerivedRuntimeElementData(),
        };
        const previewCommunication = {...initialCommunication, selectedBindingId: 20};
        const newIdentitySlot = createIdentitySlot({
            allowsEmail: false,
            identityType: 'IdentityProvider',
            availableIdentityProviders: [createIdentityProvider(true)],
            communication: initialCommunication,
        });
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValue(createBlockedTaskView({
                existingIdentitySlot: createExistingIdentitySlot({id: 'recipient'}),
                newIdentitySlot,
            }));
        vi.spyOn(CustomerTaskViewApiService.prototype, 'createRequiredIdentityAuthenticationStartLink')
            .mockReturnValue('#required-login');
        vi.spyOn(CustomerTaskViewApiService.prototype, 'deriveNewIdentityCommunication')
            .mockResolvedValue(previewCommunication);
        let resolveSelection!: (state: typeof previewCommunication) => void;
        const selectionPending = new Promise<typeof previewCommunication>((resolve) => {
            resolveSelection = resolve;
        });
        const selectCommunication = vi.spyOn(
            CustomerTaskViewApiService.prototype,
            'selectNewIdentityCommunication',
        ).mockReturnValue(selectionPending);
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);

        await user.click(await screen.findByRole('radio', {name: /Postfach/}));
        await user.click(screen.getByRole('link', {name: /Mit „BundID“ anmelden/}));

        await waitFor(() => expect(selectCommunication).toHaveBeenCalledWith(
            'instance-key',
            'task-key',
            'applicant',
            20,
            {},
        ));
        expect(window.location.hash).toBe('');

        resolveSelection(previewCommunication);
        await waitFor(() => expect(window.location.hash).toBe('#required-login'));
    });

    it('shows existing and new identity requirements together', async () => {
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValue(createBlockedTaskView({
                existingIdentitySlot: createExistingIdentitySlot(),
                newIdentitySlot: createIdentitySlot({title: 'Vertretung'}),
            }));

        render(<CustomerInstanceTaskView/>);

        expect(await screen.findByText('Empfängeridentität')).toBeInTheDocument();
        expect(screen.getByText('Vertretung')).toBeInTheDocument();
        const loginLinks = screen.getAllByRole('link', {name: /Mit „BundID“ anmelden/});
        expect(loginLinks).toHaveLength(2);
        expect(loginLinks.some((link) => link.getAttribute('href')?.includes(
            '/identities/applicant/providers/36a9a19d-f9fb-4225-a9a0-07a223820b4b/start/',
        ))).toBe(true);
        expect(screen.getByRole('button', {name: 'Mit Aufgabe fortfahren'})).toBeDisabled();
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

    it('reloads identity requirements when an event loses its identity session', async () => {
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValueOnce(createTaskView())
            .mockResolvedValueOnce(createBlockedTaskView({
                existingIdentitySlot: createExistingIdentitySlot(),
            }));
        vi.mocked(ProcessInstanceTaskApiService.prototype.putCustomerTaskView)
            .mockRejectedValue(requiredIdentityAuthenticationError());
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);
        await user.click(await screen.findByRole('button', {name: 'Daten einreichen'}));

        expect(await screen.findByRole('link', {name: /Mit „BundID“ anmelden/})).toBeInTheDocument();
        expectActionTypeNotDispatched('shell/addSnackbarMessage');
    });

    it('reloads identity requirements when derivation loses its identity session', async () => {
        vi.mocked(CustomerTaskViewApiService.prototype.getTaskView)
            .mockResolvedValueOnce(createTaskView())
            .mockResolvedValueOnce(createBlockedTaskView({
                existingIdentitySlot: createExistingIdentitySlot(),
            }));
        vi.spyOn(CustomerTaskViewApiService.prototype, 'deriveTaskView')
            .mockRejectedValue(requiredIdentityAuthenticationError());
        const user = userEvent.setup();

        render(<CustomerInstanceTaskView/>);
        await user.click(await screen.findByRole('button', {name: 'Ableiten'}));

        expect(await screen.findByRole('link', {name: /Mit „BundID“ anmelden/})).toBeInTheDocument();
        expectActionTypeNotDispatched('shell/addSnackbarMessage');
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

    it('renders completed task content without task events', async () => {
        mocks.taskIsActive = false;

        render(<CustomerInstanceTaskView/>);

        await waitFor(() => expect(mocks.elementDerivationProps).toBeDefined());
        expect(mocks.elementDerivationProps.readOnly).toBe(true);
        expect(mocks.elementDerivationProps.onEvent).toBeUndefined();
        expect(screen.queryByRole('button', {name: 'Daten einreichen'})).not.toBeInTheDocument();
    });
});

function createTaskView(overrides?: Partial<ReadyCustomerTaskView>): ReadyCustomerTaskView {
    return {
        layout: {} as GroupLayout,
        data: {field: 'initial'},
        events: [createEvent('submit', 'Daten einreichen')],
        newIdentitySlot: null,
        existingIdentitySlot: null,
        ...overrides,
    };
}

function createBlockedTaskView(overrides?: Partial<TaskViewResponse>): TaskViewResponse {
    return {
        layout: null,
        data: null,
        events: null,
        newIdentitySlot: null,
        existingIdentitySlot: null,
        ...overrides,
    } as TaskViewResponse;
}

function createIdentitySlot(overrides?: Partial<IdentitySlot>): IdentitySlot {
    return {
        id: 'applicant',
        title: 'Antragsteller:in',
        description: null,
        isOptional: false,
        isRequired: true,
        allowsEmail: true,
        identityType: null,
        emailAddress: null,
        isReady: false,
        availableIdentityProviders: [createIdentityProvider()],
        communication: null,
        ...overrides,
    };
}

function createIdentityProvider(isAuthenticatedWithThis = false): IdentitySlot['availableIdentityProviders'][number] {
    return {
        identityProviderKey: '36a9a19d-f9fb-4225-a9a0-07a223820b4b',
        identityProviderName: 'BundID',
        identityProviderAssetKey: null,
        identityProviderType: IdentityProviderType.BundID,
        isAuthenticatedWithThis,
        additionalScopes: [],
    };
}

function createExistingIdentitySlot(
    overrides?: Partial<NonNullable<TaskViewResponse['existingIdentitySlot']>>,
): NonNullable<TaskViewResponse['existingIdentitySlot']> {
    return {
        id: 'applicant',
        isReady: false,
        identityProvider: createIdentityProvider(),
        ...overrides,
    };
}

function createEvent(event: string, label: string): TaskViewEvent {
    return {event, label};
}

function createInstanceStatus() {
    return {
        title: 'Testvorgang',
        status: 'Running',
        statusOverride: '',
        tasks: [],
    };
}

function requiredIdentityAuthenticationError() {
    return {
        status: 401,
        message: 'Erneute Anmeldung erforderlich.',
        displayableToUser: true,
        details: {
            reason: 'required_identity_authentication',
        },
    };
}

function expectSnackbar(message: string): void {
    expectDispatchedAction('shell/addSnackbarMessage', expect.objectContaining({message}));
}

function expectActionTypeNotDispatched(type: string): void {
    expect(mocks.dispatch).not.toHaveBeenCalledWith(expect.objectContaining({type}));
}

function expectDispatchedAction(type: string, payload: unknown): void {
    expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({payload, type}));
}
