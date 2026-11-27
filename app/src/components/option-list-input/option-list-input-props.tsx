import {type SxProps, type Theme} from '@mui/material';
import {type FormFieldGroupLayoutProps} from '../form-field';

export interface OptionListInputValue {
    label: string;
    value: string;
    group?: string | null | undefined;
}

export interface OptionListInputProps extends FormFieldGroupLayoutProps {
    label: string;
    hint: string;
    addLabel: string;
    noItemsHint: string;
    value?: OptionListInputValue[];
    onChange: (ls: OptionListInputValue[] | undefined) => void;
    allowEmpty: boolean;
    disabled?: boolean;
    busy?: boolean;
    readOnly?: boolean;
    error?: string;
    controlSx?: SxProps<Theme>;

    labelLabel?: string;
    keyLabel?: string;
    disableKeyField?: boolean;
    groupLabel?: string;
    showGroupField?: boolean;
    // Values the group field can take, e.g. the values of a referenced select. Without them, the group is free text.
    groupOptions?: Array<{value: string; label: string}>;

    variant?: 'elevation' | 'outlined';
}
