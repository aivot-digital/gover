import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {AnyElement} from '../../any-element';
import {ElementDisplayContext} from '../../../../data/element-type/element-child-options';

export interface UiDefinitionInputFieldElement extends BaseInputElement<ElementType.UiDefinitionInput> {
    elementType: ElementType | null | undefined;
    displayContext: ElementDisplayContext | null | undefined;
}

export type UiDefinitionInputFieldElementItem = AnyElement;
