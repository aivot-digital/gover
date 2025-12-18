import {useEffect, useMemo, useState} from "react";
import {Box, Button, Paper} from "@mui/material";
import {useParams} from "react-router-dom";
import {ProcessDefinitionEntity} from "../../entities/process-definition-entity";
import {ProcessDefinitionVersionApiService} from "../../services/process-definition-version-api-service";
import {ProcessDefinitionNodeEntity} from "../../entities/process-definition-node-entity";
import {ProcessDefinitionEdgeEntity} from "../../entities/process-definition-edge-entity";
import {ProcessDefinitionApiService} from "../../services/process-definition-api-service";
import {ProcessDefinitionEdgeApiService} from "../../services/process-definition-edge-api-service";
import {ProcessDefinitionVersionEntity} from "../../entities/process-definition-version-entity";
import {ProcessDefinitionNodeApiService} from "../../services/process-definition-node-api-service";
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";
import Undo from "@aivot/mui-material-symbols-400-outlined/dist/undo/Undo";
import Redo from "@aivot/mui-material-symbols-400-outlined/dist/redo/Redo";
import {ProcessStatus} from "../../enums/process-status";
import Download from "@aivot/mui-material-symbols-400-outlined/dist/download/Download";
import {downloadObjectFile} from "../../../../utils/download-utils";
import {PageWrapper} from "../../../../components/page-wrapper/page-wrapper";
import {GenericPageHeader} from "../../../../components/generic-page-header/generic-page-header";

import Add from "@aivot/mui-material-symbols-400-outlined/dist/add/Add";
import {
    ProcessNodeProvider,
    ProcessNodeProviderApiService,
    ProcessNodeType
} from "../../services/process-node-provider-api-service";
import {generateId} from "../../../../utils/id-utils";
import {SelectNodeProviderDialog} from "../../dialogs/select-node-provider-dialog";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import {clearLoadingMessage, setLoadingMessage} from "../../../../slices/shell-slice";
import {showApiErrorSnackbar} from "../../../../slices/snackbar-slice";
import {ProcessFlowEditor} from "./components/process-flow-editor/process-flow-editor";
import {ElementDerivationContext} from "../../../elements/components/element-derivation-context";
import {TextFieldComponent} from "../../../../components/text-field/text-field-component";
import {GroupLayout} from "../../../../models/elements/form/layout/group-layout";
import {ReactFlowProvider} from "@xyflow/react";
import ProcessChart from "@aivot/mui-material-symbols-400-outlined/dist/process-chart/ProcessChart";

export interface ProcessFlow {
    definition: ProcessDefinitionEntity;
    version: ProcessDefinitionVersionEntity;
    nodes: ProcessDefinitionNodeEntity[];
    edges: ProcessDefinitionEdgeEntity[];
}

export function ProcessDetailsPage() {
    const dispatch = useAppDispatch();
    const params = useParams();

    const [processFlow, setProcessFlow] = useState<ProcessFlow | null>(null);
    const [previousProcessFlowStates, setPreviousProcessFlowStates] = useState<ProcessFlow[]>([]);
    const [futureProcessFlowStates, setFutureProcessFlowStates] = useState<ProcessFlow[]>([]);

    const [availableNodeProviders, setAvailableNodeProviders] = useState<ProcessNodeProvider[]>([]);

    const [showAddTriggerDialog, setShowAddTriggerDialog] = useState(false);
    const [selectedNode, setSelectedNode] = useState<ProcessDefinitionNodeEntity | null>(null);
    const [newNodeFor, setNewNodeFor] = useState<{
        fromNodeId: number;
        viaPort: string;
    } | null>(null);
    const [newNodeOnEdgeId, setNewNodeOnEdgeId] = useState<number | null>(null);

    useEffect(() => {
        new ProcessNodeProviderApiService()
            .getNodeProviders()
            .then(setAvailableNodeProviders);
    }, []);

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
            new ProcessDefinitionNodeApiService().listAll({
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
                    definition: definition,
                    version: version,
                    nodes: nodes.content,
                    edges: edges.content,
                });
            });
    }, [processId, processVersion]);

    const handleUndo = () => {

    };

    const handleRedo = () => {

    };

    const handleExport = () => {
        new ProcessDefinitionApiService()
            .export(processId, processVersion)
            .then((exp) => {
                downloadObjectFile(`${exp.data.process.name} - ${exp.data.version.processDefinitionVersion}.json`, exp);
            });
    };

    const handleAddFlowTrigger = (nodeProvider: ProcessNodeProvider) => {
        if (processFlow == null) {
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Füge Auslöser hinzu',
            blocking: false,
            estimatedTime: 1000,
        }));

        setShowAddTriggerDialog(false);

        new ProcessDefinitionNodeApiService()
            .create({
                id: 0,
                processDefinitionId: processFlow.definition.id,
                processDefinitionVersion: processFlow.version.processDefinitionVersion,
                name: null,
                description: null,
                codeKey: nodeProvider.key,
                dataKey: generateId(5),
                configuration: {},
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
    }

    const handleAddFollowUpNode = async (nodeProvider: ProcessNodeProvider) => {
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

        const newNode = await new ProcessDefinitionNodeApiService()
            .create({
                id: 0,
                processDefinitionId: processFlow.definition.id,
                processDefinitionVersion: processFlow.version.processDefinitionVersion,
                name: null,
                description: null,
                codeKey: nodeProvider.key,
                dataKey: generateId(5),
                configuration: {},
            });

        const edgeApi = new ProcessDefinitionEdgeApiService()

        if (existingEdge != null) {
            await edgeApi.destroy(existingEdge.id);
        }

        const newEdge = await edgeApi
            .create({
                id: 0,
                processDefinitionId: processFlow.definition.id,
                processDefinitionVersion: processFlow.version.processDefinitionVersion,
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
                    viaPort: nodeProvider.ports[0].key
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

    const handleAddInbetweenNode = async (nodeProvider: ProcessNodeProvider) => {
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

        const newNode = await new ProcessDefinitionNodeApiService()
            .create({
                id: 0,
                processDefinitionId: processFlow.definition.id,
                processDefinitionVersion: processFlow.version.processDefinitionVersion,
                name: null,
                description: null,
                codeKey: nodeProvider.key,
                dataKey: generateId(5),
                configuration: {},
            });

        const newEdgeToNewNode = await edgeApi
            .create({
                id: 0,
                processDefinitionId: processFlow.definition.id,
                processDefinitionVersion: processFlow.version.processDefinitionVersion,
                fromNodeId: existingEdge.fromNodeId,
                toNodeId: newNode.id,
                viaPort: existingEdge.viaPort,
            });

        const newEdgeFromNewNode = await edgeApi
            .create({
                id: 0,
                processDefinitionId: processFlow.definition.id,
                processDefinitionVersion: processFlow.version.processDefinitionVersion,
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

    const handleDeleteNode = async (node: ProcessDefinitionNodeEntity) => {
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

        await new ProcessDefinitionNodeApiService()
            .destroy(node.id);

        setProcessFlow({
            ...processFlow,
            nodes: processFlow.nodes.filter((n) => n.id !== node.id),
            edges: processFlow.edges.filter((e) => !edgesToRemove.some((er) => er.id === e.id)),
        });

        setSelectedNode(null);
    };

    const handleSaveNode = async (node: ProcessDefinitionNodeEntity) => {
        if (processFlow == null) {
            return;
        }

        const updated = await new ProcessDefinitionNodeApiService()
            .update(node.id, node);

        setProcessFlow({
            ...processFlow,
            nodes: processFlow.nodes.map((n) => n.id === updated.id ? updated : n),
        });

        setSelectedNode(null);
    }

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
                    }}
                >
                    <GenericPageHeader
                        title={'Prozess: ' + processFlow.definition.name}
                        badge={{
                            color: 'default',
                            label: `Version ${processFlow.version.processDefinitionVersion}`,
                        }}
                        icon={ModuleIcons.processes}
                        actions={[
                            {
                                tooltip: 'Änderung rückgängig machen',
                                icon: <Undo/>,
                                onClick: handleUndo,
                                disabled: previousProcessFlowStates.length === 0,
                                visible: processFlow.version.status === ProcessStatus.Drafted,
                            },
                            {
                                tooltip: 'Änderung wiederherstellen',
                                icon: <Redo/>,
                                onClick: handleRedo,
                                disabled: futureProcessFlowStates.length === 0,
                                visible: processFlow.version.status === ProcessStatus.Drafted,
                            },
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
                                onClick: () => setShowAddTriggerDialog(true),
                            },
                            {
                                tooltip: 'Exportieren',
                                icon: <Download/>,
                                onClick: handleExport,
                            },
                            {
                                tooltip: 'Vorgänge',
                                icon: <ProcessChart/>,
                                to: `/processes/${processFlow.definition.id}/versions/${processFlow.version.processDefinitionVersion}/instances`,
                            },
                        ]}
                    />

                    <Box
                        sx={{
                            height: '100%',
                            border: '1px solid #ccc',
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
                                    setSelectedNode(node);
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
                                            processDefinitionId: processFlow.definition.id,
                                            processDefinitionVersion: processFlow.version.processDefinitionVersion,
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
                        width: 720,
                        overflowY: 'auto',
                        p: 2,
                    }}
                >
                    <Editor
                        selectedNode={selectedNode}
                        onSave={handleSaveNode}
                        onDelete={handleDeleteNode}
                    />
                </Paper>
            </Box>

            <SelectNodeProviderDialog
                open={showAddTriggerDialog}
                nodeProviders={availableNodeProviders}
                filter={(provider) => provider.type === ProcessNodeType.Trigger}
                onClose={() => setShowAddTriggerDialog(false)}
                onSelect={handleAddFlowTrigger}
            />

            <SelectNodeProviderDialog
                open={newNodeFor != null}
                nodeProviders={availableNodeProviders}
                filter={(provider) => provider.type !== ProcessNodeType.Trigger}
                onClose={() => setNewNodeFor(null)}
                onSelect={handleAddFollowUpNode}
            />

            <SelectNodeProviderDialog
                open={newNodeOnEdgeId != null}
                nodeProviders={availableNodeProviders}
                filter={(provider) => provider.type !== ProcessNodeType.Trigger && provider.type !== ProcessNodeType.Termination}
                onClose={() => setNewNodeOnEdgeId(null)}
                onSelect={handleAddInbetweenNode}
            />
        </PageWrapper>
    );
}

interface EditorProps {
    selectedNode: ProcessDefinitionNodeEntity | null;

    onSave: (node: ProcessDefinitionNodeEntity) => void;
    onDelete: (node: ProcessDefinitionNodeEntity) => void;
}

function Editor(props: EditorProps) {
    const {
        selectedNode,
    } = props;

    const [localNode, setLocalNode] = useState<ProcessDefinitionNodeEntity | null>(selectedNode);
    const [layout, setLayout] = useState<GroupLayout | null>(null);

    useEffect(() => {
        if (selectedNode == null) {
            setLayout(null);
            setLocalNode(null)
        } else {
            setLocalNode(selectedNode);
            new ProcessDefinitionNodeApiService()
                .getConfigurationLayout(selectedNode.id)
                .then(setLayout);
        }
    }, [selectedNode]);

    const handleSaveSelected = () => {
        if (localNode != null) {
            props.onSave(localNode);
        }
    };

    const handleDeleteSelected = () => {
        if (selectedNode == null) {
            return;
        }

        props.onDelete(selectedNode);
        // TODO: show error because there are edits
    };

    if (selectedNode == null) {
        return (
            <Box>
                Placeholder
            </Box>
        );
    }

    return (
        <>
            <TextFieldComponent
                label="Eindeutiger Zugriffsschlüssel"
                value={selectedNode.dataKey}
                onChange={(val) => {
                    setLocalNode({
                        ...(localNode ?? selectedNode),
                        dataKey: val ?? '',
                    });
                }}
                required={true}
                maxCharacters={32}
            />

            <TextFieldComponent
                label="Name"
                value={selectedNode.name}
                onChange={(val) => {
                    setLocalNode({
                        ...(localNode ?? selectedNode),
                        name: val ?? null,
                    });
                }}
                maxCharacters={96}
            />

            <TextFieldComponent
                label="Kurzbeschreibung"
                value={selectedNode.description}
                onChange={(val) => {
                    setLocalNode({
                        ...(localNode ?? selectedNode),
                        description: val ?? null,
                    });
                }}
                multiline={true}
                maxCharacters={512}
            />

            {
                layout != null &&
                <ElementDerivationContext
                    element={layout}
                    elementData={(localNode ?? selectedNode).configuration}
                    onElementDataChange={(elementData) => {
                        console.log('Element data changed:', elementData);
                        setLocalNode({
                            ...(localNode ?? selectedNode),
                            configuration: elementData,
                        });
                    }}
                />
            }

            <Button
                onClick={handleSaveSelected}
            >
                Knoten speichern
            </Button>

            <Button
                onClick={handleDeleteSelected}
            >
                Knoten entfernen
            </Button>
        </>
    );
}