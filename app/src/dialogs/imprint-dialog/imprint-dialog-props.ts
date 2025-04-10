import {DialogProps} from '@mui/material/Dialog/Dialog';

export interface ImprintDialogProps extends DialogProps {
    onHide: () => void;
    isListingPage?: boolean;
}
