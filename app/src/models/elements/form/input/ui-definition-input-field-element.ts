import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import type {AnyElement} from '../../any-element';

export interface UiDefinitionInputFieldElement extends BaseInputElement<ElementType.UiDefinitionInput> {
    elementType: ElementType | null | undefined;
}

export type UiDefinitionInputFieldElementItem = AnyElement;
