import {useState} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
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
    parentPluginKey: 'test', ports: [], outputs: [],
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

function EditorPage() {
    const [version, setVersion] = useState(0);
    return <ProcessDetailsPageProvider value={{
        editable: true, structureEditable: true, onSave: testState.save,
        onDelete: async () => {}, onStartReplaceNode: () => {}, testClaim: null,
        nodeRefreshSignal: {nodeId: 1, version}, nodeProblems: [], showNodeProblemsForNodes: {},
    }}>
        <button onClick={() => setVersion((value) => value + 1)}>Element neu laden</button>
        <ProcessNodeEditor/>
    </ProcessDetailsPageProvider>;
}

function renderEditor() {
    const router = createMemoryRouter([{
        path: '/processes/10/versions/1/nodes/:nodeId',
        element: <EditorPage/>,
        children: [{path: 'tabs/configuration', element: <EditorFields/>}],
    }], {initialEntries: ['/processes/10/versions/1/nodes/1/tabs/configuration']});
    render(<RouterProvider router={router}/>);
    return router;
}

describe('ProcessNodeEditor metadata loading', () => {
    beforeEach(() => {
        HTMLElement.prototype.scrollTo = vi.fn();
        testState.save.mockImplementation(async (node: ProcessNodeEntity) => node);
        vi.spyOn(ProcessNodeApiService.prototype, 'retrieve').mockImplementation(async (id) => createNode(id));
        vi.spyOn(ProcessNodeApiService.prototype, 'getConfigurationLayout')
            .mockResolvedValue(generateElementWithDefaultValues(ElementType.GroupLayout));
        vi.spyOn(ProcessNodeApiService.prototype, 'validate').mockResolvedValue(null);
        vi.spyOn(ProcessNodeApiService.prototype, 'getIncomingMetadata').mockResolvedValue(createMetadata());
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProvider').mockResolvedValue(provider);
    });

    it('opens, edits and saves after a metadata failure without losing saved paths or variable references', async () => {
        vi.mocked(ProcessNodeApiService.prototype.getIncomingMetadata).mockRejectedValue(new Error('Metadata failed'));
        renderEditor();

        expect(await screen.findByRole('alert')).toHaveTextContent(warning);
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
        const logError = vi.spyOn(console, 'error').mockImplementation(() => {});
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
