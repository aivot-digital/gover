import {SxProps, Theme} from '@mui/material';

export interface NumberFieldComponentProps {
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
}
