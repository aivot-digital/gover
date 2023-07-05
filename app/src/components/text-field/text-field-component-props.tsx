export interface TextFieldComponentProps {
    label: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    multiline?: boolean;
    value?: string;
    error?: string;
    hint?: string;
    maxCharacters?: number;
    minCharacters?: number;
    rows?: number;
    type?: string;
    onChange: (val: string | undefined) => void;
    onBlur?: (val: string | undefined) => void;
}
