import {type DialogProps} from '@mui/material/Dialog/Dialog';
import {type Form as Application} from '../../../models/entities/form';

export interface ImportApplicationDialogProps extends DialogProps {
    onClose: () => void;
    onImport: (application: Application) => void;
}
