import {Handle, NodeProps, Position} from "@xyflow/react";
import {Box, Divider, Paper, useTheme} from "@mui/material";
import {useContext, useMemo} from "react";
import Typography from "@mui/material/Typography";
import {ProcessNodeType} from "../../../../services/process-node-provider-api-service";
import Assignment from "@aivot/mui-material-symbols-400-outlined/dist/assignment/Assignment";
import {ProviderTypeStyles} from "../../../../data/provider-type-styles";
import {KnownProviderIcons} from "../../../../data/known-provider-icons";
import {ProcessFlowEditorNodeHandle} from "./process-flow-editor-node-handle";
import {ProcessFlowEditorContext} from "./process-flow-editor-context";
import {HANDLE_SIZE, NODE_WIDTH} from "./data/process-flow-constants";
import {FlowNode} from "./utils/layout-utils";
import {getNodeDescription, getNodeName} from "./utils/node-utils";

export function ProcessFlowEditorNode(props: NodeProps<FlowNode>) {
    const theme = useTheme();

    const {
        data,
    } = props;

    const {
        selectedNode,
        onAddFollowUpNode,
        onDeleteEdge,
        showTargetHandles,
    } = useContext(ProcessFlowEditorContext);

    const {
        treeNode,
    } = data;

    const {
        node,
        provider,
    } = treeNode;

    const {
        Icon: TypeIcon,
        label: typeLabel,
        bgColor: typeBgColor,
        textColor: typeTextColor,
    } = useMemo(() => {
        return ProviderTypeStyles[provider.type];
    }, [provider.type]);

    const ProviderIcon = useMemo(() => {
        return KnownProviderIcons[provider.key] || Assignment;
    }, [provider.key]);

    return (
        <Box
            data-node-id={node.id}
            sx={{
                position: 'relative',
                width: `${Math.max(NODE_WIDTH * 2, NODE_WIDTH * (provider.ports.length + 1))}px`,
            }}
        >
            <Box
                sx={{
                    width: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                }}
            >
                {/* Type Card */}
                <Paper
                    sx={{
                        bgcolor: typeBgColor,
                        color: typeTextColor,
                        display: 'flex',
                        width: '66%',
                        height: '24px',
                        alignItems: 'center',
                        justifyContent: 'center',
                        py: 0.25,
                        px: 0.5,
                        borderBottomLeftRadius: 0,
                        borderBottomRightRadius: 0,
                    }}
                >
                    <TypeIcon
                        sx={{
                            fontSize: '16px',
                            mr: 0.5,
                        }}
                    />

                    <Typography
                        sx={{
                            fontSize: '12px',
                            fontWeight: 200,
                        }}
                    >
                        {typeLabel}
                    </Typography>
                </Paper>

                {/* Content Card */}
                <Paper
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        width: '100%',
                        height: 'calc(100% - 24px)',
                        outline: selectedNode?.id === node.id ? `2px solid ${theme.palette.primary.light}` : 'none',
                    }}
                >
                    {/* Title */}
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            padding: 1,
                        }}
                    >
                        <ProviderIcon
                            sx={{
                                mr: 1,
                            }}
                        />

                        <Typography>
                            {getNodeName(node, provider)}
                        </Typography>
                    </Box>

                    {/* Divider */}
                    <Divider
                        sx={{
                            mb: 0.25,
                            width: '100%',
                        }}
                    />

                    {/* Description */}
                    <Box
                        sx={{
                            padding: 1,
                            flex: 1,
                        }}
                    >
                        <Typography>
                            {getNodeDescription(node, provider)}
                        </Typography>
                    </Box>
                </Paper>

            </Box>

            {
                provider.type !== ProcessNodeType.Trigger &&
                <Handle
                    type="target"
                    position={Position.Top}
                    style={{
                        visibility: showTargetHandles ? 'visible' : 'hidden',
                        width: showTargetHandles ? `${HANDLE_SIZE}px` : 0,
                        height: showTargetHandles ? `${HANDLE_SIZE}px` : 0,
                        backgroundColor: 'var(--xy-edge-stroke, var(--xy-edge-stroke-default))',
                        border: 'none',
                    }}
                />
            }

            {
                provider.type !== ProcessNodeType.Termination &&
                <Box
                    sx={{
                        position: 'relative',
                        display: 'flex',
                        justifyContent: 'space-evenly',
                        alignItems: 'stretch',
                        width: '100%',
                    }}
                >
                    {
                        provider
                            .ports
                            .map((port) => (
                                <ProcessFlowEditorNodeHandle
                                    key={port.key}
                                    isConnected={data.treeNode.children.some((c) => c.port.key === port.key)}
                                    port={port}
                                    onClick={() => {
                                        onAddFollowUpNode(node.id, port.key);
                                    }}
                                    onDeleteEdge={(port) => {
                                        const edge = data
                                            .treeNode
                                            .children
                                            .find((c) => c.port.key === port.key);

                                        if (edge != null) {
                                            onDeleteEdge(edge.edge.id);
                                        }
                                    }}
                                />
                            ))
                    }
                </Box>
            }
        </Box>
    );
}
