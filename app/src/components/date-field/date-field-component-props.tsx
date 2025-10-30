import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {SxProps, TextFieldProps, Theme} from '@mui/material';
import {ReactNode} from 'react';
import {EndAction} from '../text-field/text-field-component-props';
import {DateFieldProps} from '@mui/x-date-pickers';

export interface DateFieldComponentProps {
    label: string;
    error?: string;
    autocomplete?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    value?: string;
    minDate?: Date;
    maxDate?: Date;
    mode: DateFieldComponentModelMode;
    onChange: (val: string | undefined) => void;
    onBlur?: (val: string | undefined) => void;
    sx?: SxProps<Theme>;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    endAction?: EndAction | Array<EndAction>;
    startIcon?: ReactNode;
    muiPassTroughProps?: DateFieldProps;
}
