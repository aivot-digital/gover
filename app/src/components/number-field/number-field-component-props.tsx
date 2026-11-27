import {type SxProps, type Theme} from '@mui/material';
import {type FormFieldLayoutProps} from '../form-field';

export interface NumberFieldComponentProps extends FormFieldLayoutProps {
    label: string;
    placeholder?: string;
    decimalPlaces?: number;
    hint?: string;
    error?: string;
    suffix?: string;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    value?: number | null;
    onChange: (val: number | null) => void;
    onBlur?: (val: number | null) => void;
    minValue?: number;
    maxValue?: number;
    controlSx?: SxProps<Theme>;
    size?: 'small' | 'medium';
    bufferInputUntilBlur?: boolean;
    debounce?: number;
}
