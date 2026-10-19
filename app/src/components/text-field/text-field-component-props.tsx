import {type ReactNode} from 'react';
import {type SxProps, type TextFieldProps, type Theme} from '@mui/material';
import {type FormFieldLayoutProps} from '../form-field';

export interface TextFieldComponentProps extends FormFieldLayoutProps {
    label: string;
    autocomplete?: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    readonly?: boolean;
    busy?: boolean;
    display?: boolean;
    multiline?: boolean;
    value?: string | null | undefined;
    error?: string | string[];
    hint?: string;
    maxCharacters?: number;
    minCharacters?: number;
    softLimitCharacters?: number;
    softLimitCharactersWarning?: string;
    rows?: number;
    type?: string;
    onChange: (val: string | null) => void;
    onBlur?: (val: string | null) => void;
    endAction?: EndAction | Array<EndAction>;
    copyable?: boolean;
    copyValueTemplate?: string | null;
    startIcon?: ReactNode;
    pattern?: {
        regex: string;
        message: string;
    };
    /** Validates on first blur, then while correcting the value. */
    validate?: (value: string | null) => string | undefined;
    validationRequested?: boolean;
    /** Normalizes typed/pasted text before updating the visible input. */
    normalizeInput?: (value: string) => string;
    controlSx?: SxProps<Theme>;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    size?: 'small' | 'medium';
    muiPassTroughProps?: TextFieldProps;
}

export type EndAction = {
    icon: ReactNode;
    tooltip?: string;
    ariaLabel?: string;
    onClick: () => void;
    disabled?: boolean;
};
