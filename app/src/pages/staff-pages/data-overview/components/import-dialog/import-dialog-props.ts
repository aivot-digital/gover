import {DialogProps} from '@mui/material/Dialog/Dialog';

export interface ImportDialogProps extends DialogProps {
    onImport: (files: File[]) => void;
    extension: string;
    onClose: () => void;
}
