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
import {ChipInputFieldElement} from '../models/elements/form/input/chip-input-field-element';
import {DateRangeFieldElement} from '../models/elements/form/input/date-range-field-element';
import {TimeRangeFieldElement} from '../models/elements/form/input/time-range-field-element';
import {DateTimeRangeFieldElement} from '../models/elements/form/input/date-time-range-field-element';
import {MapPointFieldElement} from '../models/elements/form/input/map-point-field-element';
import {DomainAndUserSelectItem, DomainUserSelectFieldElement} from '../models/elements/form/input/domain-user-select-field-element';
import {
    AssignmentContextFieldElement,
    AssignmentContextValue,
} from '../models/elements/form/input/assignment-context-field-element';
import {
    createDomainAndUserSelectValueKey,
    normalizeDomainAndUserSelectItem,
} from '../components/domain-user-select-field/domain-user-select-options';

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
    [ElementType.ChipInput]: chipInputFieldToYup,
    [ElementType.DateRange]: dateRangeFieldToYup,
    [ElementType.TimeRange]: timeRangeFieldToYup,
    [ElementType.DateTimeRange]: dateTimeRangeFieldToYup,
    [ElementType.MapPoint]: mapPointFieldToYup,
    [ElementType.DomainAndUserSelect]: domainUserSelectFieldToYup,
    [ElementType.AssignmentContext]: assignmentContextFieldToYup,
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

function chipInputFieldToYup(elem: ChipInputFieldElement): Schema {
    let chipSchema: yup.ArraySchema<(string | undefined)[] | undefined | null, yup.AnyObject, '', ''> = yup
        .array()
        .of(yup.string().trim());

    if (elem.required) {
        chipSchema = chipSchema
            .required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        chipSchema = chipSchema
            .nullable();
    }

    if ((elem.minItems ?? 0) > 0) {
        chipSchema = chipSchema
            .min(elem.minItems!, `Mindestens ${elem.minItems} Einträge erforderlich.`);
    }

    if ((elem.maxItems ?? 0) > 0) {
        chipSchema = chipSchema
            .max(elem.maxItems!, `Maximal ${elem.maxItems} Einträge erlaubt.`);
    }

    if (elem.allowDuplicates !== true) {
        chipSchema = chipSchema
            .test(
                'no-duplicates',
                'Mehrfach vorhandene Einträge sind nicht erlaubt.',
                (value) => value == null || new Set(value).size === value.length,
            );
    }

    return chipSchema;
}

function dateRangeFieldToYup(elem: DateRangeFieldElement): Schema {
    return buildRangeSchema(elem, 'date');
}

function timeRangeFieldToYup(elem: TimeRangeFieldElement): Schema {
    return buildRangeSchema(elem, 'time');
}

function dateTimeRangeFieldToYup(elem: DateTimeRangeFieldElement): Schema {
    return buildRangeSchema(elem, 'dateTime');
}

function buildRangeSchema(
    elem: DateRangeFieldElement | TimeRangeFieldElement | DateTimeRangeFieldElement,
    kind: 'date' | 'time' | 'dateTime',
): Schema {
    const allowOpenRange = elem.allowOpenRange === true;

    let rangeSchema: Schema = yup
        .object()
        .shape({
            start: yup.string().nullable(),
            end: yup.string().nullable(),
        })
        .test(
            'range-complete',
            'Bitte geben Sie sowohl den Start- als auch den Endwert an.',
            (value: any) => {
                if (allowOpenRange) {
                    return true;
                }

                if (value == null) {
                    return true;
                }

                const startFilled = value?.start != null && value.start.length > 0;
                const endFilled = value?.end != null && value.end.length > 0;

                // Valid states: both empty or both filled.
                return (startFilled && endFilled) || (!startFilled && !endFilled);
            },
        )
        .test(
            'range-order',
            'Der Startwert darf nicht größer als der Endwert sein.',
            (value: any) => {
                if (value == null) {
                    return true;
                }

                const start = value.start ?? undefined;
                const end = value.end ?? undefined;
                if (start == null || end == null || start.length === 0 || end.length === 0) {
                    return true;
                }

                const startValue = parseComparableRangeValue(start, kind);
                const endValue = parseComparableRangeValue(end, kind);
                if (startValue == null || endValue == null) {
                    return true;
                }

                return startValue <= endValue;
            },
        );

    if (elem.required) {
        rangeSchema = rangeSchema
            .required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`)
            .test(
                'range-filled',
                `${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`,
                (value: any) => {
                    if (allowOpenRange) {
                        return (value?.start != null && value.start.length > 0) || (value?.end != null && value.end.length > 0);
                    }

                    return value?.start != null && value.start.length > 0 && value?.end != null && value.end.length > 0;
                },
            );
    } else {
        rangeSchema = rangeSchema
            .nullable();
    }

    return rangeSchema;
}

function parseComparableRangeValue(value: string, kind: 'date' | 'time' | 'dateTime'): number | null {
    const parsed = new Date(value);
    if (isNaN(parsed.getTime())) {
        return null;
    }

    if (kind === 'time') {
        return parsed.getHours() * 60 + parsed.getMinutes();
    }

    return parsed.getTime();
}

function mapPointFieldToYup(elem: MapPointFieldElement): Schema {
    let mapPointSchema: Schema = yup
        .object()
        .shape({
            latitude: yup.number().nullable(),
            longitude: yup.number().nullable(),
            address: yup.string().nullable(),
        })
        .test(
            'map-point-pair',
            'Bitte geben Sie sowohl Breitengrad als auch Längengrad an.',
            (value: any) => {
                if (value == null) {
                    return true;
                }

                const hasLat = value?.latitude != null;
                const hasLon = value?.longitude != null;
                return (hasLat && hasLon) || (!hasLat && !hasLon);
            },
        );

    if (elem.required) {
        mapPointSchema = mapPointSchema
            .required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`)
            .test(
                'map-point-required',
                `${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`,
                (value: any) => value?.latitude != null && value?.longitude != null,
            );
    } else {
        mapPointSchema = mapPointSchema
            .nullable();
    }

    return mapPointSchema;
}

function createDomainAndUserSelectionSchema(
    opts: {
        required: boolean;
        label: string;
        minItems?: number | null;
        maxItems?: number | null;
        allowedTypes?: string[] | null;
    },
): yup.ArraySchema<(DomainAndUserSelectItem | undefined)[] | undefined | null, yup.AnyObject, '', ''> {
    let itemSchema: yup.ArraySchema<(DomainAndUserSelectItem | undefined)[] | undefined | null, yup.AnyObject, '', ''> = yup
        .array()
        .of(
            yup
                .object({
                    type: yup.string().oneOf(['orgUnit', 'team', 'user']).required(),
                    id: yup.string().trim().required(),
                })
                .required()
                .test(
                    'normalize-item',
                    'Ungültiger Eintrag.',
                    (value) => normalizeDomainAndUserSelectItem(value) != null,
                ),
        );

    if (opts.required) {
        itemSchema = itemSchema
            .required(`${opts.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        itemSchema = itemSchema
            .nullable();
    }

    if ((opts.minItems ?? 0) > 0) {
        itemSchema = itemSchema
            .min(opts.minItems!, `Mindestens ${opts.minItems} Einträge erforderlich.`);
    }

    if ((opts.maxItems ?? 0) > 0) {
        itemSchema = itemSchema
            .max(opts.maxItems!, `Maximal ${opts.maxItems} Einträge erlaubt.`);
    }

    itemSchema = itemSchema
        .test(
            'no-duplicates',
            'Mehrfach vorhandene Einträge sind nicht erlaubt.',
            (value) => {
                if (value == null) {
                    return true;
                }

                const normalizedValues = value
                    .map((entry) => normalizeDomainAndUserSelectItem(entry))
                    .filter((entry): entry is DomainAndUserSelectItem => entry != null);

                return new Set(normalizedValues.map(createDomainAndUserSelectValueKey)).size === normalizedValues.length;
            },
        );

    if (opts.allowedTypes != null) {
        const allowedTypes = new Set(opts.allowedTypes);
        itemSchema = itemSchema
            .test(
                'allowed-types',
                'Eintragstyp ist laut Konfiguration nicht erlaubt.',
                (value) => {
                    if (value == null) {
                        return true;
                    }

                    return value
                        .map((entry) => normalizeDomainAndUserSelectItem(entry))
                        .filter((entry): entry is DomainAndUserSelectItem => entry != null)
                        .every((entry) => allowedTypes.has(entry.type));
                },
            );
    }

    return itemSchema;
}

function domainUserSelectFieldToYup(elem: DomainUserSelectFieldElement): Schema {
    return createDomainAndUserSelectionSchema({
        required: elem.required === true,
        label: elem.label ?? 'Dieses Feld',
        minItems: elem.minItems,
        maxItems: elem.maxItems,
        allowedTypes: elem.allowedTypes,
    });
}

function assignmentContextFieldToYup(elem: AssignmentContextFieldElement): Schema {
    let domainSelectionSchema = createDomainAndUserSelectionSchema({
        required: elem.required === true,
        label: elem.label ?? 'Dieses Feld',
        minItems: elem.minItems,
        maxItems: elem.maxItems,
        allowedTypes: elem.allowedTypes,
    });

    if (elem.required) {
        domainSelectionSchema = domainSelectionSchema
            .min(1, `Mindestens 1 Eintrag erforderlich.`);
    }

    let assignmentContextSchema: Schema = yup
        .object()
        .shape({
            domainAndUserSelection: domainSelectionSchema,
            preferPreviousTaskAssignee: yup.boolean().nullable(),
            preferUninvolvedUser: yup.boolean().nullable(),
            preferProcessInstanceAssignee: yup.boolean().nullable(),
        })
        .test(
            'single-preference-only',
            'Es darf nur eine Bevorzugungs-Option ausgewählt werden.',
            (value: unknown) => {
                if (value == null || typeof value !== 'object') {
                    return true;
                }

                const typedValue = value as AssignmentContextValue;
                const enabledPreferences = [
                    typedValue.preferPreviousTaskAssignee === true,
                    typedValue.preferUninvolvedUser === true,
                    typedValue.preferProcessInstanceAssignee === true,
                ]
                    .filter((entry) => entry).length;

                return enabledPreferences <= 1;
            },
        )
        .test(
            'normalize-assignment-context',
            'Ungültiger Eintrag.',
            (value: unknown) => {
                if (value == null) {
                    return elem.required !== true;
                }

                const typedValue = value as AssignmentContextValue;
                const hasSelection = (typedValue.domainAndUserSelection ?? []).length > 0;
                const hasPreference = typedValue.preferPreviousTaskAssignee === true ||
                    typedValue.preferUninvolvedUser === true ||
                    typedValue.preferProcessInstanceAssignee === true;

                if (!hasSelection && !hasPreference) {
                    return elem.required !== true;
                }

                return true;
            },
        );

    if (elem.required) {
        assignmentContextSchema = assignmentContextSchema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        assignmentContextSchema = assignmentContextSchema.nullable();
    }

    return assignmentContextSchema;
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
