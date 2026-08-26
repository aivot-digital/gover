import {SelectFieldComponentOption} from './select-field-component-option';
import {SxProps, TextFieldProps, Theme} from '@mui/material';
import {EndAction} from '../text-field/text-field-component-props';

export interface SelectFieldComponentProps {
    label: string;
    ariaLabelledBy?: string;
    ariaDescribedBy?: string;
    ariaInvalid?: boolean;
    ariaRequired?: boolean;
    autocomplete?: string;
    placeholder?: string;
    hint?: string;
    disabled?: boolean;
    readOnly?: boolean;
    required?: boolean;
    error?: string;
    value?: string | null;
    onChange: (val: string | null) => void;
    options: SelectFieldComponentOption[];
    emptyStatePlaceholder?: string;
    includeEmptyOption?: boolean;
    sx?: SxProps<Theme>;
    startIcon?: React.ReactNode;
    endAction?: EndAction | Array<EndAction>;
    muiPassTroughProps?: TextFieldProps;
    size?: 'small' | 'medium';
}
