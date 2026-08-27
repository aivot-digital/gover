import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface ProcessIdentityIdInputElement extends BaseInputElement<ElementType.ProcessIdentityIdInput> {
    placeholder: string | null | undefined;
    minItems: number | null | undefined;
    maxItems: number | null | undefined;
}
