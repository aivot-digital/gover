import {SelectFieldComponentOption} from "./select-field-component-option";

export interface SelectFieldComponentProps {
    label: string;
    autocomplete?: string;
    placeholder?: string;
    hint?: string;
    disabled?: boolean;
    required?: boolean;
    error?: string;
    value?: string;
    onChange: (val: string | undefined) => void;
    options: SelectFieldComponentOption[];
    emptyStatePlaceholder?: string;
}
