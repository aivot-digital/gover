import {AlertColor} from '@mui/material';
import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';

export interface AlertElement extends BaseFormElement<ElementType.Alert> {
    title?: string;
    text?: string;
    alertType?: AlertColor;
}
