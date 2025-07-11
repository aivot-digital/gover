import {FileUploadElementItem} from './elements/form/input/file-upload-element';
import {AnyElement} from './elements/any-element';
import {IdentityValue} from '../modules/identity/models/identity-value';
import {ElementType} from '../data/element-type/element-type';

type ElementInputValue =
    undefined |
    null |
    string | // Text, Date, Time, Radio, Select
    number | // Number
    boolean | // Checkbox
    string[] | // MultiSelect
    FileUploadElementItem[] | // FileUpload
    ElementInputs[] | // Replicating List Container
    Record<string, string | number | null | undefined>[] | // Table
    IdentityValue // Identity inputs
    ;

type AnyElementDataValue =
    undefined |
    null |
    string | // Text, Date, Time, Radio, Select
    number | // Number
    boolean | // Checkbox
    string[] | // MultiSelect
    FileUploadElementItem[] | // FileUpload
    ElementData[] | // Replicating List Container
    Record<string, string | number | null | undefined>[] | // Table
    IdentityValue // Identity inputs
    ;

export type AnyElementDataObject = {
    $type: ElementType;
    inputValue: AnyElementDataValue;
    isVisible: boolean | null | undefined;
    isPrefilled: boolean | null | undefined;
    isDirty: boolean | null | undefined;
    computedOverride: AnyElement | null | undefined;
    computedValue: AnyElementDataValue;
    computedErrors: string[] | null | undefined;
    value: AnyElementDataValue; // This is the combination of inputValue and computedValue
}

function isAnyElementDataObject(obj: any): obj is AnyElementDataObject {
    return obj && typeof obj === 'object' &&
        '$type' in obj &&
        'inputValue' in obj &&
        'isVisible' in obj &&
        'isPrefilled' in obj &&
        'isDirty' in obj &&
        'computedOverride' in obj &&
        'computedValue' in obj &&
        'computedErrors' in obj;
}

export type ElementData = Partial<Record<string, AnyElementDataObject>>;

export type ElementInputs = Record<string, ElementInputValue>;

export function walkElementData(
    data: ElementData,
    callback: (key: string, value: AnyElementDataObject) => void,
): void {
    for (const key in data) {
        const value = data[key];
        if (isAnyElementDataObject(value)) {
            callback(key, value);

            if (value.$type === ElementType.ReplicatingContainer) {
                const childData = value.value as ElementData[];
                if (Array.isArray(childData)) {
                    childData.forEach((child, index) => {
                        walkElementData(child, callback);
                    });
                }
            }
        }
    }
}