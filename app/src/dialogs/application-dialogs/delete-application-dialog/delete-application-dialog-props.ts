import {FormListProjection} from '../../../models/entities/form';

export interface DeleteApplicationDialogProps {
    application?: FormListProjection;
    onDelete: () => void;
    onCancel: () => void;
}
