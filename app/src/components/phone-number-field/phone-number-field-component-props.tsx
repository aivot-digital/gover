import {SxProps, TextFieldProps} from '@mui/material';

export interface PhoneNumberFieldComponentProps {
    label: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    readonly?: boolean;
    busy?: boolean;
    value?: string | null | undefined;
    error?: string | string[];
    hint?: string;
    onChange: (val: string | null) => void;
    onBlur?: (val: string | null) => void;
    sx?: SxProps;
    size?: 'small' | 'medium';
    muiPassTroughProps?: TextFieldProps;
}
