import {SxProps} from '@mui/material';

export interface SearchInputProps {
    value: string;
    onChange: (val: string) => void;
    placeholder: string;
    autoFocus?: boolean;
    sx?: SxProps;
}
