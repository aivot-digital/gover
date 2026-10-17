import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface DepartmentSelectInputElement extends BaseInputElement<ElementType.DepartmentSelectInput> {
    placeholder: string | null | undefined;
    dialogTitle: string | null | undefined;
}
