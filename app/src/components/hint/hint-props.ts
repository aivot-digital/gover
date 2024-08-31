import {ReactNode} from 'react';
import {SxProps} from '@mui/material';

export interface HintProps {
    summary: ReactNode;
    detailsTitle: string;
    details: ReactNode;
    sx?: SxProps;
}