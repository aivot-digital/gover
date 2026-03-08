import React, {type ReactNode} from 'react';
import {Handle, Position} from '@xyflow/react';
import {Box, IconButton, useTheme} from '@mui/material';
import Typography from '@mui/material/Typography';
import {type ProcessNodePort} from '../../../../services/process-node-provider-api-service';
import Chip from '@mui/material/Chip';
import {useConfirm} from '../../../../../../providers/confirm-provider';
import {Add} from '@mui/icons-material';
import Close from '@aivot/mui-material-symbols-400-outlined/dist/close/Close';
import {
    ADD_BUTTON_DISTANCE,
    ADD_BUTTON_ICON_SIZE,
    ADD_BUTTON_SIZE,
    HANDLE_COLOR,
    HANDLE_SIZE,
    HANDLE_WIDTH,
} from './data/process-flow-constants';
import './process-flow-editor-animations.css';

const CHIP_HEIGHT = 24;
const PORT_DOT_SIZE = 10;
const PORT_DOT_GAP = 6;
const TOP_PORT_CONNECTOR_HEIGHT = ADD_BUTTON_DISTANCE + (PORT_DOT_SIZE / 2) + PORT_DOT_GAP;
const CONNECTED_PORT_STEM_HEIGHT = 0;
const CONNECTED_SOURCE_HANDLE_OFFSET = 5;
const CONNECTED_SOURCE_HANDLE_TOP = ADD_BUTTON_DISTANCE + PORT_DOT_SIZE + PORT_DOT_GAP + CHIP_HEIGHT + CONNECTED_SOURCE_HANDLE_OFFSET;
const CONNECTED_PORT_SPACER_HEIGHT = ADD_BUTTON_DISTANCE + ADD_BUTTON_SIZE + (ADD_BUTTON_DISTANCE * 1.25) - CONNECTED_PORT_STEM_HEIGHT;
const ACTIVE_RUNTIME_DASH_ARRAY = '10 10';
const ACTIVE_RUNTIME_DASH_ANIMATION = 'active-edge-dash-scroll 2s linear infinite';

interface ProcessFlowEditorNodeHandleProps {
    editable: boolean;
    isConnected: boolean;
    port: ProcessNodePort;
    onClick: () => void;
    onDeleteEdge: (port: ProcessNodePort) => void;
    wasPerformed: boolean;
}

export function ProcessFlowEditorNodeHandle(props: ProcessFlowEditorNodeHandleProps): ReactNode {
    const theme = useTheme();

    const {
        editable,
        isConnected,
        port,
        onClick,
        onDeleteEdge,
        wasPerformed,
    } = props;

    const confirm = useConfirm();

    const handleDeleteEdge = (): void => {
        void confirm({
            title: 'Verbindung aufheben',
            children: (
                <Typography>
                    Möchten Sie die Verbindung &quot;{port.label}&quot; wirklich aufheben?
                </Typography>
            ),
        })
            .then((confirmed) => {
                if (confirmed) {
                    onDeleteEdge(port);
                }
            });
    };

    return (
        <Box
            sx={{
                position: 'relative',
                width: '100%',
                display: 'flex',
                justifyContent: 'center',
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    height: '100%',
                    flexDirection: 'column',
                    alignItems: 'center',
                }}
            >
                <Box
                    sx={{
                        mt: `-${PORT_DOT_SIZE / 2}px`,
                        width: `${PORT_DOT_SIZE}px`,
                        height: `${PORT_DOT_SIZE}px`,
                        borderRadius: '999px',
                        bgcolor: wasPerformed ? theme.palette.primary.main : HANDLE_COLOR,
                        border: `2px solid ${theme.palette.background.paper}`,
                        boxShadow: '0 6px 14px rgba(15, 23, 42, 0.16)',
                        zIndex: 1,
                    }}
                />

                <Box
                    sx={{
                        height: `${TOP_PORT_CONNECTOR_HEIGHT}px`,
                    }}
                >
                    <PortConnector
                        height={TOP_PORT_CONNECTOR_HEIGHT}
                        wasPerformed={wasPerformed}
                    />
                </Box>

                <Chip
                    label={port.label}
                    size="small"
                    variant="outlined"
                    sx={{
                        bgcolor: 'background.paper',
                        borderColor: wasPerformed ? theme.palette.primary.main : HANDLE_COLOR,
                    }}
                    deleteIcon={editable ? <Close/> : undefined}
                    onDelete={isConnected && editable ? handleDeleteEdge : undefined}
                />

                {
                    isConnected ?
                        <>
                            <Box
                                sx={{
                                    height: CONNECTED_PORT_STEM_HEIGHT,
                                }}
                            >
                                <PortConnector
                                    height={CONNECTED_PORT_STEM_HEIGHT}
                                    wasPerformed={wasPerformed}
                                />
                            </Box>

                            <Box
                                sx={{
                                    height: CONNECTED_PORT_SPACER_HEIGHT,
                                }}
                            />
                        </> :
                        <>
                            <Box
                                sx={{
                                    height: ADD_BUTTON_DISTANCE,
                                }}
                            >
                                <PortConnector
                                    height={ADD_BUTTON_DISTANCE}
                                    wasPerformed={wasPerformed}
                                />
                            </Box>

                            <IconButton
                                sx={{
                                    bgcolor: 'background.paper',
                                    border: `${HANDLE_WIDTH}px solid`,
                                    borderColor: HANDLE_COLOR,
                                    padding: 0,
                                    width: ADD_BUTTON_SIZE,
                                    height: ADD_BUTTON_SIZE,
                                }}
                                onClick={(event) => {
                                    event.stopPropagation();
                                    event.preventDefault();

                                    onClick();
                                }}
                                disabled={!editable}
                            >
                                <Add
                                    sx={{
                                        fontSize: ADD_BUTTON_ICON_SIZE,
                                    }}
                                />
                            </IconButton>

                            <Box
                                sx={{
                                    height: ADD_BUTTON_DISTANCE * 1.25,
                                }}
                            >
                                <PortConnector
                                    height={ADD_BUTTON_DISTANCE * 1.25}
                                    wasPerformed={wasPerformed}
                                />
                            </Box>
                        </>
                }
            </Box>

            <Handle
                type="source"
                id={port.key}
                position={isConnected ? Position.Top : Position.Bottom}
                style={{
                    opacity: isConnected ? 0 : 1,
                    pointerEvents: isConnected ? 'none' : 'all',
                    width: `${HANDLE_SIZE}px`,
                    height: `${HANDLE_SIZE}px`,
                    backgroundColor: 'var(--xy-edge-stroke, var(--xy-edge-stroke-default))',
                    border: 'none',
                    ...(isConnected ? {
                        top: `${CONNECTED_SOURCE_HANDLE_TOP}px`,
                        bottom: 'auto',
                    } : {
                        bottom: '-4px',
                    }),
                }}
            />
        </Box>
    );
}

interface PortConnectorProps {
    height: number;
    wasPerformed: boolean;
}

function PortConnector(props: PortConnectorProps): ReactNode {
    const theme = useTheme();
    const {
        height,
        wasPerformed,
    } = props;
    if (height <= 0) {
        return null;
    }
    const connectorHeight = Math.max(height, HANDLE_WIDTH);
    const shouldAnimateDash = wasPerformed && connectorHeight > 10;

    return (
        <Box
            component="svg"
            width={HANDLE_WIDTH}
            height={connectorHeight}
            viewBox={`0 0 ${HANDLE_WIDTH} ${connectorHeight}`}
            preserveAspectRatio="none"
            sx={{
                display: 'block',
                overflow: 'visible',
            }}
        >
            <line
                x1={HANDLE_WIDTH / 2}
                y1={HANDLE_WIDTH / 2}
                x2={HANDLE_WIDTH / 2}
                y2={Math.max(connectorHeight - (HANDLE_WIDTH / 2), HANDLE_WIDTH / 2)}
                stroke={wasPerformed ? theme.palette.primary.main : HANDLE_COLOR}
                strokeWidth={HANDLE_WIDTH}
                strokeLinecap="round"
                strokeDasharray={wasPerformed ? ACTIVE_RUNTIME_DASH_ARRAY : undefined}
                style={{
                    animation: shouldAnimateDash ? ACTIVE_RUNTIME_DASH_ANIMATION : undefined,
                }}
            />
        </Box>
    );
}
