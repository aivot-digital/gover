import React, {type ReactNode, useCallback, useEffect, useMemo, useState} from 'react';
import {Box, Button, Divider, Paper, Typography} from '@mui/material';
import {Outlet, useLocation, useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {type ProcessEntity} from '../../entities/process-entity';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {type ProcessDefinitionEdgeEntity} from '../../entities/process-definition-edge-entity';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessDefinitionEdgeApiService} from '../../services/process-definition-edge-api-service';
import {type ProcessVersionEntity} from '../../entities/process-version-entity';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../../components/generic-page-header/generic-page-header';
import {
    type ProcessNodeProvider,
    ProcessNodeProviderApiService,
    ProcessNodeType,
} from '../../services/process-node-provider-api-service';
import {type ProcessNodeTypeLimit, SelectNodeProviderDialog} from '../../dialogs/select-node-provider-dialog';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {
    addSnackbarMessage,
    clearLoadingMessage,
    setLoadingMessage,
    SnackbarSeverity,
    SnackbarType,
} from '../../../../slices/shell-slice';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {ProcessFlowEditor} from './components/process-flow-editor/process-flow-editor';
import {ReactFlowProvider} from '@xyflow/react';
import {ProcessDetailsPageProvider} from './process-details-page-context';
import {Allotment} from 'allotment';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import {
    ProcessDetailsPageMoreMenu,
    type ProcessDetailsPageMoreMenuEvent,
} from './components/process-details-page-more-menu';
import {uploadObjectFile} from '../../../../utils/download-utils';
import {ProcessTestClaimApiService} from '../../services/process-test-claim-api-service';
import {useConfirm} from '../../../../providers/confirm-provider';
import {type ProcessTestClaimEntity} from '../../entities/process-test-claim-entity';
import {type User} from '../../../users/models/user';
import {UsersApiService} from '../../../users/users-api-service';
import {resolveUserName} from '../../../users/utils/resolve-user-name';
import {type ProcessInstanceEntity} from '../../entities/process-instance-entity';
import {type ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {type ProcessInstanceEventEntity} from '../../entities/process-instance-event-entity';
import {type ProcessInstanceAttachmentEntity} from '../../entities/process-instance-attachment-entity';
import {type ProcessInstanceAttachmentSetEntity} from '../../entities/process-instance-attachment-set-entity';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessInstanceAttachmentApiService} from '../../services/process-instance-attachment-api-service';
import {ProcessInstanceAttachmentSetApiService} from '../../services/process-instance-attachment-set-api-service';
import {ProcessInstanceStatus} from '../../enums/process-instance-status';
import {BaseApiService, RequestOptions} from '../../../../services/base-api-service';
import {ProcessInstanceEventDialog} from '../../dialogs/process-instance-event-dialog';
import {getProcessNodeProviderKey} from './components/process-flow-editor/utils/process-flow-graph-utils';
import {ProcessDetailsPageSkeleton} from './components/process-details-page-skeleton';
import {useDelayedVisibility} from '../../../../hooks/use-delayed-visibility';
import Undo from '@aivot/mui-material-symbols-400-n25-outlined/Undo';
import Redo from '@aivot/mui-material-symbols-400-n25-outlined/Redo';
import Refresh from '@aivot/mui-material-symbols-400-n25-outlined/Refresh';
import Settings from '@aivot/mui-material-symbols-400-n25-outlined/Settings';
import {type Action} from '../../../../components/actions/actions-props';
import HomeStorage from '@aivot/mui-material-symbols-400-n25-outlined/HomeStorage';
import News from '@aivot/mui-material-symbols-400-n25-outlined/News';
import AttachFile from '@aivot/mui-material-symbols-400-n25-outlined/AttachFile';
import {ProcessConnectExistingNodeDialog} from './components/process-connect-existing-node-dialog';
import {getNodeName} from './components/process-flow-editor/utils/node-utils';
import SwapHoriz from '@aivot/mui-material-symbols-400-n25-outlined/SwapHoriz';
import UploadFile from '@aivot/mui-material-symbols-400-n25-outlined/UploadFile';
import {type ProcessNodeExport} from '../../entities/process-node-export';
import {ProcessSettingsDialog} from '../../dialogs/process-settings-dialog/process-settings-dialog';
import {ProcessTestClaimProcessInstancesDialog} from '../../dialogs/process-test-claim-process-instances-dialog';
import {useNotImplemented} from '../../../../hooks/use-not-implemented';
import {getMinDisplayableAreaWidth} from '../../../../utils/display-area-utils';
import {ProcessNodeProblems} from '../../entities/process-node-problems';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectUser} from '../../../../slices/user-slice';
import {addEntityHistoryItem} from '../../../../slices/entity-history-slice';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {generateId} from '../../../../utils/id-utils';
import {
    PROCESS_NODE_EDITOR_SKIP_CHANGE_BLOCKER_STATE_KEY,
} from './components/process-node-editor/process-node-editor-change-blocker';
import {ProcessStatus} from '../../enums/process-status';
import {useSyncState} from '../../../../hooks/use-sync-state';
import {useProcessExport} from '../../../../hooks/use-process-export';
import {ProcessVersionsDialog} from '../../dialogs/process-versions-dialog';
import {NodeProblemsAlert} from '../../components/node-problems-alert';
import {ProcessPublishDialog} from '../../dialogs/process-publish-dialog';
import {useRefreshPermissionSet} from '../../../permissions/hooks/use-permissions';
import {getProcessNodeLimit, isFormModuleEnabled, isProcessNodeTypeUnlimited} from '../../../../utils/module-flags';
import {
    buildProcessInstanceAttachmentSetItems,
    ProcessInstanceAttachmentSetList,
} from '../../components/process-instance-attachment-set-list';

export const SHOW_ERRORS_ROUTER_STATE = 'show-errors-on-load';

const FORM_PLUGIN_KEY = 'de.aivot.form';
const PROCESS_DETAILS_PAGE_SKELETON_DELAY = 250;
/**
 * @deprecated Obsolete, use ProviderTypeStyles instead
 */
const PROCESS_NODE_TYPE_LABELS: Record<ProcessNodeType, string> = {
    [ProcessNodeType.Trigger]: 'Auslöser',
    [ProcessNodeType.Action]: 'Aktionen',
    [ProcessNodeType.FlowControl]: 'Flusselemente',
    [ProcessNodeType.Termination]: 'Abschlüsse',
};

const DISPLAYABLE_AREA = getMinDisplayableAreaWidth();
export const MIN_EDITOR_DRAWER_WIDTH_PX = 540;
const EDITOR_PANE_TOGGLE_BUTTON_SIZE_PX = 24;

export interface ProcessFlow {
    definition: ProcessEntity;
    version: ProcessVersionEntity;
    nodes: ProcessNodeEntity[];
    edges: ProcessDefinitionEdgeEntity[];
}

interface ReplaceNodeRequest {
    nodeId: number;
}

interface NewNodeRequest {
    fromNodeId: number;
    viaPort: string;
}

interface NodeRefreshSignal {
    nodeId: number | null;
    version: number;
}

interface RecreatedEdgePlan {
    originalEdge: ProcessDefinitionEdgeEntity;
    createPayload: ProcessDefinitionEdgeEntity;
}

interface NodeReplacementPlan {
    replacementNode: ProcessNodeEntity;
    unchangedOutgoingEdges: ProcessDefinitionEdgeEntity[];
    recreatedOutgoingEdges: RecreatedEdgePlan[];
    removedOutgoingEdges: ProcessDefinitionEdgeEntity[];
}

type NodeImportContext = 'trigger' | 'follow-up' | 'in-between';

function canReplaceNodeType(currentType: ProcessNodeType, replacementType: ProcessNodeType): boolean {
    if (currentType === ProcessNodeType.Trigger || replacementType === ProcessNodeType.Trigger) {
        return currentType === ProcessNodeType.Trigger && replacementType === ProcessNodeType.Trigger;
    }

    if (currentType === ProcessNodeType.Termination || replacementType === ProcessNodeType.Termination) {
        return currentType === ProcessNodeType.Termination && replacementType === ProcessNodeType.Termination;
    }

    return true;
}

function isProcessNodeProviderEnabled(provider: ProcessNodeProvider): boolean {
    return provider.parentPluginKey !== FORM_PLUGIN_KEY || isFormModuleEnabled();
}

function countProcessNodesOfType(
    processFlow: ProcessFlow | null,
    providerCache: Record<string, ProcessNodeProvider>,
    type: ProcessNodeType,
    excludedNodeId?: number,
): number {
    if (processFlow == null) {
        return 0;
    }

    return processFlow.nodes.filter((node) => {
        if (node.id === excludedNodeId) {
            return false;
        }

        const currentProvider = providerCache[getProcessNodeProviderKey(
            node.processNodeDefinitionKey,
            node.processNodeDefinitionVersion,
        )];

        return currentProvider?.type === type;
    }).length;
}

function getProcessNodeTypeLimit(
    processFlow: ProcessFlow | null,
    providerCache: Record<string, ProcessNodeProvider>,
    type: ProcessNodeType,
    excludedNodeId?: number,
): ProcessNodeTypeLimit | undefined {
    if (processFlow == null || isProcessNodeTypeUnlimited(type)) {
        return undefined;
    }

    return {
        current: countProcessNodesOfType(processFlow, providerCache, type, excludedNodeId),
        limit: getProcessNodeLimit(type),
    };
}

function getProcessNodeTypeLimits(
    processFlow: ProcessFlow | null,
    providerCache: Record<string, ProcessNodeProvider>,
    excludedNodeId?: number,
): Partial<Record<ProcessNodeType, ProcessNodeTypeLimit>> {
    const limits: Partial<Record<ProcessNodeType, ProcessNodeTypeLimit>> = {};

    for (const type of Object.values(ProcessNodeType) as ProcessNodeType[]) {
        const limit = getProcessNodeTypeLimit(processFlow, providerCache, type, excludedNodeId);

        if (limit != null) {
            limits[type] = limit;
        }
    }

    return limits;
}

function isProcessNodeTypeLimitReached(
    type: ProcessNodeType,
    processFlow: ProcessFlow | null,
    providerCache: Record<string, ProcessNodeProvider>,
    excludedNodeId?: number,
): boolean {
    return processFlow != null &&
        !isProcessNodeTypeUnlimited(type) &&
        countProcessNodesOfType(processFlow, providerCache, type, excludedNodeId) >= getProcessNodeLimit(type);
}

function canPlaceProcessNodeProvider(
    provider: ProcessNodeProvider,
    processFlow: ProcessFlow | null,
    providerCache: Record<string, ProcessNodeProvider>,
    excludedNodeId?: number,
): boolean {
    if (!isProcessNodeProviderEnabled(provider)) {
        return false;
    }

    if (processFlow == null || isProcessNodeTypeUnlimited(provider.type)) {
        return true;
    }

    return countProcessNodesOfType(processFlow, providerCache, provider.type, excludedNodeId) < getProcessNodeLimit(provider.type);
}

function getProcessNodeLimitReachedEmptyMessage(
    nodeProviders: ProcessNodeProvider[],
    isCandidateProvider: (provider: ProcessNodeProvider) => boolean,
    processFlow: ProcessFlow | null,
    providerCache: Record<string, ProcessNodeProvider>,
    excludedNodeId?: number,
): ReactNode | undefined {
    const limitedTypes = new Set<ProcessNodeType>();

    for (const provider of nodeProviders) {
        if (
            isCandidateProvider(provider) &&
            isProcessNodeProviderEnabled(provider) &&
            isProcessNodeTypeLimitReached(provider.type, processFlow, providerCache, excludedNodeId)
        ) {
            limitedTypes.add(provider.type);
        }
    }

    if (limitedTypes.size === 0) {
        return undefined;
    }

    if (limitedTypes.size === 1) {
        const type = Array.from(limitedTypes)[0]!;
        const limit = getProcessNodeTypeLimit(processFlow, providerCache, type, excludedNodeId);

        if (limit == null) {
            return `Das Limit für ${PROCESS_NODE_TYPE_LABELS[type]} in dieser Prozessversion ist erreicht.`;
        }

        return `Das Limit für ${PROCESS_NODE_TYPE_LABELS[type]} in dieser Prozessversion ist erreicht: ${limit.current}/${limit.limit}.`;
    }

    const limitLabels = Array
        .from(limitedTypes)
        .map((type) => {
            const limit = getProcessNodeTypeLimit(processFlow, providerCache, type, excludedNodeId);
            return limit == null
                ? PROCESS_NODE_TYPE_LABELS[type]
                : `${PROCESS_NODE_TYPE_LABELS[type]} ${limit.current}/${limit.limit}`;
        });

    return (
        <Typography>
            Die Limits für kompatible Prozesselemente in dieser Prozessversion sind erreicht: <br/>
            {
                limitLabels.map((l) => (
                    <React.Fragment key={l}>
                        {l} <br/>
                    </React.Fragment>
                ))
            }
        </Typography>
    );
}

function isReplacementCandidateProvider(
    provider: ProcessNodeProvider,
    replaceNodeSource: ProcessNodeEntity | null,
    providerCache: Record<string, ProcessNodeProvider>,
): boolean {
    if (replaceNodeSource == null) {
        return false;
    }

    const currentProvider = providerCache[getProcessNodeProviderKey(
        replaceNodeSource.processNodeDefinitionKey,
        replaceNodeSource.processNodeDefinitionVersion,
    )];
    if (currentProvider == null) {
        return false;
    }

    if (
        provider.key === replaceNodeSource.processNodeDefinitionKey &&
        provider.majorVersion === replaceNodeSource.processNodeDefinitionVersion
    ) {
        return false;
    }

    return canReplaceNodeType(currentProvider.type, provider.type);
}

function isFollowUpCandidateProvider(
    provider: ProcessNodeProvider,
    processFlow: ProcessFlow | null,
    newNodeFor: NewNodeRequest | null,
): boolean {
    if (provider.type === ProcessNodeType.Trigger) {
        return false;
    }

    if (processFlow == null || newNodeFor == null) {
        return true;
    }

    const requiresOutgoingPort = processFlow.edges.some((edge) => (
        edge.fromNodeId === newNodeFor.fromNodeId &&
        edge.viaPort === newNodeFor.viaPort
    ));

    return !requiresOutgoingPort || provider.ports.length > 0;
}

function isInbetweenCandidateProvider(provider: ProcessNodeProvider): boolean {
    return provider.type !== ProcessNodeType.Trigger && provider.ports.length > 0;
}

function getUnavailableProcessNodeProviderMessage(provider: ProcessNodeProvider): string {
    if (!isProcessNodeProviderEnabled(provider)) {
        return 'Die Formularerweiterung ist auf dieser Instanz nicht aktiviert.';
    }

    return `Das Limit für ${PROCESS_NODE_TYPE_LABELS[provider.type]} in dieser Prozessversion ist erreicht.`;
}

function formatOutgoingConnectionSummary(preservedOutgoingEdgeCount: number, removedOutgoingEdgeCount: number): string {
    if (preservedOutgoingEdgeCount === 0 && removedOutgoingEdgeCount === 0) {
        return 'Es gibt derzeit keine ausgehenden Verbindungen.';
    }

    if (removedOutgoingEdgeCount === 0) {
        return preservedOutgoingEdgeCount === 1
            ? 'Die ausgehende Verbindung wird übernommen.'
            : `Alle ${preservedOutgoingEdgeCount} ausgehenden Verbindungen werden übernommen.`;
    }

    if (preservedOutgoingEdgeCount === 0) {
        return removedOutgoingEdgeCount === 1
            ? 'Keine ausgehende Verbindung kann übernommen werden. Die bestehende Verbindung wird entfernt.'
            : `Keine ausgehende Verbindung kann übernommen werden. ${removedOutgoingEdgeCount} bestehende Verbindungen werden entfernt.`;
    }

    const preservedText = preservedOutgoingEdgeCount === 1
        ? 'Eine ausgehende Verbindung wird übernommen.'
        : `${preservedOutgoingEdgeCount} ausgehende Verbindungen werden übernommen.`;
    const removedText = removedOutgoingEdgeCount === 1
        ? 'Eine Verbindung kann nicht übernommen werden und wird entfernt.'
        : `${removedOutgoingEdgeCount} Verbindungen können nicht übernommen werden und werden entfernt.`;

    return `${preservedText} ${removedText}`;
}

function formatRemovedOutgoingConnectionsMessage(removedOutgoingEdgeCount: number): string {
    return removedOutgoingEdgeCount === 1
        ? 'Eine ausgehende Verbindung konnte nicht übernommen werden und wurde entfernt.'
        : `${removedOutgoingEdgeCount} ausgehende Verbindungen konnten nicht übernommen werden und wurden entfernt.`;
}

function getProviderPortOrderIndex(provider: ProcessNodeProvider, portKey: string): number {
    const portIndex = provider.ports.findIndex((port) => port.key === portKey);
    return portIndex === -1 ? Number.MAX_SAFE_INTEGER : portIndex;
}

function buildNodeReplacementPlan(
    processFlow: ProcessFlow,
    node: ProcessNodeEntity,
    currentProvider: ProcessNodeProvider,
    replacementProvider: ProcessNodeProvider,
): NodeReplacementPlan {
    // Preserve outgoing connections deterministically: keep same-key ports first, then map any
    // remaining edges onto the remaining replacement ports in order. Extra edges are dropped when
    // the new provider simply cannot host the same fan-out anymore.
    const outgoingEdges = processFlow
        .edges
        .filter((edge) => edge.fromNodeId === node.id)
        .sort((leftEdge, rightEdge) => (
            getProviderPortOrderIndex(currentProvider, leftEdge.viaPort) - getProviderPortOrderIndex(currentProvider, rightEdge.viaPort) ||
            leftEdge.id - rightEdge.id
        ));
    const replacementPortKeys = replacementProvider.ports.map((port) => port.key);
    const usedReplacementPortKeys = new Set<string>();
    const unchangedOutgoingEdges: ProcessDefinitionEdgeEntity[] = [];
    const recreatedOutgoingEdges: RecreatedEdgePlan[] = [];
    const removedOutgoingEdges: ProcessDefinitionEdgeEntity[] = [];

    for (const outgoingEdge of outgoingEdges) {
        const preferredReplacementPortKey = replacementPortKeys.find((portKey) => (
            portKey === outgoingEdge.viaPort &&
            !usedReplacementPortKeys.has(portKey)
        ));
        const fallbackReplacementPortKey = replacementPortKeys.find((portKey) => (
            !usedReplacementPortKeys.has(portKey)
        ));
        const replacementPortKey = preferredReplacementPortKey ?? fallbackReplacementPortKey ?? null;

        if (replacementPortKey == null) {
            removedOutgoingEdges.push(outgoingEdge);
            continue;
        }

        usedReplacementPortKeys.add(replacementPortKey);

        if (replacementPortKey === outgoingEdge.viaPort) {
            unchangedOutgoingEdges.push(outgoingEdge);
            continue;
        }

        recreatedOutgoingEdges.push({
            originalEdge: outgoingEdge,
            createPayload: {
                ...outgoingEdge,
                id: 0,
                viaPort: replacementPortKey,
            },
        });
    }

    return {
        replacementNode: {
            ...node,
            processNodeDefinitionKey: replacementProvider.key,
            processNodeDefinitionVersion: replacementProvider.majorVersion,
            configuration: {},
            outputMappings: {},
        },
        unchangedOutgoingEdges,
        recreatedOutgoingEdges,
        removedOutgoingEdges,
    };
}

function getNodeProviderFromList(
    nodeProviders: ProcessNodeProvider[],
    key: string,
    version: number,
): ProcessNodeProvider | null {
    return nodeProviders.find((provider) => (
        provider.key === key &&
        provider.majorVersion === version
    )) ?? null;
}

export function getProcessNodeEditURL(processId: number, processVersion: number, nodeId: number, searchParams?: URLSearchParams) {
    return `/processes/${processId}/versions/${processVersion}/nodes/${nodeId}?${searchParams != null ? searchParams.toString() : ''}`;
}

export function ProcessDetailsPage(): ReactNode {
    const params = useParams();
    const [searchParams, setSearchParams] = useSearchParams();
    const {pathname, state} = useLocation();
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const confirm = useConfirm();
    const user = useAppSelector(selectUser);
    const notImplemented = useNotImplemented();
    const refreshPermissionSet = useRefreshPermissionSet();

    const [processFlow, setProcessFlow] = useState<ProcessFlow | null>(null);
    const [isLoadingProcessFlow, setIsLoadingProcessFlow] = useState(true);
    const [runtimeData, setRuntimeData] = useState<{
        instance: ProcessInstanceEntity;
        tasks: ProcessInstanceTaskEntity[];
        events: ProcessInstanceEventEntity[];
        attachments: ProcessInstanceAttachmentEntity[];
        attachmentSets: ProcessInstanceAttachmentSetEntity[];
    } | null>(null);
    const [isRefreshingRuntimeData, setIsRefreshingRuntimeData] = useState(false);
    const [availableNodeProviders, setAvailableNodeProviders] = useState<ProcessNodeProvider[]>([]);
    const [flowNodeProviderCache, setFlowNodeProviderCache] = useState<Record<string, ProcessNodeProvider>>({});
    const [isLoadingFlowNodeProviders, setIsLoadingFlowNodeProviders] = useState(false);
    const [hasFlowNodeProviderLoadError, setHasFlowNodeProviderLoadError] = useState(false);
    const [readyFlowEditorKey, setReadyFlowEditorKey] = useState<string | null>(null);
    const [showSettingsDialog, setShowSettingsDialog] = useState(false);
    const [processNodeProblems, setProcessNodeProblems] = useState<ProcessNodeProblems[]>([]);
    const [showProcessNodeProblemsForNodes, setShowProcessNodeProblemsForNodes] = useState<Record<number, boolean>>({});
    const [isEditorPaneCollapsed, setIsEditorPaneCollapsed] = useState(false);
    const [hideEditorPaneExpandButton, setHideEditorPaneExpandButton] = useState(false);
    const [editorPaneWidth, setEditorPaneWidth] = useState(MIN_EDITOR_DRAWER_WIDTH_PX);
    const [lastExpandedEditorPaneWidth, setLastExpandedEditorPaneWidth] = useState(MIN_EDITOR_DRAWER_WIDTH_PX);
    const [showVersionsDialog, setShowVersionsDialog] = useState(false);
    const [showPublishDialog, setShowPublishDialog] = useState(false);

    const [showAddTriggerDialog, setShowAddTriggerDialog] = useState(false);
    const [newNodeFor, setNewNodeFor] = useState<NewNodeRequest | null>(null);
    const [newNodeOnEdgeId, setNewNodeOnEdgeId] = useState<number | null>(null);
    const [replaceNodeRequest, setReplaceNodeRequest] = useState<ReplaceNodeRequest | null>(null);
    const [connectExistingNodeRequest, setConnectExistingNodeRequest] = useState<{
        sourceNodeId: number;
        preferredPortKey: string | null;
    } | null>(null);
    const [nodeRefreshSignal, setNodeRefreshSignal] = useState<NodeRefreshSignal>({
        nodeId: null,
        version: 0,
    });

    const [showMenuAtEl, setShowMenuAtEl] = useState<HTMLElement | null>(null);
    const [showProcessInstanceEventsDialog, setShowProcessInstanceEventsDialog] = useState(false);
    const [showProcessTestClaimInstancesDialog, setShowProcessTestClaimInstancesDialog] = useState(false);
    const showProcessDetailsPageSkeleton = useDelayedVisibility(
        isLoadingProcessFlow && processFlow == null,
        PROCESS_DETAILS_PAGE_SKELETON_DELAY,
    );

    useEffect(() => {
        document.body.dataset.hasFlowEditor = 'true';

        return () => {
            delete document.body.dataset.hasFlowEditor;
        };
    }, []);

    const handleDownloadAttachment = useCallback(async (attachment: ProcessInstanceAttachmentEntity): Promise<void> => {
        try {
            const blob = await new BaseApiService().getBlob(`/api/process-instance-attachments/${encodeURIComponent(attachment.key)}/file/?download=true`);
            const objectUrl = URL.createObjectURL(blob);

            const link = document.createElement('a');
            link.href = objectUrl;
            link.download = attachment.fileName;
            link.style.display = 'none';

            document.body.appendChild(link);
            link.click();
            link.remove();

            URL.revokeObjectURL(objectUrl);
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Der Anhang konnte nicht heruntergeladen werden.'));
        }
    }, [dispatch]);

    // Fetch the available node providers on mount to display them in the add node dialog
    useEffect(() => {
        new ProcessNodeProviderApiService()
            .getNodeProviders()
            .then(setAvailableNodeProviders)
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die verfügbaren Prozesselemente konnten nicht geladen werden.'));
            });
    }, []);

    // Extract the process id and version from the route to load the process flow
    const {
        processId,
        processVersion,
        processVersionParam,
    } = useMemo(() => {
        const processId = parseInt(params.processId ?? '0', 10);
        const processVersionParam = params.processVersion;
        const processVersion = processVersionParam === 'latest' ?
            0 :
            parseInt(processVersionParam ?? '0', 10);

        return {
            processId: Number.isNaN(processId) ? 0 : processId,
            processVersion: Number.isNaN(processVersion) ? 0 : processVersion,
            processVersionParam,
        };
    }, [params]);

    useEffect(() => {
        if (processId < 1 || processVersionParam !== 'latest') {
            return;
        }

        let cancelled = false;
        setProcessFlow(null);
        setIsLoadingProcessFlow(true);

        new ProcessDefinitionApiService()
            .retrieve(processId)
            .then((definition) => {
                if (cancelled) {
                    return;
                }

                const latestVersion = definition.draftedVersion ??
                    definition.publishedVersion ??
                    (definition.versionCount > 0 ? definition.versionCount : null);

                if (latestVersion == null) {
                    setIsLoadingProcessFlow(false);
                    dispatch(showErrorSnackbar('Für diesen Prozess ist keine Version vorhanden.'));
                    return;
                }

                const search = searchParams.toString();
                navigate(
                    `${pathname.replace(/\/versions\/latest(?=\/|$)/, `/versions/${latestVersion}`)}${search.length > 0 ? `?${search}` : ''}`,
                    {
                        replace: true,
                        state,
                    },
                );
            })
            .catch((error) => {
                if (cancelled) {
                    return;
                }

                setIsLoadingProcessFlow(false);
                dispatch(showApiErrorSnackbar(error, 'Die neueste Prozessversion konnte nicht geladen werden.'));
            });

        return () => {
            cancelled = true;
        };
    }, [dispatch, navigate, pathname, processId, processVersionParam, searchParams, state]);

    // Reset the versions dialog when the process id or process version change to compensate for process switching while the dialog is open
    useEffect(() => {
        setShowVersionsDialog(false);
    }, [processId, processVersion]);

    const [currentTestClaim, setCurrentTestClaim] = useSyncState<{
        claim: ProcessTestClaimEntity;
        user: User | null;
    } | null>(`process_${processId}_${processVersion}_test_claim`, null);

    const showProcessExport = useProcessExport();

    useEffect(() => {
        if (processFlow == null || processVersion < 1) {
            setProcessNodeProblems([]);
            return;
        }
        new ProcessDefinitionVersionApiService()
            .validate({
                processDefinitionId: processId,
                processDefinitionVersion: processVersion,
            })
            .then((problems) => {
                setProcessNodeProblems(problems);

                const problemNodeIds = new Set(problems.map((problem) => problem.node.id));
                const savedWithErrorsNodeIds = processFlow.nodes
                    .filter((node) => node.savedWithErrors && problemNodeIds.has(node.id))
                    .map((node) => node.id);

                if (savedWithErrorsNodeIds.length === 0) {
                    return;
                }

                setShowProcessNodeProblemsForNodes((previousShownProblems) => {
                    let hasChanged = false;
                    const nextShownProblems = {
                        ...previousShownProblems,
                    };

                    for (const nodeId of savedWithErrorsNodeIds) {
                        if (nextShownProblems[nodeId] !== true) {
                            nextShownProblems[nodeId] = true;
                            hasChanged = true;
                        }
                    }

                    return hasChanged ? nextShownProblems : previousShownProblems;
                });
            });
    }, [processId, processVersion, processFlow?.nodes, processFlow?.edges]);

    const instanceId = useMemo(() => {
        const instanceIdParam = searchParams.get('instanceId');
        if (instanceIdParam == null) {
            return null;
        }
        return parseInt(instanceIdParam);
    }, [searchParams]);

    const activeTestClaimId = currentTestClaim?.claim.id ?? runtimeData?.instance.createdForTestClaimId ?? null;
    const showRuntimePendingStartHint = runtimeData != null &&
        runtimeData.instance.status === ProcessInstanceStatus.Created &&
        runtimeData.tasks.length === 0;

    const requiredFlowNodeProviders = useMemo(() => {
        if (processFlow == null) {
            return [];
        }

        return Array.from(new Map(
            processFlow.nodes.map((node) => [
                getProcessNodeProviderKey(node.processNodeDefinitionKey, node.processNodeDefinitionVersion),
                {
                    key: node.processNodeDefinitionKey,
                    version: node.processNodeDefinitionVersion,
                },
            ]),
        ).values());
    }, [processFlow?.nodes]);

    const requiredFlowNodeProviderSignature = useMemo(() => (
        requiredFlowNodeProviders
            .map((providerReference) => getProcessNodeProviderKey(providerReference.key, providerReference.version))
            .sort()
            .join('|')
    ), [requiredFlowNodeProviders]);

    const flowEditorKey = useMemo(() => {
        if (processFlow == null) {
            return null;
        }

        return `${processFlow.definition.id}:${processFlow.version.processVersion}`;
    }, [processFlow]);

    const flowNodeProviders = useMemo(() => (
        requiredFlowNodeProviders
            .map((providerReference) => flowNodeProviderCache[getProcessNodeProviderKey(providerReference.key, providerReference.version)])
            .filter((provider): provider is ProcessNodeProvider => provider != null)
    ), [flowNodeProviderCache, requiredFlowNodeProviders]);

    const isFlowEditorReady = requiredFlowNodeProviderSignature.length === 0 || flowNodeProviders.length === requiredFlowNodeProviders.length;
    const shouldKeepFlowEditorMounted = flowEditorKey != null && readyFlowEditorKey === flowEditorKey;

    const processInstanceAttachmentSetItems = useMemo(() => {
        if (runtimeData == null || processFlow == null) {
            return [];
        }

        const tasksById = new Map(runtimeData.tasks.map((task) => [task.id, task]));
        const nodesById = new Map(processFlow.nodes.map((node) => [node.id, node]));

        return buildProcessInstanceAttachmentSetItems(
            runtimeData.attachmentSets,
            runtimeData.attachments,
            {includeEmpty: true},
        ).map((item) => {
            const task = item.attachmentSet.processInstanceTaskId == null
                ? runtimeData.tasks.find((candidate) => candidate.processNodeId === runtimeData.instance.initialNodeId)
                : tasksById.get(item.attachmentSet.processInstanceTaskId);
            const nodeId = task?.processNodeId ?? (
                item.attachmentSet.processInstanceTaskId == null ? runtimeData.instance.initialNodeId : null
            );
            const node = nodeId == null ? null : nodesById.get(nodeId);
            const provider = node == null
                ? null
                : flowNodeProviderCache[getProcessNodeProviderKey(
                    node.processNodeDefinitionKey,
                    node.processNodeDefinitionVersion,
                )];

            return {
                ...item,
                createdByLabel: node == null
                    ? 'Unbekanntes Prozesselement'
                    : provider == null
                        ? (node.name ?? node.processNodeDefinitionKey)
                        : getNodeName(node, provider),
            };
        });
    }, [flowNodeProviderCache, processFlow, runtimeData]);

    const handleOpenAttachmentSetsDialog = useCallback((): void => {
        if (processInstanceAttachmentSetItems.length === 0) {
            return;
        }

        void confirm({
            title: 'Anlagensätze',
            width: 'md',
            hideCancelButton: true,
            confirmButtonText: 'Schließen',
            children: (
                <ProcessInstanceAttachmentSetList
                    items={processInstanceAttachmentSetItems}
                    title={null}
                    onDownload={(attachment) => {
                        void handleDownloadAttachment(attachment);
                    }}
                />
            ),
        });
    }, [confirm, handleDownloadAttachment, processInstanceAttachmentSetItems]);

    const selectedNode = useMemo(() => {
        if (processFlow == null) {
            return null;
        }

        const selectedNodeIdRaw = params.nodeId;
        if (selectedNodeIdRaw == null) {
            return null;
        }

        const selectedNodeId = parseInt(selectedNodeIdRaw, 10);
        if (Number.isNaN(selectedNodeId)) {
            return null;
        }

        return processFlow.nodes.find((node) => node.id === selectedNodeId) ?? null;
    }, [params.nodeId, processFlow]);

    const handleCollapseEditorPane = useCallback((): void => {
        setLastExpandedEditorPaneWidth((previousWidth) => Math.max(previousWidth, editorPaneWidth, MIN_EDITOR_DRAWER_WIDTH_PX));
        setEditorPaneWidth(0);
        setIsEditorPaneCollapsed(true);
    }, [editorPaneWidth]);

    const handleExpandEditorPane = useCallback((): void => {
        setEditorPaneWidth((currentWidth) => currentWidth > 0 ? currentWidth : lastExpandedEditorPaneWidth);
        setIsEditorPaneCollapsed(false);
    }, [lastExpandedEditorPaneWidth]);

    const handleEditorPaneDragEnd = useCallback((sizes: number[]): void => {
        const nextEditorPaneWidth = sizes[1] ?? MIN_EDITOR_DRAWER_WIDTH_PX;
        if (nextEditorPaneWidth <= 0) {
            return;
        }

        setHideEditorPaneExpandButton(false);
        setEditorPaneWidth(nextEditorPaneWidth);
        setLastExpandedEditorPaneWidth(nextEditorPaneWidth);
    }, []);

    const handleSelectNode = useCallback((node: ProcessNodeEntity | null): void => {
        if (processFlow == null) {
            return;
        }

        if (node == null) {
            navigate(`/processes/${processFlow.definition.id}/versions/${processFlow.version.processVersion}?${searchParams.toString()}`);
            return;
        }

        handleExpandEditorPane();
        navigate(getProcessNodeEditURL(processFlow.definition.id, processFlow.version.processVersion, node.id, searchParams));
    }, [handleExpandEditorPane, navigate, processFlow, searchParams]);

    useEffect(() => {
        if (selectedNode == null) {
            return;
        }

        handleExpandEditorPane();
    }, [handleExpandEditorPane, selectedNode?.id]);

    // Load the process flow whenever the process id or version changes
    useEffect(() => {
        if (processId < 1 || processVersion < 1 || processVersionParam === 'latest') {
            setProcessFlow(null);
            setIsLoadingProcessFlow(processVersionParam === 'latest');
            return;
        }

        let cancelled = false;
        setIsLoadingProcessFlow(true);

        Promise.all([
            new ProcessDefinitionApiService().retrieve(processId),
            new ProcessDefinitionVersionApiService().retrieve({
                processDefinitionId: processId,
                processDefinitionVersion: processVersion,
            }),
            new ProcessNodeApiService().listAll({
                processId: processId,
                processVersion: processVersion,
            }),
            new ProcessDefinitionEdgeApiService().listAll({
                processDefinitionId: processId,
                processDefinitionVersion: processVersion,
            }),
        ])
            .then(([definition, version, nodes, edges]) => {
                if (cancelled) {
                    return;
                }

                setProcessFlow({
                    definition,
                    version,
                    nodes: nodes.content,
                    edges: edges.content,
                });

                dispatch(addEntityHistoryItem({
                    link: `/processes/${processId}/versions/${processVersion}`,
                    title: `${definition.internalTitle} (Version ${processVersion})`,
                    type: ServerEntityType.Processes,
                }));
            })
            .catch((error) => {
                if (cancelled) {
                    return;
                }

                dispatch(showApiErrorSnackbar(error, 'Der Prozessfluss konnte nicht geladen werden.'));
            })
            .finally(() => {
                if (cancelled) {
                    return;
                }

                setIsLoadingProcessFlow(false);
            });

        new ProcessTestClaimApiService()
            .listAll({
                processId,
                processVersion,
            })
            .then(({content}) => {
                if (cancelled) {
                    return;
                }

                if (content.length > 0) {
                    const claim = content[0];
                    const claimOwnerUser = user?.id === claim.owningUserId ? user : null;

                    setCurrentTestClaim({
                        claim,
                        user: claimOwnerUser,
                    });

                    if (claimOwnerUser != null) {
                        return;
                    }

                    new UsersApiService()
                        .retrieve(claim.owningUserId)
                        .then((claimOwnerUser) => {
                            if (cancelled) {
                                return;
                            }

                            setCurrentTestClaim((previousClaim) => {
                                if (previousClaim?.claim.id !== claim.id) {
                                    return previousClaim;
                                }

                                return {
                                    claim,
                                    user: claimOwnerUser,
                                };
                            });
                        })
                        .catch(() => {
                            // Keep the claim visible even if the owning user cannot be resolved.
                        });
                } else {
                    setCurrentTestClaim(null);
                }
            })
            .catch((err) => {
                if (cancelled) {
                    return;
                }

                dispatch(showApiErrorSnackbar(err, 'Die Testansprüche konnten nicht geladen werden.'));
            });

        return () => {
            cancelled = true;
        };
    }, [dispatch, processId, processVersion, processVersionParam, user]);

    useEffect(() => {
        if (requiredFlowNodeProviders.length === 0) {
            setIsLoadingFlowNodeProviders(false);
            setHasFlowNodeProviderLoadError(false);
            return;
        }

        const missingProviderReferences = requiredFlowNodeProviders.filter((providerReference) => (
            flowNodeProviderCache[getProcessNodeProviderKey(providerReference.key, providerReference.version)] == null
        ));

        if (missingProviderReferences.length === 0) {
            setIsLoadingFlowNodeProviders(false);
            setHasFlowNodeProviderLoadError(false);
            return;
        }

        let cancelled = false;

        setIsLoadingFlowNodeProviders(true);
        setHasFlowNodeProviderLoadError(false);

        Promise.all(missingProviderReferences.map((providerReference) => (
            new ProcessNodeProviderApiService().getNodeProvider(providerReference.key, providerReference.version)
        )))
            .then((providers) => {
                if (cancelled) {
                    return;
                }

                setFlowNodeProviderCache((previousCache) => {
                    const nextCache = {
                        ...previousCache,
                    };

                    for (const provider of providers) {
                        nextCache[getProcessNodeProviderKey(provider.key, provider.majorVersion)] = provider;
                    }

                    return nextCache;
                });
            })
            .catch((error) => {
                if (cancelled) {
                    return;
                }

                setHasFlowNodeProviderLoadError(true);
                dispatch(showApiErrorSnackbar(error, 'Die für die Prozessansicht benötigten Prozesselementdefinitionen konnten nicht geladen werden.'));
            })
            .finally(() => {
                if (cancelled) {
                    return;
                }

                setIsLoadingFlowNodeProviders(false);
            });

        return () => {
            cancelled = true;
        };
    }, [dispatch, flowNodeProviderCache, requiredFlowNodeProviders]);

    useEffect(() => {
        if (isFlowEditorReady && flowEditorKey != null && readyFlowEditorKey !== flowEditorKey) {
            setReadyFlowEditorKey(flowEditorKey);
        }
    }, [flowEditorKey, isFlowEditorReady, readyFlowEditorKey]);

    const loadRuntimeData = useCallback(() => {
        if (instanceId == null) {
            setRuntimeData(null);
            return Promise.resolve();
        }

        dispatch(setLoadingMessage({
            message: 'Lade Laufzeitdaten',
            blocking: false,
            estimatedTime: 1000,
        }));

        setIsRefreshingRuntimeData(true);
        new ProcessInstanceApiService()
            .retrieve(instanceId)
            .then((instance) => {
                return Promise.all([
                    Promise.resolve(instance),
                    new ProcessInstanceTaskApiService().listAll({
                        processInstanceId: instanceId,
                    }),
                    new ProcessInstanceAttachmentApiService().listAll({
                        processInstanceId: instanceId,
                    }),
                    new ProcessInstanceAttachmentSetApiService().listAll({
                        processInstanceId: instanceId,
                    }),
                    /* new ProcessInstanceEventApiService().listAll({
                        processInstanceId: instanceId,
                    })*/Promise.resolve([]),
                ]);
            })
            .then(([instance, tasks, attachments, attachmentSets, events]) => {
                setRuntimeData({
                    instance,
                    tasks: tasks.content,
                    attachments: attachments.content,
                    attachmentSets: attachmentSets.content,
                    events,
                });
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die Prozessinstanz konnte nicht geladen werden.'));
            })
            .finally(() => {
                setIsRefreshingRuntimeData(false);
                dispatch(clearLoadingMessage());
            });
    }, [instanceId, dispatch]);

    const handleSelectProcessTestClaimInstance = useCallback((selectedInstanceId: number): void => {
        setShowProcessTestClaimInstancesDialog(false);

        if (selectedInstanceId === instanceId) {
            void loadRuntimeData();
            return;
        }

        const nextSearchParams = new URLSearchParams(searchParams);
        nextSearchParams.set('instanceId', selectedInstanceId.toString());
        setSearchParams(nextSearchParams);
    }, [instanceId, loadRuntimeData, searchParams, setSearchParams]);

    useEffect(() => {
        void loadRuntimeData();
    }, [loadRuntimeData]);

    useEffect(() => {
        if (Object.keys(showProcessNodeProblemsForNodes).length > 0) {
            return;
        }

        if (state !== SHOW_ERRORS_ROUTER_STATE) {
            return;
        }

        if (processFlow == null) {
            return;
        }

        setShowProcessNodeProblemsForNodes(processFlow.nodes.reduce((acc, n) => ({
            ...acc,
            [n.id]: true,
        }), {}));
    }, [state, processFlow?.nodes]);

    const handleAddFlowTrigger = (nodeProvider: ProcessNodeProvider): void => {
        if (processFlow == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Füge Auslöser hinzu',
            blocking: false,
            estimatedTime: 1000,
        }));

        setShowAddTriggerDialog(false);

        new ProcessNodeApiService()
            .create({
                ...ProcessNodeApiService.initialize(),
                processId: processFlow.definition.id,
                processVersion: processFlow.version.processVersion,
                processNodeDefinitionKey: nodeProvider.key,
                processNodeDefinitionVersion: nodeProvider.majorVersion,
            })
            .then((newNode) => {
                setProcessFlow((prevProcess) => {
                    if (prevProcess == null) {
                        return prevProcess;
                    }

                    return {
                        ...prevProcess,
                        nodes: [
                            ...prevProcess.nodes,
                            newNode,
                        ],
                    };
                });
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Der Auslöser konnte nicht hinzugefügt werden.'));
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });
    };

    const handleAddImportedTriggerNode = useCallback((importedNode: ProcessNodeEntity): void => {
        setProcessFlow((prevProcess) => {
            if (prevProcess == null) {
                return prevProcess;
            }

            return {
                ...prevProcess,
                nodes: [
                    ...prevProcess.nodes,
                    importedNode,
                ],
            };
        });
        setShowAddTriggerDialog(false);
    }, []);

    const handleAddFollowUpNode = async (nodeProvider: ProcessNodeProvider): Promise<void> => {
        if (processFlow == null) {
            return;
        }

        if (newNodeFor == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Füge Prozesselement hinzu',
            blocking: false,
            estimatedTime: 1000,
        }));

        const existingEdge = processFlow.edges.find((edge) => (
            edge.fromNodeId === newNodeFor.fromNodeId &&
            edge.viaPort === newNodeFor.viaPort
        ));

        const newNode = await new ProcessNodeApiService()
            .create({
                ...ProcessNodeApiService.initialize(),
                processId: processFlow.definition.id,
                processVersion: processFlow.version.processVersion,
                processNodeDefinitionKey: nodeProvider.key,
                processNodeDefinitionVersion: nodeProvider.majorVersion,
            });

        const edgeApi = new ProcessDefinitionEdgeApiService();

        if (existingEdge != null) {
            if (nodeProvider.ports.length === 0) {
                dispatch(addSnackbarMessage({
                    key: 'process-follow-up-node-missing-port',
                    type: SnackbarType.AutoHiding,
                    severity: SnackbarSeverity.Warning,
                    message: 'Dieser Prozesselementtyp kann hier nicht eingefügt werden, da er keinen Ausgangsport besitzt.',
                }));
                dispatch(clearLoadingMessage());
                return;
            }

            await edgeApi.destroy(existingEdge.id);
        }

        const newEdge = await edgeApi
            .create({
                id: 0,
                processId: processFlow.definition.id,
                processVersion: processFlow.version.processVersion,
                fromNodeId: newNodeFor.fromNodeId,
                toNodeId: newNode.id,
                viaPort: newNodeFor.viaPort,
            });

        let newlyCreatedFollowUpEdge: ProcessDefinitionEdgeEntity | null = null;
        if (existingEdge != null) {
            newlyCreatedFollowUpEdge = await new ProcessDefinitionEdgeApiService()
                .create({
                    ...existingEdge,
                    id: 0,
                    fromNodeId: newNode.id,
                    viaPort: nodeProvider.ports[0].key,
                });
        }

        setProcessFlow({
            ...processFlow,
            nodes: [
                ...processFlow.nodes,
                newNode,
            ],
            edges: [
                ...processFlow.edges.filter((edge) => edge.id !== existingEdge?.id),
                newEdge,
                ...(newlyCreatedFollowUpEdge != null ? [newlyCreatedFollowUpEdge] : []),
            ],
        });

        dispatch(clearLoadingMessage());
    };

    const handleAddImportedFollowUpNode = useCallback(async (
        importedNode: ProcessNodeEntity,
        importedProvider: ProcessNodeProvider,
    ): Promise<void> => {
        if (processFlow == null || newNodeFor == null) {
            return;
        }

        const existingEdge = processFlow.edges.find((edge) => (
            edge.fromNodeId === newNodeFor.fromNodeId &&
            edge.viaPort === newNodeFor.viaPort
        ));

        const edgeApi = new ProcessDefinitionEdgeApiService();

        if (existingEdge != null) {
            if (importedProvider.ports.length === 0) {
                throw new Error('imported-follow-up-node-missing-port');
            }

            await edgeApi.destroy(existingEdge.id);
        }

        const newEdge = await edgeApi.create({
            id: 0,
            processId: processFlow.definition.id,
            processVersion: processFlow.version.processVersion,
            fromNodeId: newNodeFor.fromNodeId,
            toNodeId: importedNode.id,
            viaPort: newNodeFor.viaPort,
        });

        let newlyCreatedFollowUpEdge: ProcessDefinitionEdgeEntity | null = null;
        if (existingEdge != null) {
            newlyCreatedFollowUpEdge = await edgeApi.create({
                ...existingEdge,
                id: 0,
                fromNodeId: importedNode.id,
                viaPort: importedProvider.ports[0].key,
            });
        }

        setProcessFlow({
            ...processFlow,
            nodes: [
                ...processFlow.nodes,
                importedNode,
            ],
            edges: [
                ...processFlow.edges.filter((edge) => edge.id !== existingEdge?.id),
                newEdge,
                ...(newlyCreatedFollowUpEdge != null ? [newlyCreatedFollowUpEdge] : []),
            ],
        });

        setNewNodeFor(null);
    }, [newNodeFor, processFlow]);

    const handleAddInbetweenNode = async (nodeProvider: ProcessNodeProvider): Promise<void> => {
        if (processFlow == null) {
            return;
        }

        if (newNodeOnEdgeId == null) {
            return;
        }

        const existingEdge = processFlow
            .edges
            .find((edge) => edge.id === newNodeOnEdgeId);

        if (existingEdge == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Füge Prozesselement hinzu',
            blocking: false,
            estimatedTime: 1000,
        }));

        if (nodeProvider.ports.length === 0) {
            dispatch(addSnackbarMessage({
                key: 'process-inbetween-node-missing-port',
                type: SnackbarType.AutoHiding,
                severity: SnackbarSeverity.Warning,
                message: 'Dieser Prozesselementtyp kann hier nicht eingefügt werden, da er keinen Ausgangsport besitzt.',
            }));
            dispatch(clearLoadingMessage());
            return;
        }

        const edgeApi = new ProcessDefinitionEdgeApiService();

        await edgeApi.destroy(existingEdge.id);

        const newNode = await new ProcessNodeApiService()
            .create({
                ...ProcessNodeApiService.initialize(),
                processId: processFlow.definition.id,
                processVersion: processFlow.version.processVersion,
                processNodeDefinitionKey: nodeProvider.key,
                processNodeDefinitionVersion: nodeProvider.majorVersion,
            });

        const newEdgeToNewNode = await edgeApi
            .create({
                id: 0,
                processId: processFlow.definition.id,
                processVersion: processFlow.version.processVersion,
                fromNodeId: existingEdge.fromNodeId,
                toNodeId: newNode.id,
                viaPort: existingEdge.viaPort,
            });

        const newEdgeFromNewNode = await edgeApi
            .create({
                id: 0,
                processId: processFlow.definition.id,
                processVersion: processFlow.version.processVersion,
                fromNodeId: newNode.id,
                toNodeId: existingEdge.toNodeId,
                viaPort: nodeProvider.ports[0].key,
            });

        setProcessFlow({
            ...processFlow,
            nodes: [
                ...processFlow.nodes,
                newNode,
            ],
            edges: [
                ...processFlow.edges.filter((edge) => edge.id !== existingEdge.id),
                newEdgeToNewNode,
                newEdgeFromNewNode,
            ],
        });

        setNewNodeOnEdgeId(null);

        dispatch(clearLoadingMessage());
    };

    const handleAddImportedInbetweenNode = useCallback(async (
        importedNode: ProcessNodeEntity,
        importedProvider: ProcessNodeProvider,
    ): Promise<void> => {
        if (processFlow == null || newNodeOnEdgeId == null) {
            return;
        }

        const existingEdge = processFlow
            .edges
            .find((edge) => edge.id === newNodeOnEdgeId);

        if (existingEdge == null) {
            return;
        }

        if (importedProvider.ports.length === 0) {
            throw new Error('imported-inbetween-node-missing-port');
        }

        const edgeApi = new ProcessDefinitionEdgeApiService();

        await edgeApi.destroy(existingEdge.id);

        const newEdgeToNewNode = await edgeApi.create({
            id: 0,
            processId: processFlow.definition.id,
            processVersion: processFlow.version.processVersion,
            fromNodeId: existingEdge.fromNodeId,
            toNodeId: importedNode.id,
            viaPort: existingEdge.viaPort,
        });

        const newEdgeFromNewNode = await edgeApi.create({
            id: 0,
            processId: processFlow.definition.id,
            processVersion: processFlow.version.processVersion,
            fromNodeId: importedNode.id,
            toNodeId: existingEdge.toNodeId,
            viaPort: importedProvider.ports[0].key,
        });

        setProcessFlow({
            ...processFlow,
            nodes: [
                ...processFlow.nodes,
                importedNode,
            ],
            edges: [
                ...processFlow.edges.filter((edge) => edge.id !== existingEdge.id),
                newEdgeToNewNode,
                newEdgeFromNewNode,
            ],
        });

        setNewNodeOnEdgeId(null);
    }, [newNodeOnEdgeId, processFlow]);

    const handleDeleteNode = async (node: ProcessNodeEntity): Promise<void> => {
        if (processFlow == null) {
            return;
        }

        const nodeProvider = flowNodeProviderCache[getProcessNodeProviderKey(
            node.processNodeDefinitionKey,
            node.processNodeDefinitionVersion,
        )];
        const incomingEdges = processFlow.edges.filter((edge) => edge.toNodeId === node.id);
        const outgoingEdges = processFlow.edges.filter((edge) => edge.fromNodeId === node.id);
        const edgesToRemove = processFlow.edges.filter((edge) => (
            edge.fromNodeId === node.id ||
            edge.toNodeId === node.id
        ));
        const remainingEdges = processFlow.edges.filter((edge) => (
            !edgesToRemove.some((edgeToRemove) => edgeToRemove.id === edge.id)
        ));
        const bridgeTargetEdge = nodeProvider?.ports.length === 1 && outgoingEdges.length === 1
            ? outgoingEdges[0]
            : null;
        const bridgeEdgePayloads = bridgeTargetEdge == null
            ? []
            : incomingEdges
                .map((incomingEdge) => ({
                    id: 0,
                    processId: processFlow.definition.id,
                    processVersion: processFlow.version.processVersion,
                    fromNodeId: incomingEdge.fromNodeId,
                    toNodeId: bridgeTargetEdge.toNodeId,
                    viaPort: incomingEdge.viaPort,
                }))
                .filter((payload, index, payloads) => (
                    payload.fromNodeId !== node.id &&
                    payload.toNodeId !== node.id &&
                    !remainingEdges.some((edge) => (
                        edge.fromNodeId === payload.fromNodeId &&
                        edge.toNodeId === payload.toNodeId &&
                        edge.viaPort === payload.viaPort
                    )) &&
                    payloads.findIndex((candidate) => (
                        candidate.fromNodeId === payload.fromNodeId &&
                        candidate.toNodeId === payload.toNodeId &&
                        candidate.viaPort === payload.viaPort
                    )) === index
                ));

        const edgeApi = new ProcessDefinitionEdgeApiService();

        await Promise
            .all(edgesToRemove.map((e) => edgeApi.destroy(e.id)));

        await new ProcessNodeApiService()
            .destroy(node.id);

        const createdBridgeEdges = await Promise.all(
            bridgeEdgePayloads.map((payload) => edgeApi.create(payload)),
        );

        setProcessFlow({
            ...processFlow,
            nodes: processFlow.nodes.filter((n) => n.id !== node.id),
            edges: [
                ...remainingEdges,
                ...createdBridgeEdges,
            ],
        });

        if (selectedNode?.id === node.id) {
            await navigate(
                `/processes/${processFlow.definition.id}/versions/${processFlow.version.processVersion}`,
                {
                    state: {
                        [PROCESS_NODE_EDITOR_SKIP_CHANGE_BLOCKER_STATE_KEY]: true,
                    },
                },
            );
        }
    };

    const handleSaveNode = async (node: ProcessNodeEntity, options?: RequestOptions): Promise<ProcessNodeEntity> => {
        if (processFlow == null) {
            throw new Error('Process flow is not loaded');
        }

        const updated = await new ProcessNodeApiService()
            .update(node.id, node, options);

        setProcessFlow({
            ...processFlow,
            nodes: processFlow.nodes.map((n) => n.id === updated.id ? updated : n),
        });

        setShowProcessNodeProblemsForNodes((prev) => ({
            ...prev,
            [updated.id]: true,
        }));

        return updated;
    };

    const handleExport = (): void => {
        showProcessExport(processId, processVersion);
    };

    const handleTest = async (): Promise<void> => {
        if (currentTestClaim != null) {
            confirm({
                title: 'Testmodus bereits aktiv',
                children: (
                    <Typography>
                        Der Prozess befindet sich bereits im Testmodus.
                        Sie müssen den aktuellen Testmodus beenden, bevor Sie einen neuen Starten können.
                    </Typography>
                ),
                confirmButtonText: 'Ok',
                hideCancelButton: true,
            });
            return;
        }

        try {
            const problems = await new ProcessDefinitionVersionApiService()
                .validate({
                    processDefinitionId: processId,
                    processDefinitionVersion: processVersion,
                });
            setProcessNodeProblems(problems);
            setShowProcessNodeProblemsForNodes(problems.reduce((acc, prob) => ({
                ...acc,
                [prob.node.id]: true,
            }), {}));

            const confirmed = await confirm({
                title: 'Prozessmodellierung testen',
                children: (
                    <>
                        <Typography>
                            Möchten Sie die Prozessmodellierung testen?
                            Die Prozessversion kann während des Tests nicht veröffentlicht werden.
                            Sie können den Test jederzeit abbrechen.
                            Alle gestarteten Vorgänge werden dabei beendet und gelöscht.
                        </Typography>
                        {
                            problems.length > 0 &&
                            <NodeProblemsAlert
                                problems={problems}
                                availableNodeProviders={availableNodeProviders}
                                mode="test"
                                sx={{
                                    marginTop: '1rem',
                                }}
                            />
                        }
                    </>
                ),
                confirmButtonText: problems.length > 0 ? 'Test trotzdem starten' : 'Test starten',
            });

            if (!confirmed) {
                return;
            }

            const res = await new ProcessTestClaimApiService()
                .create({
                    ...ProcessTestClaimApiService.initialize(),
                    processId,
                    processVersion,
                });

            if (res == null || user == null) {
                return;
            }

            try {
                // Starting a test claim creates runtime instances that can affect process-instance permissions.
                await refreshPermissionSet({broadcast: true});
            } catch (refreshError) {
                dispatch(showApiErrorSnackbar(refreshError, 'Die Berechtigungen konnten nach dem Start des Tests nicht aktualisiert werden.'));
            }

            setCurrentTestClaim({
                claim: res,
                user,
            });

            dispatch(showSuccessSnackbar('Der Test wurde gestartet.'));
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Der Test konnte nicht gestartet werden.'));
        }
    };

    const handleEndTestClaim = useCallback((): void => {
        if (currentTestClaim == null) {
            return;
        }

        confirm({
            title: 'Testanspruch löschen',
            children: (
                <Typography>
                    Möchten Sie den Testanspruch wirklich löschen? Dadurch wird der
                    Test für diesen Prozess sofort beendet und die Bearbeitung des
                    Prozesses wieder freigegeben.
                    Alle gestarteten Vorgänge werden dabei beendet und gelöscht.
                </Typography>
            ),
            confirmButtonText: 'Testanspruch löschen',
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return;
                }

                return new ProcessTestClaimApiService()
                    .destroy(currentTestClaim.claim.id)
                    .then(() => {
                        // Removing the claim also removes its runtime access, so refresh before updating the local UI state.
                        return refreshPermissionSet({broadcast: true})
                            .catch((refreshError) => {
                                dispatch(showApiErrorSnackbar(refreshError, 'Die Berechtigungen konnten nach dem Löschen des Testanspruchs nicht aktualisiert werden.'));
                            });
                    })
                    .then(() => {
                        setCurrentTestClaim(null);
                        setRuntimeData(null);
                        setShowProcessTestClaimInstancesDialog(false);
                        dispatch(showSuccessSnackbar('Testanspruch wurde gelöscht.'));

                        if (instanceId != null) {
                            const nextSearchParams = new URLSearchParams(searchParams);
                            nextSearchParams.delete('instanceId');
                            setSearchParams(nextSearchParams);
                        }
                    });
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Testanspruch konnte nicht gelöscht werden.'));
            });
    }, [confirm, currentTestClaim, dispatch, instanceId, refreshPermissionSet, searchParams, setSearchParams]);

    const handleDeleteProcess = useCallback((): void => {
        if (processFlow == null) {
            return;
        }

        const processToDelete = processFlow.definition;
        confirm({
            title: 'Prozess löschen',
            children: (
                <Typography>
                    Möchten Sie den Prozess wirklich löschen?
                    Alle zugehörigen Versionen, Modellierungen und Vorgänge werden dabei entfernt.
                    Dieser Vorgang kann nicht rückgängig gemacht werden.
                </Typography>
            ),
            confirmationText: processToDelete.internalTitle,
            inputLabel: 'Interner Titel zur Bestätigung',
            inputPlaceholder: processToDelete.internalTitle,
            confirmButtonText: 'Prozess endgültig löschen',
            isDestructive: true,
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return;
                }

                dispatch(setLoadingMessage({
                    message: 'Lösche Prozess',
                    blocking: false,
                    estimatedTime: 1200,
                }));

                return new ProcessDefinitionApiService()
                    .destroy(processToDelete.id)
                    .then(() => {
                        dispatch(showSuccessSnackbar('Der Prozess wurde erfolgreich gelöscht.'));
                        navigate('/processes', {
                            replace: true,
                        });
                    })
                    .catch((error) => {
                        dispatch(showApiErrorSnackbar(error, 'Der Prozess konnte nicht gelöscht werden.'));
                    })
                    .finally(() => {
                        dispatch(clearLoadingMessage());
                    });
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Der Löschdialog konnte nicht geöffnet werden.'));
            });
    }, [confirm, dispatch, navigate, processFlow]);

    const handleMenuEvent = (event: ProcessDetailsPageMoreMenuEvent): void => {
        switch (event) {
            case 'export':
                handleExport();
                break;
            case 'test':
                handleTest();
                break;
            case 'instances':
                navigate(`/process-instances?processId=${processFlow?.definition.id}&processVersion=${processFlow?.version.processVersion}`);
                break;
            case 'delete':
                handleDeleteProcess();
                break;
            default:
                notImplemented();
                break;
        }
    };

    const handleOpenAddTriggerDialog = useCallback(() => {
        setShowAddTriggerDialog(true);
    }, []);
    const handleOpenReplaceNodeDialog = useCallback((node: ProcessNodeEntity): void => {
        setReplaceNodeRequest({
            nodeId: node.id,
        });
    }, []);
    const handleCloneNode = useCallback((node: ProcessNodeEntity): void => {
        dispatch(setLoadingMessage({
            blocking: false,
            message: 'Dupliziere Prozesselement',
            estimatedTime: 1200,
        }));

        new ProcessNodeApiService()
            .create({
                ...node,
                dataKey: generateId(5),
            })
            .then((createdNode) => {
                setProcessFlow((flow) => flow != null ? ({
                    ...flow,
                    nodes: [
                        ...flow.nodes,
                        createdNode,
                    ],
                }) : null);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Das Prozesselement konnte nicht dupliziert werden.'));
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });
    }, []);
    const handleImportNode = useCallback(async (context: NodeImportContext): Promise<void> => {
        if (processFlow == null) {
            return;
        }

        try {
            const importedNodeExport = await uploadObjectFile<ProcessNodeExport>('application/json');
            if (importedNodeExport == null) {
                return;
            }

            const importedProvider = getNodeProviderFromList(
                availableNodeProviders,
                importedNodeExport.node.processNodeDefinitionKey,
                importedNodeExport.node.processNodeDefinitionVersion,
            );
            if (importedProvider == null) {
                dispatch(showErrorSnackbar('Die Prozesselementdefinition aus dem Import ist in dieser Instanz nicht verfügbar.'));
                return;
            }

            if (context === 'trigger' && importedProvider.type !== ProcessNodeType.Trigger) {
                dispatch(showErrorSnackbar('In diesem Dialog können nur importierte Auslöser eingefügt werden.'));
                return;
            }

            if (context !== 'trigger' && importedProvider.type === ProcessNodeType.Trigger) {
                dispatch(showErrorSnackbar('Importierte Auslöser können hier nicht eingefügt werden.'));
                return;
            }

            if (context === 'follow-up' && newNodeFor != null) {
                const requiresOutgoingPort = processFlow.edges.some((edge) => (
                    edge.fromNodeId === newNodeFor.fromNodeId &&
                    edge.viaPort === newNodeFor.viaPort
                ));
                if (requiresOutgoingPort && importedProvider.ports.length === 0) {
                    dispatch(showErrorSnackbar('Dieser importierte Prozesselementtyp kann hier nicht eingefügt werden, da er keinen Ausgangsport besitzt.'));
                    return;
                }
            }

            if (context === 'in-between' && importedProvider.ports.length === 0) {
                dispatch(showErrorSnackbar('Dieser importierte Prozesselementtyp kann hier nicht eingefügt werden, da er keinen Ausgangsport besitzt.'));
                return;
            }

            if (!canPlaceProcessNodeProvider(importedProvider, processFlow, flowNodeProviderCache)) {
                dispatch(showErrorSnackbar(getUnavailableProcessNodeProviderMessage(importedProvider)));
                return;
            }

            dispatch(setLoadingMessage({
                message: 'Importiere Prozesselement',
                blocking: false,
                estimatedTime: 1000,
            }));

            const importedNode = await new ProcessNodeApiService()
                .import(processFlow.definition.id, processFlow.version.processVersion, importedNodeExport);

            setFlowNodeProviderCache((previousCache) => ({
                ...previousCache,
                [getProcessNodeProviderKey(importedProvider.key, importedProvider.majorVersion)]: importedProvider,
            }));

            if (context === 'trigger') {
                handleAddImportedTriggerNode(importedNode);
            } else if (context === 'follow-up') {
                await handleAddImportedFollowUpNode(importedNode, importedProvider);
            } else {
                await handleAddImportedInbetweenNode(importedNode, importedProvider);
            }

            dispatch(showSuccessSnackbar('Das Prozesselement wurde importiert.'));
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Das Prozesselement konnte nicht importiert werden.'));
        } finally {
            dispatch(clearLoadingMessage());
        }
    }, [
        availableNodeProviders,
        dispatch,
        flowNodeProviderCache,
        handleAddImportedFollowUpNode,
        handleAddImportedInbetweenNode,
        handleAddImportedTriggerNode,
        newNodeFor,
        processFlow,
    ]);
    const handleCreateEdge = useCallback((fromNodeId: number, toNodeId: number, viaPortKey: string): void => {
        if (processFlow == null) {
            return;
        }

        new ProcessDefinitionEdgeApiService()
            .create({
                id: 0,
                processId: processFlow.definition.id,
                processVersion: processFlow.version.processVersion,
                fromNodeId,
                toNodeId,
                viaPort: viaPortKey,
            })
            .then((newEdge) => {
                setProcessFlow((prevProcess) => {
                    if (prevProcess == null) {
                        return prevProcess;
                    }

                    return {
                        ...prevProcess,
                        edges: [
                            ...prevProcess.edges,
                            newEdge,
                        ],
                    };
                });
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Verbindung konnte nicht erstellt werden.'));
            });
    }, [dispatch, processFlow]);
    const handleReplaceNode = useCallback(async (node: ProcessNodeEntity, replacementProvider: ProcessNodeProvider): Promise<void> => {
        if (processFlow == null) {
            return;
        }

        const currentProvider = flowNodeProviderCache[getProcessNodeProviderKey(
            node.processNodeDefinitionKey,
            node.processNodeDefinitionVersion,
        )];
        if (currentProvider == null) {
            dispatch(showErrorSnackbar('Der aktuelle Prozesselementtyp konnte nicht aufgelöst werden.'));
            return;
        }

        if (!canReplaceNodeType(currentProvider.type, replacementProvider.type)) {
            dispatch(showErrorSnackbar('Auslöser und Endelemente können nur durch denselben Prozesselementtyp ersetzt werden.'));
            return;
        }

        const replacementPlan = buildNodeReplacementPlan(processFlow, node, currentProvider, replacementProvider);
        const preservedOutgoingEdgeCount = replacementPlan.unchangedOutgoingEdges.length + replacementPlan.recreatedOutgoingEdges.length;
        const removedOutgoingEdgeCount = replacementPlan.removedOutgoingEdges.length;
        const outgoingConnectionSummary = formatOutgoingConnectionSummary(
            preservedOutgoingEdgeCount,
            removedOutgoingEdgeCount,
        );

        const confirmed = await confirm({
            title: 'Prozesselement ersetzen',
            confirmButtonText: 'Ersetzen',
            children: (
                <Box sx={{display: 'flex', flexDirection: 'column', gap: 1}}>
                    <Typography>
                        Möchten Sie das Prozesselement <strong>{getNodeName(node, currentProvider)}</strong> wirklich
                        durch <strong>{replacementProvider.name}</strong> ersetzen?
                    </Typography>
                    <Typography>
                        Name, Kurzbeschreibung und Datenschlüssel bleiben erhalten.
                        Sämtliche Konfiguration und Zuweisungen werden zurückgesetzt.
                    </Typography>
                    <Typography>
                        Bestehende eingehende Verbindungen bleiben erhalten. {outgoingConnectionSummary}
                    </Typography>
                </Box>
            ),
        });

        if (!confirmed) {
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Ersetze Prozesselement',
            blocking: false,
            estimatedTime: 1000,
        }));

        const edgeApi = new ProcessDefinitionEdgeApiService();
        const nodeApi = new ProcessNodeApiService();
        // Recreated edges are removed before the node update and created afterwards. That avoids
        // temporarily persisting an edge whose `viaPort` no longer exists on the current node type.
        const edgesToRemove = [
            ...replacementPlan.removedOutgoingEdges,
            ...replacementPlan.recreatedOutgoingEdges.map((edgePlan) => edgePlan.originalEdge),
        ];

        try {
            await Promise.all(edgesToRemove.map((edge) => edgeApi.destroy(edge.id)));

            const updatedNode = await nodeApi.update(node.id, replacementPlan.replacementNode);
            const recreatedEdges = await Promise.all(
                replacementPlan.recreatedOutgoingEdges.map((edgePlan) => edgeApi.create(edgePlan.createPayload)),
            );

            setFlowNodeProviderCache((previousCache) => ({
                ...previousCache,
                [getProcessNodeProviderKey(replacementProvider.key, replacementProvider.majorVersion)]: replacementProvider,
            }));
            setProcessFlow((previousProcess) => {
                if (previousProcess == null) {
                    return previousProcess;
                }

                return {
                    ...previousProcess,
                    nodes: previousProcess.nodes.map((processNode) => processNode.id === updatedNode.id ? updatedNode : processNode),
                    edges: [
                        ...previousProcess.edges.filter((edge) => !edgesToRemove.some((edgeToRemove) => edgeToRemove.id === edge.id)),
                        ...recreatedEdges,
                    ],
                };
            });
            setNodeRefreshSignal((previousSignal) => ({
                nodeId: node.id,
                version: previousSignal.version + 1,
            }));

            dispatch(showSuccessSnackbar('Das Prozesselement wurde ersetzt.'));
            if (removedOutgoingEdgeCount > 0) {
                dispatch(addSnackbarMessage({
                    key: `process-node-replaced-removed-edges-${node.id}-${replacementProvider.key}-${replacementProvider.majorVersion}`,
                    type: SnackbarType.AutoHiding,
                    severity: SnackbarSeverity.Warning,
                    message: formatRemovedOutgoingConnectionsMessage(removedOutgoingEdgeCount),
                }));
            }
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Das Prozesselement konnte nicht ersetzt werden.'));
        } finally {
            dispatch(clearLoadingMessage());
        }
    }, [confirm, dispatch, flowNodeProviderCache, processFlow]);
    const headerActions = useMemo<Action[]>(() => {
        const isInTestMode = currentTestClaim != null;
        const testClaimInstanceActions: Action[] = activeTestClaimId == null ? [] : [
            {
                tooltip: 'Test-Vorgänge anzeigen',
                ariaLabel: 'Test-Vorgänge anzeigen',
                icon: ModuleIcons.submissions,
                onClick: () => {
                    setShowProcessTestClaimInstancesDialog(true);
                },
            },
            'separator' as const,
        ];
        const runtimeActions: Action[] = instanceId == null ? [] : [
            {
                tooltip: 'Laufzeitdaten neu laden',
                ariaLabel: 'Laufzeitdaten neu laden',
                icon: <Refresh/>,
                onClick: () => {
                    void loadRuntimeData();
                },
                disabled: isRefreshingRuntimeData,
            },
            {
                tooltip: 'Vorgangsereignisse anzeigen',
                ariaLabel: 'Vorgangsereignisse anzeigen',
                icon: <News/>,
                onClick: () => {
                    setShowProcessInstanceEventsDialog(true);
                },
                disabled: runtimeData == null,
            },
            {
                tooltip: 'Anlagensätze anzeigen',
                ariaLabel: 'Anlagensätze anzeigen',
                icon: <AttachFile/>,
                onClick: handleOpenAttachmentSetsDialog,
                visible: processInstanceAttachmentSetItems.length > 0,
            },
            'separator' as const,
        ];

        return [
            ...testClaimInstanceActions,
            ...runtimeActions,
            ...(!isInTestMode ? [
                {
                    tooltip: 'Rückgängig',
                    ariaLabel: 'Rückgängig',
                    icon: <Undo/>,
                    onClick: notImplemented,
                },
                {
                    tooltip: 'Wiederholen',
                    ariaLabel: 'Wiederholen',
                    icon: <Redo/>,
                    onClick: notImplemented,
                    disabled: true,
                },
                'separator' as const,
            ] : []),
            {
                tooltip: 'Versionen',
                ariaLabel: 'Versionen',
                icon: <HomeStorage/>,
                onClick: () => {
                    setShowVersionsDialog(true);
                },
            },
            {
                tooltip: 'Einstellungen',
                ariaLabel: 'Einstellungen',
                icon: <Settings/>,
                onClick: () => {
                    setShowSettingsDialog(true);
                    setShowSettingsDialog(true);
                },
            },
            {
                tooltip: 'Weitere Optionen',
                ariaLabel: 'Weitere Optionen',
                icon: <MoreVert/>,
                onClick: (event) => {
                    setShowMenuAtEl(event.currentTarget as HTMLElement);
                },
            },
            'separator',
            {
                label: 'Veröffentlichen',
                tooltip: 'Prozessversion veröffentlichen',
                disabledTooltip: 'Vor der Veröffentlichung muss der laufende Test beendet werden.',
                icon: null,
                onClick: () => {
                    setShowPublishDialog(true);
                },
                variant: 'contained',
                disabled: processFlow == null || isInTestMode,
                visible: processFlow?.version.status === ProcessStatus.Drafted || processFlow?.version.status === ProcessStatus.Revoked,
                activeStyle: {
                    ml: 1,
                },
            },
            {
                label: 'Zurückziehen',
                tooltip: 'Prozessversion zurückziehen',
                disabledTooltip: 'Vor dem Zurückziehen muss der laufende Test beendet werden.',
                icon: null,
                onClick: () => {
                    if (processFlow == null) {
                        return;
                    }
                    new ProcessDefinitionVersionApiService()
                        .revoke({
                            processDefinitionId: processFlow.definition.id,
                            processDefinitionVersion: processFlow.version.processVersion,
                        })
                        .then((updatedVersion) => {
                            setProcessFlow({
                                ...processFlow,
                                version: updatedVersion,
                                definition: {
                                    ...processFlow.definition,
                                    publishedVersion: null,
                                },
                            });
                            dispatch(showSuccessSnackbar('Die Prozessversion wurde zurückgezogen.'));
                        })
                        .catch((err) => {
                            dispatch(showApiErrorSnackbar(err, 'Die Prozessversion konnte nicht zurückgezogen werden.'));
                        });
                },
                variant: 'contained',
                disabled: processFlow == null || isInTestMode,
                visible: processFlow?.version.status === ProcessStatus.Published,
                activeStyle: {
                    ml: 1,
                },
            },
        ];
    }, [
        processFlow,
        activeTestClaimId,
        currentTestClaim,
        instanceId,
        isRefreshingRuntimeData,
        loadRuntimeData,
        handleOpenAttachmentSetsDialog,
        processInstanceAttachmentSetItems.length,
        runtimeData,
        notImplemented,
        handleDeleteProcess,
    ]);
    const currentTestClaimOwnerName = useMemo(() => {
        if (currentTestClaim == null) {
            return '';
        }

        if (currentTestClaim.user != null) {
            return resolveUserName(currentTestClaim.user);
        }

        if (currentTestClaim.claim.owningUserId === user?.id && user != null) {
            return resolveUserName(user);
        }

        return 'Unbekannte Mitarbeiter:in';
    }, [currentTestClaim, user]);
    const connectExistingNodeSource = useMemo(() => {
        if (processFlow == null || connectExistingNodeRequest == null) {
            return null;
        }

        return processFlow.nodes.find((node) => node.id === connectExistingNodeRequest.sourceNodeId) ?? null;
    }, [connectExistingNodeRequest, processFlow]);
    const replaceNodeSource = useMemo(() => {
        if (processFlow == null || replaceNodeRequest == null) {
            return null;
        }

        return processFlow.nodes.find((node) => node.id === replaceNodeRequest.nodeId) ?? null;
    }, [processFlow, replaceNodeRequest]);

    const handleAddDraft = useCallback((process: number, version?: number) => {
        dispatch(setLoadingMessage({
            message: 'Neue Version wird erzeugt',
            estimatedTime: 2000,
            blocking: true,
        }));

        return new ProcessDefinitionApiService()
            .addNewVersion(process, version)
            .then((createdVersion) => {
                navigate(`/processes/${createdVersion.processId}/versions/${createdVersion.processVersion}`);
                return createdVersion;
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Anlegen einer neuen Version'));
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });
    }, [dispatch, navigate]);

    if (processFlow == null) {
        if (showProcessDetailsPageSkeleton) {
            return <ProcessDetailsPageSkeleton/>;
        }

        return (
            <PageWrapper
                title="Prozess"
                fullWidth={true}
                fullHeight={true}
            >
                <Box
                    sx={{
                        height: '100vh',
                    }}
                />
            </PageWrapper>
        );
    }

    const isProcessEditable = processFlow.version.status === ProcessStatus.Drafted;
    const isProcessStructureEditable = isProcessEditable && currentTestClaim == null;

    return (
        <PageWrapper
            title="Prozess"
            fullWidth={true}
            fullHeight={true}
        >
            <Box
                sx={{
                    height: '100vh',
                    '--focus-border': (theme) => theme.palette.secondary.main,
                    position: 'relative',
                }}
            >
                <Allotment
                    onDragStart={() => setHideEditorPaneExpandButton(true)}
                    onDragEnd={handleEditorPaneDragEnd}
                >
                    <Allotment.Pane minSize={DISPLAYABLE_AREA - MIN_EDITOR_DRAWER_WIDTH_PX}>
                        <Box
                            sx={{
                                px: 2,
                                py: 2,
                                height: '100%',
                                display: 'flex',
                                flexDirection: 'column',
                            }}
                        >
                            <GenericPageHeader
                                title={'Prozess: ' + processFlow.definition.internalTitle}
                                badge={{
                                    color: 'default',
                                    label: `Version ${processFlow.version.processVersion}`,
                                }}
                                icon={ModuleIcons.processes}
                                actions={headerActions}
                            />

                            <Box
                                sx={{
                                    flex: 1,
                                    minHeight: 0,
                                    borderRadius: 1,
                                    mt: 2,
                                    mb: -2, // compensate for parent `py: 2`
                                    ml: -2, // compensate for parent `px: 2`
                                    mr: -2, // compensate for parent `px: 2`
                                }}
                            >
                                {
                                    isFlowEditorReady || shouldKeepFlowEditorMounted ?
                                        <ReactFlowProvider>
                                            <ProcessFlowEditor
                                                editable={isProcessStructureEditable}
                                                processFlow={processFlow}
                                                nodeProviders={flowNodeProviders}
                                                onDownloadAttachment={handleDownloadAttachment}
                                                onAddTrigger={handleOpenAddTriggerDialog}
                                                topLeftPanel={
                                                    currentTestClaim == null ? undefined : (
                                                        <Box
                                                            sx={{
                                                                display: 'flex',
                                                                flexDirection: 'column',
                                                                alignItems: 'flex-start',
                                                                gap: 0,
                                                            }}
                                                        >
                                                            <Box
                                                                sx={{
                                                                    width: '100%',
                                                                    display: 'flex',
                                                                    alignItems: 'center',
                                                                    gap: 0.75,
                                                                }}
                                                            >
                                                                <Box
                                                                    className="process-flow-editor-status-dot"
                                                                    sx={{
                                                                        width: 10,
                                                                        height: 10,
                                                                        borderRadius: '50%',
                                                                        color: 'warning.main',
                                                                        bgcolor: 'currentColor',
                                                                        flexShrink: 0,
                                                                        transform: 'translateY(-1px)',
                                                                        mr: 0.25,
                                                                    }}
                                                                />
                                                                <Typography
                                                                    variant="caption"
                                                                    sx={{
                                                                        color: 'warning.dark',
                                                                        fontWeight: 700,
                                                                        letterSpacing: 0.3,
                                                                        textTransform: 'uppercase',
                                                                        mr: 2,
                                                                    }}
                                                                >
                                                                    Testmodus
                                                                </Typography>
                                                                <Button
                                                                    size="small"
                                                                    color="warning"
                                                                    variant="text"
                                                                    onClick={handleEndTestClaim}
                                                                    sx={{
                                                                        minWidth: 0,
                                                                        ml: 'auto',
                                                                        px: 0.5,
                                                                        py: 0.125,
                                                                        borderRadius: 1,
                                                                        fontSize: '0.75rem',
                                                                        fontWeight: 600,
                                                                        lineHeight: 1.2,
                                                                        textTransform: 'none',
                                                                        transform: 'translateY(-1px)',
                                                                    }}
                                                                >
                                                                    Beenden
                                                                </Button>
                                                            </Box>
                                                            <Divider
                                                                sx={{
                                                                    width: 'calc(100% + 24px)',
                                                                    mx: '-12px',
                                                                    mt: 1,
                                                                    mb: 1.25,
                                                                    borderColor: 'rgba(15, 23, 42, 0.12)',
                                                                }}
                                                            />
                                                            <Typography
                                                                variant="body2"
                                                                title={currentTestClaimOwnerName}
                                                                sx={{
                                                                    maxWidth: '100%',
                                                                    overflow: 'hidden',
                                                                    textOverflow: 'ellipsis',
                                                                    whiteSpace: 'nowrap',
                                                                    color: 'text.secondary',
                                                                    fontSize: '0.8125rem',
                                                                    fontWeight: 500,
                                                                    lineHeight: 1.3,
                                                                }}
                                                            >
                                                                Im Test durch {currentTestClaimOwnerName}
                                                            </Typography>
                                                        </Box>
                                                    )
                                                }
                                                topRightPanel={
                                                    showRuntimePendingStartHint ? (
                                                        <Box
                                                            sx={{
                                                                display: 'flex',
                                                                flexDirection: 'column',
                                                                alignItems: 'flex-start',
                                                                gap: 0.75,
                                                            }}
                                                        >
                                                            <Typography
                                                                variant="caption"
                                                                sx={{
                                                                    color: 'info.dark',
                                                                    fontWeight: 700,
                                                                    letterSpacing: 0.3,
                                                                    textTransform: 'uppercase',
                                                                    lineHeight: 1.2,
                                                                }}
                                                            >
                                                                Hinweis
                                                            </Typography>
                                                            <Typography
                                                                variant="body2"
                                                                sx={{
                                                                    color: 'text.secondary',
                                                                    fontSize: '0.8125rem',
                                                                    fontWeight: 500,
                                                                    lineHeight: 1.45,
                                                                }}
                                                            >
                                                                Der Vorgang wartet auf den Start der automatischen
                                                                Abwicklung.
                                                            </Typography>
                                                        </Box>
                                                    ) : undefined
                                                }
                                                selectedNode={selectedNode}
                                                onSelectNode={handleSelectNode}
                                                onAddFollowUpNode={(fromNodeId, viaPort) => {
                                                    setNewNodeFor({
                                                        fromNodeId,
                                                        viaPort,
                                                    });
                                                }}
                                                onAddInbetweenNode={(forEdgeId) => {
                                                    setNewNodeOnEdgeId(forEdgeId);
                                                }}
                                                onAddEdge={handleCreateEdge}
                                                onConnectNodeToExisting={(node, preferredPortKey) => {
                                                    setConnectExistingNodeRequest({
                                                        sourceNodeId: node.id,
                                                        preferredPortKey: preferredPortKey ?? null,
                                                    });
                                                }}
                                                onStartReplaceNode={handleOpenReplaceNodeDialog}
                                                onStartCloneNode={handleCloneNode}
                                                onDeleteEdge={(edgeId) => {
                                                    new ProcessDefinitionEdgeApiService()
                                                        .destroy(edgeId)
                                                        .then(() => {
                                                            setProcessFlow((prevProcess) => {
                                                                if (prevProcess == null) {
                                                                    return prevProcess;
                                                                }

                                                                return {
                                                                    ...prevProcess,
                                                                    edges: prevProcess.edges.filter((edge) => edge.id !== edgeId),
                                                                };
                                                            });
                                                        });
                                                }}
                                                onDeleteNode={handleDeleteNode}
                                                runtimeData={runtimeData}
                                                onReloadRuntimeData={loadRuntimeData}
                                                nodeProblems={processNodeProblems}
                                                showNodeProblemsForNodes={showProcessNodeProblemsForNodes}
                                            />
                                        </ReactFlowProvider> :
                                        <Paper
                                            sx={{
                                                height: '100%',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                p: 2,
                                            }}
                                        >
                                            <Typography color="text.secondary">
                                                {
                                                    hasFlowNodeProviderLoadError ?
                                                        'Die versionierten Prozesselemente konnten nicht geladen werden.' :
                                                        isLoadingFlowNodeProviders ?
                                                            'Lade versionierte Prozesselemente...' :
                                                            'Bereite Prozesselemente vor...'
                                                }
                                            </Typography>
                                        </Paper>
                                }
                            </Box>

                        </Box>
                    </Allotment.Pane>

                    <Allotment.Pane
                        minSize={MIN_EDITOR_DRAWER_WIDTH_PX}
                        preferredSize={MIN_EDITOR_DRAWER_WIDTH_PX}
                        visible={!isEditorPaneCollapsed}
                    >
                        <Paper
                            sx={{
                                px: 0,
                                boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.1)',
                                borderLeft: '1px solid #E0E7E0',
                                borderRadius: 0,
                                position: 'relative',
                                height: '100%',
                                overflow: 'hidden',
                            }}
                        >
                            <ProcessDetailsPageProvider
                                value={{
                                    editable: isProcessEditable,
                                    structureEditable: isProcessStructureEditable,
                                    onSave: handleSaveNode,
                                    onDelete: handleDeleteNode,
                                    onStartReplaceNode: handleOpenReplaceNodeDialog,
                                    nodeRefreshSignal: nodeRefreshSignal,
                                    testClaim: currentTestClaim?.claim ?? null,
                                    nodeProblems: processNodeProblems,
                                    showNodeProblemsForNodes: showProcessNodeProblemsForNodes,
                                }}
                            >
                                <Outlet/>
                            </ProcessDetailsPageProvider>
                        </Paper>


                    </Allotment.Pane>
                </Allotment>

                {
                    /* TODO: Implement this again, when the corresponding user story is worked on.
                    <IconButton
                        aria-label={isEditorPaneCollapsed ? 'Editor einblenden' : 'Editor ausblenden'}
                        onClick={isEditorPaneCollapsed ? handleExpandEditorPane : handleCollapseEditorPane}
                        sx={{
                            display: hideEditorPaneExpandButton ? 'none' : undefined,
                            position: 'absolute',
                            fontSize: '50%',
                            top: '50%',
                            right: isEditorPaneCollapsed
                                ? 0
                                : editorPaneWidth,
                            transform: 'translateY(-50%)',
                            zIndex: 40,
                            width: EDITOR_PANE_TOGGLE_BUTTON_SIZE_PX,
                            height: 56,
                            borderRadius: '12px 0 0 12px',
                            border: '1px solid',
                            borderColor: 'divider',
                            bgcolor: 'background.paper',
                            boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.14)',
                            '&:hover': {
                                bgcolor: 'background.paper',
                            },
                        }}
                    >
                        {isEditorPaneCollapsed ? <ChevronLeft/> : <ChevronRight/>}
                    </IconButton>
                    */
                }
            </Box>

            <SelectNodeProviderDialog
                open={showAddTriggerDialog}
                nodeProviders={availableNodeProviders}
                title="Auslöser hinzufügen"
                titleActions={[{
                    label: 'Importieren',
                    icon: <UploadFile sx={{fontSize: 18}}/>,
                    onClick: () => {
                        void handleImportNode('trigger');
                    },
                }]}
                filter={(provider) => (
                    provider.type === ProcessNodeType.Trigger &&
                    canPlaceProcessNodeProvider(provider, processFlow, flowNodeProviderCache)
                )}
                emptyFilteredMessage={getProcessNodeLimitReachedEmptyMessage(
                    availableNodeProviders,
                    (provider) => provider.type === ProcessNodeType.Trigger,
                    processFlow,
                    flowNodeProviderCache,
                )}
                nodeTypeLimits={getProcessNodeTypeLimits(processFlow, flowNodeProviderCache)}
                onClose={() => {
                    setShowAddTriggerDialog(false);
                }}
                onSelect={handleAddFlowTrigger}
            />

            <ProcessConnectExistingNodeDialog
                open={connectExistingNodeSource != null}
                processFlow={processFlow}
                nodeProviders={flowNodeProviders}
                preferredPortKey={connectExistingNodeRequest?.preferredPortKey ?? null}
                sourceNode={connectExistingNodeSource}
                onClose={() => {
                    setConnectExistingNodeRequest(null);
                }}
                onConnect={(fromNodeId, toNodeId, viaPortKey) => {
                    handleCreateEdge(fromNodeId, toNodeId, viaPortKey);
                }}
            />

            <SelectNodeProviderDialog
                open={replaceNodeSource != null}
                nodeProviders={availableNodeProviders}
                title="Prozesselement ersetzen"
                primaryActionLabel="Ersetzen"
                primaryActionIcon={<SwapHoriz sx={{fontSize: 18}}/>}
                filter={(provider) => {
                    if (!isReplacementCandidateProvider(provider, replaceNodeSource, flowNodeProviderCache)) {
                        return false;
                    }

                    return canPlaceProcessNodeProvider(provider, processFlow, flowNodeProviderCache, replaceNodeSource?.id);
                }}
                emptyFilteredMessage={getProcessNodeLimitReachedEmptyMessage(
                    availableNodeProviders,
                    (provider) => isReplacementCandidateProvider(provider, replaceNodeSource, flowNodeProviderCache),
                    processFlow,
                    flowNodeProviderCache,
                    replaceNodeSource?.id,
                )}
                nodeTypeLimits={getProcessNodeTypeLimits(processFlow, flowNodeProviderCache, replaceNodeSource?.id)}
                onClose={() => {
                    setReplaceNodeRequest(null);
                }}
                onSelect={(provider) => {
                    if (replaceNodeSource == null) {
                        return;
                    }

                    void handleReplaceNode(replaceNodeSource, provider);
                }}
            />

            <SelectNodeProviderDialog
                open={newNodeFor != null}
                nodeProviders={availableNodeProviders}
                titleActions={[{
                    label: 'Importieren',
                    icon: <UploadFile sx={{fontSize: 18}}/>,
                    onClick: () => {
                        void handleImportNode('follow-up');
                    },
                }]}
                filter={(provider) => {
                    if (!isFollowUpCandidateProvider(provider, processFlow, newNodeFor)) {
                        return false;
                    }

                    return canPlaceProcessNodeProvider(provider, processFlow, flowNodeProviderCache);
                }}
                emptyFilteredMessage={getProcessNodeLimitReachedEmptyMessage(
                    availableNodeProviders,
                    (provider) => isFollowUpCandidateProvider(provider, processFlow, newNodeFor),
                    processFlow,
                    flowNodeProviderCache,
                )}
                nodeTypeLimits={getProcessNodeTypeLimits(processFlow, flowNodeProviderCache)}
                onClose={() => {
                    setNewNodeFor(null);
                }}
                onSelect={handleAddFollowUpNode}
            />

            <SelectNodeProviderDialog
                open={newNodeOnEdgeId != null}
                nodeProviders={availableNodeProviders}
                titleActions={[{
                    label: 'Importieren',
                    icon: <UploadFile sx={{fontSize: 18}}/>,
                    onClick: () => {
                        void handleImportNode('in-between');
                    },
                }]}
                filter={(provider) => (
                    isInbetweenCandidateProvider(provider) &&
                    canPlaceProcessNodeProvider(provider, processFlow, flowNodeProviderCache)
                )}
                emptyFilteredMessage={getProcessNodeLimitReachedEmptyMessage(
                    availableNodeProviders,
                    isInbetweenCandidateProvider,
                    processFlow,
                    flowNodeProviderCache,
                )}
                nodeTypeLimits={getProcessNodeTypeLimits(processFlow, flowNodeProviderCache)}
                onClose={() => {
                    setNewNodeOnEdgeId(null);
                }}
                onSelect={handleAddInbetweenNode}
            />

            <ProcessDetailsPageMoreMenu
                anchorEl={showMenuAtEl}
                onClose={() => {
                    setShowMenuAtEl(null);
                }}
                onMenuEvent={handleMenuEvent}
            />

            {
                runtimeData != null &&
                <ProcessInstanceEventDialog
                    open={showProcessInstanceEventsDialog}
                    onClose={() => {
                        setShowProcessInstanceEventsDialog(false);
                    }}
                    instanceId={runtimeData.instance.id}
                    taskId={null}
                />
            }

            <ProcessTestClaimProcessInstancesDialog
                open={showProcessTestClaimInstancesDialog}
                onClose={() => {
                    setShowProcessTestClaimInstancesDialog(false);
                }}
                testClaimId={activeTestClaimId}
                selectedInstanceId={runtimeData?.instance.id ?? instanceId}
                onSelectInstance={handleSelectProcessTestClaimInstance}
            />

            <ProcessSettingsDialog
                open={showSettingsDialog}
                onClose={() => {
                    setShowSettingsDialog(false);
                }}
                process={processFlow.definition}
                version={processFlow.version}
                onProcessChange={(process) => {
                    setProcessFlow((current) => current == null ? current : {
                        ...current,
                        definition: process,
                    });
                }}
                onVersionChange={(version) => {
                    setProcessFlow((current) => current == null ? current : {
                        ...current,
                        version,
                    });
                }}
            />

            <ProcessVersionsDialog
                open={showVersionsDialog}
                process={processFlow.definition}
                currentOpenVersion={processFlow.version.processVersion}
                onClose={() => {
                    setShowVersionsDialog(false);
                }}
                onNewDraft={({process, version}) => {
                    return handleAddDraft(process.id, version.processVersion);
                }}
                onDeleteVersion={(process, version) => {
                    if (version == processVersion) {
                        navigate('/processes');
                    }
                }}
            />

            <ProcessPublishDialog
                open={showPublishDialog}
                onClose={() => {
                    setShowPublishDialog(false);
                }}
                process={processFlow.definition}
                version={processFlow.version}
                availableNodeProviders={availableNodeProviders}
                onPublish={(publishedVersion) => {
                    setProcessFlow({
                        ...processFlow,
                        definition: {
                            ...processFlow.definition,
                            publishedVersion: publishedVersion.processVersion,
                        },
                        version: publishedVersion,
                    });
                    setShowPublishDialog(false);
                }}
            />
        </PageWrapper>
    );
}
