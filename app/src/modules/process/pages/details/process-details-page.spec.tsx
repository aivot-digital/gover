import {type PropsWithChildren} from 'react';
import {act, render, screen, waitFor} from '@testing-library/react';
import {createMemoryRouter, RouterProvider, useRouteError} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {type ApiError} from '../../../../models/api-error';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {type ProcessDefinitionEdgeEntity} from '../../entities/process-definition-edge-entity';
import {StaffShellError} from '../../../../shells/staff/staff-shell-error';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {ProcessDefinitionEdgeApiService} from '../../services/process-definition-edge-api-service';
import {ProcessNodeProviderApiService} from '../../services/process-node-provider-api-service';
import {ProcessTestClaimApiService} from '../../services/process-test-claim-api-service';
import {SearchItemService} from '../../../search/search-item-service';
import {ProcessDetailsPage} from './process-details-page';

const mocks = vi.hoisted(() => ({
    onDeleteNode: undefined as ((node: ProcessNodeEntity) => Promise<void>) | undefined,
    dispatch: vi.fn(),
    setClaim: vi.fn(),
    refreshPermissions: vi.fn(),
    runtimeData: {
        runtimeData: null,
        isRefreshingRuntimeData: false,
        loadRuntimeData: vi.fn(),
        clearRuntimeData: vi.fn(),
    },
}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../../hooks/use-app-selector', () => ({useAppSelector: () => undefined}));
vi.mock('../../../../providers/confirm-provider', () => ({useConfirm: () => vi.fn()}));
vi.mock('../../../../hooks/use-sync-state', () => ({useSyncState: () => [null, mocks.setClaim]}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasProcessPermission: () => false,
    useHasSystemPermission: () => false,
    useRefreshPermissionSet: () => mocks.refreshPermissions,
}));
vi.mock('../../hooks/use-process-runtime-data', () => ({useProcessRuntimeData: () => mocks.runtimeData}));
vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: PropsWithChildren) => children,
}));
vi.mock('../../../../components/generic-page-header/generic-page-header', () => ({
    GenericPageHeader: ({title}: {title: string}) => <h1>{title}</h1>,
}));
vi.mock('allotment', () => ({
    Allotment: Object.assign(({children}: PropsWithChildren) => <div>{children}</div>, {
        Pane: ({children}: PropsWithChildren) => <div>{children}</div>,
    }),
}));
vi.mock('./components/process-flow-editor/process-flow-editor', () => ({
    ProcessFlowEditor: (props: {onDeleteNode?: (node: ProcessNodeEntity) => Promise<void>}) => {
        mocks.onDeleteNode = props.onDeleteNode;
        return <p>Prozessfluss</p>;
    },
}));
vi.mock('../../dialogs/select-node-provider-dialog', () => ({SelectNodeProviderDialog: () => null}));
vi.mock('./components/process-connect-existing-node-dialog', () => ({ProcessConnectExistingNodeDialog: () => null}));
vi.mock('./components/process-details-page-more-menu', () => ({ProcessDetailsPageMoreMenu: () => null}));
vi.mock('./components/process-notes-overview-dialog', () => ({ProcessNotesOverviewDialog: () => null}));
vi.mock('../../dialogs/process-test-claim-process-instances-dialog', () => ({ProcessTestClaimProcessInstancesDialog: () => null}));
vi.mock('../../dialogs/process-settings-dialog/process-settings-dialog', () => ({ProcessSettingsDialog: () => null}));
vi.mock('../../dialogs/process-versions-dialog', () => ({ProcessVersionsDialog: () => null}));
vi.mock('../../dialogs/process-publish-dialog', () => ({ProcessPublishDialog: () => null}));

function RouteError() {
    return <StaffShellError error={useRouteError() as ApiError} />;
}

function renderPage(version = '1') {
    const router = createMemoryRouter([
        {
            path: '/processes/:processId/versions/:processVersion',
            element: <ProcessDetailsPage />,
            errorElement: <RouteError />,
        },
    ], {initialEntries: [`/processes/1/versions/${version}`]});
    render(<RouterProvider router={router} />);
    return router;
}

function apiError(status: number): ApiError {
    return {status, message: 'Die Berechtigung process_definition.read ist erforderlich.', details: null, displayableToUser: true};
}

describe('ProcessDetailsPage loading', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
        vi.spyOn(ProcessDefinitionApiService.prototype, 'retrieve').mockImplementation(async (id) => ({
            ...ProcessDefinitionApiService.initialize(), id, internalTitle: 'Testprozess', draftedVersion: 1,
        }));
        vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'retrieve').mockImplementation(async (id) => ({
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: id.processDefinitionId,
            processVersion: id.processDefinitionVersion,
        }));
        vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'validate').mockResolvedValue({nodeProblems: [], versionProblems: []});
        const emptyPage = {content: [], page: {size: 999, number: 0, totalElements: 0, totalPages: 0}};
        vi.spyOn(ProcessNodeApiService.prototype, 'listAll').mockResolvedValue(emptyPage);
        vi.spyOn(ProcessDefinitionEdgeApiService.prototype, 'listAll').mockResolvedValue(emptyPage);
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProviders').mockResolvedValue([]);
        vi.spyOn(ProcessTestClaimApiService.prototype, 'listAll').mockResolvedValue(emptyPage);
        vi.spyOn(SearchItemService.prototype, 'recordRecentSearchItem').mockResolvedValue(undefined);
    });

    it.each(['1', 'latest'])('shows the staff shell access-denied screen when loading version %s is forbidden', async (version) => {
        vi.mocked(ProcessDefinitionApiService.prototype.retrieve).mockRejectedValue(apiError(403));
        renderPage(version);

        expect(await screen.findByRole('heading', {name: 'Zugriff verweigert (Fehler 403)'})).toBeInTheDocument();
        expect(screen.getByText('process_definition.read', {selector: 'code'})).toBeInTheDocument();
        expect(screen.queryByText('Prozessfluss')).not.toBeInTheDocument();
    });

    it.each(['1', 'latest'])('shows the staff shell not-found screen when version %s cannot be found', async (version) => {
        vi.mocked(ProcessDefinitionApiService.prototype.retrieve).mockRejectedValue(apiError(404));
        renderPage(version);

        expect(await screen.findByRole('heading', {name: 'Seite nicht gefunden (Fehler 404)'})).toBeInTheDocument();
    });

    it.each([
        {status: 403, title: 'Zugriff verweigert (Fehler 403)'},
        {status: 404, title: 'Seite nicht gefunden (Fehler 404)'},
        {status: 503, title: 'Beim Laden ist ein Fehler aufgetreten. (Fehler 503)'},
    ])('preserves a version endpoint failure with status $status', async ({status, title}) => {
        vi.mocked(ProcessDefinitionVersionApiService.prototype.retrieve).mockRejectedValue(apiError(status));
        renderPage();

        expect(await screen.findByRole('heading', {name: title})).toBeInTheDocument();
    });

    it.each(['1', 'latest'])('handles unexpected failures while loading version %s as server errors', async (version) => {
        vi.mocked(ProcessDefinitionApiService.prototype.retrieve).mockRejectedValue(new Error('Internal failure'));
        renderPage(version);

        expect(await screen.findByRole('heading', {name: 'Beim Laden ist ein Fehler aufgetreten. (Fehler 500)'})).toBeInTheDocument();
        expect(screen.getByText('Der Prozessfluss konnte nicht geladen werden.')).toBeInTheDocument();
        expect(screen.queryByText('Internal failure')).not.toBeInTheDocument();
    });

    it('shows a load error when the required process nodes cannot be loaded', async () => {
        vi.mocked(ProcessNodeApiService.prototype.listAll).mockRejectedValue(apiError(403));
        renderPage();

        expect(await screen.findByRole('heading', {name: 'Zugriff verweigert (Fehler 403)'})).toBeInTheDocument();
    });

    it('shows a load error when the required process edges cannot be loaded', async () => {
        vi.mocked(ProcessDefinitionEdgeApiService.prototype.listAll).mockRejectedValue(apiError(503));
        renderPage();

        expect(await screen.findByRole('heading', {name: 'Beim Laden ist ein Fehler aufgetreten. (Fehler 503)'})).toBeInTheDocument();
    });

    it.each(['1', 'latest'])('loads an accessible process through version %s', async (version) => {
        const router = renderPage(version);

        expect(await screen.findByRole('heading', {name: 'Prozess: Testprozess'})).toBeInTheDocument();
        expect(router.state.location.pathname).toBe('/processes/1/versions/1');
        expect(screen.queryByRole('heading', {name: /Fehler/})).not.toBeInTheDocument();
    });

    it('keeps optional test-claim failures local', async () => {
        vi.mocked(ProcessTestClaimApiService.prototype.listAll).mockRejectedValue(apiError(403));
        renderPage();

        expect(await screen.findByRole('heading', {name: 'Prozess: Testprozess'})).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: /Fehler/})).not.toBeInTheDocument();
    });

    it.each(['1', 'latest'])('ignores an obsolete load failure after navigating away from version %s', async (version) => {
        let rejectFirst!: (error: ApiError) => void;
        vi.mocked(ProcessDefinitionApiService.prototype.retrieve).mockImplementationOnce(() => new Promise((_, reject) => {
            rejectFirst = reject;
        }));
        const router = renderPage(version);
        await waitFor(() => expect(ProcessDefinitionApiService.prototype.retrieve).toHaveBeenCalledWith(1));
        await act(async () => router.navigate('/processes/2/versions/1'));
        expect(await screen.findByRole('heading', {name: 'Prozess: Testprozess'})).toBeInTheDocument();

        await act(async () => rejectFirst(apiError(403)));
        expect(screen.getByRole('heading', {name: 'Prozess: Testprozess'})).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: /Fehler/})).not.toBeInTheDocument();
    });

    it('recovers from a load error when navigating to an accessible process', async () => {
        vi.mocked(ProcessDefinitionApiService.prototype.retrieve).mockRejectedValueOnce(apiError(403));
        const router = renderPage();
        expect(await screen.findByRole('heading', {name: 'Zugriff verweigert (Fehler 403)'})).toBeInTheDocument();

        await act(async () => router.navigate('/processes/2/versions/1'));
        expect(await screen.findByRole('heading', {name: 'Prozess: Testprozess'})).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: /Fehler/})).not.toBeInTheDocument();
    });
});

describe('ProcessDetailsPage node deletion', () => {
    const port = {key: 'out', deadlineKey: null, label: 'Weiter', description: ''};
    const node = (id: number, ports = [port]): ProcessNodeEntity => ({
        ...ProcessNodeApiService.initialize(),
        id,
        processId: 1,
        processVersion: 1,
        processNodeDefinitionKey: 'test.node',
        processNodeDefinitionVersion: 1,
        ports,
    });
    const edge = (id: number, fromNodeId: number, toNodeId: number): ProcessDefinitionEdgeEntity => ({
        id,
        processId: 1,
        processVersion: 1,
        fromNodeId,
        toNodeId,
        viaPort: 'out',
    });
    const page = <T, >(content: T[]) => ({content, page: {size: 999, number: 0, totalElements: content.length, totalPages: 1}});
    // Nodes 1 and 4 lead into node 2, which leads into node 3: deleting node 2 bridges 1 → 3 and 4 → 3.
    const deletedNode = node(2);

    beforeEach(() => {
        vi.clearAllMocks();
        mocks.onDeleteNode = undefined;
        vi.spyOn(console, 'error').mockImplementation(() => {});
        vi.spyOn(ProcessDefinitionApiService.prototype, 'retrieve').mockImplementation(async (id) => ({
            ...ProcessDefinitionApiService.initialize(), id, internalTitle: 'Testprozess', draftedVersion: 1,
        }));
        vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'retrieve').mockImplementation(async (id) => ({
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: id.processDefinitionId,
            processVersion: id.processDefinitionVersion,
        }));
        vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'validate').mockResolvedValue({nodeProblems: [], versionProblems: []});
        vi.spyOn(ProcessNodeApiService.prototype, 'listAll').mockResolvedValue(page([node(1), deletedNode, node(3, []), node(4)]));
        vi.spyOn(ProcessDefinitionEdgeApiService.prototype, 'listAll').mockResolvedValue(page([edge(10, 1, 2), edge(11, 4, 2), edge(12, 2, 3)]));
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProviders').mockResolvedValue([]);
        vi.spyOn(ProcessNodeProviderApiService.prototype, 'getNodeProvider').mockResolvedValue({key: 'test.node', majorVersion: 1} as never);
        vi.spyOn(ProcessTestClaimApiService.prototype, 'listAll').mockResolvedValue(page([]));
        vi.spyOn(SearchItemService.prototype, 'recordRecentSearchItem').mockResolvedValue(undefined);
        vi.spyOn(ProcessDefinitionEdgeApiService.prototype, 'destroy').mockResolvedValue(undefined);
        vi.spyOn(ProcessNodeApiService.prototype, 'destroy').mockResolvedValue(undefined);
    });

    async function deleteNode(): Promise<void> {
        renderPage();
        // The flow editor only mounts once the node definitions are loaded.
        expect(await screen.findByText('Prozessfluss')).toBeInTheDocument();
        await act(async () => mocks.onDeleteNode?.(deletedNode));
    }

    it('creates the bridging connections one after another', async () => {
        let active = 0;
        let maxActive = 0;
        let nextId = 100;
        const create = vi.spyOn(ProcessDefinitionEdgeApiService.prototype, 'create').mockImplementation(async (payload) => {
            active++;
            maxActive = Math.max(maxActive, active);
            await new Promise((resolve) => setTimeout(resolve, 0));
            active--;
            return {...payload, id: nextId++};
        });

        await deleteNode();

        expect(create).toHaveBeenCalledTimes(2);
        expect(create).toHaveBeenCalledWith(expect.objectContaining({fromNodeId: 1, toNodeId: 3, viaPort: 'out'}));
        expect(create).toHaveBeenCalledWith(expect.objectContaining({fromNodeId: 4, toNodeId: 3, viaPort: 'out'}));
        expect(maxActive).toBe(1);
        expect(mocks.dispatch).not.toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Das Prozesselement konnte nicht gelöscht werden.'}),
        }));
    });

    it('reports a failed deletion and reloads nodes and connections', async () => {
        vi.spyOn(ProcessDefinitionEdgeApiService.prototype, 'create').mockRejectedValue({
            status: 500, message: 'Internal Server Error', details: null, displayableToUser: false,
        } satisfies ApiError);

        await deleteNode();

        expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Das Prozesselement konnte nicht gelöscht werden.'}),
        }));
        await waitFor(() => expect(ProcessNodeApiService.prototype.listAll).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(ProcessDefinitionEdgeApiService.prototype.listAll).toHaveBeenCalledTimes(2));
    });
});
