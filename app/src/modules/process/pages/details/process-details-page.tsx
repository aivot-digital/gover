import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import {Box, Paper} from '@mui/material';
import {Outlet, useNavigate, useParams} from 'react-router-dom';
import {ProcessEntity} from '../../entities/process-entity';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
import {ProcessNodeEntity} from '../../entities/process-node-entity';
import {ProcessDefinitionEdgeEntity} from '../../entities/process-definition-edge-entity';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessDefinitionEdgeApiService} from '../../services/process-definition-edge-api-service';
import {ProcessVersionEntity} from '../../entities/process-version-entity';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import Download from '@aivot/mui-material-symbols-400-outlined/dist/download/Download';
import {downloadObjectFile} from '../../../../utils/download-utils';
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
import {clearLoadingMessage, setLoadingMessage} from '../../../../slices/shell-slice';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {ProcessFlowEditor} from './components/process-flow-editor/process-flow-editor';
import {ReactFlowProvider} from '@xyflow/react';
import ProcessChart from '@aivot/mui-material-symbols-400-outlined/dist/process-chart/ProcessChart';
import {ProcessDetailsPageProvider} from './process-details-page-context';

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

    const [processFlow, setProcessFlow] = useState<ProcessFlow | null>(null);

    const [availableNodeProviders, setAvailableNodeProviders] = useState<ProcessNodeProvider[]>([]);

    const [showAddTriggerDialog, setShowAddTriggerDialog] = useState(false);
    const [selectedNode, setSelectedNode] = useState<ProcessNodeEntity | null>(null);
    const [newNodeFor, setNewNodeFor] = useState<{
        fromNodeId: number;
        viaPort: string;
    } | null>(null);
    const [newNodeOnEdgeId, setNewNodeOnEdgeId] = useState<number | null>(null);

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
    }, [processId, processVersion]);


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
                        badge={{
                            color: 'default',
                            label: `Version ${processFlow.version.processVersion}`,
                        }}
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
                                tooltip: 'Exportieren',
                                icon: <Download/>,
                                onClick: handleExport,
                            },
                            {
                                tooltip: 'Vorgänge',
                                icon: <ProcessChart/>,
                                to: `/processes/${processFlow.definition.id}/versions/${processFlow.version.processVersion}/instances`,
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
                                        fromNodeId: fromNodeId,
                                        viaPort: viaPort,
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
                                            fromNodeId: fromNodeId,
                                            toNodeId: toNodeId,
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
        </PageWrapper>
    );
}
