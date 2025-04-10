import {SxProps} from '@mui/material';

export interface ChecklistItem {
    label: string;
    done: boolean;
}

export interface ChecklistProps {
    items: ChecklistItem[];
    sx?: SxProps;
}
