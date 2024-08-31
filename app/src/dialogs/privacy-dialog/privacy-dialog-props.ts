import {DialogProps} from '@mui/material/Dialog/Dialog';

export interface PrivacyDialogProps extends DialogProps {
    onHide: () => void;
    isListingPage?: boolean;
}
