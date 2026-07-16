import React, {type ReactNode} from 'react';
import {Box, Button, Tooltip, Typography} from '@mui/material';
import InfoOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Info';

interface SelectionListRowProps {
    icon: ReactNode;
    title: ReactNode;
    titleAdornment?: ReactNode;
    description?: ReactNode;
    selected?: boolean;
    onShowDetails?: () => void;
    detailsLabel?: string;
    detailsIcon?: ReactNode;
    primaryActionLabel: string;
    primaryActionIcon: ReactNode;
    onPrimaryAction: () => void;
    primaryActionDisabled?: boolean;
    primaryActionDisabledTooltip?: string;
}

export function SelectionListRow(props: SelectionListRowProps): ReactNode {
    const primaryAction = (
        <Button
            variant="contained"
            size="small"
            startIcon={props.primaryActionIcon}
            onClick={props.onPrimaryAction}
            disabled={props.primaryActionDisabled}
        >
            {props.primaryActionLabel}
        </Button>
    );

    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 1.75,
                px: 2.25,
                py: 1.9,
                bgcolor: props.selected ? 'action.hover' : 'transparent',
                opacity: props.primaryActionDisabled === true ? 0.55 : 1,
            }}
        >
            <Box
                sx={{
                    width: 38,
                    height: 38,
                    borderRadius: '50%',
                    bgcolor: 'grey.100',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                }}
            >
                {props.icon}
            </Box>

            <Box
                sx={{
                    minWidth: 0,
                    flex: 1,
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 0.75,
                        minWidth: 0,
                        flexWrap: 'wrap',
                    }}
                >
                    <Typography
                        fontWeight={700}
                        sx={{
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

                {
                    props.description != null &&
                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{mt: 0.5}}
                    >
                        {props.description}
                    </Typography>
                }
            </Box>

            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    flexShrink: 0,
                    pl: 1.5,
                }}
            >
                {
                    props.onShowDetails != null &&
                    <Button
                        variant="text"
                        size="small"
                        startIcon={props.detailsIcon ?? <InfoOutlinedIcon sx={{fontSize: 18}}/>}
                        onClick={props.onShowDetails}
                    >
                        {props.detailsLabel ?? 'Details'}
                    </Button>
                }
                {
                    props.primaryActionDisabledTooltip != null ?
                        <Tooltip title={props.primaryActionDisabledTooltip}>
                            <span>{primaryAction}</span>
                        </Tooltip> :
                        primaryAction
                }
            </Box>
        </Box>
    );
}
