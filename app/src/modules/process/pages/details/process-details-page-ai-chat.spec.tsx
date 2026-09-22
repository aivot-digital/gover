import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {useEffect, type PropsWithChildren} from 'react';
import {createMemoryRouter, RouterProvider} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProcessDetailsPage, type ProcessFlow} from './process-details-page';
import {useProcessDetailsPageContext} from './process-details-page-context';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
import {ProcessDefinitionEdgeApiService} from '../../services/process-definition-edge-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {ProcessNodeProviderApiService, ProcessNodeType} from '../../services/process-node-provider-api-service';
import {ProcessTestClaimApiService} from '../../services/process-test-claim-api-service';
import {SearchItemService} from '../../../search/search-item-service';
import {AiChatService} from '../../../ai/services/ai-chat-service';
import {Permission} from '../../../../data/permissions/permission';
import {ProcessStatus} from '../../enums/process-status';

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(), noop: vi.fn(), user: {id: 'user'}, permissions: new Set<string>(),
    save: vi.fn(async () => {}), refresh: vi.fn(async () => {}),
}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../../hooks/use-app-selector', () => ({useAppSelector: () => mocks.user}));
vi.mock('../../../../providers/confirm-provider', () => ({useConfirm: () => mocks.noop}));
vi.mock('../../../../hooks/use-not-implemented', () => ({useNotImplemented: () => mocks.noop}));
vi.mock('../../../../hooks/use-process-export', () => ({useProcessExport: () => mocks.noop}));
vi.mock('../../hooks/use-delete-process', () => ({useDeleteProcess: () => mocks.noop}));
vi.mock('../../hooks/use-revoke-process-version', () => ({useRevokeProcessVersion: () => mocks.noop}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useRefreshPermissionSet: () => mocks.noop,
    useHasSystemPermission: (key: string) => mocks.permissions.has(key),
    useHasProcessPermission: (id: number, key: string) => id === 42 && mocks.permissions.has(key),
}));
vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({PageWrapper: ({children}: PropsWithChildren) => children}));
vi.mock('allotment', () => ({Allotment: Object.assign(({children}: PropsWithChildren) => <div>{children}</div>, {
    Pane: ({children}: PropsWithChildren) => <div>{children}</div>,
})}));
vi.mock('./components/process-flow-editor/process-flow-editor', () => ({
    ProcessFlowEditor: ({processFlow, editable}: {processFlow: ProcessFlow; editable: boolean}) =>
        <div><output aria-label="Prozessgraph">{processFlow.nodes.map(node => node.name).join(',')}</output>
            <button disabled={!editable}>Knoten anlegen</button></div>,
}));
vi.mock('../../dialogs/select-node-provider-dialog', () => ({SelectNodeProviderDialog: () => null}));
vi.mock('../../dialogs/process-instance-event-dialog', () => ({ProcessInstanceEventDialog: () => null}));
vi.mock('./components/process-connect-existing-node-dialog', () => ({ProcessConnectExistingNodeDialog: () => null}));
vi.mock('../../dialogs/process-settings-dialog/process-settings-dialog', () => ({ProcessSettingsDialog: () => null}));
vi.mock('../../dialogs/process-test-claim-process-instances-dialog', () => ({ProcessTestClaimProcessInstancesDialog: () => null}));
vi.mock('../../dialogs/process-versions-dialog', () => ({ProcessVersionsDialog: () => null}));
vi.mock('../../dialogs/process-publish-dialog', () => ({ProcessPublishDialog: () => null}));
vi.mock('./components/process-notes-overview-dialog', () => ({ProcessNotesOverviewDialog: () => null}));
vi.mock('./components/process-details-page-more-menu', () => ({ProcessDetailsPageMoreMenu: () => null}));

function SelectedNode() {
    const {registerChatEditor} = useProcessDetailsPageContext();
    useEffect(() => registerChatEditor({nodeId: 7, save: mocks.save, refresh: mocks.refresh}), [registerChatEditor]);
    return <span>Ausgewählter Knoten</span>;
}

describe('process editor AI chat integration', () => {
    const node = {...ProcessNodeApiService.initialize(), id: 7, name: 'Alter Knoten', processId: 42,
        processVersion: 2, processNodeDefinitionKey: 'action', processNodeDefinitionVersion: 1};
    beforeEach(() => {
        mocks.permissions = new Set([Permission.AI_CHAT_USE, Permission.PROCESS_DEFINITION_READ, Permission.PROCESS_DEFINITION_UPDATE]);
        vi.spyOn(ProcessDefinitionApiService.prototype, 'retrieve').mockResolvedValue({id: 42, internalTitle: 'Testprozess'} as never);
        vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'retrieve').mockResolvedValue({processVersion: 2, status: ProcessStatus.Drafted} as never);
        vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'validate').mockResolvedValue({nodeProblems: []} as never);
        vi.spyOn(ProcessNodeApiService.prototype, 'listAll').mockResolvedValue({content: [node]} as never);
        vi.spyOn(ProcessDefinitionEdgeApiService.prototype, 'listAll').mockResolvedValue({content: []} as never);
        vi.spyOn(ProcessTestClaimApiService.prototype, 'listAll').mockResolvedValue({content: []} as never);
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProviders').mockResolvedValue([]);
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProvider').mockResolvedValue({key: 'action', majorVersion: 1, type: ProcessNodeType.Action, name: 'Aktion', ports: []} as never);
        vi.spyOn(SearchItemService.prototype, 'recordRecentSearchItem').mockResolvedValue(undefined as never);
        vi.spyOn(AiChatService.prototype, 'startChatSession').mockResolvedValue({sessionId: 'chat'});
        vi.spyOn(AiChatService.prototype, 'sendMessage').mockImplementation(
            async (_id, _text, chunk, _signal, _data, onAccepted) => {
                onAccepted?.();
                chunk('Geändert.');
            },
        );
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    async function mount() {
        const router = createMemoryRouter([{
            path: '/processes/:processId/versions/:processVersion', element: <ProcessDetailsPage/>,
            children: [{path: 'nodes/:nodeId', element: <SelectedNode/>}],
        }], {initialEntries: ['/processes/42/versions/2/nodes/7']});
        render(<RouterProvider router={router}/>);
        await screen.findByText('Ausgewählter Knoten');
        return router;
    }

    async function send() {
        fireEvent.click(screen.getByRole('button', {name: 'KI-Chat'}));
        fireEvent.change(screen.getByRole('textbox', {name: 'Nachricht'}), {target: {value: 'Ändern'}});
        fireEvent.click(screen.getByRole('button', {name: 'Absenden'}));
        await waitFor(() => expect(AiChatService.prototype.sendMessage).toHaveBeenCalledTimes(1));
    }

    it.each([Permission.AI_CHAT_USE, Permission.PROCESS_DEFINITION_READ, Permission.PROCESS_DEFINITION_UPDATE])(
        'does not offer the chat without %s', async permission => {
            mocks.permissions.delete(permission);
            await mount();
            expect(screen.queryByRole('button', {name: 'KI-Chat'})).not.toBeInTheDocument();
        },
    );

    it('does not offer editing of a published version', async () => {
        vi.mocked(ProcessDefinitionVersionApiService.prototype.retrieve).mockResolvedValue({processVersion: 2, status: ProcessStatus.Published} as never);
        await mount();
        expect(screen.getByRole('button', {name: 'KI-Chat'})).toBeDisabled();
    });

    it('disables the chat while a process test is active', async () => {
        vi.mocked(ProcessTestClaimApiService.prototype.listAll).mockResolvedValue({
            content: [{id: 9, owningUserId: 'user', processId: 42, processVersion: 2}],
        } as never);
        await mount();
        expect(screen.getByRole('button', {name: 'KI-Chat'})).toBeDisabled();
    });

    it('locks structure changes, publication and chat closing until the turn finishes', async () => {
        let finish!: () => void;
        vi.mocked(AiChatService.prototype.sendMessage).mockReturnValue(new Promise(resolve => {finish = resolve;}));
        await mount();
        await send();
        expect(screen.getByRole('button', {name: 'Knoten anlegen'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Veröffentlichen'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'KI-Chat schließen'})).toBeDisabled();
        await act(async () => finish());
        await waitFor(() => expect(screen.getByRole('button', {name: 'Knoten anlegen'})).toBeEnabled());
        fireEvent.click(screen.getByRole('button', {name: 'KI-Chat schließen'}));
        expect(screen.queryByRole('textbox', {name: 'Nachricht'})).not.toBeInTheDocument();
    });

    it('reloads graph and selected configuration after a turn while preserving selection', async () => {
        const router = await mount();
        vi.mocked(ProcessNodeApiService.prototype.listAll).mockResolvedValue({content: [{...node, name: 'Neuer Name'}]} as never);
        await send();
        await waitFor(() => expect(screen.getByRole('status', {name: 'Prozessgraph'})).toHaveTextContent('Neuer Name'));
        expect(mocks.save).toHaveBeenCalledTimes(1);
        expect(mocks.refresh).toHaveBeenCalledTimes(1);
        expect(ProcessDefinitionEdgeApiService.prototype.listAll).toHaveBeenCalledTimes(2);
        expect(router.state.location.pathname).toBe('/processes/42/versions/2/nodes/7');
    });

    it('returns to the overview when the selected node was deleted', async () => {
        const router = await mount();
        vi.mocked(ProcessNodeApiService.prototype.listAll).mockResolvedValue({content: []} as never);
        await send();
        await waitFor(() => expect(router.state.location.pathname).toBe('/processes/42/versions/2'));
        expect(mocks.refresh).not.toHaveBeenCalled();
        await waitFor(() => expect(screen.queryByText('Ausgewählter Knoten')).not.toBeInTheDocument());
    });

    it('ignores an old reload after navigating to a different process version', async () => {
        const router = await mount();
        let resolve!: (value: unknown) => void;
        vi.mocked(ProcessNodeApiService.prototype.listAll).mockReturnValueOnce(new Promise(yes => {resolve = yes as typeof resolve;}));
        await send();
        await waitFor(() => expect(ProcessNodeApiService.prototype.listAll).toHaveBeenCalledTimes(2));
        vi.mocked(ProcessDefinitionVersionApiService.prototype.retrieve).mockResolvedValue({processVersion: 3, status: ProcessStatus.Drafted} as never);
        await act(async () => { await router.navigate('/processes/42/versions/3'); });
        await act(async () => { resolve({content: []}); });
        expect(router.state.location.pathname).toBe('/processes/42/versions/3');
        expect(mocks.refresh).not.toHaveBeenCalled();
    });
});
