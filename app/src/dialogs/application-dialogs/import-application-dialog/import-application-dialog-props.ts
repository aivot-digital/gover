import {type DialogProps} from '@mui/material/Dialog';
import {FormDetailsResponseDTO} from '../../../modules/forms/dtos/form-details-response-dto';

export interface ImportApplicationDialogProps extends DialogProps {
    onClose: () => void;
    onImport: (form: FormDetailsResponseDTO) => void;
}
