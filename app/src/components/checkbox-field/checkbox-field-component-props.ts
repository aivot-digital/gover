import {type SxProps, type Theme} from '@mui/material';
import {type FormFieldLayoutProps} from '../form-field';

export interface CheckboxFieldComponentProps extends FormFieldLayoutProps {
    label: string;
    error?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    readOnly?: boolean;
    value?: boolean | null;
    onChange: (val: boolean) => void;
    variant?: 'standard' | 'switch';
    controlSx?: SxProps<Theme>;
    invisibleLabel?: boolean;
}
