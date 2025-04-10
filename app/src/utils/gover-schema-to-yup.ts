import { AnyElement } from '../models/elements/any-element';
import { isAnyInputElement } from '../models/elements/form/input/any-input-element';
import { ElementType } from '../data/element-type/element-type';
import * as yup from 'yup';
import { isAnyElementWithChildren } from '../models/elements/any-element-with-children';

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
                }
                if (elem.pattern?.regex) {
                    try {
                        const regex = new RegExp(elem.pattern.regex);
                        fieldSchema = fieldSchema.matches(regex, elem.pattern.message || "Das Format der Eingabe ist ungültig.");
                    } catch (error) {
                        console.warn(`Ungültige Regex im Schema für Feld ${elem.id}:`, error);
                    }
                }
                break;

            case ElementType.Number:
                fieldSchema = yup.number();
                if (elem.required) {
                    fieldSchema = fieldSchema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
                }
                break;

            case ElementType.Select:
                fieldSchema = yup.string().trim();
                if (elem.required) {
                    fieldSchema = fieldSchema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
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

            default:
                console.warn(`Unhandled field type in goverSchemaToYup: ${elem.type}`);
                fieldSchema = yup.mixed();
        }

        res[elem.id] = fieldSchema;
    }

    // If element has children, recursively generate schema
    if (isAnyElementWithChildren(elem)) {
        for (const child of elem.children) {
            const childSchema = goverSchemaToYup(child);
            res = {
                ...res,
                ...childSchema,
            }
        }
    }

    return res;
}
