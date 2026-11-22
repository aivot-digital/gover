import {AuthService} from '../../../../../../services/auth-service';
import {EditorDraftStorage} from '../../../../../../services/editor-draft-storage';
import {getObservedEditorDraftLogoutId, signalEditorDraftLogout} from '../../../../../../services/editor-draft-logout';
import {prepareEditorDraftLogin, stopEditorDraftForLogout} from '../../../../../../services/editor-draft-login';
import {UsersApiService} from '../../../../../users/users-api-service';
import {ProcessNodeEditorOutputsTab} from './tabs/process-node-editor-outputs-tab';
import {ProcessNodeEditorConfigurationTab} from './tabs/process-node-editor-configuration-tab';
import userEvent from '@testing-library/user-event';
import {useState} from 'react';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, type MockInstance, vi} from 'vitest';
import {createMemoryRouter, RouterProvider} from 'react-router-dom';
import {ProcessNodeEditor} from './process-node-editor';
import {useProcessNodeEditorContext} from './process-node-editor-context';
import {ProcessDetailsPageProvider} from '../../process-details-page-context';
import {ProcessNodeApiService} from '../../../../services/process-node-api-service';
import {
    ProcessNodeProviderApiService,
    ProcessNodeType,
    type ProcessNodeProvider,
} from '../../../../services/process-node-provider-api-service';
import {type ProcessNodeEntity} from '../../../../entities/process-node-entity';
import {type ProcessNodeProblems} from '../../../../entities/process-node-problems';
import {type ProcessNodeDefinitionMetadata} from '../../../../entities/process-node-definition-metadata';
import {ProcessDataKeyInputComponent} from '../../../../../../views/process-data-key-input-field-view';
import {generateElementWithDefaultValues} from '../../../../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../../../../data/element-type/element-type';
import {InputMode, InputVariableSource} from '../../../../../../models/input-mode';
import {type DerivedRuntimeElementData} from '../../../../../../models/element-data';
import {SnackbarAction} from '../../../../../../slices/shell-slice';

const testState = vi.hoisted(() => ({
    userId: 'user', dispatch: vi.fn(), save: vi.fn(), stored: vi.fn(), confirm: vi.fn(),
    remove: vi.fn(), replace: vi.fn(), loadDraft: vi.fn(), clearDraft: vi.fn(), trackDraft: vi.fn(), realDraft: false,
    beginManualSave: vi.fn(async () => ({signal: new AbortController().signal, generation: 'current', resumesDrafts: false})),
    confirmManualSave: vi.fn(), isDraftRecoveryCurrent: vi.fn().mockResolvedValue(true), backUpDraft: vi.fn(),
}));

vi.mock('../../../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => testState.dispatch}));
vi.mock('../../../../../../hooks/use-app-selector', () => ({useAppSelector: () => ({id: testState.userId})}));
vi.mock('../../../../../../providers/confirm-provider', () => ({useConfirm: () => testState.confirm}));
vi.mock('../../../../../../components/view-dispatcher/view-dispatcher.component', () => ({ViewDispatcherComponent: () => null}));
vi.mock('../../../../../../hooks/use-editor-draft', async (importOriginal) => {
    const original = await importOriginal<typeof import('../../../../../../hooks/use-editor-draft')>();
    return {
        useEditorDraft: (props: Parameters<typeof original.useEditorDraft>[0]) => {
            testState.trackDraft(props);
            if (testState.realDraft) return original.useEditorDraft(props);
            return {
                loadDraft: testState.loadDraft, clearDraft: testState.clearDraft, discardForNavigation: testState.clearDraft, isReady: true,
                draftLogoutSignal: undefined, isDraftRecoveryCurrent: testState.isDraftRecoveryCurrent,
                beginManualSave: testState.beginManualSave, confirmManualSave: testState.confirmManualSave,
                backUpDraft: testState.backUpDraft,
            };
        },
    };
});
vi.mock('../../../../components/process-node-provider-details', () => ({
    getProcessNodeProviderIcon: () => () => null,
    ProcessNodeProviderDetailsDialog: () => null,
}));

const warning = 'Auswahlvorschläge und wiederverwendbare Inhalte konnten nicht geladen werden.';
const provider: ProcessNodeProvider = {
    key: 'test.node', componentKey: 'node', componentType: 'process-node', componentVersion: '1.0.0',
    deprecationNotice: null, majorVersion: 1, type: ProcessNodeType.Action, executionTypes: [],
    name: 'Testelement', abstractDescription: '', description: '', documentationUrl: null,
    parentPluginKey: 'test', outputs: [],
};

function createNode(id: number): ProcessNodeEntity {
    return {
        ...ProcessNodeApiService.initialize(),
        id, processId: 10, processVersion: 1, name: `Element ${id}`, dataKey: `node${id}`,
        processNodeDefinitionKey: provider.key, processNodeDefinitionVersion: 1,
        outputMappings: {result: 'name'},
        configuration: {
            reference: {type: InputMode.Variable, reference: {source: InputVariableSource.ProcessData, path: 'person.name'}},
        },
    };
}

function createMetadata(): ProcessNodeDefinitionMetadata {
    return {
        reusableUiDefinitions: [], forwardedAttachmentSets: [], forwardedIdentities: [], inputVariables: [],
        forwardedProcessDataKeys: [{processDataKey: 'people[*].name', label: 'Name', subLabel: null, origin: createNode(1)}],
    };
}

function EditorFields() {
    const {node, setNode, isEditable} = useProcessNodeEditorContext();
    const mapping = node.configuration.reference;
    const reference = mapping?.type === InputMode.Variable ? mapping.reference :
        mapping?.type === InputMode.NoCode && mapping.operand.type === 'NoCodeNodeDataReference' ? mapping.operand : undefined;
    return <>
        <label>
            Name bearbeiten
            <input disabled={!isEditable} value={node.name ?? ''} onChange={(event) => setNode({...node, name: event.target.value}, false)}/>
        </label>
        {reference != null && <label>
            Variablenpfad
            <input disabled={!isEditable} value={reference.path ?? ''} onChange={(event) => {
                const path = event.target.value;
                setNode({...node, configuration: {...node.configuration, reference: 'source' in reference
                    ? {type: InputMode.Variable, reference: {...reference, path}}
                    : {type: InputMode.NoCode, operand: {...reference, path}},
                }}, false);
            }}/>
        </label>}
        <ProcessDataKeyInputComponent
            label="Gespeicherter Pfad"
            scopeProcessDataKey="people"
            value={node.outputMappings.result}
            disabled={!isEditable}
            onChange={(value) => setNode({...node, outputMappings: {...node.outputMappings, result: value}}, false)}
        />
    </>;
}

function EditorPage({editable: initiallyEditable}: {editable: boolean}) {
    const [editable, setEditable] = useState(initiallyEditable);
    const [version, setVersion] = useState(0);
    return <ProcessDetailsPageProvider value={{
        editable, structureEditable: editable, onSave: testState.save, onStoredNode: testState.stored,
        onDelete: testState.remove, onStartReplaceNode: testState.replace, testClaim: null,
        nodeRefreshSignal: {nodeId: 1, version}, nodeProblems: [], showNodeProblemsForNodes: {},
    }}>
        <button onClick={() => setVersion((value) => value + 1)}>Element neu laden</button>
        <button onClick={() => setEditable((value) => !value)}>Bearbeitungsrecht wechseln</button>
        <ProcessNodeEditor/>
    </ProcessDetailsPageProvider>;
}

function renderEditor({editable = true, tab = 'configuration', realConfiguration = false}: {editable?: boolean; tab?: string; realConfiguration?: boolean} = {}) {
    const router = createMemoryRouter([{
        path: '/processes/:processId/versions/:processVersion/nodes/:nodeId',
        element: <EditorPage editable={editable}/>,
        children: [
            {path: 'tabs/configuration', element: realConfiguration ? <ProcessNodeEditorConfigurationTab/> : <EditorFields/>},
            {path: 'tabs/more', element: <EditorFields/>},
            {path: 'tabs/outputs', element: <ProcessNodeEditorOutputsTab/>},
        ],
    }], {initialEntries: [`/processes/10/versions/1/nodes/1/tabs/${tab}`]});
    render(<RouterProvider router={router}/>);
    return router;
}

async function findReadyTextbox(name: string | RegExp) {
    return waitFor(() => {
        const field = screen.getByRole('textbox', {name});
        expect(field.closest('[inert]')).toBeNull();
        return field;
    });
}

describe('ProcessNodeEditor metadata loading', () => {
    let logError: MockInstance<typeof console.error>;

    beforeEach(() => {
        testState.userId = 'user';
        logError = vi.spyOn(console, 'error').mockImplementation(() => {});
        // jsdom does not implement element scrolling, which the editor uses on navigation.
        Object.defineProperty(HTMLElement.prototype, 'scrollTo', {value: vi.fn(), configurable: true, writable: true});
        testState.confirm.mockResolvedValue(true);
        testState.loadDraft.mockResolvedValue(null);
        testState.clearDraft.mockResolvedValue(undefined);
        testState.save.mockImplementation(async (node: ProcessNodeEntity) => node);
        vi.spyOn(ProcessNodeApiService.prototype, 'retrieve').mockImplementation(async (id) => createNode(id));
        vi.spyOn(ProcessNodeApiService.prototype, 'getConfigurationLayout')
            .mockResolvedValue(generateElementWithDefaultValues(ElementType.GroupLayout));
        vi.spyOn(ProcessNodeApiService.prototype, 'validate').mockResolvedValue(null);
        vi.spyOn(ProcessNodeApiService.prototype, 'getIncomingMetadata').mockResolvedValue(createMetadata());
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProvider').mockResolvedValue(provider);
    });

    afterEach(() => {
        Reflect.deleteProperty(HTMLElement.prototype, 'scrollTo');
    });

    it('opens, edits and saves after a metadata failure without losing saved paths or variable references', async () => {
        const error = new Error('Metadata failed');
        vi.mocked(ProcessNodeApiService.prototype.getIncomingMetadata).mockRejectedValue(error);
        renderEditor();

        // The editing hint is only added once draft recovery has finished, after the alert itself appears.
        await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(`${warning} Sie können die Konfiguration weiterhin bearbeiten.`));
        expect(logError).toHaveBeenCalledWith(error);
        expect(screen.getByRole('textbox', {name: /Gespeicherter Pfad/})).toHaveValue('name');
        expect(screen.queryByRole('button', {name: 'Vorgangsdatenpfad auswählen'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Konfiguration speichern'})).toBeDisabled();

        fireEvent.change(screen.getByRole('textbox', {name: 'Name bearbeiten'}), {target: {value: 'Geändert'}});
        fireEvent.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));

        await waitFor(() => expect(testState.save).toHaveBeenCalledOnce());
        expect(testState.save.mock.calls[0][0]).toMatchObject({
            name: 'Geändert', outputMappings: {result: 'name'}, configuration: createNode(1).configuration,
        });
        await waitFor(() => expect(screen.getByRole('button', {name: 'Konfiguration speichern'})).toBeDisabled());
    });

    it('sends only one save while a save is running', async () => {
        const pending = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(pending.promise);
        renderEditor();
        fireEvent.change(await findReadyTextbox('Name bearbeiten'), {target: {value: 'Geändert'}});

        const saveButton = screen.getByRole('button', {name: 'Konfiguration speichern'});
        fireEvent.click(saveButton);
        fireEvent.click(saveButton);

        await waitFor(() => expect(saveButton).toBeDisabled());
        await waitFor(() => expect(testState.save).toHaveBeenCalledOnce());
        await act(async () => pending.resolve({...createNode(1), name: 'Geändert'}));
        expect(testState.save).toHaveBeenCalledOnce();
    });

    it('preserves the unlocked state across tabs and failed saves, and relocks after a successful save', async () => {
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        const user = userEvent.setup();
        renderEditor({tab: 'outputs'});
        const input = await findReadyTextbox(/^Datenschlüssel/);
        expect(input).toHaveAttribute('readonly');
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        fireEvent.change(input, {target: {value: 'renamed_node'}});
        await user.click(screen.getByRole('tab', {name: 'Weiteres'}));
        await user.click(screen.getByRole('tab', {name: 'Ausgangsdaten'}));
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).not.toHaveAttribute('readonly');
        expect(testState.confirm).toHaveBeenCalledOnce();
        testState.save.mockRejectedValueOnce(new Error('Save failed'));
        await user.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));
        await waitFor(() => expect(testState.save).toHaveBeenCalledOnce());
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).not.toHaveAttribute('readonly');
        await user.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));
        await waitFor(() => expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveAttribute('readonly'));
        expect(testState.save.mock.calls[1][0].dataKey).toBe('renamed_node');
    });

    it('rejects an empty required node key even without blur', async () => {
        renderEditor({tab: 'outputs'});
        const input = await findReadyTextbox(/^Datenschlüssel/);
        await userEvent.setup().click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        fireEvent.change(input, {target: {value: ''}});
        fireEvent.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));
        expect(testState.save).not.toHaveBeenCalled();
        expect(input).toHaveAttribute('aria-invalid', 'true');
        await waitFor(() => expect(screen.getByRole('textbox', {name: /^Datenschlüssel/}))
            .toHaveAccessibleDescription(/Geben Sie einen Datenschlüssel ein/));
    });

    it('protects unsaved changes when navigating back to another node after an internal tab switch', async () => {
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        const user = userEvent.setup();
        const router = renderEditor();
        await screen.findByRole('textbox', {name: 'Name bearbeiten'});
        await user.click(screen.getByRole('tab', {name: 'Ausgangsdaten'}));
        await act(() => router.navigate('/processes/10/versions/1/nodes/2/tabs/configuration'));
        await waitFor(() => expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Element 2'));
        const nameInput = screen.getByRole('textbox', {name: 'Name bearbeiten'});
        fireEvent.change(nameInput, {target: {value: 'Ungespeichert'}});

        await act(() => router.navigate(-1));

        expect(await screen.findByRole('dialog', {name: 'Ungespeicherte Änderungen'})).toBeVisible();
        expect(nameInput).toHaveValue('Ungespeichert');
    });

    it('keeps manual locking across tabs without saving or changing the node key', async () => {
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        const user = userEvent.setup();
        renderEditor({tab: 'outputs'});
        const input = await findReadyTextbox(/^Datenschlüssel/);
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        fireEvent.change(input, {target: {value: 'renamed_node'}});
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel sperren'}));
        expect(input).toHaveAttribute('readonly');
        expect(testState.save).not.toHaveBeenCalled();
        await user.click(screen.getByRole('tab', {name: 'Weiteres'}));
        await user.click(screen.getByRole('tab', {name: 'Ausgangsdaten'}));
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveAttribute('readonly');
        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).toHaveValue('renamed_node');
        await user.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));
        await waitFor(() => expect(testState.save).toHaveBeenCalledOnce());
        expect(testState.save.mock.calls[0][0].dataKey).toBe('renamed_node');
    });

    it('does not offer editing in the metadata warning when the configuration is read-only', async () => {
        vi.mocked(ProcessNodeApiService.prototype.getIncomingMetadata).mockRejectedValue(new Error('Metadata failed'));
        renderEditor({editable: false});

        const alert = await screen.findByRole('alert');
        expect(alert).toHaveTextContent(warning);
        expect(testState.loadDraft).not.toHaveBeenCalled();
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({draftKey: null, enabled: false});
        expect(alert).not.toHaveTextContent('bearbeiten');
    });

    it('shows the metadata warning only on tabs that use suggestions', async () => {
        vi.mocked(ProcessNodeApiService.prototype.getIncomingMetadata).mockRejectedValue(new Error('Metadata failed'));
        renderEditor({tab: 'more'});

        await screen.findByRole('textbox', {name: 'Name bearbeiten'});
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('does not warn for successfully loaded empty metadata', async () => {
        vi.mocked(ProcessNodeApiService.prototype.getIncomingMetadata)
            .mockResolvedValue({...createMetadata(), forwardedProcessDataKeys: []});
        renderEditor();

        await screen.findByRole('textbox', {name: 'Name bearbeiten'});
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('restores suggestions and removes the warning after a successful refresh', async () => {
        vi.mocked(ProcessNodeApiService.prototype.getIncomingMetadata)
            .mockRejectedValueOnce(new Error('Metadata failed'));
        renderEditor();
        await screen.findByRole('alert');

        fireEvent.click(screen.getByRole('button', {name: 'Element neu laden'}));

        await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
        const path = screen.getByRole('textbox', {name: /Gespeicherter Pfad/});
        await waitFor(() => expect(path).toHaveAttribute('readonly'));
        expect(path).toHaveValue('name');
    });

    it('clears previous suggestions when the next node metadata fails', async () => {
        const router = renderEditor();
        await screen.findByRole('textbox', {name: 'Name bearbeiten'});
        expect(screen.getByRole('textbox', {name: /Gespeicherter Pfad/})).toHaveAttribute('readonly');
        vi.mocked(ProcessNodeApiService.prototype.getIncomingMetadata).mockRejectedValue(new Error('Metadata failed'));

        await act(() => router.navigate('/processes/10/versions/1/nodes/2/tabs/configuration'));

        await screen.findByRole('alert');
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Element 2');
        expect(screen.getByRole('textbox', {name: /Gespeicherter Pfad/})).not.toHaveAttribute('readonly');
        expect(screen.getByRole('textbox', {name: /Gespeicherter Pfad/})).toHaveValue('name');
    });

    it('keeps the shown node and its suggestions without allowing a save under the failed node route', async () => {
        const router = renderEditor();
        await screen.findByRole('textbox', {name: 'Name bearbeiten'});
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockRejectedValue(new Error('Node failed'));

        await act(() => router.navigate('/processes/10/versions/1/nodes/2/tabs/configuration'));

        await waitFor(() => expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Die Details für das Prozesselement konnten nicht geladen werden.'}),
        })));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Element 1');
        expect(screen.getByRole('textbox', {name: /Gespeicherter Pfad/})).toHaveAttribute('readonly');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        fireEvent.change(screen.getByRole('textbox', {name: 'Name bearbeiten'}), {target: {value: 'Geändert'}});
        expect(screen.getByRole('button', {name: 'Konfiguration speichern'})).toBeDisabled();
        expect(testState.save).not.toHaveBeenCalled();
    });

    it.each(['resolve', 'reject'] as const)('ignores a late metadata %s after switching nodes', async (outcome) => {
        const pending = Promise.withResolvers<ProcessNodeDefinitionMetadata>();
        vi.mocked(ProcessNodeApiService.prototype.getIncomingMetadata).mockReturnValueOnce(pending.promise);
        const router = renderEditor();

        await act(() => router.navigate('/processes/10/versions/1/nodes/2/tabs/configuration'));
        await screen.findByRole('textbox', {name: 'Name bearbeiten'});
        await act(async () => {
            if (outcome === 'resolve') pending.resolve({...createMetadata(), forwardedProcessDataKeys: []});
            else pending.reject(new Error('Late failure'));
        });

        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Element 2');
        expect(screen.getByRole('textbox', {name: /Gespeicherter Pfad/})).toHaveValue('name');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it.each(['retrieve', 'getConfigurationLayout', 'validate', 'getNodeProvider'] as const)
    ('still reports a required %s request failure', async (method) => {
        const error = new Error('Required data failed');
        if (method === 'getNodeProvider') {
            vi.mocked(ProcessNodeProviderApiService.prototype.getNodeProvider).mockRejectedValue(error);
        } else {
            vi.mocked(ProcessNodeApiService.prototype[method]).mockRejectedValue(error);
        }
        renderEditor();

        await waitFor(() => expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Die Details für das Prozesselement konnten nicht geladen werden.'}),
        })));
        expect(logError).toHaveBeenCalledWith(error);
        expect(screen.queryByRole('textbox', {name: 'Name bearbeiten'})).not.toBeInTheDocument();
    });
});

describe('ProcessNodeEditor drafts and save conflicts', () => {
    const timeout = {status: 504, message: 'Timeout', details: {reason: 'timeout'}, displayableToUser: true};
    const lockConflict = {
        status: 409,
        message: 'Der Datensatz wurde zwischenzeitlich geändert.',
        details: {reason: 'lock_version_conflict'},
        displayableToUser: true,
    };

    beforeEach(() => {
        testState.userId = 'user';
        vi.spyOn(console, 'error').mockImplementation(() => {});
        Object.defineProperty(HTMLElement.prototype, 'scrollTo', {value: vi.fn(), configurable: true, writable: true});
        testState.dispatch.mockReset();
        testState.save.mockReset();
        testState.confirm.mockReset().mockResolvedValue(true);
        testState.loadDraft.mockReset().mockResolvedValue(null);
        testState.clearDraft.mockReset().mockResolvedValue(undefined);
        testState.backUpDraft.mockReset().mockResolvedValue(false);
        vi.spyOn(ProcessNodeApiService.prototype, 'retrieve').mockImplementation(async (id) => ({...createNode(id), lockVersion: 1}));
        vi.spyOn(ProcessNodeApiService.prototype, 'getConfigurationLayout')
            .mockResolvedValue(generateElementWithDefaultValues(ElementType.GroupLayout));
        vi.spyOn(ProcessNodeApiService.prototype, 'validate').mockResolvedValue(null);
        vi.spyOn(ProcessNodeApiService.prototype, 'getIncomingMetadata').mockResolvedValue(createMetadata());
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProvider').mockResolvedValue(provider);
    });

    afterEach(() => {
        Reflect.deleteProperty(HTMLElement.prototype, 'scrollTo');
    });

    async function editName(name: string) {
        const input = await findReadyTextbox('Name bearbeiten');
        fireEvent.change(input, {target: {value: name}});
    }

    const saveButton = () => screen.getByRole('button', {name: 'Konfiguration speichern'});
    const nodeRoute = (id: number) => `/processes/10/versions/1/nodes/${id}/tabs/configuration`;

    async function requestNodeChange(router: ReturnType<typeof renderEditor>, id: number) {
        await act(() => router.navigate(nodeRoute(id)));
        return screen.findByRole('dialog', {name: 'Ungespeicherte Änderungen'});
    }

    async function discardAndOpenSecondNode(router: ReturnType<typeof renderEditor>) {
        const dialog = await requestNodeChange(router, 2);
        fireEvent.click(within(dialog).getByRole('button', {name: 'Änderungen verwerfen'}));
        await screen.findByDisplayValue('Element 2');
    }

    describe('an editor left open across an explicit logout', () => {
        const marker = () => getObservedEditorDraftLogoutId() ?? 'initial';
        let account: string;
        let authenticated: boolean;

        beforeEach(() => {
            testState.realDraft = true;
            account = 'user';
            authenticated = true;
            vi.spyOn(AuthService, 'isAuthenticated').mockImplementation(() => authenticated);
            vi.spyOn(EditorDraftStorage, 'openGeneration').mockImplementation(async () => marker());
            vi.spyOn(EditorDraftStorage, 'isCurrentGeneration').mockImplementation(async (value) => value === marker());
            vi.spyOn(EditorDraftStorage, 'load').mockResolvedValue(null);
            vi.spyOn(EditorDraftStorage, 'remove').mockResolvedValue();
            vi.spyOn(EditorDraftStorage, 'reserve').mockResolvedValue(true);
            vi.spyOn(EditorDraftStorage, 'save').mockResolvedValue();
            vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockImplementation(async () => ({
                ...new UsersApiService().initialize(), id: account,
            }));
        });
        afterEach(() => {
            testState.realDraft = false;
        });

        async function logoutInOtherTab() {
            authenticated = false;
            const id = crypto.randomUUID();
            signalEditorDraftLogout(id);
            await act(() => stopEditorDraftForLogout(id));
            vi.mocked(EditorDraftStorage.save).mockClear();
            return id;
        }

        it('resumes backups only for the same account after confirmation, keeping input typed during that save', async () => {
            const pending = Promise.withResolvers<ProcessNodeEntity>();
            testState.save.mockReturnValue(pending.promise);
            renderEditor();
            await editName('Im Editor behalten');
            const generation = await logoutInOtherTab();
            account = 'another-account';
            authenticated = true;
            fireEvent.click(saveButton());
            await waitFor(() => expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
                payload: expect.objectContaining({message: expect.stringContaining('Melden Sie sich zum Speichern')}),
            })));
            expect(testState.save).not.toHaveBeenCalled();
            expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Im Editor behalten');

            account = 'user';
            fireEvent.click(saveButton());
            await waitFor(() => expect(testState.save).toHaveBeenCalledOnce());
            await editName('Während des Speicherns eingegeben');
            expect(await prepareEditorDraftLogin()).toBeUndefined();
            expect(EditorDraftStorage.save).not.toHaveBeenCalled();

            const confirmed = {...createNode(1), name: 'Im Editor behalten', notes: 'Vom Server', lockVersion: 2};
            await act(async () => pending.resolve(confirmed));
            expect(saveButton()).toBeEnabled();
            await prepareEditorDraftLogin();
            expect(EditorDraftStorage.save).toHaveBeenCalledWith(expect.objectContaining({
                userId: 'user', key: 'process-node/1/configuration', base: confirmed, baseLockVersion: 2,
                mine: {...confirmed, name: 'Während des Speicherns eingegeben'},
            }), generation);
        });

        it.each(['late response', 'failed save'] as const)('keeps local drafts suspended after a %s', async (outcome) => {
            const pending = Promise.withResolvers<ProcessNodeEntity>();
            testState.save.mockReturnValue(pending.promise);
            renderEditor();
            await editName('Beim Klick');
            if (outcome === 'failed save') {
                await logoutInOtherTab();
                authenticated = true;
            }
            fireEvent.click(saveButton());
            await waitFor(() => expect(testState.save).toHaveBeenCalledOnce());
            if (outcome === 'late response') await logoutInOtherTab();
            await editName('Im Arbeitsspeicher');
            if (outcome === 'late response') {
                await act(async () => pending.resolve({...createNode(1), name: 'Verspätet', notes: 'Anderer Stand', lockVersion: 99}));
            } else {
                await act(async () => pending.reject(new Error('Speichern fehlgeschlagen')));
            }
            expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({baseLockVersion: 1, edited: {name: 'Im Arbeitsspeicher'}});
            expect(await prepareEditorDraftLogin()).toBeUndefined();
            expect(EditorDraftStorage.save).not.toHaveBeenCalled();
        });

        it('closes the recovery dialog and releases the server preview when another tab logs out', async () => {
            const base = {...createNode(1), lockVersion: 1};
            vi.mocked(EditorDraftStorage.load).mockResolvedValue({
                base, baseLockVersion: 1, mine: {...base, name: 'Alter Entwurf'}, key: 'process-node/1/configuration',
                userId: account, tabId: 'this-tab', savedAt: Date.now(),
            });
            renderEditor();
            await screen.findByRole('dialog', {name: 'Ungespeicherte Änderungen wiederherstellen?'});
            await logoutInOtherTab();
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            const name = await findReadyTextbox('Name bearbeiten');
            expect(name).toHaveValue(base.name);
            fireEvent.change(name, {target: {value: 'Im Editor erlaubt'}});
            expect(await prepareEditorDraftLogin()).toBeUndefined();
            expect(EditorDraftStorage.save).not.toHaveBeenCalled();
        });
    });

    it.each(['Späterer Name', 'Element 1'])('keeps "%s" edited after the saved name and uses the confirmed version on the next save', async (later) => {
        const first = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(first.promise).mockImplementation(async (node: ProcessNodeEntity) => node);
        renderEditor();
        await editName('Gesendet');
        fireEvent.click(saveButton());
        await editName(later);
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            hasChanged: true, edited: {name: later}, saveCheckpoint: {phase: 'pending', submitted: {name: 'Gesendet'}},
        });
        const saved = {...createNode(1), name: 'Gesendet', notes: 'Vom Server', lockVersion: 2};
        await act(async () => first.resolve(saved));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue(later);
        expect(saveButton()).toBeEnabled();
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            hasChanged: true, base: saved, baseLockVersion: 2, edited: {...saved, name: later}, saveCheckpoint: undefined,
        });
        expect(testState.save).toHaveBeenCalledTimes(1);
        fireEvent.click(saveButton());
        await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(2));
        expect(testState.save.mock.calls[1][0]).toEqual({...saved, name: later});
    });

    it.each(['retry', 'discard'])('preserves later edits when resolving a save conflict by %s', async (decision) => {
        const first = Promise.withResolvers<ProcessNodeEntity>();
        const second = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
        renderEditor();
        await editName('Gesendet');
        fireEvent.click(saveButton());
        await editName('Später bearbeitet');
        const stored = {...createNode(1), name: 'Anderer Name', notes: 'Notiz vom Server', lockVersion: 2};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(stored);
        await act(async () => first.reject(lockConflict));
        const dialog = await screen.findByRole('dialog', {name: 'Neuerer Stand gespeichert'});
        if (decision === 'retry') {
            fireEvent.click(within(dialog).getByRole('button', {name: 'Übernehmen und speichern'}));
            await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(2));
            expect(testState.save.mock.calls[1][0]).toMatchObject({...stored, name: 'Gesendet'});
            await act(async () => second.resolve({...stored, name: 'Gesendet', lockVersion: 3}));
        } else {
            fireEvent.click(within(dialog).getByRole('button', {name: 'Änderungen verwerfen'}));
        }
        await waitFor(() => expect(saveButton()).toBeEnabled());
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Später bearbeitet');
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            hasChanged: true, baseLockVersion: decision === 'retry' ? 3 : 2,
            edited: {name: 'Später bearbeitet', notes: 'Notiz vom Server'},
        });
        expect(testState.save).toHaveBeenCalledTimes(decision === 'retry' ? 2 : 1);
    });

    it('keeps all edits after a failed PUT and drops the in-flight checkpoint', async () => {
        const first = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(first.promise);
        renderEditor();
        await editName('Gesendet');
        fireEvent.click(saveButton());
        await editName('Noch offen');
        await act(async () => first.reject(new Error('Failed to save')));
        expect(saveButton()).toBeEnabled();
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            hasChanged: true, baseLockVersion: 1, edited: {name: 'Noch offen'}, saveCheckpoint: undefined,
        });
    });

    it('shows a rejected save without applying its field errors to newer edits', async () => {
        const first = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(first.promise);
        renderEditor();
        await editName('Gesendet');
        fireEvent.click(saveButton());
        await editName('Neu eingegeben');
        await act(async () => first.reject({
            status: 400, message: 'Konfiguration ungültig', displayableToUser: true,
            details: {effectiveValues: {}, elementStates: {}},
        }));
        expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Konfiguration ungültig'}),
        }));
        expect(saveButton()).toBeEnabled();
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Neu eingegeben');
    });

    it.each([
        {later: 'Element 1', committed: true, count: 2},
        {later: 'Gesendet', committed: true, count: 1},
        {later: 'Element 1', committed: false, count: 2},
    ])('reconciles a timed-out PUT with the stored version on the next explicit save ($later, committed: $committed)', async ({later, committed, count}) => {
        const first = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(first.promise).mockImplementation(async (node: ProcessNodeEntity) => node);
        renderEditor();
        await editName('Gesendet');
        fireEvent.click(saveButton());
        await editName(later);
        await act(async () => first.reject(timeout));
        expect(saveButton()).toBeEnabled();
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            hasChanged: true, edited: {name: later}, saveCheckpoint: {phase: 'pending', submitted: {name: 'Gesendet'}},
        });

        const stored = {...createNode(1), ...(committed ? {name: 'Gesendet', lockVersion: 2} : {lockVersion: 1})};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(stored);
        fireEvent.click(saveButton());
        await waitFor(() => expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            hasChanged: false, edited: {...stored, name: later}, saveCheckpoint: undefined,
        }));
        expect(testState.stored).toHaveBeenCalledWith(stored);
        expect(testState.save).toHaveBeenCalledTimes(count);
        if (count === 2) expect(testState.save.mock.calls[1][0]).toEqual({...stored, name: later});
    });

    it('keeps a reversion if a timed-out PUT commits after the reconciliation read', async () => {
        const base = {...createNode(1), lockVersion: 1};
        const first = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(first.promise).mockRejectedValueOnce(lockConflict)
            .mockImplementation(async (node: ProcessNodeEntity) => node);
        renderEditor();
        await editName('Gesendet');
        fireEvent.click(saveButton());
        await editName('Element 1');
        await act(async () => first.reject({status: 503, message: 'Verbindung unterbrochen', details: {reason: 'network_unreachable'}, displayableToUser: false}));
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValueOnce(base)
            .mockResolvedValueOnce({...base, name: 'Gesendet', lockVersion: 2});
        fireEvent.click(saveButton());
        await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(3));
        expect(testState.save.mock.calls[2][0]).toEqual({...base, name: 'Element 1', lockVersion: 2});
    });

    it('protects an interrupted edit until a third possible value can be saved against its own version', async () => {
        const base = {...createNode(1), lockVersion: 1};
        const guard = Promise.withResolvers<ProcessNodeEntity>();
        const actualSave = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockRejectedValueOnce(timeout)
            .mockReturnValueOnce(guard.promise).mockReturnValueOnce(actualSave.promise);
        renderEditor();
        await editName('Erster Versuch');
        fireEvent.click(saveButton());
        await waitFor(() => expect(saveButton()).toBeEnabled());
        await editName('Element 1');
        fireEvent.change(screen.getByRole('textbox', {name: 'Variablenpfad'}), {target: {value: 'person.titel'}});
        fireEvent.click(saveButton());
        await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(2));
        expect(testState.save.mock.calls[1][0]).toEqual(base);
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            saveCheckpoint: {phase: 'pending', submitted: {name: 'Erster Versuch'}},
            edited: {name: 'Element 1', configuration: {reference: {reference: {path: 'person.titel'}}}},
        });
        await editName('Noch später');
        await act(async () => guard.resolve({...base, lockVersion: 2}));
        expect(testState.save.mock.calls[2][0]).toMatchObject({
            lockVersion: 2, name: 'Element 1', configuration: {reference: {reference: {path: 'person.titel'}}},
        });
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            saveCheckpoint: {phase: 'pending', baseLockVersion: 2, attempt: {base: {lockVersion: 2}}},
            edited: {name: 'Noch später'},
        });
        await act(async () => actualSave.resolve({...testState.save.mock.calls[2][0], lockVersion: 3}));
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            base: {lockVersion: 3}, edited: {name: 'Noch später', lockVersion: 3}, saveCheckpoint: undefined,
        });
        expect(testState.dispatch).not.toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: expect.stringContaining('zusammengeführt')}),
        }));
    });

    it('drops the rejected PUT checkpoint when the 409 lookup times out', async () => {
        testState.save.mockRejectedValueOnce(lockConflict);
        renderEditor();
        await editName('Meine Änderung');
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockRejectedValueOnce(timeout);
        fireEvent.click(saveButton());
        await waitFor(() => expect(saveButton()).toBeEnabled());
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            hasChanged: true, edited: {name: 'Meine Änderung'}, saveCheckpoint: undefined,
        });
    });

    it('defers a structural decision for edits made after Save until the next explicit save', async () => {
        const first = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(first.promise).mockImplementation(async (node: ProcessNodeEntity) => node);
        renderEditor();
        await editName('Gesendet');
        fireEvent.click(saveButton());
        fireEvent.change(screen.getByRole('textbox', {name: 'Variablenpfad'}), {target: {value: 'person.titel'}});
        const stored = {...createNode(1), name: 'Gesendet', lockVersion: 2, configuration: {reference: {type: InputMode.Literal as const, value: 'Fest'}}};
        await act(async () => first.resolve(stored));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Variablenpfad'})).toHaveValue('person.titel');
        expect(testState.trackDraft.mock.lastCall?.[0].saveCheckpoint).toMatchObject({phase: 'confirmed'});

        fireEvent.click(saveButton());
        const dialog = await screen.findByRole('dialog', {name: 'Neuerer Stand gespeichert'});
        expect(testState.save).toHaveBeenCalledTimes(1);
        fireEvent.click(within(dialog).getByRole('radio', {name: 'Gespeicherter Stand soll übernommen werden'}));
        fireEvent.click(within(dialog).getByRole('button', {name: 'Übernehmen und speichern'}));
        await waitFor(() => expect(screen.queryByRole('textbox', {name: 'Variablenpfad'})).not.toBeInTheDocument());
        expect(testState.save).toHaveBeenCalledTimes(1);
        expect(saveButton()).toBeDisabled();
    });

    it('keeps the latest name and confirmed lock version when the initial derivation finishes after saving', async () => {
        const first = Promise.withResolvers<ProcessNodeEntity>();
        const derivation = Promise.withResolvers<DerivedRuntimeElementData>();
        vi.spyOn(ProcessNodeApiService.prototype, 'deriveConfiguration').mockReturnValue(derivation.promise);
        testState.save.mockReturnValueOnce(first.promise);
        renderEditor({realConfiguration: true});
        const input = await findReadyTextbox(/^Name des Elementes/);
        fireEvent.change(input, {target: {value: 'Gesendet'}});
        fireEvent.click(saveButton());
        fireEvent.change(input, {target: {value: 'Spätere Eingabe'}});
        await act(async () => first.resolve({...createNode(1), name: 'Gesendet', lockVersion: 2}));
        await act(async () => derivation.resolve({effectiveValues: {}, elementStates: {}}));
        expect(input).toHaveValue('Spätere Eingabe');
        expect(saveButton()).toBeEnabled();
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({
            base: {name: 'Gesendet', lockVersion: 2}, edited: {name: 'Spätere Eingabe', lockVersion: 2},
        });
    });

    it.each([false, true])('recovers after login before the PUT response (committed: %s)', async (committed) => {
        const base = {...createNode(1), lockVersion: 1};
        const submitted = {...base, name: 'Gesendet'};
        const stored = committed ? {...submitted, lockVersion: 2} : base;
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(stored);
        testState.loadDraft.mockResolvedValue({base, baseLockVersion: 1, mine: base, saveCheckpoint: {phase: 'pending', submitted}});
        renderEditor();
        const dialog = await screen.findByRole('dialog', {name: 'Ungespeicherte Änderungen wiederherstellen?'});
        fireEvent.click(within(dialog).getByRole('button', {name: 'Änderungen wiederherstellen'}));
        await waitFor(() => expect(saveButton()).toBeEnabled());
        expect(testState.trackDraft.mock.lastCall?.[0].saveCheckpoint).toEqual(committed ? undefined : {phase: 'pending', submitted});
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Element 1');
        expect(testState.save).not.toHaveBeenCalled();
    });

    it('never retries a discarded save after opening another node and saves the new node by its own ID', async () => {
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        const firstSave = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(firstSave.promise).mockImplementation(async (node: ProcessNodeEntity) => node);
        const router = renderEditor();
        await editName('Verworfene Änderung');
        fireEvent.click(saveButton());
        await discardAndOpenSecondNode(router);

        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(1), notes: 'Geänderte Notiz', lockVersion: 2});
        await act(async () => firstSave.reject(lockConflict));

        expect(testState.save).toHaveBeenCalledTimes(1);
        expect(router.state.location.pathname).toBe(nodeRoute(2));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Element 2');
        await editName('Änderung in Element 2');
        fireEvent.click(saveButton());
        await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(2));
        expect(testState.save.mock.calls[1][0]).toMatchObject({id: 2, name: 'Änderung in Element 2'});
    });

    it('does not send a save still waiting for authentication when the user discards it', async () => {
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        vi.spyOn(AuthService, 'isAccessTokenValid').mockReturnValue(false);
        const refresh = Promise.withResolvers<void>();
        const refreshAuth = vi.spyOn(AuthService, 'refresh').mockReturnValue(refresh.promise);
        const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(Response.json(createNode(1)));
        testState.save.mockImplementation((node, options) => new ProcessNodeApiService().update(node.id, node, options));
        const router = renderEditor();

        await editName('Verworfene Änderung');
        fireEvent.click(saveButton());
        await waitFor(() => expect(refreshAuth).toHaveBeenCalledOnce());
        await discardAndOpenSecondNode(router);
        await act(async () => refresh.resolve());

        expect(fetch).not.toHaveBeenCalled();
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Element 2');
    });

    it.each([
        {outcome: 'success', targetNode: 2},
        {outcome: 'error', targetNode: 2},
        {outcome: 'success', targetNode: 1},
    ])('ignores a stale $outcome for node 1 while another save is pending for node $targetNode', async ({outcome, targetNode}) => {
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        const oldSave = Promise.withResolvers<ProcessNodeEntity>();
        const newSave = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(oldSave.promise).mockReturnValueOnce(newSave.promise);
        const router = renderEditor();
        await editName('Erster Besuch');
        fireEvent.click(saveButton());
        await discardAndOpenSecondNode(router);
        if (targetNode === 1) {
            await act(() => router.navigate(nodeRoute(1)));
            await screen.findByDisplayValue('Element 1');
        }
        await editName('Zweiter Besuch');
        fireEvent.click(saveButton());
        await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(2));
        testState.dispatch.mockClear();

        await act(async () => {
            if (outcome === 'success') oldSave.resolve({...createNode(1), name: 'Erster Besuch'});
            else oldSave.reject(new Error('Late save error'));
        });

        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Zweiter Besuch');
        expect(saveButton()).toBeDisabled();
        expect(testState.dispatch).not.toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: expect.any(String)}),
        }));
        await act(async () => newSave.resolve({...createNode(targetNode), name: 'Zweiter Besuch'}));
    });

    it('cancels retries as soon as discarding starts and lets the user save again if draft deletion fails', async () => {
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        const firstSave = Promise.withResolvers<ProcessNodeEntity>();
        const deletion = Promise.withResolvers<void>();
        testState.save.mockReturnValueOnce(firstSave.promise).mockImplementation(async (node: ProcessNodeEntity) => node);
        testState.clearDraft.mockReturnValueOnce(deletion.promise);
        const router = renderEditor();
        await editName('Meine Änderung');
        fireEvent.click(saveButton());
        const dialog = await requestNodeChange(router, 2);
        fireEvent.click(within(dialog).getByRole('button', {name: 'Änderungen verwerfen'}));
        await act(async () => firstSave.reject(lockConflict));
        expect(testState.save).toHaveBeenCalledTimes(1);

        await act(async () => deletion.reject(new Error('Draft deletion failed')));
        expect(within(dialog).getByRole('alert')).toBeVisible();
        fireEvent.click(within(dialog).getByRole('button', {name: 'Abbrechen'}));
        expect(router.state.location.pathname).toBe(nodeRoute(1));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Meine Änderung');
        fireEvent.click(saveButton());
        await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(2));
        expect(testState.save.mock.calls[1][0]).toMatchObject({id: 1, name: 'Meine Änderung'});
    });

    it('keeps the current save valid after switching editor tabs and cancelling a node change', async () => {
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        const firstSave = Promise.withResolvers<ProcessNodeEntity>();
        testState.save.mockReturnValueOnce(firstSave.promise).mockImplementation(async (node: ProcessNodeEntity) => node);
        const router = renderEditor();
        await editName('Mein Name');
        fireEvent.click(saveButton());
        await userEvent.setup().click(screen.getByRole('tab', {name: 'Weiteres'}));
        const dialog = await requestNodeChange(router, 2);
        fireEvent.click(within(dialog).getByRole('button', {name: 'Abbrechen'}));

        const storedNode = {...createNode(1), notes: 'Neue Notiz', lockVersion: 2};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(storedNode);
        await act(async () => firstSave.reject(lockConflict));
        expect(testState.save).toHaveBeenCalledTimes(2);
        expect(testState.save.mock.calls[1][0]).toEqual({...storedNode, name: 'Mein Name'});
        expect(testState.clearDraft).not.toHaveBeenCalled();
    });

    it('closes an open save conflict when the node is reloaded', async () => {
        testState.save.mockRejectedValueOnce(lockConflict);
        renderEditor();
        await editName('Mein Name');
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(1), name: 'Neu geladen', lockVersion: 2});
        const reload = screen.getByRole('button', {name: 'Element neu laden'});
        fireEvent.click(saveButton());
        expect(await screen.findByRole('dialog', {name: 'Neuerer Stand gespeichert'})).toBeVisible();

        fireEvent.click(reload);
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Neu geladen');
        expect(testState.save).toHaveBeenCalledTimes(1);
    });

    it('ignores delayed validation of a saved node after opening a different node', async () => {
        const validation = Promise.withResolvers<ProcessNodeProblems>();
        testState.save.mockImplementation(async (node: ProcessNodeEntity) => node);
        const router = renderEditor();
        await editName('Gespeicherter Name');
        const validate = vi.mocked(ProcessNodeApiService.prototype.validate);
        validate.mockReturnValueOnce(validation.promise);
        fireEvent.click(saveButton());
        await waitFor(() => expect(validate).toHaveBeenCalledTimes(2));
        await act(() => router.navigate('/processes/10/versions/1/nodes/2/tabs/outputs'));
        await screen.findByDisplayValue('node2');

        await act(async () => validation.resolve({
            node: createNode(1),
            commonErrors: {dataKey: ['Fehler in Element 1']},
            derivedRuntimeElementData: {effectiveValues: {}, elementStates: {}},
            problems: [],
        }));

        expect(screen.getByRole('textbox', {name: /^Datenschlüssel/})).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.queryByText('Fehler in Element 1')).not.toBeInTheDocument();
    });

    it('allows editing and ignores validation that describes an earlier save of the same node', async () => {
        const validation = Promise.withResolvers<ProcessNodeProblems>();
        testState.save.mockImplementation(async (node: ProcessNodeEntity) => ({...node, lockVersion: 2}));
        renderEditor({tab: 'outputs'});
        const input = await findReadyTextbox(/^Datenschlüssel/);
        await userEvent.setup().click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        fireEvent.change(input, {target: {value: 'gesendet'}});
        vi.mocked(ProcessNodeApiService.prototype.validate).mockReturnValueOnce(validation.promise);
        fireEvent.click(saveButton());
        await waitFor(() => expect(input).toHaveAttribute('readonly'));
        await userEvent.setup().click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        fireEvent.change(input, {target: {value: 'neu'}});
        expect(saveButton()).toBeEnabled();
        await act(async () => validation.resolve({
            node: {...createNode(1), dataKey: 'gesendet'}, commonErrors: {dataKey: ['Fehler im alten Stand']},
            derivedRuntimeElementData: {effectiveValues: {}, elementStates: {}}, problems: [],
        }));
        expect(input).not.toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveValue('neu');
    });

    it('merges a save rejected by a newer stored state and saves again without asking', async () => {
        const storedNode = {...createNode(1), notes: 'Neue Notiz', lockVersion: 2};
        testState.save
            .mockRejectedValueOnce(lockConflict)
            .mockImplementation(async (node: ProcessNodeEntity) => node);
        renderEditor();
        await editName('Mein Name');
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(storedNode);

        fireEvent.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));

        await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(2));
        expect(testState.save.mock.calls[1][0]).toEqual({...storedNode, name: 'Mein Name'});
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        await waitFor(() => expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({
                message: 'Ihre Änderungen wurden mit dem zwischenzeitlich gespeicherten Stand zusammengeführt und gespeichert.',
            }),
        })));
    });

    it('asks which state applies where both changed the same value', async () => {
        const storedNode = {...createNode(1), name: 'Anderer Name', lockVersion: 2};
        testState.save
            .mockRejectedValueOnce(lockConflict)
            .mockImplementation(async (node: ProcessNodeEntity) => node);
        renderEditor();
        await editName('Mein Name');
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(storedNode);

        fireEvent.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));

        const dialog = await screen.findByRole('dialog', {name: 'Neuerer Stand gespeichert'});
        expect(within(dialog).getByRole('group', {name: 'Abweichende Einstellung: Name des Elementes'})).toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole('radio', {name: 'Gespeicherter Stand soll übernommen werden'}));
        fireEvent.click(within(dialog).getByRole('button', {name: 'Übernehmen und speichern'}));

        await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(2));
        expect(testState.save.mock.calls[1][0]).toEqual(storedNode);
    });

    describe.each([InputMode.Variable, InputMode.NoCode] as const)('%s element data references', (mode) => {
        function configuration(nodeDataKey: string, path: string): ProcessNodeEntity['configuration'] {
            return {reference: mode === InputMode.Variable
                ? {type: mode, reference: {source: InputVariableSource.ElementData, nodeDataKey, path}}
                : {type: mode, operand: {type: 'NoCodeNodeDataReference', nodeDataKey, path}}};
        }

        const base = {...createNode(1), configuration: configuration('review', 'email'), lockVersion: 1};
        const mine = {...base, configuration: configuration('review', 'name'), name: 'Mein Name'};
        const stored = {...base, configuration: configuration('manager', 'email'), notes: 'Notiz vom Server', lockVersion: 2};

        async function referenceChoices(dialogName: string) {
            const dialog = within(await screen.findByRole('dialog', {name: dialogName}));
            expect(dialog.getByRole('status')).toHaveTextContent('1 abweichende Angabe');
            expect(dialog.getAllByRole('radio')).toHaveLength(2);
            return {
                dialog,
                mine: dialog.getByRole('radio', {name: 'Meine Änderung soll behalten werden', description: /_\.review\.name/}),
                theirs: dialog.getByRole('radio', {name: 'Gespeicherter Stand soll übernommen werden', description: /_\.manager\.email/}),
            };
        }

        it.each(['mine', 'theirs'] as const)('waits for the full reference decision before retrying a 409 (%s)', async (choice) => {
            vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValueOnce(base).mockResolvedValue(stored);
            testState.save.mockRejectedValueOnce(lockConflict).mockImplementation(async (node: ProcessNodeEntity) => node);
            renderEditor();
            await editName(mine.name);
            fireEvent.change(screen.getByRole('textbox', {name: 'Variablenpfad'}), {target: {value: 'name'}});
            fireEvent.click(saveButton());

            const options = await referenceChoices('Neuerer Stand gespeichert');
            expect(testState.save.mock.calls.map(([node]) => node)).toEqual([mine]);
            fireEvent.click(options[choice]);
            fireEvent.click(options.dialog.getByRole('button', {name: 'Übernehmen und speichern'}));

            await waitFor(() => expect(testState.save).toHaveBeenCalledTimes(2));
            expect(testState.save.mock.calls[1][0]).toEqual({
                ...stored, name: mine.name, configuration: choice === 'mine' ? mine.configuration : stored.configuration,
            });
        });

        it.each(['draft', 'interrupted save'])('restores the chosen reference and independent changes after login (%s)', async (kind) => {
            vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(stored);
            testState.loadDraft.mockResolvedValue({
                base, mine, baseLockVersion: 1, saveCheckpoint: kind === 'draft' ? undefined :
                    {phase: 'pending', submitted: {...base, configuration: configuration('intake', 'email')}},
            });
            testState.save.mockImplementation(async (node: ProcessNodeEntity) => node);
            renderEditor();

            const options = await referenceChoices('Ungespeicherte Änderungen wiederherstellen?');
            fireEvent.click(options.mine);
            fireEvent.click(options.dialog.getByRole('button', {name: 'Änderungen wiederherstellen'}));
            await waitFor(() => expect(saveButton()).toBeEnabled());
            expect(testState.save).not.toHaveBeenCalled();
            expect(screen.getByRole('textbox', {name: 'Variablenpfad'})).toHaveValue('name');
            fireEvent.click(saveButton());

            await waitFor(() => expect(testState.save).toHaveBeenCalledOnce());
            expect(testState.save.mock.calls[0][0]).toEqual({...stored, configuration: mine.configuration, name: mine.name});
        });

        it('recognizes the interrupted write on the server and restores the later selection without a conflict', async () => {
            vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(stored);
            testState.loadDraft.mockResolvedValue({
                base, mine, baseLockVersion: 1,
                saveCheckpoint: {phase: 'pending', submitted: {...base, configuration: stored.configuration}},
            });
            testState.save.mockImplementation(async (node: ProcessNodeEntity) => node);
            renderEditor();

            const dialog = within(await screen.findByRole('dialog', {name: 'Ungespeicherte Änderungen wiederherstellen?'}));
            expect(dialog.queryByRole('radio')).not.toBeInTheDocument();
            fireEvent.click(dialog.getByRole('button', {name: 'Änderungen wiederherstellen'}));
            await waitFor(() => expect(saveButton()).toBeEnabled());
            expect(testState.save).not.toHaveBeenCalled();
            fireEvent.click(saveButton());
            await waitFor(() => expect(testState.save).toHaveBeenCalledOnce());
            expect(testState.save.mock.calls[0][0]).toEqual({...stored, configuration: mine.configuration, name: mine.name});
        });
    });

    it('keeps the changes unsaved when the conflict dialog is cancelled', async () => {
        testState.save.mockRejectedValue(lockConflict);
        renderEditor();
        await editName('Mein Name');
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(1), name: 'Anderer Name', lockVersion: 2});

        fireEvent.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));
        const dialog = await screen.findByRole('dialog', {name: 'Neuerer Stand gespeichert'});
        fireEvent.click(within(dialog).getByRole('button', {name: 'Abbrechen'}));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(testState.save).toHaveBeenCalledTimes(1);
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Mein Name');
    });

    it('reports a conflict that persists after several merged attempts instead of retrying forever', async () => {
        testState.save.mockRejectedValue(lockConflict);
        renderEditor();
        await editName('Mein Name');
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(1), notes: 'Neue Notiz', lockVersion: 2});

        fireEvent.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));

        await waitFor(() => expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: expect.stringContaining(lockConflict.message)}),
        })));
        expect(testState.save).toHaveBeenCalledTimes(3);
        expect(testState.backUpDraft).toHaveBeenCalledOnce();
    });

    it('offers to restore the stored changes after reloading when a persistent conflict leaves them backed up', async () => {
        testState.save.mockRejectedValue(lockConflict);
        testState.backUpDraft.mockResolvedValue(true);
        renderEditor();
        await editName('Mein Name');

        fireEvent.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));

        await waitFor(() => expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({
                message: expect.stringContaining('Ihre Änderungen sind in diesem Browser zwischengespeichert'),
                action: SnackbarAction.ReloadPage,
            }),
        })));
        expect(testState.dispatch).not.toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: expect.stringContaining(lockConflict.message)}),
        }));
    });

    it('keeps the server preview locked across tabs until the draft is restored', async () => {
        const node = {...createNode(1), lockVersion: 1};
        const draft = {base: node, baseLockVersion: 1, mine: {...node, name: 'Entwurf'}};
        const read = Promise.withResolvers<typeof draft>();
        testState.loadDraft.mockReturnValueOnce(read.promise);
        renderEditor();
        const input = await screen.findByRole('textbox', {name: 'Name bearbeiten'});
        expect(input).toHaveValue('Element 1');
        expect(input).toBeDisabled();
        expect(input.closest('[inert]')).not.toBeNull();
        // Nested editors can deliver a callback even when the browser blocks direct interaction.
        fireEvent.change(input, {target: {value: 'Verfrüht'}});
        fireEvent.click(saveButton());
        expect(testState.trackDraft.mock.lastCall?.[0]).toMatchObject({enabled: false, edited: {name: 'Element 1'}});
        expect(testState.save).not.toHaveBeenCalled();
        const user = userEvent.setup();
        await user.click(screen.getByRole('button', {name: ''}));
        expect(screen.getByRole('menuitem', {name: 'Ersetzen'})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('menuitem', {name: 'Löschen'})).toHaveAttribute('aria-disabled', 'true');
        await user.keyboard('{Escape}');
        await user.click(screen.getByRole('tab', {name: 'Weiteres'}));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toBeDisabled();
        expect(testState.loadDraft).toHaveBeenCalledTimes(1);
        await act(async () => read.resolve(draft));
        const dialog = await screen.findByRole('dialog', {name: 'Ungespeicherte Änderungen wiederherstellen?'});
        expect(testState.trackDraft.mock.lastCall?.[0].enabled).toBe(false);
        fireEvent.click(within(dialog).getByRole('button', {name: 'Änderungen wiederherstellen'}));
        await waitFor(() => expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Entwurf'));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toBeEnabled();
        expect(testState.trackDraft.mock.lastCall?.[0].enabled).toBe(true);
        expect(saveButton()).toBeEnabled();
        expect(testState.clearDraft).not.toHaveBeenCalled();
    });

    it('invalidates a pending lookup on same-node refresh before the fresh node is available', async () => {
        const base = {...createNode(1), lockVersion: 1};
        const draft = {base, baseLockVersion: 1, mine: {...base, name: 'Alter Entwurf'}};
        const read = Promise.withResolvers<typeof draft>();
        const fetchNode = Promise.withResolvers<ProcessNodeEntity>();
        testState.loadDraft.mockReturnValueOnce(read.promise);
        renderEditor();
        await waitFor(() => expect(testState.loadDraft).toHaveBeenCalledOnce());
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockReturnValueOnce(fetchNode.promise);
        fireEvent.click(screen.getByRole('button', {name: 'Element neu laden'}));
        await act(async () => read.resolve(draft));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toBeDisabled();
        expect(testState.clearDraft).not.toHaveBeenCalled();
        await act(async () => fetchNode.resolve({...base, name: 'Neu geladen', lockVersion: 2}));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Neu geladen');
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toBeEnabled();
        expect(testState.loadDraft).toHaveBeenCalledTimes(2);
    });

    it.each(['delete', 'unlock'])('ignores a stale %s confirmation after refreshing the same node', async (action) => {
        const confirmation = Promise.withResolvers<boolean>();
        testState.confirm.mockReturnValueOnce(confirmation.promise);
        renderEditor({tab: 'outputs'});
        await findReadyTextbox(/^Datenschlüssel/);
        const user = userEvent.setup();
        if (action === 'delete') {
            await user.click(screen.getByRole('button', {name: ''}));
            await user.click(screen.getByRole('menuitem', {name: 'Löschen'}));
        } else {
            await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        }
        expect(testState.confirm).toHaveBeenCalledOnce();
        fireEvent.click(screen.getByRole('button', {name: 'Element neu laden'}));
        await waitFor(() => expect(testState.loadDraft).toHaveBeenCalledTimes(2));
        const field = await findReadyTextbox(/^Datenschlüssel/);
        await act(async () => confirmation.resolve(true));
        expect(testState.remove).not.toHaveBeenCalled();
        expect(field).toHaveAttribute('readonly');
    });

    it('requires a new decision when the account changes on the same route', async () => {
        const read = Promise.withResolvers<null>();
        const router = renderEditor();
        await editName('Änderung des ersten Kontos');
        testState.trackDraft.mockClear();
        testState.userId = 'second-user';
        testState.loadDraft.mockReturnValueOnce(read.promise);
        await act(() => router.navigate(nodeRoute(1) + '?account=second'));
        await waitFor(() => expect(testState.loadDraft).toHaveBeenCalledTimes(2));
        const input = screen.getByRole('textbox', {name: 'Name bearbeiten'});
        expect(input).toBeDisabled();
        expect(input).toHaveValue('Element 1');
        expect(testState.trackDraft.mock.calls.every(([props]) => !props.enabled)).toBe(true);
        await act(async () => read.resolve(null));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toBeEnabled();
        expect(saveButton()).toBeDisabled();
    });

    it('starts a new check when editing becomes available again for the same node', async () => {
        renderEditor();
        await findReadyTextbox('Name bearbeiten');
        const toggle = screen.getByRole('button', {name: 'Bearbeitungsrecht wechseln'});
        fireEvent.click(toggle);
        await waitFor(() => expect(testState.trackDraft.mock.lastCall?.[0].draftKey).toBeNull());
        const read = Promise.withResolvers<null>();
        testState.loadDraft.mockReturnValueOnce(read.promise);
        fireEvent.click(toggle);
        await waitFor(() => expect(testState.loadDraft).toHaveBeenCalledTimes(2));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toBeDisabled();
        expect(testState.trackDraft.mock.lastCall?.[0].enabled).toBe(false);
        await act(async () => read.resolve(null));
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toBeEnabled();
    });

    it('releases the server preview if draft storage is unavailable', async () => {
        testState.loadDraft.mockRejectedValueOnce(new Error('Storage unavailable'));
        renderEditor();
        await editName('Eingabe nach Speicherfehler');
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Eingabe nach Speicherfehler');
        expect(saveButton()).toBeEnabled();
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it.each([false, true])('expires recovery when the route changes (dialog already open: %s)', async (shown) => {
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockImplementation(async (id) => createNode(id));
        const node = createNode(1);
        const oldDraft = {base: node, baseLockVersion: null, mine: {...node, name: 'Entwurf aus Element 1'}};
        const read = Promise.withResolvers<typeof oldDraft>();
        testState.loadDraft.mockReturnValueOnce(read.promise);
        const router = renderEditor();
        await waitFor(() => expect(testState.loadDraft).toHaveBeenCalledTimes(1));
        if (shown) {
            await act(async () => read.resolve(oldDraft));
            expect(await screen.findByRole('dialog', {name: 'Ungespeicherte Änderungen wiederherstellen?'})).toBeInTheDocument();
        }

        await act(async () => { await router.navigate('/processes/10/versions/1/nodes/2/tabs/configuration'); });
        await waitFor(() => expect(testState.loadDraft).toHaveBeenCalledTimes(2));
        if (!shown) {
            await act(async () => read.resolve(oldDraft));
        }
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toHaveValue('Element 2');
        expect(testState.clearDraft).not.toHaveBeenCalled();
    });

    it('unlocks the server state only after the discarded draft has been removed', async () => {
        const node = {...createNode(1), lockVersion: 1};
        const deletion = Promise.withResolvers<void>();
        testState.clearDraft.mockReturnValueOnce(deletion.promise);
        testState.loadDraft.mockResolvedValue({
            userId: 'user', key: 'process-node/1/configuration', savedAt: Date.now(),
            base: node, baseLockVersion: 1, mine: {...node, name: 'Entwurf'},
        });
        renderEditor();

        const dialog = await screen.findByRole('dialog', {name: 'Ungespeicherte Änderungen wiederherstellen?'});
        fireEvent.click(within(dialog).getByRole('button', {name: 'Änderungen verwerfen'}));

        await waitFor(() => expect(testState.clearDraft).toHaveBeenCalled());
        const preview = screen.getByRole('textbox', {name: 'Name bearbeiten'});
        expect(preview).toHaveValue('Element 1');
        expect(preview).toBeDisabled();
        expect(testState.trackDraft.mock.lastCall?.[0].enabled).toBe(false);
        await act(async () => deletion.resolve());
        expect(screen.getByRole('textbox', {name: 'Name bearbeiten'})).toBeEnabled();
    });

    it('removes a draft without asking when its changes were saved elsewhere', async () => {
        const node = {...createNode(1), name: 'Entwurf', lockVersion: 2};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(node);
        testState.loadDraft.mockResolvedValue({
            userId: 'user', key: 'process-node/1/configuration', savedAt: Date.now(),
            base: {...node, name: 'Element 1', lockVersion: 1}, baseLockVersion: 1, mine: {...node, lockVersion: 1},
        });
        renderEditor();

        await waitFor(() => expect(testState.clearDraft).toHaveBeenCalled());
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
});
