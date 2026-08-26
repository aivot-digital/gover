import {SxProps, Theme} from '@mui/material';

export interface NumberFieldComponentProps {
    id?: string;
    label: string;
    placeholder?: string;
    decimalPlaces?: number;
    hint?: string;
    error?: string;
    suffix?: string;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    value?: number | null;
    onChange: (val: number | null) => void;
    onBlur?: (val: number | null) => void;
    minValue?: number;
    maxValue?: number;
    sx?: SxProps<Theme>;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    margin?: 'none' | 'dense' | 'normal';
    size?: 'small' | 'medium';
    ariaLabelledBy?: string;
    ariaDescribedBy?: string;
    ariaInvalid?: boolean;
    ariaRequired?: boolean;
}
