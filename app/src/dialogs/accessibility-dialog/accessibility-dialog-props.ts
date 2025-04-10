import {DialogProps} from '@mui/material/Dialog/Dialog';

export interface AccessibilityDialogProps extends DialogProps {
    onHide: () => void;
    isListingPage?: boolean;
}
