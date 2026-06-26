import {SxProps} from '@mui/material';

export interface CheckboxFieldComponentProps {
    label: string;
    error?: string;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    value?: boolean | null;
    onChange: (val: boolean) => void;
    variant?: 'standard' | 'switch';
    sx?: SxProps;
    invisibleLabel?: boolean;
}
