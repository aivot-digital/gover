import {ElementType} from '../../../../data/element-type/element-type';
import {BaseLayoutElement} from './base-layout-element';
import {BaseInputElement} from '../input-elements/base-input-element';
import {AnyFormElement} from '../any-form-element';

export interface ReplicatingContainerElement extends BaseLayoutElement<AnyFormElement, ElementType.ReplicatingContainer>, BaseInputElement<string[], ElementType.ReplicatingContainer> {
    required?: boolean;
    minimumRequiredSets?: number;
    maximumSets?: number;

    headlineTemplate?: string;
    addLabel?: string;
    removeLabel?: string;
}
