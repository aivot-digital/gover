import {type BaseFormElement} from '../base-form-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {type AnyFormElement} from '../any-form-element';

export interface GroupLayout extends BaseFormElement<ElementType.GroupLayout> {
    children: AnyFormElement[];
    marketplaceLink: GroupLayoutMarketplaceLink | null;
}

export interface GroupLayoutMarketplaceLink {
    marketplaceId: string;
    marketplaceVersion: string;
}

export function isGroupLayout(obj: any): obj is GroupLayout {
    return obj != null && obj.type === ElementType.GroupLayout;
}

export function isPresetGroupLayout(obj: any): boolean {
    return isGroupLayout(obj) && obj.marketplaceLink != null;
}
