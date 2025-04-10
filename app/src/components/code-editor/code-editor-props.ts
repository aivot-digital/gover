import {SxProps} from '@mui/material';

export interface CodeEditorProps {
    label?: string;
    value?: string;
    onChange: (value: string) => void;
    disabled?: boolean;
    height?: string;
    sx?: SxProps;
    language?: string;
}