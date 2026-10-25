import {AuthService} from '../../../../../../services/auth-service';
import {ProcessNodeEditorOutputsTab} from './tabs/process-node-editor-outputs-tab';
import userEvent from '@testing-library/user-event';
import {useState} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
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
import {type ProcessNodeDefinitionMetadata} from '../../../../entities/process-node-definition-metadata';
import {ProcessDataKeyInputComponent} from '../../../../../../views/process-data-key-input-field-view';
import {generateElementWithDefaultValues} from '../../../../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../../../../data/element-type/element-type';
import {InputMode, InputVariableSource} from '../../../../../../models/input-mode';

const testState = vi.hoisted(() => ({dispatch: vi.fn(), save: vi.fn(), confirm: vi.fn()}));

vi.mock('../../../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => testState.dispatch}));
vi.mock('../../../../../../providers/confirm-provider', () => ({useConfirm: () => testState.confirm}));
vi.mock('./components/process-node-editor-menu', () => ({ProcessNodeEditorMenu: () => null}));
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
    const {node, setNode} = useProcessNodeEditorContext();
    return <>
        <label>
            Name bearbeiten
            <input value={node.name ?? ''} onChange={(event) => setNode({...node, name: event.target.value}, false)}/>
        </label>
        <ProcessDataKeyInputComponent
            label="Gespeicherter Pfad"
            scopeProcessDataKey="people"
            value={node.outputMappings.result}
            onChange={(value) => setNode({...node, outputMappings: {...node.outputMappings, result: value}}, false)}
        />
    </>;
}

function EditorPage({editable}: {editable: boolean}) {
    const [version, setVersion] = useState(0);
    return <ProcessDetailsPageProvider value={{
        editable, structureEditable: editable, onSave: testState.save,
        onDelete: async () => {}, onStartReplaceNode: () => {}, testClaim: null,
        nodeRefreshSignal: {nodeId: 1, version}, nodeProblems: [], showNodeProblemsForNodes: {},
    }}>
        <button onClick={() => setVersion((value) => value + 1)}>Element neu laden</button>
        <ProcessNodeEditor/>
    </ProcessDetailsPageProvider>;
}

function renderEditor({editable = true, tab = 'configuration'}: {editable?: boolean; tab?: string} = {}) {
    const router = createMemoryRouter([{
        path: '/processes/:processId/versions/:processVersion/nodes/:nodeId',
        element: <EditorPage editable={editable}/>,
        children: [
            {path: 'tabs/configuration', element: <EditorFields/>},
            {path: 'tabs/more', element: <EditorFields/>},
            {path: 'tabs/outputs', element: <ProcessNodeEditorOutputsTab/>},
        ],
    }], {initialEntries: [`/processes/10/versions/1/nodes/1/tabs/${tab}`]});
    render(<RouterProvider router={router}/>);
    return router;
}

describe('ProcessNodeEditor metadata loading', () => {
    let logError: MockInstance<typeof console.error>;

    beforeEach(() => {
        logError = vi.spyOn(console, 'error').mockImplementation(() => {});
        // jsdom does not implement element scrolling, which the editor uses on navigation.
        Object.defineProperty(HTMLElement.prototype, 'scrollTo', {value: vi.fn(), configurable: true, writable: true});
        testState.confirm.mockResolvedValue(true);
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

        expect(await screen.findByRole('alert')).toHaveTextContent(`${warning} Sie können die Konfiguration weiterhin bearbeiten.`);
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

    it('preserves the unlocked state across tabs and failed saves, and relocks after a successful save', async () => {
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        const user = userEvent.setup();
        renderEditor({tab: 'outputs'});
        const input = await screen.findByRole('textbox', {name: /^Datenschlüssel/});
        expect(input).toHaveAttribute('readonly');
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        fireEvent.change(input, {target: {value: 'renamed_node'}});
        await user.click(screen.getByRole('tab', {name: 'Weiteres'}));
        await user.click(screen.getByRole('tab', {name: 'Datenausgänge'}));
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
        const input = await screen.findByRole('textbox', {name: /^Datenschlüssel/});
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
        await user.click(screen.getByRole('tab', {name: 'Datenausgänge'}));
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
        const input = await screen.findByRole('textbox', {name: /^Datenschlüssel/});
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel entsperren'}));
        fireEvent.change(input, {target: {value: 'renamed_node'}});
        await user.click(screen.getByRole('button', {name: 'Datenschlüssel sperren'}));
        expect(input).toHaveAttribute('readonly');
        expect(testState.save).not.toHaveBeenCalled();
        await user.click(screen.getByRole('tab', {name: 'Weiteres'}));
        await user.click(screen.getByRole('tab', {name: 'Datenausgänge'}));
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

    it('keeps the shown node and its suggestions when the next node cannot be loaded', async () => {
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
