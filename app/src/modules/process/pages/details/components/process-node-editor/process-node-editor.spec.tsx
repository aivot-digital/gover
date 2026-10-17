import {useState} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, type MockInstance, vi} from 'vitest';
import {createMemoryRouter, RouterProvider} from 'react-router-dom';
import {ProcessNodeEditor} from './process-node-editor';
import {useProcessNodeEditorContext} from './process-node-editor-context';
import {ProcessDetailsPageProvider, type ProcessDetailsPageContextType} from '../../process-details-page-context';
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
import {type ProcessChatEditor} from '../../hooks/use-process-ai-chat';

const testState = vi.hoisted(() => ({dispatch: vi.fn(), save: vi.fn(), confirm: vi.fn()}));

vi.mock('../../../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => testState.dispatch}));
vi.mock('../../../../../../providers/confirm-provider', () => ({useConfirm: () => testState.confirm}));
vi.mock('../../../../../../hooks/use-change-blocker-2', () => ({
    useChangeBlocker: ({original, edited}: {original: unknown; edited: unknown}) => ({
        hasChanged: JSON.stringify(original) !== JSON.stringify(edited), dialog: null,
    }),
}));
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
        registerChatEditor: () => () => {},
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
        path: '/processes/10/versions/1/nodes/:nodeId',
        element: <EditorPage editable={editable}/>,
        children: [
            {path: 'tabs/configuration', element: <EditorFields/>},
            {path: 'tabs/more', element: <EditorFields/>},
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

function Configuration() {
    const {node, setNode, isEditable} = useProcessNodeEditorContext();
    return <><output aria-label="Knotenname">{node.name}</output>
        <button disabled={!isEditable} onClick={() => setNode({...node, name: 'Geändert'}, false)}>Name ändern</button>
    </>;
}

describe('node editor chat bridge', () => {
    let bridge: ProcessChatEditor | null;
    let context: ProcessDetailsPageContextType;
    const node = {...ProcessNodeApiService.initialize(), id: 7, processId: 42, processVersion: 2,
        name: 'Ursprünglich', processNodeDefinitionKey: 'action', processNodeDefinitionVersion: 1};

    beforeEach(() => {
        bridge = null;
        Element.prototype.scrollTo = vi.fn();
        vi.spyOn(ProcessNodeApiService.prototype, 'retrieve').mockResolvedValue(node);
        vi.spyOn(ProcessNodeApiService.prototype, 'getConfigurationLayout').mockResolvedValue({
            type: ElementType.GroupLayout, id: 'root', children: [
                {type: ElementType.UiDefinitionInput, id: 'form', openExternalEditor: true},
                {type: ElementType.Text, id: 'text'},
            ],
        } as never);
        vi.spyOn(ProcessNodeApiService.prototype, 'validate').mockResolvedValue({node, problems: [], commonErrors: {}} as never);
        vi.spyOn(ProcessNodeApiService.prototype, 'getIncomingMetadata').mockResolvedValue({} as never);
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProvider').mockResolvedValue({
            key: 'action', majorVersion: 1, name: 'Aktion', type: ProcessNodeType.Action,
            ports: [], outputs: [], executionTypes: [],
        } as never);
        context = {
            editable: true, structureEditable: true, testClaim: null, nodeProblems: [], showNodeProblemsForNodes: {},
            nodeRefreshSignal: {nodeId: null, version: 0}, onSave: vi.fn(async node => node),
            onDelete: vi.fn(), onStartReplaceNode: vi.fn(),
            registerChatEditor: editor => {
                bridge = editor;
                return () => { if (bridge === editor) bridge = null; };
            },
        };
    });

    async function mount() {
        const router = createMemoryRouter([{
            path: '/processes/:processId/versions/:processVersion/nodes/:nodeId',
            element: <ProcessDetailsPageProvider value={context}><ProcessNodeEditor/></ProcessDetailsPageProvider>,
            children: [{index: true, element: <Configuration/>}],
        }], {initialEntries: ['/processes/42/versions/2/nodes/7']});
        const view = render(<RouterProvider router={router}/>);
        await screen.findByRole('button', {name: 'Name ändern'});
        return view;
    }

    it('saves dirty edits with external form fields omitted and avoids saving a clean node', async () => {
        await mount();
        await act(async () => bridge!.save());
        expect(context.onSave).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: 'Name ändern'}));
        await act(async () => bridge!.save());
        expect(context.onSave).toHaveBeenCalledExactlyOnceWith(
            expect.objectContaining({id: 7, name: 'Geändert'}), {query: {omitConfigSave: ['form']}},
        );
        expect(screen.getByRole('button', {name: 'Konfiguration speichern'})).toBeDisabled();
    });

    it('shares an in-flight manual save with the chat instead of submitting it twice', async () => {
        let resolve!: (value: typeof node) => void;
        vi.mocked(context.onSave).mockReturnValue(new Promise(yes => { resolve = yes; }));
        await mount();
        fireEvent.click(screen.getByRole('button', {name: 'Name ändern'}));
        fireEvent.click(screen.getByRole('button', {name: 'Konfiguration speichern'}));
        let pending!: Promise<void>;
        act(() => { pending = bridge!.save(); });
        expect(context.onSave).toHaveBeenCalledTimes(1);
        await act(async () => { resolve({...node, name: 'Geändert'}); await pending; });
    });

    it('rejects on save failure and retains dirty changes', async () => {
        vi.mocked(context.onSave).mockRejectedValueOnce(new Error('Save failed'));
        await mount();
        fireEvent.click(screen.getByRole('button', {name: 'Name ändern'}));
        await act(async () => { await expect(bridge!.save()).rejects.toThrow('Save failed'); });
        expect(screen.getByRole('status', {name: 'Knotenname'})).toHaveTextContent('Geändert');
        expect(screen.getByRole('button', {name: 'Konfiguration speichern'})).toBeEnabled();
    });

    it('refreshes node, layout, validation and metadata and unregisters on unmount', async () => {
        const view = await mount();
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValueOnce({...node, name: 'Von KI geändert'});
        await act(async () => bridge!.refresh());
        expect(screen.getByRole('status', {name: 'Knotenname'})).toHaveTextContent('Von KI geändert');
        expect(ProcessNodeApiService.prototype.getConfigurationLayout).toHaveBeenCalledTimes(2);
        expect(ProcessNodeApiService.prototype.getIncomingMetadata).toHaveBeenCalledTimes(2);
        expect(ProcessNodeApiService.prototype.validate).toHaveBeenCalledTimes(2);
        view.unmount();
        await waitFor(() => expect(bridge).toBeNull());
    });
});
