import React, {type ReactNode} from 'react';
import {Box, Button, Typography} from '@mui/material';

interface SelectionDetailsPanelProps {
    icon: ReactNode;
    label: ReactNode;
    title: ReactNode;
    titleAdornment?: ReactNode;
    description?: ReactNode;
    children?: ReactNode;
    primaryActionLabel: string;
    primaryActionIcon: ReactNode;
    onPrimaryAction: () => void;
    onClose: () => void;
    primaryActionDisabled?: boolean;
    iconBackgroundColor?: string;
    iconColor?: string;
}

export function SelectionDetailsPanel(props: SelectionDetailsPanelProps): ReactNode {
    return (
        <>
            <Box
                sx={{
                    p: 2,
                    pt: 1.75,
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                    }}
                >
                    <Box
                        sx={{
                            width: 38,
                            height: 38,
                            minWidth: 38,
                            minHeight: 38,
                            aspectRatio: '1 / 1',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            borderRadius: '50%',
                            flexShrink: 0,
                            backgroundColor: props.iconBackgroundColor ?? 'grey.100',
                            color: props.iconColor ?? 'text.secondary',
                        }}
                    >
                        {props.icon}
                    </Box>

                    <Box sx={{minWidth: 0, flex: 1}}>
                        <Typography
                            variant="caption"
                            sx={{
                                display: 'block',
                                lineHeight: 1.2,
                                mt: 0.5,
                            }}
                        >
                            {props.label}
                        </Typography>

                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1,
                                minWidth: 0,
                            }}
                        >
                            <Typography
                                variant="h6"
                                lineHeight={1.2}
                                sx={{
                                    flex: 1,
                                    minWidth: 0,
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                }}
                            >
                                {props.title}
                            </Typography>
                            {props.titleAdornment}
                        </Box>
                    </Box>
                </Box>
            </Box>

            <Box
                sx={{
                    flex: 1,
                    overflowY: 'auto',
                    px: 2.25,
                    pt: 2.25,
                    pb: 3.75,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 2.5,
                }}
            >
                {
                    props.description != null &&
                    <Typography variant="body2" color="text.secondary">
                        {props.description}
                    </Typography>
                }
                {props.children}
            </Box>

            <Box
                sx={{
                    px: 2,
                    pt: 2,
                    pb: 2.5,
                    borderTop: '1px solid',
                    borderColor: 'divider',
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 1,
                }}
            >
                <Button
                    variant="contained"
                    startIcon={props.primaryActionIcon}
                    onClick={props.onPrimaryAction}
                    disabled={props.primaryActionDisabled}
                >
                    {props.primaryActionLabel}
                </Button>
                <Button
                    variant="text"
                    onClick={props.onClose}
                >
                    Details schließen
                </Button>
            </Box>
        </>
    );
}
