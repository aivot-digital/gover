import {Box, Button, Typography} from '@mui/material';
import React from 'react';
import {EmptyDataListPlaceholderProps} from './empty-data-list-placeholder-props';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';

export function EmptyDataListPlaceholder(props: EmptyDataListPlaceholderProps) {
    return (
        <Box sx={{textAlign: 'center'}}>
            <Typography>
                {props.helperText}
            </Typography>
            <Button
                sx={{mt: 2}}
                endIcon={<AddOutlinedIcon/>}
                variant="text"
                onClick={props.onAdd}
            >
                {props.addText}
            </Button>
        </Box>
    );
}
