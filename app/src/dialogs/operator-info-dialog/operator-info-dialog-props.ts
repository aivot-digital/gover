import {NoCodeOperatorDetailsDTO} from '../../models/dtos/no-code-operator-details-dto';

export interface OperatorInfoDialogProps {
    operator?: NoCodeOperatorDetailsDTO;
    onClose: () => void;
}