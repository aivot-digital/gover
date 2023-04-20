import {ElementType} from '../../../../data/element-type/element-type';
import {BaseFormElement} from "../base-form-element";
import {BaseInputElement} from "../base-input-element";

export interface ReplicatingContainerLayout extends BaseInputElement<string[], ElementType.ReplicatingContainer> {
    required?: boolean;
    minimumRequiredSets?: number;
    maximumSets?: number;

    children: BaseFormElement<any>;

    headlineTemplate?: string;
    addLabel?: string;
    removeLabel?: string;
}
