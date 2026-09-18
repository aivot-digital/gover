import {type SxProps, type TextFieldProps, type Theme} from '@mui/material';
import {type FormFieldLayoutProps} from '../form-field';

export interface PhoneNumberFieldComponentProps extends FormFieldLayoutProps {
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
    controlSx?: SxProps<Theme>;
    size?: 'small' | 'medium';
    muiPassTroughProps?: TextFieldProps;
}
