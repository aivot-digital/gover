import {DialogProps} from '@mui/material/Dialog/Dialog';
import {Application} from '../../models/entities/application';

export interface ImportApplicationDialogProps extends DialogProps {
    onClose: () => void;
    onImport: (application: Application) => void;
}
