import {SxProps} from '@mui/material';
import {AlertComponentProps} from '../alert/alert-component-props';

export interface CodeEditorProps {
    label?: string;
    value?: string;
    onChange: (value: string) => void;
    disabled?: boolean;
    height?: string;
    sx?: SxProps;
    language?: string;
    typeHints?: {
        name: string;
        content: string;
    }[];
    alert?: AlertComponentProps;
}