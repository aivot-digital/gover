import {FormListResponseDTO} from '../../../modules/forms/dtos/form-list-response-dto';

export interface DeleteApplicationDialogProps {
    form?: FormListResponseDTO;
    onDelete: (form: FormListResponseDTO) => void;
    onCancel: () => void;
}
