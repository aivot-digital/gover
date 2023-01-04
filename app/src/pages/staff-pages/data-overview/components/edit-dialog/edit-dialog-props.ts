import {DialogProps} from '@mui/material/Dialog/Dialog';
import {EditField} from '../../data-overview-props';

export interface EditDialogProps<T> extends DialogProps {
    item?: T;
    onClose: () => void;
    onSave: (item: T) => void;
    fieldsToEdit: (EditField<T> | string)[];
    toPrimaryString: (item: T) => string;
}
