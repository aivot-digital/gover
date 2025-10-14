import {AnyElement} from '../models/elements/any-element';
import {AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {ElementType} from '../data/element-type/element-type';
import * as yup from 'yup';
import {NumberSchema, Schema, StringSchema} from 'yup';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isReplicatingContainerLayout, ReplicatingContainerLayout} from '../models/elements/form/layout/replicating-container-layout';
import {TextFieldElement} from '../models/elements/form/input/text-field-element';
import {SelectFieldElement} from '../models/elements/form/input/select-field-element';
import {RadioFieldElement} from '../models/elements/form/input/radio-field-element';
import {ElementData, ElementDataObject, newElementDataObject} from '../models/element-data';
import {DataObjectItem} from '../modules/data-objects/models/data-object-item';
import {mapElementData} from './element-data-utils';

/**
 * @deprecated Use goverSchemaToYup2 instead
 * @param elem
 */
export function goverSchemaToYup(elem: AnyElement): any {
    let res: Record<string, any> = {};

    if (isAnyInputElement(elem)) {
        let fieldSchema;

        switch (elem.type) {
            case ElementType.Text:
                fieldSchema = yup.string().trim();
                if (elem.minCharacters) {
                    fieldSchema = fieldSchema.min(elem.minCharacters, `Mindestens ${elem.minCharacters} Zeichen erforderlich.`);
                }
                if (elem.maxCharacters) {
                    fieldSchema = fieldSchema.max(elem.maxCharacters, `Maximal ${elem.maxCharacters} Zeichen erlaubt.`);
                }
                if (elem.required) {
                    fieldSchema = fieldSchema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
                } else {
                    fieldSchema = fieldSchema.nullable();
                }
                if (elem.pattern?.regex) {
                    try {
                        const regex = new RegExp(elem.pattern.regex);
                        fieldSchema = fieldSchema.matches(regex, elem.pattern.message || 'Das Format der Eingabe ist ungültig.');
                    } catch (error) {
                        console.warn(`Ungültige Regex im Schema für Feld ${elem.id}:`, error);
                    }
                }
                break;

            case ElementType.Number:
                fieldSchema = yup.number();
                if (elem.required) {
                    fieldSchema = fieldSchema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
                } else {
                    fieldSchema = fieldSchema.nullable();
                }
                break;

            case ElementType.Select:
                fieldSchema = yup.string().trim();
                if (elem.required) {
                    fieldSchema = fieldSchema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
                } else {
                    fieldSchema = fieldSchema.nullable();
                }
                if (elem.options) {
                    const validValues = elem.options
                        ?.map(opt => (typeof opt === 'object' && 'value' in opt ? opt.value : opt))
                        .filter(value => typeof value === 'string' || typeof value === 'number'); // Ensure valid values
                    if (validValues.length > 0) {
                        fieldSchema = fieldSchema.oneOf(validValues, `Ungültiger Wert ausgewählt.`);
                    }
                }
                break;

            case ElementType.ReplicatingContainer:
                let childSchemaObject: Record<string, any> = {};
                for (const child of elem.children ?? []) {
                    childSchemaObject[child.id] = {
                        inputValue: goverSchemaToYup(child)[child.id],
                    };
                }

                const childSchema = yup
                    .object()
                    .shape(childSchemaObject);

                fieldSchema = yup
                    .array()
                    .of(childSchema);

                if (elem.required) {
                    fieldSchema = fieldSchema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
                } else {
                    fieldSchema = fieldSchema.nullable();
                }

                break;

            default:
                console.warn(`Unhandled field type in goverSchemaToYup: ${elem.type}`);
                fieldSchema = yup.mixed();
        }

        res[elem.id] = yup.object().shape({
            inputValue: fieldSchema,
        });
    }

    // If element has children, recursively generate schema
    if (isAnyElementWithChildren(elem) && !isReplicatingContainerLayout(elem)) {
        for (const child of elem.children ?? []) {
            const childSchema = goverSchemaToYup(child);
            res = {
                ...res,
                ...childSchema,
            };
        }
    }

    return res;
}


export function goverSchemaToYup2(elem: AnyElement): Record<string, Schema> {
    let elementDataShape: Record<string, Schema> = {};

    if (isAnyInputElement(elem)) {
        // Get mapper function based on element type, default to generic mapper
        const schemaMapper = YupSchemaMap[elem.type] ?? genericFieldToYup;

        // Generate schema for the element
        const elementSchema = schemaMapper(elem);

        // Generate the nested object shape for the underlying ElementDataObject
        let elementdataObjectShape = yup.object().shape({
            inputValue: elementSchema,
        });

        if (elem.required) {
            elementdataObjectShape = elementdataObjectShape
                .required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
        }

        // Extend the existing shape with the new element schema
        elementDataShape = {
            ...elementDataShape,
            [elem.id]: elementdataObjectShape,
        };
    }

    // If element has children, recursively generate schema. Omit children of replicating containers because they were handled previously.
    if (isAnyElementWithChildren(elem) && !isReplicatingContainerLayout(elem)) {
        for (const child of elem.children ?? []) {
            const childSchema = goverSchemaToYup2(child);

            elementDataShape = {
                ...elementDataShape,
                ...childSchema,
            };
        }
    }

    return elementDataShape;
}

const YupSchemaMap: {
    [T in ElementType]?: (elem: any) => Schema;
} = {
    [ElementType.Text]: textFieldToYup,
    [ElementType.Number]: numberFieldToYup,
    [ElementType.Select]: selectFieldToYup,
    [ElementType.Radio]: selectFieldToYup,
    [ElementType.ReplicatingContainer]: replicatingContainerToYup,
};

function genericFieldToYup(elem: AnyInputElement): Schema {
    if (elem.required) {
        return yup.mixed().required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        return yup.mixed().nullable();
    }
}

function textFieldToYup(elem: TextFieldElement): Schema {
    let textFieldSchema: StringSchema<string | undefined | null> = yup
        .string()
        .trim();

    if (elem.minCharacters) {
        textFieldSchema = textFieldSchema
            .min(elem.minCharacters, `Mindestens ${elem.minCharacters} Zeichen erforderlich.`);
    }

    if (elem.maxCharacters) {
        textFieldSchema = textFieldSchema
            .max(elem.maxCharacters, `Maximal ${elem.maxCharacters} Zeichen erlaubt.`);
    }

    if (elem.required) {
        textFieldSchema = textFieldSchema
            .required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        textFieldSchema = textFieldSchema
            .nullable();
    }

    if (elem.pattern?.regex) {
        try {
            const regex = new RegExp(elem.pattern.regex);
            textFieldSchema = textFieldSchema
                .matches(regex, elem.pattern.message || 'Das Format der Eingabe ist ungültig.');
        } catch (error) {
            console.warn(`Ungültige Regex im Schema für Feld ${elem.id}:`, error);
        }
    }

    return textFieldSchema;
}

function numberFieldToYup(elem: AnyInputElement): Schema {
    let numberFieldSchema: NumberSchema<number | undefined | null> = yup
        .number();

    if (elem.required) {
        numberFieldSchema = numberFieldSchema
            .required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        numberFieldSchema = numberFieldSchema
            .nullable();
    }

    return numberFieldSchema;
}

function selectFieldToYup(elem: SelectFieldElement | RadioFieldElement): Schema {
    let selectFieldSchema: StringSchema<string | undefined | null> = yup
        .string()
        .trim();

    if (elem.required) {
        selectFieldSchema = selectFieldSchema
            .required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        selectFieldSchema = selectFieldSchema
            .nullable();
    }

    if (elem.options) {
        const validValues = (elem.options ?? [])
            .map(opt => opt.value);

        if (validValues.length > 0) {
            selectFieldSchema = selectFieldSchema
                .oneOf(validValues, `Ungültiger Wert ausgewählt.`);
        }
    }

    return selectFieldSchema;
}

function replicatingContainerToYup(elem: ReplicatingContainerLayout): Schema {
    let childShape: Record<string, Schema> = {};

    for (const child of elem.children ?? []) {
        const childSchema = goverSchemaToYup2(child);
        childShape = {
            ...childShape,
            ...childSchema,
        };
    }

    const childSchema = yup
        .object()
        .shape(childShape);

    let elementShema: any = yup
        .array()
        .of(childSchema);

    if (elem.required) {
        elementShema = elementShema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        elementShema = elementShema.nullable();
    }

    return elementShema;
}

export function applyYupErrorsToElementData(rootElement: AnyElement, elementData: ElementData, errors: Record<string, string>): ElementData {
    console.log(errors);

    function applyErrorsToElementData(element: AnyElement, value: ElementDataObject | null | undefined, parents: Array<AnyElement | number>) {
        if (!isAnyInputElement(element)) {
            return null;
        }

        let val: ElementDataObject = {
            ...newElementDataObject(element.type),
            ...value,
            computedErrors: null,
        };

        let path = 'data.';
        for (const p of parents) {
            if (typeof p === 'number') {
                path += `inputValue[${p}].`;
            } else if (isReplicatingContainerLayout(p)) {
                path += `${p.id}.`;
            }
        }

        path += `${element.id}.inputValue`;

        console.log(path);

        const err = errors[path as keyof DataObjectItem];
        if (err != null) {
            val.computedErrors = [err];
        }

        return val;
    }

    const updatedElementData = mapElementData(
        rootElement,
        elementData,
        applyErrorsToElementData,
    );

    return updatedElementData;
}