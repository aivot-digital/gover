import {ReactNode} from 'react';
import {SxProps} from '@mui/material';

export interface TextFieldComponentProps {
    label: string;
    autocomplete?: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    display?: boolean;
    multiline?: boolean;
    value?: string;
    error?: string;
    hint?: string;
    maxCharacters?: number;
    minCharacters?: number;
    softLimitCharacters?: number;
    softLimitCharactersWarning?: string;
    rows?: number;
    type?: string;
    onChange: (val: string | undefined) => void;
    onBlur?: (val: string | undefined) => void;
    endAction?: EndAction | Array<EndAction>;
    startIcon?: ReactNode;
    pattern?: {
        regex: string;
        message: string;
    };
    sx?: SxProps;
    bufferInputUntilBlur?: boolean;
    debounce?: number;
    size?: 'small' | 'medium';
}

type EndAction = {
    icon: ReactNode;
    tooltip?: string;
    onClick: () => void;
};
