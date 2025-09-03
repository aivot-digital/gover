import {FormDetailsResponseDTO} from '../../../modules/forms/dtos/form-details-response-dto';

export interface DeleteApplicationDialogProps {
    form?: FormDetailsResponseDTO;
    onDelete: () => void;
    onCancel: () => void;
}
