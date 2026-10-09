import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {createMemoryRouter, RouterProvider} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProcessNodeEditor} from './process-node-editor';
import {ProcessDetailsPageProvider, type ProcessDetailsPageContextType} from '../../process-details-page-context';
import {useProcessNodeEditorContext} from './process-node-editor-context';
import {ProcessNodeApiService} from '../../../../services/process-node-api-service';
import {ProcessNodeProviderApiService, ProcessNodeType} from '../../../../services/process-node-provider-api-service';
import {ElementType} from '../../../../../../data/element-type/element-type';
import {type ProcessChatEditor} from '../../hooks/use-process-ai-chat';

const mocks = vi.hoisted(() => ({dispatch: vi.fn(), confirm: vi.fn()}));
vi.mock('../../../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../../../../providers/confirm-provider', () => ({useConfirm: () => mocks.confirm}));
vi.mock('../../../../../../hooks/use-change-blocker-2', () => ({
    useChangeBlocker: ({original, edited}: {original: unknown; edited: unknown}) => ({
        hasChanged: JSON.stringify(original) !== JSON.stringify(edited), dialog: null,
    }),
}));
vi.mock('./components/process-node-editor-menu', () => ({ProcessNodeEditorMenu: () => null}));
vi.mock('../../../../components/process-node-provider-details', () => ({
    getProcessNodeProviderIcon: () => () => null, ProcessNodeProviderDetailsDialog: () => null,
}));

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
