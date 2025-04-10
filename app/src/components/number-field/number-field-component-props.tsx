import {SxProps, Theme} from "@mui/material";

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
    value?: number;
    onChange: (val: number | undefined) => void;
    onBlur?: (val: number | undefined) => void;
    minValue?: number;
    maxValue?: number;
    sx?: SxProps<Theme>;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
}
