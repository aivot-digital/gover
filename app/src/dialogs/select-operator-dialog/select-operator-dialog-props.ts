import {NoCodeOperatorDetailsDTO} from '../../models/dtos/no-code-operator-details-dto';
import {NoCodeDataType} from '../../data/no-code-data-type';

export interface SelectOperatorDialogProps {
    open: boolean;
    operators: NoCodeOperatorDetailsDTO[];
    onSelect: (operator: NoCodeOperatorDetailsDTO) => void;
    onClose: () => void;
    desiredReturnType: NoCodeDataType;
}