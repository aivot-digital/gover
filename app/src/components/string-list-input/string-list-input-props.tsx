import {type SxProps, type Theme} from '@mui/material';
import {type FormFieldGroupLayoutProps} from '../form-field';

export interface StringListInputProps extends FormFieldGroupLayoutProps {
    label: string;
    hint: string;
    addLabel: string;
    noItemsHint: string;
    value?: string[];
    onChange: (ls: string[] | undefined) => void;
    allowEmpty: boolean;
    disabled?: boolean;
    busy?: boolean;
    readOnly?: boolean;
    error?: string;
    controlSx?: SxProps<Theme>;
}
