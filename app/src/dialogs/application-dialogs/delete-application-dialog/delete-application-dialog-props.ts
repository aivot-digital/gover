import {FormListResponseDTO} from '../../../modules/forms/dtos/form-list-response-dto';

export interface DeleteApplicationDialogProps {
    form?: FormListResponseDTO;
    onDelete: () => void;
    onCancel: () => void;
}
