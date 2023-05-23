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
    onChange: (val: string | undefined) => void;
}
