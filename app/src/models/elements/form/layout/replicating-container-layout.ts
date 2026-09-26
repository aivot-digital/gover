import {ElementType} from '../../../../data/element-type/element-type';
import {BaseInputElement} from '../base-input-element';
import {AnyFormElement} from '../any-form-element';
import {AnyElement} from '../../any-element';

export interface ReplicatingContainerLayout extends BaseInputElement<ElementType.ReplicatingContainer> {
    minimumRequiredSets: number | null | undefined;
    maximumSets: number | null | undefined;
    headlineTemplate: string | null | undefined;
    addLabel: string | null | undefined;
    removeLabel: string | null | undefined;
    children: AnyFormElement[] | null | undefined;
}

export function isReplicatingContainerLayout(element: AnyElement): element is ReplicatingContainerLayout {
    return element.type === ElementType.ReplicatingContainer;
}