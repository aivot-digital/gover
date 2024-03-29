export interface CheckboxFieldComponentProps {
    label: string;
    error?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    value?: boolean;
    onChange: (val: boolean) => void;
}
