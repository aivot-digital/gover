import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {SxProps, TextFieldProps, Theme} from '@mui/material';
import {ReactNode} from 'react';
import {EndAction} from '../text-field/text-field-component-props';
import {DateFieldProps} from '@mui/x-date-pickers';
import {DateValueIso} from '../../utils/temporal-types';

export interface DateFieldComponentProps {
    label: string;
    error?: string;
    autocomplete?: string;
    hint?: string;
    hideHelperText?: boolean;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    value?: string | null;
    minDate?: string;
    maxDate?: string;
    mode: DateFieldComponentModelMode;
    onChange: (val: DateValueIso | null) => void;
    onBlur?: (val: DateValueIso | null) => void;
    sx?: SxProps<Theme>;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    endAction?: EndAction | Array<EndAction>;
    startIcon?: ReactNode;
    muiPassTroughProps?: DateFieldProps;
}
