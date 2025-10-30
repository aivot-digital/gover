import {SelectFieldComponentOption} from './select-field-component-option';
import {SxProps, TextFieldProps, Theme} from '@mui/material';
import {EndAction} from '../text-field/text-field-component-props';

export interface SelectFieldComponentProps {
    label: string;
    autocomplete?: string;
    placeholder?: string;
    hint?: string;
    disabled?: boolean;
    readOnly?: boolean;
    required?: boolean;
    error?: string;
    value?: string;
    onChange: (val: string | undefined) => void;
    options: SelectFieldComponentOption[];
    emptyStatePlaceholder?: string;
    sx?: SxProps<Theme>;
    startIcon?: React.ReactNode;
    endAction?: EndAction | Array<EndAction>;
    muiPassTroughProps?: TextFieldProps;
}
