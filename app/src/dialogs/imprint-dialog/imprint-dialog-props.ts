import {DialogProps} from '@mui/material/Dialog';

export interface ImprintDialogProps extends DialogProps {
    onHide: () => void;
    isListingPage?: boolean;
}
