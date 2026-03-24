import {type AnyContentElement, isAnyContentElement} from './content/any-content-element';
import {type AnyInputElement, isAnyInputElement} from './input/any-input-element';
import {type AnyLayoutElement} from './layout/any-layout-element';
import {isGroupLayout} from './layout/group-layout';
import {isReplicatingContainerLayout} from './layout/replicating-container-layout';

export type AnyFormElement =
    AnyContentElement |
    AnyInputElement |
    AnyLayoutElement;

function isAnyLayoutElement(obj: any): obj is AnyLayoutElement {
    return isGroupLayout(obj) || isReplicatingContainerLayout(obj);
}

export function isAnyFormElement(obj: any): obj is AnyFormElement {
    return isAnyContentElement(obj) || isAnyInputElement(obj) || isAnyLayoutElement(obj);
}