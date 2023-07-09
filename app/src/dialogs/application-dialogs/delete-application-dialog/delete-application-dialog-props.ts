import { type ListApplication } from '../../../models/entities/list-application';

export interface DeleteApplicationDialogProps {
    application?: ListApplication;
    onDelete: () => void;
    onCancel: () => void;
}
