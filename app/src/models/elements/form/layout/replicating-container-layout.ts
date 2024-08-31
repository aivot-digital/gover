import {ElementType} from '../../../../data/element-type/element-type';
import {BaseInputElement} from "../base-input-element";
import {AnyFormElement} from "../any-form-element";

export interface ReplicatingContainerLayout extends BaseInputElement<string[], ElementType.ReplicatingContainer> {
    required?: boolean;
    minimumRequiredSets?: number;
    maximumSets?: number;

    children: AnyFormElement[];

    headlineTemplate?: string;
    addLabel?: string;
    removeLabel?: string;
}
