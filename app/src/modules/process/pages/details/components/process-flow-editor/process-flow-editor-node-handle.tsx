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
    ADD_BUTTON_SIZE,
    HANDLE_COLOR,
    HANDLE_SIZE,
    HANDLE_WIDTH,
} from './data/process-flow-constants';
import './process-flow-editor-animations.css';

const CHIP_HEIGHT = 24;
const CONNECTED_PORT_STEM_HEIGHT = 4;
const CONNECTED_SOURCE_HANDLE_TOP = ADD_BUTTON_DISTANCE + CHIP_HEIGHT + CONNECTED_PORT_STEM_HEIGHT;
const CONNECTED_PORT_SPACER_HEIGHT = ADD_BUTTON_DISTANCE + ADD_BUTTON_SIZE + (ADD_BUTTON_DISTANCE * 1.25) - CONNECTED_PORT_STEM_HEIGHT;

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
                        height: ADD_BUTTON_DISTANCE,
                        width: `${HANDLE_WIDTH}px`,
                        backgroundColor: wasPerformed ? undefined : HANDLE_COLOR,
                        backgroundImage: wasPerformed ?
                            `repeating-linear-gradient(to bottom, ${theme.palette.primary.main} 0 8px, transparent 8px 16px)` :
                            undefined,
                        backgroundSize: wasPerformed ? '100% 16px' : undefined,
                        animation: wasPerformed ? 'active-handle-dash-scroll 1s linear infinite' : undefined,
                    }}
                />

                <Chip
                    label={port.label}
                    size="small"
                    variant="outlined"
                    sx={{
                        bgcolor: 'background.paper',
                        borderColor: HANDLE_COLOR,
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
                                    width: `${HANDLE_WIDTH}px`,
                                    backgroundColor: wasPerformed ? undefined : HANDLE_COLOR,
                                    backgroundImage: wasPerformed ?
                                        `repeating-linear-gradient(to bottom, ${theme.palette.primary.main} 0 8px, transparent 8px 16px)` :
                                        undefined,
                                    backgroundSize: wasPerformed ? '100% 16px' : undefined,
                                    animation: wasPerformed ? 'active-handle-dash-scroll 1s linear infinite' : undefined,
                                }}
                            />

                            <Box
                                sx={{
                                    height: CONNECTED_PORT_SPACER_HEIGHT,
                                }}
                            />
                        </> :
                        <>
                            <Box
                                sx={{
                                    minHeight: ADD_BUTTON_DISTANCE,
                                    flex: 1,
                                    width: `${HANDLE_WIDTH}px`,
                                    backgroundColor: wasPerformed ? undefined : HANDLE_COLOR,
                                    backgroundImage: wasPerformed ?
                                        `repeating-linear-gradient(to bottom, ${theme.palette.primary.main} 0 8px, transparent 8px 16px)` :
                                        undefined,
                                    backgroundSize: wasPerformed ? '100% 16px' : undefined,
                                    animation: wasPerformed ? 'active-handle-dash-scroll 1s linear infinite' : undefined,
                                }}
                            />

                            <IconButton
                                sx={{
                                    bgcolor: 'background.paper',
                                    border: `${HANDLE_WIDTH}px solid`,
                                    borderColor: HANDLE_COLOR,
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
                                        fontSize: ADD_BUTTON_SIZE - 2,
                                    }}
                                />
                            </IconButton>

                            <Box
                                sx={{
                                    minHeight: ADD_BUTTON_DISTANCE * 1.25,
                                    flex: 1,
                                    width: `${HANDLE_WIDTH}px`,
                                    backgroundColor: wasPerformed ? undefined : HANDLE_COLOR,
                                    backgroundImage: wasPerformed ?
                                        `repeating-linear-gradient(to bottom, ${theme.palette.primary.main} 0 8px, transparent 8px 16px)` :
                                        undefined,
                                    backgroundSize: wasPerformed ? '100% 16px' : undefined,
                                    animation: wasPerformed ? 'active-handle-dash-scroll 1s linear infinite' : undefined,
                                }}
                            />
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
