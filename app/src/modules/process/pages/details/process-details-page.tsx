import React, {type ReactNode, useCallback, useEffect, useMemo, useState} from 'react';
import {Box, Button, Chip, Divider, Paper, Typography} from '@mui/material';
import {Outlet, useNavigate, useParams, useSearchParams} from 'react-router-dom';
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
import {SelectNodeProviderDialog} from '../../dialogs/select-node-provider-dialog';
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
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';
import {
    ProcessDetailsPageMoreMenu,
    type ProcessDetailsPageMoreMenuEvent,
} from './components/process-details-page-more-menu';
import {downloadObjectFile} from '../../../../utils/download-utils';
import {uploadObjectFile} from '../../../../utils/download-utils';
import {ProcessTestClaimApiService} from '../../services/process-test-claim-api-service';
import {useConfirm} from '../../../../providers/confirm-provider';
import {type ProcessTestClaimEntity} from '../../entities/process-test-claim-entity';
import {type User} from '../../../users/models/user';
import {UsersApiService} from '../../../users/users-api-service';
import {useUser} from '../../../../hooks/use-admin-guard';
import {resolveUserName} from '../../../users/utils/resolve-user-name';
import {type ProcessInstanceEntity} from '../../entities/process-instance-entity';
import {type ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {type ProcessInstanceEventEntity} from '../../entities/process-instance-event-entity';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {BaseApiService} from '../../../../services/base-api-service';
import Download from '@aivot/mui-material-symbols-400-outlined/dist/download/Download';
import {ProcessInstanceEventDialog} from '../../dialogs/process-instance-event-dialog';
import {getProcessNodeProviderKey} from './components/process-flow-editor/utils/process-flow-graph-utils';
import {ProcessDetailsPageSkeleton} from './components/process-details-page-skeleton';
import {useDelayedVisibility} from '../../../../hooks/use-delayed-visibility';
import Undo from '@mui/icons-material/Undo';
import Redo from '@mui/icons-material/Redo';
import Refresh from '@mui/icons-material/Refresh';
import Settings from '@aivot/mui-material-symbols-400-outlined/dist/settings/Settings';
import {type Action} from '../../../../components/actions/actions-props';
import HomeStorage from '@aivot/mui-material-symbols-400-outlined/dist/home-storage/HomeStorage';
import News from '@aivot/mui-material-symbols-400-outlined/dist/news/News';
import {ProcessConnectExistingNodeDialog} from './components/process-connect-existing-node-dialog';
import {getNodeName} from './components/process-flow-editor/utils/node-utils';
import SwapHoriz from '@mui/icons-material/SwapHoriz';
import UploadFile from '@aivot/mui-material-symbols-400-outlined/dist/upload-file/UploadFile';
import {type ProcessNodeExport} from '../../entities/process-node-export';
import {ProcessSettingsDialog} from '../../dialogs/process-settings-dialog/process-settings-dialog';
import {useNotImplemented} from '../../../../hooks/useNotImplemented';

const PROCESS_DETAILS_PAGE_SKELETON_DELAY = 150;

interface RuntimeAttachment {
    key: string;
    fileName: string;
}

export interface ProcessFlow {
    definition: ProcessEntity;
    version: ProcessVersionEntity;
    nodes: ProcessNodeEntity[];
    edges: ProcessDefinitionEdgeEntity[];
}

interface ReplaceNodeRequest {
    nodeId: number;
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

export function ProcessDetailsPage(): ReactNode {
    const params = useParams();
    const [searchParams, setSearchParams] = useSearchParams();
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const confirm = useConfirm();
    const user = useUser();
    const notImplemented = useNotImplemented();

    const [processFlow, setProcessFlow] = useState<ProcessFlow | null>(null);
    const [runtimeData, setRuntimeData] = useState<{
        instance: ProcessInstanceEntity;
        tasks: ProcessInstanceTaskEntity[];
        events: ProcessInstanceEventEntity[];
    } | null>(null);
    const [isRefreshingRuntimeData, setIsRefreshingRuntimeData] = useState(false);
    const [availableNodeProviders, setAvailableNodeProviders] = useState<ProcessNodeProvider[]>([]);
    const [flowNodeProviderCache, setFlowNodeProviderCache] = useState<Record<string, ProcessNodeProvider>>({});
    const [isLoadingFlowNodeProviders, setIsLoadingFlowNodeProviders] = useState(false);
    const [hasFlowNodeProviderLoadError, setHasFlowNodeProviderLoadError] = useState(false);
    const [readyFlowEditorKey, setReadyFlowEditorKey] = useState<string | null>(null);
    const [showSettingsDialog, setShowSettingsDialog] = useState(false);


    const [showAddTriggerDialog, setShowAddTriggerDialog] = useState(false);
    const [newNodeFor, setNewNodeFor] = useState<{
        fromNodeId: number;
        viaPort: string;
    } | null>(null);
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

    const [currentTestClaim, setCurrentTestClaim] = useState<{
        claim: ProcessTestClaimEntity;
        user: User;
    } | null>(null);

    const [showMenuAtEl, setShowMenuAtEl] = useState<HTMLElement | null>(null);
    const [showProcessInstanceEventsDialog, setShowProcessInstanceEventsDialog] = useState(false);
    const showProcessDetailsPageSkeleton = useDelayedVisibility(processFlow == null, PROCESS_DETAILS_PAGE_SKELETON_DELAY);

    useEffect(() => {
        document.body.dataset.hasFlowEditor = 'true';

        return () => {
            delete document.body.dataset.hasFlowEditor;
        };
    }, []);

    const runtimeAttachments = useMemo(() => {
        if (runtimeData == null) {
            return [];
        }

        const collected = new Map<string, RuntimeAttachment>();

        const addAttachment = (attachmentLike: any): void => {
            if (attachmentLike == null || typeof attachmentLike !== 'object') {
                return;
            }

            const key = typeof attachmentLike.attachmentKey === 'string'
                ? attachmentLike.attachmentKey
                : typeof attachmentLike.key === 'string'
                    ? attachmentLike.key
                    : null;
            if (key == null || key.trim().length === 0 || collected.has(key)) {
                return;
            }

            const fileName = typeof attachmentLike.fileName === 'string'
                ? attachmentLike.fileName
                : typeof attachmentLike.filename === 'string'
                    ? attachmentLike.filename
                    : `Anhang ${collected.size + 1}`;

            collected.set(key, {
                key,
                fileName,
            });
        };

        for (const task of runtimeData.tasks) {
            addAttachment(task?.nodeData);
        }

        const payloadAttachments = runtimeData.instance.initialPayload?.attachments;
        if (Array.isArray(payloadAttachments)) {
            for (const payloadAttachment of payloadAttachments) {
                addAttachment(payloadAttachment);
            }
        }

        return Array.from(collected.values());
    }, [runtimeData]);

    const handleDownloadAttachment = async (attachment: RuntimeAttachment): Promise<void> => {
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
    };

    // Fetch the available node providers on mount to display them in the add node dialog
    useEffect(() => {
        new ProcessNodeProviderApiService()
            .getNodeProviders()
            .then(setAvailableNodeProviders)
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die verfügbaren Prozessknoten konnten nicht geladen werden.'));
            });
    }, []);

    // Extract the process id and version from the route to load the process flow
    const {
        processId,
        processVersion,
    } = useMemo(() => {
        const processId = params.processId;
        const processVersion = params.processVersion;

        return {
            processId: parseInt(processId ?? '0'),
            processVersion: parseInt(processVersion ?? '0'),
        };
    }, [params]);

    const instanceId = useMemo(() => {
        const instanceIdParam = searchParams.get('instanceId');
        if (instanceIdParam == null) {
            return null;
        }
        return parseInt(instanceIdParam);
    }, [searchParams]);

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

    // Load the process flow whenever the process id or version changes
    useEffect(() => {
        if (processId == null || processVersion == null) {
            setProcessFlow(null);
            return;
        }

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
                setProcessFlow({
                    definition,
                    version,
                    nodes: nodes.content,
                    edges: edges.content,
                });
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Der Prozessfluss konnte nicht geladen werden.'));
            });

        new ProcessTestClaimApiService()
            .listAll({
                processId,
                processVersion,
            })
            .then(({content}) => {
                if (content.length > 0) {
                    const claim = content[0];
                    new UsersApiService()
                        .retrieve(claim.owningUserId)
                        .then((user) => {
                            setCurrentTestClaim({
                                claim,
                                user,
                            });
                        })
                        .catch((err) => {
                            dispatch(showApiErrorSnackbar(err, 'Der Testanspruch konnte nicht geladen werden.'));
                        });
                } else {
                    setCurrentTestClaim(null);
                }
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Testansprüche konnten nicht geladen werden.'));
            });
    }, [processId, processVersion]);

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
                dispatch(showApiErrorSnackbar(error, 'Die für die Prozessansicht benötigten Knotendefinitionen konnten nicht geladen werden.'));
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
                    /* new ProcessInstanceEventApiService().listAll({
                        processInstanceId: instanceId,
                    })*/Promise.resolve([]),
                ]);
            })
            .then(([instance, tasks, events]) => {
                setRuntimeData({
                    instance,
                    tasks: tasks.content,
                    events: [],
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

    useEffect(() => {
        void loadRuntimeData();
    }, [loadRuntimeData]);

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
            message: 'Füge Knoten hinzu',
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
                    message: 'Dieser Knotentyp kann hier nicht eingefügt werden, da er keinen Ausgangsport besitzt.',
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
            message: 'Füge Knoten hinzu',
            blocking: false,
            estimatedTime: 1000,
        }));

        if (nodeProvider.ports.length === 0) {
            dispatch(addSnackbarMessage({
                key: 'process-inbetween-node-missing-port',
                type: SnackbarType.AutoHiding,
                severity: SnackbarSeverity.Warning,
                message: 'Dieser Knotentyp kann hier nicht eingefügt werden, da er keinen Ausgangsport besitzt.',
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

        await navigate(`/processes/${processFlow.definition.id}/versions/${processFlow.version.processVersion}`);
    };

    const handleSaveNode = async (node: ProcessNodeEntity): Promise<void> => {
        if (processFlow == null) {
            return;
        }

        const updated = await new ProcessNodeApiService().update(node.id, node);

        setProcessFlow({
            ...processFlow,
            nodes: processFlow.nodes.map((n) => n.id === updated.id ? updated : n),
        });
    };

    const handleExport = (): void => {
        new ProcessDefinitionApiService()
            .export(processId, processVersion)
            .then((exp) => {
                downloadObjectFile(`${exp.process.internalTitle} - ${exp.version.processVersion}.json`, exp);
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Der Prozess konnte nicht exportiert werden.'));
            });
    };

    const handleTest = (): void => {
        confirm({
            title: 'Prozessmodellierung testen',
            children: (
                <Typography>
                    Möchten Sie die Prozessmodellierung testen?
                    Dabei wird die weitere Bearbeitung des Prozesses gesperrt, bis der Test abgeschlossen ist.
                    Sie können den Test jederzeit abbrechen.
                    Alle gestarteten Vorgänge werden dabei beendet und gelöscht.
                </Typography>
            ),
            confirmButtonText: 'Test starten',
        })
            .then((confirm) => {
                if (!confirm) {
                    return;
                }

                return new ProcessTestClaimApiService()
                    .create({
                        ...ProcessTestClaimApiService.initialize(),
                        processId,
                        processVersion,
                    });
            })
            .then((res) => {
                if (res == null || user == null) {
                    return;
                }
                setCurrentTestClaim({
                    claim: res,
                    user,
                });
                dispatch(showSuccessSnackbar('Der Test wurde gestartet. Der Prozess ist nun für die Bearbeitung gesperrt.'));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Test konnte nicht gestartet werden.'));
            });
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
                        setCurrentTestClaim(null);
                        setRuntimeData(null);
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
    }, [confirm, currentTestClaim, dispatch, instanceId, searchParams, setSearchParams]);

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
                importedNodeExport.data.node.processNodeDefinitionKey,
                importedNodeExport.data.node.processNodeDefinitionVersion,
            );
            if (importedProvider == null) {
                dispatch(showErrorSnackbar('Die Knotendefinition aus dem Import ist in dieser Instanz nicht verfügbar.'));
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
                    dispatch(showErrorSnackbar('Dieser importierte Knotentyp kann hier nicht eingefügt werden, da er keinen Ausgangsport besitzt.'));
                    return;
                }
            }

            if (context === 'in-between' && importedProvider.ports.length === 0) {
                dispatch(showErrorSnackbar('Dieser importierte Knotentyp kann hier nicht eingefügt werden, da er keinen Ausgangsport besitzt.'));
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
            dispatch(showErrorSnackbar('Der aktuelle Knotentyp konnte nicht aufgelöst werden.'));
            return;
        }

        if (!canReplaceNodeType(currentProvider.type, replacementProvider.type)) {
            dispatch(showErrorSnackbar('Auslöser und Endelemente können nur durch denselben Knotentyp ersetzt werden.'));
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
            'separator' as const,
        ];

        return [
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
                onClick: notImplemented,
            },
            {
                tooltip: 'Einstellungen',
                ariaLabel: 'Einstellungen',
                icon: <Settings/>,
                onClick: () => {
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
                disabledTooltip: 'Während des Tests kann der Prozess nicht veröffentlicht werden.',
                icon: null,
                onClick: notImplemented,
                variant: 'contained',
                disabled: isInTestMode,
                activeStyle: {ml: 1},
            },
        ];
    }, [
        currentTestClaim,
        instanceId,
        isRefreshingRuntimeData,
        loadRuntimeData,
        runtimeData,
        notImplemented,
        handleDeleteProcess,
    ]);
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
                }}
            >
                <Allotment>
                    <Allotment.Pane minSize={760}>
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
                                                editable={currentTestClaim == null}
                                                processFlow={processFlow}
                                                nodeProviders={flowNodeProviders}
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
                                                                title={resolveUserName(currentTestClaim.user)}
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
                                                                Im Test durch {resolveUserName(currentTestClaim.user)}
                                                            </Typography>
                                                        </Box>
                                                    )
                                                }
                                                selectedNode={selectedNode}
                                                onSelectNode={(node) => {
                                                    if (node == null) {
                                                        navigate(`/processes/${processFlow.definition.id}/versions/${processFlow.version.processVersion}?${searchParams.toString()}`);
                                                        return;
                                                    }
                                                    navigate(`/processes/${processFlow.definition.id}/versions/${processFlow.version.processVersion}/nodes/${node.id}?${searchParams.toString()}`);
                                                }}
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
                                                        'Die versionierten Prozessknoten konnten nicht geladen werden.' :
                                                        isLoadingFlowNodeProviders ?
                                                            'Lade versionierte Prozessknoten...' :
                                                            'Bereite Prozessknoten vor...'
                                                }
                                            </Typography>
                                        </Paper>
                                }
                            </Box>

                            {
                                runtimeData != null && runtimeAttachments.length > 0 &&
                                <Paper
                                    sx={{
                                        mt: 2,
                                        p: 2,
                                        display: 'flex',
                                        flexWrap: 'wrap',
                                        alignItems: 'center',
                                        gap: 1,
                                    }}
                                >
                                    <Typography variant="h6">
                                        Anhänge
                                    </Typography>

                                    {
                                        runtimeAttachments.map((attachment) => (
                                            <Chip
                                                key={attachment.key}
                                                variant="outlined"
                                                label={attachment.fileName}
                                                sx={{
                                                    maxWidth: 320,
                                                    '& .MuiChip-label': {
                                                        overflow: 'hidden',
                                                        textOverflow: 'ellipsis',
                                                    },
                                                }}
                                                onDelete={() => {
                                                    void handleDownloadAttachment(attachment);
                                                }}
                                                deleteIcon={
                                                    <Download color="primary"/>
                                                }
                                            />
                                        ))
                                    }
                                </Paper>
                            }
                        </Box>
                    </Allotment.Pane>

                    <Allotment.Pane
                        minSize={560}
                        preferredSize={560}
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
                                    editable: currentTestClaim == null,
                                    onSave: handleSaveNode,
                                    onDelete: handleDeleteNode,
                                    onStartReplaceNode: handleOpenReplaceNodeDialog,
                                    nodeRefreshSignal,
                                }}
                            >
                                <Outlet/>
                            </ProcessDetailsPageProvider>
                        </Paper>
                    </Allotment.Pane>
                </Allotment>
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
                filter={(provider) => provider.type === ProcessNodeType.Trigger}
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
                    if (replaceNodeSource == null) {
                        return false;
                    }

                    const currentProvider = flowNodeProviderCache[getProcessNodeProviderKey(
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

                    if (!canReplaceNodeType(currentProvider.type, provider.type)) {
                        return false;
                    }

                    return true;
                }}
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
                    if (provider.type === ProcessNodeType.Trigger) {
                        return false;
                    }

                    if (newNodeFor == null) {
                        return true;
                    }

                    const requiresOutgoingPort = processFlow.edges.some((edge) => (
                        edge.fromNodeId === newNodeFor.fromNodeId &&
                        edge.viaPort === newNodeFor.viaPort
                    ));

                    return !requiresOutgoingPort || provider.ports.length > 0;
                }}
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
                    provider.type !== ProcessNodeType.Trigger &&
                    provider.ports.length > 0
                )}
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

            <ProcessSettingsDialog
                open={showSettingsDialog}
                onClose={() => {
                    setShowSettingsDialog(false);
                }}
                process={processFlow.definition}
                version={processFlow.version}
            />
        </PageWrapper>
    );
}
