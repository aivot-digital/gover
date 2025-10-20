import {AnyElement} from './elements/any-element';
import {ElementType} from '../data/element-type/element-type';

export type ElementDataObject = {
    $type: ElementType;
    inputValue: any | null | undefined;
    previousInputValue: any | null | undefined;
    isVisible: boolean | null | undefined;
    isPrefilled: boolean | null | undefined;
    isDirty: boolean | null | undefined;
    computedOverride: AnyElement | null | undefined;
    computedValue: any | null | undefined;
    computedErrors: string[] | null | undefined;
}

export function newElementDataObject(type: ElementType): ElementDataObject {
    return {
        $type: type,
        inputValue: null,
        previousInputValue: null,
        isVisible: true,
        isPrefilled: false,
        isDirty: false,
        computedOverride: null,
        computedValue: null,
        computedErrors: null,
    }
}

export type ElementData = Partial<Record<string, ElementDataObject>>;

export function hasElementDataSomeInput(elementData: ElementData | null | undefined): boolean {
    if (elementData == null) {
        return false;
    }

    for (const key of Object.keys(elementData)) {
        const elementDataObject = elementData[key];
        if (elementDataObject != null && elementDataObject.inputValue != null) {
            return true;
        }
    }
    return false;
}
