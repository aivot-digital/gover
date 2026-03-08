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
import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
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
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
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
import Refresh from '@mui/icons-material/Refresh';
import {ProcessInstanceEventDialog} from '../../dialogs/process-instance-event-dialog';
import News from '@aivot/mui-material-symbols-400-outlined/dist/news/News';
import {getProcessNodeProviderKey} from './components/process-flow-editor/utils/process-flow-graph-utils';
import {ProcessDetailsPageSkeleton} from './components/process-details-page-skeleton';
import {useDelayedVisibility} from '../../../../hooks/use-delayed-visibility';

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

export function ProcessDetailsPage(): ReactNode {
    const params = useParams();
    const [searchParams, setSearchParams] = useSearchParams();
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const confirm = useConfirm();
    const user = useUser();

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

    const [showAddTriggerDialog, setShowAddTriggerDialog] = useState(false);
    const [newNodeFor, setNewNodeFor] = useState<{
        fromNodeId: number;
        viaPort: string;
    } | null>(null);
    const [newNodeOnEdgeId, setNewNodeOnEdgeId] = useState<number | null>(null);

    const [currentTestClaim, setCurrentTestClaim] = useState<{
        claim: ProcessTestClaimEntity;
        user: User;
    } | null>(null);

    const [showMenuAtEl, setShowMenuAtEl] = useState<HTMLElement | null>(null);
    const [showProcessInstanceEventsDialog, setShowProcessInstanceEventsDialog] = useState(false);
    const showProcessDetailsPageSkeleton = useDelayedVisibility(processFlow == null, PROCESS_DETAILS_PAGE_SKELETON_DELAY);

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
                downloadObjectFile(`${exp.data.process.internalTitle} - ${exp.data.version.processVersion}.json`, exp);
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
            default:
                dispatch(addSnackbarMessage({
                    key: 'unknown-process-details-event',
                    type: SnackbarType.AutoHiding,
                    severity: SnackbarSeverity.Info,
                    message: 'Diese Funktion ist noch nicht implementiert.',
                }));
                break;
        }
    };
    const handleOpenAddTriggerDialog = useCallback(() => {
        setShowAddTriggerDialog(true);
    }, []);

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
                                actions={[
                                    {
                                        tooltip: 'Laufzeitdaten neu laden',
                                        icon: <Refresh/>,
                                        onClick: () => {
                                            void loadRuntimeData();
                                        },
                                        visible: instanceId != null,
                                        disabled: isRefreshingRuntimeData,
                                    },
                                    {
                                        tooltip: 'Vorgangsereignisse anzeigen',
                                        icon: <News/>,
                                        onClick: () => {
                                            setShowProcessInstanceEventsDialog(true);
                                        },
                                        visible: runtimeData != null,
                                    },
                                    {
                                        label: 'Auslöser',
                                        tooltip: 'Neuen Auslöser hinzufügen',
                                        disabledTooltip: 'Während des Tests können keine Auslöser hinzugefügt werden.',
                                        icon: <Add/>,
                                        onClick: handleOpenAddTriggerDialog,
                                        disabled: currentTestClaim != null,
                                        variant: 'contained',
                                    },
                                    {
                                        tooltip: 'Mehr',
                                        icon: <MoreVert/>,
                                        onClick: (event) => {
                                            setShowMenuAtEl(event.target as HTMLElement);
                                        },
                                    },
                                ]}
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
                                                onAddEdge={(fromNodeId, toNodeId, viaPortKey) => {
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
                                                        });
                                                }}
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
                                    editable: true,
                                    onSave: handleSaveNode,
                                    onDelete: handleDeleteNode,
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
                filter={(provider) => provider.type === ProcessNodeType.Trigger}
                onClose={() => {
                    setShowAddTriggerDialog(false);
                }}
                onSelect={handleAddFlowTrigger}
            />

            <SelectNodeProviderDialog
                open={newNodeFor != null}
                nodeProviders={availableNodeProviders}
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
        </PageWrapper>
    );
}
