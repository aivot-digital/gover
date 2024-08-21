import {type BaseFormElement} from '../base-form-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {type AnyFormElement} from '../any-form-element';

export interface GroupLayout extends BaseFormElement<ElementType.Container> {
    children: AnyFormElement[];
    storeLink: GroupLayoutStoreLink | null;
}

export interface GroupLayoutStoreLink {
    storeId: string;
    storeVersion: string;
}

export function isGroupLayout(obj: any): obj is GroupLayout {
    return obj != null && obj.type === ElementType.Container;
}

export function isPresetGroupLayout(obj: any): boolean {
    return isGroupLayout(obj) && obj.storeLink != null;
}
