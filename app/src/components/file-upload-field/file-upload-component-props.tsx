import type {SxProps, Theme} from '@mui/material';
import type {FormFieldLayoutProps} from '../form-field';

export interface FileUploadComponentProps extends FormFieldLayoutProps {
    value?: File[] | null;
    onChange: (val: File[] | null) => void;
    error?: string;
    label: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    extensions?: string[];
    isMultifile?: boolean;
    maxFiles?: number;
    minFiles?: number;
    hint?: string;
    controlSx?: SxProps<Theme>;
}
