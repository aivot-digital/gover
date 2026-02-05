import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {Box, Paper, Typography} from '@mui/material';
import {Outlet, useNavigate, useParams} from 'react-router-dom';
import {type ProcessEntity} from '../../entities/process-entity';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {type ProcessDefinitionEdgeEntity} from '../../entities/process-definition-edge-entity';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessDefinitionEdgeApiService} from '../../services/process-definition-edge-api-service';
import {type ProcessVersionEntity} from '../../entities/process-version-entity';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
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
import ProcessChart from '@aivot/mui-material-symbols-400-outlined/dist/process-chart/ProcessChart';
import {ProcessDetailsPageProvider} from './process-details-page-context';
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';
import {ProcessDetailsPageMoreMenu, ProcessDetailsPageMoreMenuEvent} from './components/process-details-page-more-menu';
import {downloadObjectFile} from '../../../../utils/download-utils';
import {ProcessTestClaimApiService} from '../../services/process-test-claim-api-service';
import {useConfirm} from '../../../../providers/confirm-provider';
import {ProcessTestClaimEntity} from '../../entities/process-test-claim-entity';
import {User} from '../../../users/models/user';
import {UsersApiService} from '../../../users/users-api-service';
import {useUser} from '../../../../hooks/use-admin-guard';
import {resolveUserName} from '../../../users/utils/resolve-user-name';

export interface ProcessFlow {
    definition: ProcessEntity;
    version: ProcessVersionEntity;
    nodes: ProcessNodeEntity[];
    edges: ProcessDefinitionEdgeEntity[];
}

export function ProcessDetailsPage(): ReactNode {
    const params = useParams();
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const confirm = useConfirm();
    const user = useUser();

    const [processFlow, setProcessFlow] = useState<ProcessFlow | null>(null);

    const [availableNodeProviders, setAvailableNodeProviders] = useState<ProcessNodeProvider[]>([]);

    const [showAddTriggerDialog, setShowAddTriggerDialog] = useState(false);
    const [selectedNode, setSelectedNode] = useState<ProcessNodeEntity | null>(null);
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
                processDefinitionId: processId,
                processDefinitionVersion: processVersion,
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
                processNodeDefinitionVersion: nodeProvider.version,
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
                processNodeDefinitionVersion: nodeProvider.version,
            });

        const edgeApi = new ProcessDefinitionEdgeApiService();

        if (existingEdge != null) {
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

        const edgeApi = new ProcessDefinitionEdgeApiService();

        await edgeApi.destroy(existingEdge.id);

        const newNode = await new ProcessNodeApiService()
            .create({
                ...ProcessNodeApiService.initialize(),
                processId: processFlow.definition.id,
                processVersion: processFlow.version.processVersion,
                processNodeDefinitionKey: nodeProvider.key,
                processNodeDefinitionVersion: nodeProvider.version,
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

        const edgesToRemove = processFlow.edges.filter((edge) => (
            edge.fromNodeId === node.id ||
            edge.toNodeId === node.id
        ));

        const edgeApi = new ProcessDefinitionEdgeApiService();

        await Promise
            .all(edgesToRemove.map((e) => edgeApi.destroy(e.id)));

        await new ProcessNodeApiService()
            .destroy(node.id);

        setProcessFlow({
            ...processFlow,
            nodes: processFlow.nodes.filter((n) => n.id !== node.id),
            edges: processFlow.edges.filter((e) => !edgesToRemove.some((er) => er.id === e.id)),
        });

        await navigate(`/processes/${processFlow.definition.id}/versions/${processFlow.version.processVersion}`);
    };

    const handleSaveNode = async (node: ProcessNodeEntity): Promise<void> => {
        if (processFlow == null) {
            return;
        }

        const updated = await new ProcessNodeApiService().update(node.id, node);

        dispatch(showSuccessSnackbar('Der Knoten wurde erfolgreich gespeichert.'));

        setProcessFlow({
            ...processFlow,
            nodes: processFlow.nodes.map((n) => n.id === updated.id ? updated : n),
        });
    };

    const handleExport = (): void => {
        new ProcessDefinitionApiService()
            .export(processId, processVersion)
            .then((exp) => {
                downloadObjectFile(`${exp.data.process.internalTitle} - ${exp.data.version.processVersion}.gp`, exp);
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

    const handleMenuEvent = (event: ProcessDetailsPageMoreMenuEvent): void => {
        switch (event) {
            case 'export':
                handleExport();
                break;
            case 'test':
                handleTest();
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

    if (processFlow == null) {
        return (
            <GenericDetailsSkeleton/>
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
                    display: 'flex',
                    height: '100vh',
                }}
            >
                <Box
                    sx={{
                        flex: 1,
                        px: 2,
                        py: 2,
                        height: '100vh',
                        display: 'flex',
                        flexDirection: 'column',
                    }}
                >
                    <GenericPageHeader
                        title={'Prozess: ' + processFlow.definition.internalTitle}
                        badge={
                            currentTestClaim != null ?
                                [
                                    {
                                        color: 'default',
                                        label: `Version ${processFlow.version.processVersion}`,
                                    },
                                    {
                                        color: 'warning',
                                        label: `Im Test durch ${resolveUserName(currentTestClaim.user)}`,
                                    },
                                ] :
                                {
                                    color: 'default',
                                    label: `Version ${processFlow.version.processVersion}`,
                                }
                        }
                        icon={ModuleIcons.processes}
                        actions={[
                            'separator',
                            /*
                            {
                                tooltip: 'Historie anzeigen',
                                icon: <AccessTimeIcon/>,
                                onClick: () => {
                                    setShowRevisions(true);
                                },
                                visible: canViewHistory,
                            },
                             */
                            {
                                tooltip: 'Auslöser hinzufügen',
                                icon: <Add/>,
                                onClick: () => {
                                    setShowAddTriggerDialog(true);
                                },
                            },
                            {
                                tooltip: 'Vorgänge',
                                icon: <ProcessChart/>,
                                to: `/processes/${processFlow.definition.id}/versions/${processFlow.version.processVersion}/instances`,
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
                            height: '100%',
                            borderRadius: 1,
                            mt: 2,
                        }}
                    >
                        <ReactFlowProvider>
                            <ProcessFlowEditor
                                editable={true}
                                processFlow={processFlow}
                                nodeProviders={availableNodeProviders}
                                selectedNode={selectedNode}
                                onSelectNode={(node) => {
                                    if (node == null) {
                                        setSelectedNode(null);
                                        return;
                                    }
                                    setSelectedNode(node);
                                    navigate(`/processes/${processFlow.definition.id}/versions/${processFlow.version.processVersion}/nodes/${node.id}`);
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
                            />
                        </ReactFlowProvider>
                    </Box>
                </Box>

                <Paper
                    sx={{
                        width: 680,
                        height: '100vh',
                        borderLeft: '1px solid #ccc',
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
                filter={(provider) => provider.type !== ProcessNodeType.Trigger}
                onClose={() => {
                    setNewNodeFor(null);
                }}
                onSelect={handleAddFollowUpNode}
            />

            <SelectNodeProviderDialog
                open={newNodeOnEdgeId != null}
                nodeProviders={availableNodeProviders}
                filter={(provider) => provider.type !== ProcessNodeType.Trigger && provider.type !== ProcessNodeType.Termination}
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
        </PageWrapper>
    );
}
