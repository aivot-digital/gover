import {Box, Button, Typography} from '@mui/material';
import React from 'react';
import {EmptyDataListPlaceholderProps} from './empty-data-list-placeholder-props';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';

export function EmptyDataListPlaceholder(props: EmptyDataListPlaceholderProps) {
    return (
        <Box
            sx={{
                maxWidth: 600,
                textAlign: 'center',
                px: 3,
                py: 3,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
            }}
        >
            {
                props.title != null &&
                <Typography
                    variant="h6"
                    component="h2"
                    sx={{
                        lineHeight: 1.25,
                    }}
                >
                    {props.title}
                </Typography>
            }

            {
                (props.description != null || props.helperText != null) &&
                <Typography
                    variant="body1"
                    color="text.secondary"
                    sx={{
                        mt: props.title != null ? 1 : 0,
                        lineHeight: 1.55,
                    }}
                >
                    {props.description ?? props.helperText}
                </Typography>
            }

            {
                props.addText != null &&
                props.onAdd != null &&
                <Button
                    sx={{mt: 2.5}}
                    startIcon={<AddOutlinedIcon/>}
                    variant="outlined"
                    onClick={props.onAdd}
                >
                    {props.addText}
                </Button>
            }
        </Box>
    );
}
