import {type BaseInputElement} from '../base-input-element';
import {type ElementType} from '../../../../data/element-type/element-type';

export interface SecretSelectInputElement extends BaseInputElement<ElementType.SecretSelectInput> {
    placeholder: string | null | undefined;
}
