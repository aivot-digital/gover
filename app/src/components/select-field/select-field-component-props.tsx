import {SelectFieldComponentOption} from "./select-field-component-option";
import {SxProps, Theme} from "@mui/material";

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
}
