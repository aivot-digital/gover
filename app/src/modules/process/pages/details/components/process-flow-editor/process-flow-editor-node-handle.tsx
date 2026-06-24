import React, {type ReactNode} from 'react';
import {Handle, Position} from '@xyflow/react';
import {Box, IconButton, Tooltip, useTheme} from '@mui/material';
import Typography from '@mui/material/Typography';
import {type ProcessNodePort} from '../../../../services/process-node-provider-api-service';
import Chip from '@mui/material/Chip';
import {alpha} from '@mui/material/styles';
import {useConfirm} from '../../../../../../providers/confirm-provider';
import {Add} from '@mui/icons-material';
import Link from '@mui/icons-material/Link';
import LinkOffOutlinedIcon from '@mui/icons-material/LinkOffOutlined';
import {
    ADD_BUTTON_DISTANCE,
    ADD_BUTTON_ICON_SIZE,
    ADD_BUTTON_SIZE,
    HANDLE_COLOR,
    INTERACTIVE_HANDLE_SIZE,
    HANDLE_SIZE,
    HANDLE_WIDTH,
} from './data/process-flow-constants';
import './process-flow-editor-animations.css';

const CHIP_HEIGHT = 24;
const PORT_CHIP_ACTION_ICON_SIZE = 16;
const PORT_CHIP_ACTION_SLOT_SIZE = 18;
const PORT_CHIP_PATH_GAP = 6;
const PORT_DOT_SIZE = 10;
const PORT_DOT_GAP = 6;
const TOP_PORT_CONNECTOR_HEIGHT = ADD_BUTTON_DISTANCE + (PORT_DOT_SIZE / 2) + PORT_DOT_GAP - PORT_CHIP_PATH_GAP;
const CONNECTED_PORT_STEM_HEIGHT = 0;
const CONNECTED_SOURCE_HANDLE_OFFSET = PORT_CHIP_PATH_GAP + 5;
const CONNECTED_SOURCE_HANDLE_TOP = ADD_BUTTON_DISTANCE + PORT_DOT_SIZE + PORT_DOT_GAP + CHIP_HEIGHT + CONNECTED_SOURCE_HANDLE_OFFSET;
const CONNECTED_PORT_SPACER_HEIGHT = ADD_BUTTON_DISTANCE + ADD_BUTTON_SIZE + (ADD_BUTTON_DISTANCE * 1.25) - CONNECTED_PORT_STEM_HEIGHT - PORT_CHIP_PATH_GAP;
const OPEN_PORT_ACTION_CONNECTOR_HEIGHT = ADD_BUTTON_DISTANCE;
const OPEN_PORT_CONNECTOR_HEIGHT = ADD_BUTTON_DISTANCE + ADD_BUTTON_SIZE + (ADD_BUTTON_DISTANCE * 1.25) - PORT_CHIP_PATH_GAP;
const ACTIVE_RUNTIME_DASH_ARRAY = '10 10';
const ACTIVE_RUNTIME_DASH_ANIMATION = 'active-edge-dash-scroll 2s linear infinite';

interface ProcessFlowEditorNodeHandleProps {
    editable: boolean;
    isConnected: boolean;
    port: ProcessNodePort;
    onClick: () => void;
    onConnectToExisting: (port: ProcessNodePort) => void;
    onDeleteEdge: (port: ProcessNodePort) => void;
    wasPerformed: boolean;
}

interface PortChipActionIconProps {
    children: ReactNode;
    className?: string;
    onClick?: React.MouseEventHandler<HTMLSpanElement>;
    tooltip: string;
}

function PortChipActionIcon(props: PortChipActionIconProps): ReactNode {
    const {
        children,
        className,
        onClick,
        tooltip,
    } = props;

    return (
        <Tooltip title={tooltip} arrow>
            <span
                className={className}
                onClick={onClick}
                style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                }}
            >
                {children}
            </span>
        </Tooltip>
    );
}

export function ProcessFlowEditorNodeHandle(props: ProcessFlowEditorNodeHandleProps): ReactNode {
    const theme = useTheme();

    const {
        editable,
        isConnected,
        port,
        onClick,
        onConnectToExisting,
        onDeleteEdge,
        wasPerformed,
    } = props;

    const confirm = useConfirm();
    const portAccentColor = wasPerformed ? theme.palette.primary.main : theme.palette.text.secondary;
    const portBackgroundColor = isConnected ?
        theme.palette.background.default :
        theme.palette.background.paper;
    const portBorderColor = wasPerformed ?
        alpha(theme.palette.primary.main, 0.42) :
        isConnected ?
            alpha(theme.palette.text.primary, 0.18) :
            alpha(theme.palette.text.primary, 0.34);
    const portBorderStyle = isConnected || wasPerformed ? 'solid' : 'dashed';
    const portTextColor = wasPerformed ?
        theme.palette.primary.dark :
        theme.palette.text.secondary;
    const portTitle = port.description ?
        `${port.label}: ${port.description}` :
        port.label;

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
                    width: '100%',
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
                        height: TOP_PORT_CONNECTOR_HEIGHT,
                    }}
                >
                    <PortConnector
                        height={TOP_PORT_CONNECTOR_HEIGHT}
                        wasPerformed={wasPerformed}
                    />
                </Box>

                <Box
                    sx={{
                        height: PORT_CHIP_PATH_GAP,
                    }}
                />

                <Chip
                    label={
                        <Box
                            component="span"
                            title={portTitle}
                            sx={{
                                display: 'block',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }}
                        >
                            {port.label}
                        </Box>
                    }
                    size="small"
                    variant="outlined"
                    sx={{
                        height: CHIP_HEIGHT,
                        maxWidth: 'calc(100% - 8px)',
                        borderRadius: '999px',
                        bgcolor: portBackgroundColor,
                        borderColor: portBorderColor,
                        borderStyle: portBorderStyle,
                        color: portTextColor,
                        boxShadow: 'none',
                        cursor: 'default',
                        transition: theme.transitions.create(
                            ['background-color', 'border-color', 'color'],
                            {
                                duration: theme.transitions.duration.shortest,
                            }
                        ),
                        '& .MuiChip-label': {
                            minWidth: 0,
                            pl: 1,
                            pr: editable ? 0.5 : 1,
                            fontSize: '0.75rem',
                            fontWeight: 500,
                            lineHeight: 1.2,
                            letterSpacing: 0,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        },
                        '& .MuiChip-deleteIcon': {
                            width: PORT_CHIP_ACTION_SLOT_SIZE,
                            height: PORT_CHIP_ACTION_SLOT_SIZE,
                            mr: 0.5,
                            ml: -0.125,
                            borderRadius: '999px',
                            color: alpha(portAccentColor, isConnected ? 0.32 : 0.46),
                            transition: theme.transitions.create(
                                ['background-color', 'color'],
                                {
                                    duration: theme.transitions.duration.shortest,
                                }
                            ),
                        },
                        '& .MuiChip-deleteIcon:hover': {
                            bgcolor: alpha(portAccentColor, 0.08),
                            color: alpha(portAccentColor, 0.82),
                        },
                        '&:hover': {
                            bgcolor: portBackgroundColor,
                            borderColor: portBorderColor,
                            color: portTextColor,
                        },
                    }}
                    deleteIcon={editable ? (
                        isConnected ?
                            <PortChipActionIcon tooltip="Verbindung aufheben">
                                <LinkOffOutlinedIcon sx={{fontSize: PORT_CHIP_ACTION_ICON_SIZE}}/>
                            </PortChipActionIcon> :
                            <PortChipActionIcon tooltip="Mit bestehendem Knoten verbinden">
                                <Link sx={{fontSize: PORT_CHIP_ACTION_ICON_SIZE}}/>
                            </PortChipActionIcon>
                    ) : undefined}
                    onDelete={editable ? (
                        isConnected ?
                            handleDeleteEdge :
                            () => {
                                onConnectToExisting(port);
                            }
                    ) : undefined}
                />

                <Box
                    sx={{
                        height: PORT_CHIP_PATH_GAP,
                    }}
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
                        editable ?
                            <>
                                <Box
                                    sx={{
                                        height: OPEN_PORT_ACTION_CONNECTOR_HEIGHT,
                                    }}
                                >
                                    <PortConnector
                                        height={OPEN_PORT_ACTION_CONNECTOR_HEIGHT}
                                        wasPerformed={wasPerformed}
                                    />
                                </Box>

                                <Tooltip title="Element hinzufügen" arrow>
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
                                    >
                                        <Add
                                            sx={{
                                                fontSize: ADD_BUTTON_ICON_SIZE,
                                            }}
                                        />
                                    </IconButton>
                                </Tooltip>

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
                            </> :
                            <Box
                                sx={{
                                    height: OPEN_PORT_CONNECTOR_HEIGHT,
                                }}
                            >
                                <PortConnector
                                    height={OPEN_PORT_CONNECTOR_HEIGHT}
                                    wasPerformed={wasPerformed}
                                />
                            </Box>
                }
            </Box>

            <Handle
                type="source"
                id={port.key}
                position={isConnected ? Position.Top : Position.Bottom}
                style={{
                    opacity: isConnected ? 0 : 1,
                    pointerEvents: isConnected ? 'none' : 'all',
                    width: `${isConnected ? HANDLE_SIZE : INTERACTIVE_HANDLE_SIZE}px`,
                    height: `${isConnected ? HANDLE_SIZE : INTERACTIVE_HANDLE_SIZE}px`,
                    backgroundColor: theme.palette.background.paper,
                    border: `${isConnected ? 0 : 2}px solid ${HANDLE_COLOR}`,
                    cursor: isConnected ? 'default' : 'crosshair',
                    ...(isConnected ? {
                        top: `${CONNECTED_SOURCE_HANDLE_TOP}px`,
                        bottom: 'auto',
                    } : {
                        bottom: `-${INTERACTIVE_HANDLE_SIZE / 2}px`,
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
