export interface StringListInputProps {
    label: string;
    hint: string;
    addLabel: string;
    noItemsHint: string;
    value?: string[];
    onChange: (ls: string[] | undefined) => void;
    allowEmpty: boolean;
    disabled?: boolean;
}
