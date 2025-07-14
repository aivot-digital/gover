import {AnyElement} from './elements/any-element';
import {ElementType} from '../data/element-type/element-type';

export type ElementDataObject = {
    $type: ElementType;
    inputValue: any | null | undefined;
    isVisible: boolean | null | undefined;
    isPrefilled: boolean | null | undefined;
    isDirty: boolean | null | undefined;
    computedOverride: AnyElement | null | undefined;
    computedValue: any | null | undefined;
    computedErrors: string[] | null | undefined;
}

export type ElementData = Partial<Record<string, ElementDataObject>>;
