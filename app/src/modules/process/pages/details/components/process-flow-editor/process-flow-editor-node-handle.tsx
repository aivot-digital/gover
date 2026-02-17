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
        confirm({
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
            }}
        >
            {
                !isConnected &&
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
                            backgroundColor: HANDLE_COLOR,
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

                    <Box
                        sx={{
                            minHeight: ADD_BUTTON_DISTANCE,
                            flex: 1,
                            width: `${HANDLE_WIDTH}px`,
                            backgroundColor: HANDLE_COLOR,
                        }}
                    />

                    {
                        !isConnected &&
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
                    }

                    <Box
                        sx={{
                            minHeight: ADD_BUTTON_DISTANCE * 1.25,
                            flex: 1,
                            borderLeftWidth: `${HANDLE_WIDTH}px`,
                            borderLeftColor: wasPerformed ? theme.palette.primary.main : HANDLE_COLOR,
                            borderLeftStyle: wasPerformed ? 'dashed' : 'solid',
                            animation: wasPerformed ? 'active-edge-dash-scroll 2s linear infinite' : undefined,
                        }}
                    />
                </Box>
            }

            <Handle
                type="source"
                id={port.key}
                position={Position.Bottom}
                style={{
                    visibility: isConnected ? 'hidden' : 'visible',
                    width: isConnected ? 0 : `${HANDLE_SIZE}px`,
                    height: isConnected ? 0 : `${HANDLE_SIZE}px`,
                    backgroundColor: 'var(--xy-edge-stroke, var(--xy-edge-stroke-default))',
                    border: 'none',
                    bottom: isConnected ? 0 : '-4px',
                }}
            />
        </Box>
    );
}
