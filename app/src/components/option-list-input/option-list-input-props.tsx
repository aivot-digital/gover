export interface OptionListInputValue {
    label: string;
    value: string;
}

export interface OptionListInputProps {
    label: string;
    hint: string;
    addLabel: string;
    noItemsHint: string;
    value?: OptionListInputValue[];
    onChange: (ls: OptionListInputValue[] | undefined) => void;
    allowEmpty: boolean;
    disabled?: boolean;

    labelLabel?: string;
    keyLabel?: string;
}
