import {styled} from '@mui/material/styles';
import MuiAccordion, {AccordionProps} from '@mui/material/Accordion';
import MuiAccordionSummary, {AccordionSummaryProps} from '@mui/material/AccordionSummary';
import MuiAccordionDetails from '@mui/material/AccordionDetails';
import React from 'react';
import {Box} from '@mui/material';

export const AccordionGroup = styled(Box)(({ theme }) => ({
    display: 'flex',
    flexDirection: 'column',
    '& > .MuiAccordion-root:first-of-type': {
        borderTopLeftRadius: theme.shape.borderRadius,
        borderTopRightRadius: theme.shape.borderRadius,
    },
    '& > .MuiAccordion-root:last-of-type': {
        borderBottomLeftRadius: theme.shape.borderRadius,
        borderBottomRightRadius: theme.shape.borderRadius,
    },
    '& > .MuiAccordion-root.Mui-expanded': {
        borderRadius: theme.shape.borderRadius,
    },
}));

export const Accordion = styled((props: AccordionProps) => (
    <MuiAccordion
        elevation={0}
        square
        {...props}
    />
))(({theme}) => ({
    'border': `1px solid ${theme.palette.grey[400]}`,
    'margin-bottom': '-1px',
    '&:before': {
        display: 'none',
    },
}));

export const AccordionSummary = styled((props: AccordionSummaryProps) => (
    <MuiAccordionSummary
        {...props}
    />
))(({theme}) => ({
    minHeight: 48,
    '&.Mui-expanded': {
        minHeight: 48,
    },
    '& .MuiAccordionSummary-content': {
        margin: '8px 0',
        '&.Mui-expanded': {
            margin: '8px 0',
        },
    },
}));

export const AccordionDetails = styled(MuiAccordionDetails)(({theme}) => ({
    padding: theme.spacing(2),
    paddingBottom: theme.spacing(4),
    borderTop: `1px solid ${theme.palette.grey[400]}`,
}));