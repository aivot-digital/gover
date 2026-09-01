import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {type SxProps, type Theme} from '@mui/material';
import {type ReactNode} from 'react';
import {type EndAction} from '../text-field/text-field-component-props';
import {type DateFieldProps} from '@mui/x-date-pickers';
import {type DateValueIso} from '../../utils/temporal-types';
import {type FormFieldLayoutProps} from '../form-field';

export interface DateFieldComponentProps extends FormFieldLayoutProps {
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
    controlSx?: SxProps<Theme>;
    size?: 'small' | 'medium';
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    endAction?: EndAction | Array<EndAction>;
    startIcon?: ReactNode;
    muiPassTroughProps?: DateFieldProps;
}
