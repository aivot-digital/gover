import {Box, Button, Typography} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faPlus} from '@fortawesome/pro-light-svg-icons';
import React from 'react';
import {EmptyDataListPlaceholderProps} from './empty-data-list-placeholder-props';

export function EmptyDataListPlaceholder(props: EmptyDataListPlaceholderProps) {
    return (
        <Box sx={{textAlign: 'center'}}>
            <Typography>
                {props.helperText}
            </Typography>
            <Button
                sx={{mt: 2}}
                endIcon={<FontAwesomeIcon icon={faPlus}/>}
                variant="text"
                onClick={props.onAdd}
            >
                {props.addText}
            </Button>
        </Box>
    );
}
