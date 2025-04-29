import type {DialogProps} from "@mui/material/Dialog/Dialog";

export interface ExportApplicationDialogProps  extends DialogProps{
    open: boolean;
    onCancel: () => void;
    onExport: () => void;
}