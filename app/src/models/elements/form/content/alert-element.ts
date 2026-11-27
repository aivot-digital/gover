import {AlertColor} from '@mui/material';
import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from '../base-form-element';

export interface AlertElement extends BaseFormElement<ElementType.Alert> {
    title: string | null | undefined;
    text: string | null | undefined;
    alertType: AlertColor | null | undefined;
}
