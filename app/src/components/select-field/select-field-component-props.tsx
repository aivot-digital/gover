import {
    type SelectFieldComponentOption,
    type SelectFieldValue,
} from './select-field-component-option';
import {type SxProps, type TextFieldProps, type Theme} from '@mui/material';
import {type EndAction} from '../text-field/text-field-component-props';
import {type ReactNode} from 'react';
import {type FormFieldLayoutProps} from '../form-field';
import {SelectFieldPresentation} from '../../models/elements/form/input/select-field-presentation';

export interface SelectFieldComponentProps<T extends SelectFieldValue = string> extends FormFieldLayoutProps {
    label: string;
    autocomplete?: string;
    placeholder?: string;
    hint?: string;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    required?: boolean;
    error?: string;
    value?: T | null;
    onChange: (val: T | null) => void;
    options: SelectFieldComponentOption<T>[];
    presentation?: SelectFieldPresentation;
    emptyStatePlaceholder?: string;
    includeEmptyOption?: boolean;
    controlSx?: SxProps<Theme>;
    startIcon?: ReactNode;
    endAction?: EndAction | Array<EndAction>;
    muiPassTroughProps?: TextFieldProps;
    size?: 'small' | 'medium';
}
