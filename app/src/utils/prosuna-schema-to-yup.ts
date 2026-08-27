import {AnyElement} from '../models/elements/any-element';
import {AnyInputElement, isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {ElementType} from '../data/element-type/element-type';
import * as yup from 'yup';
import {NumberSchema, Schema, StringSchema} from 'yup';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {
    isReplicatingContainerLayout,
    ReplicatingContainerLayout,
} from '../models/elements/form/layout/replicating-container-layout';
import {TextFieldElement} from '../models/elements/form/input/text-field-element';
import {SelectFieldElement} from '../models/elements/form/input/select-field-element';
import {RadioFieldElement} from '../models/elements/form/input/radio-field-element';
import {
    applyComputedErrors,
    AuthoredElementValues,
    ComputedElementErrors,
    ComputedElementStates,
    createComputedElementSubState,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    isReplicatingContainerElementValue,
    resolveComputedElementSubState,
    resolveComputedElementSubStateStates,
    resolveReplicatingContainerElementValues,
} from '../models/element-data';
import {ChipInputFieldElement} from '../models/elements/form/input/chip-input-field-element';
import {DateRangeFieldElement} from '../models/elements/form/input/date-range-field-element';
import {TimeRangeFieldElement} from '../models/elements/form/input/time-range-field-element';
import {DateTimeRangeFieldElement} from '../models/elements/form/input/date-time-range-field-element';
import {MapPointFieldElement} from '../models/elements/form/input/map-point-field-element';
import {
    DomainAndUserSelectItem,
    DomainUserSelectFieldElement,
} from '../models/elements/form/input/domain-user-select-field-element';
import {
    AssignmentContextFieldElement,
    AssignmentContextValue,
} from '../models/elements/form/input/assignment-context-field-element';
import {DataModelSelectFieldElement} from '../models/elements/form/input/data-model-select-field-element';
import {DataObjectSelectFieldElement} from '../models/elements/form/input/data-object-select-field-element';
import {NoCodeInputFieldElement} from '../models/elements/form/input/no-code-input-field-element';
import {
    createDomainAndUserSelectValueKey,
    normalizeDomainAndUserSelectItem,
} from '../components/domain-user-select-field/domain-user-select-options';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';
import {
    compareInstantIso,
    CalendarDatePrecision,
    dateValueToDateTime,
    isInstantIso,
    localTimeIsoToDateTime,
} from './temporal-utils';
import {DateFieldComponentModelMode} from '../models/elements/form/input/date-field-element';


export function prosunaSchemaToYup(elem: AnyElement, states: ComputedElementStates): Record<string, Schema> {
    // Invisible elements will not be validated
    if (states[elem.id]?.visible === false) {
        return {};
    }

    let elementDataShape: Record<string, Schema> = {};

    if (isAnyInputElement(elem)) {
        // Get mapper function based on element type, default to generic mapper
        const schemaMapper = YupSchemaMap[elem.type] ?? genericFieldToYup;

        // Generate schema for the element
        const elementSchema = schemaMapper(elem, states);

        // Extend the existing shape with the new element schema
        elementDataShape = {
            ...elementDataShape,
            [elem.id]: elementSchema,
        };
    }

    // If element has children, recursively generate schema. Omit children of replicating containers because they were handled previously.
    if (isAnyElementWithChildren(elem) && !isReplicatingContainerLayout(elem)) {
        for (const child of elem.children ?? []) {
            const childSchema = prosunaSchemaToYup(child, states);

            elementDataShape = {
                ...elementDataShape,
                ...childSchema,
            };
        }
    }

    return elementDataShape;
}

const YupSchemaMap: {
    [T in ElementType]?: (elem: any, states: ComputedElementStates) => Schema;
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
    [ElementType.DataModelSelect]: dynamicSelectFieldToYup,
    [ElementType.DataObjectSelect]: dynamicSelectFieldToYup,
    [ElementType.ProcessDataKeyInput]: processDataKeyInputFieldToYup,
    [ElementType.NoCodeInput]: noCodeInputFieldToYup,
    [ElementType.HtmlTemplateInput]: htmlTemplateInputFieldToYup,
    [ElementType.StoragePathSelector]: storagePathSelectorInputFieldToYup,
    [ElementType.ReplicatingContainer]: replicatingContainerToYup,
    [ElementType.ProcessInstanceAttachmentSetSelect]: chipInputFieldToYup,
    [ElementType.ProcessIdentityIdInput]: chipInputFieldToYup,
};

function genericFieldToYup(elem: AnyInputElement): Schema {
    if (elem.required) {
        return yup.mixed().required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        return yup.mixed().nullable();
    }
}

function htmlTemplateInputFieldToYup(elem: AnyInputElement): Schema {
    let schema: Schema = yup
        .object()
        .shape({
            assetKey: yup.string().nullable(),
            slots: yup.object().nullable(),
        })
        .nullable();

    if (elem.required) {
        schema = schema
            .test(
                'html-template-asset-key-required',
                `${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`,
                (value: any) => typeof value?.assetKey === 'string' && value.assetKey.trim().length > 0,
            );
    }

    return schema;
}

function storagePathSelectorInputFieldToYup(elem: AnyInputElement): Schema {
    return yup
        .object()
        .shape({
            storageProviderId: yup.number().nullable(),
            path: yup.string().trim().nullable(),
        })
        .nullable()
        .test(
            'storage-path-selector-complete',
            `${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`,
            (value: any) => {
                const hasStorageProvider = value?.storageProviderId != null;
                const hasPath = typeof value?.path === 'string' && value.path.trim().length > 0;

                if (!hasStorageProvider && !hasPath) {
                    return elem.required !== true;
                }

                return hasStorageProvider && hasPath;
            },
        );
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

    if ((elem.optionsSource ?? OptionsSourceType.Manual) === OptionsSourceType.Manual && elem.options) {
        const validValues = (elem.options ?? [])
            .map(opt => typeof opt === 'string' ? opt : opt.value);

        if (validValues.length > 0) {
            selectFieldSchema = selectFieldSchema
                .oneOf(validValues, `Ungültiger Wert ausgewählt.`);
        }
    }

    return selectFieldSchema;
}

function dynamicSelectFieldToYup(elem: DataModelSelectFieldElement | DataObjectSelectFieldElement): Schema {
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

    return selectFieldSchema;
}

function processDataKeyInputFieldToYup(elem: AnyInputElement): Schema {
    let processDataKeySchema: StringSchema<string | undefined | null> = yup
        .string()
        .trim();

    if (elem.required) {
        processDataKeySchema = processDataKeySchema
            .required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        processDataKeySchema = processDataKeySchema
            .nullable();
    }

    return processDataKeySchema
        .matches(
            /^[a-zA-Z0-9.*_]+$/,
            'Der Prozessdaten-Schlüssel darf nur Buchstaben, Zahlen, Punkte, Unterstriche und Sternchen enthalten.',
        );
}

function replicatingContainerToYup(elem: ReplicatingContainerLayout, states: ComputedElementStates): Schema {
    const rowSchema = yup.lazy((row, options) => {
        const path = (options as {path?: string}).path;
        const rowIndex = typeof path === 'string' ? parseArrayIndex(path) : -1;
        const rowId = isReplicatingContainerElementValue(row) ? row.id : null;
        const rowStates = resolveComputedElementSubStateStates(resolveComputedElementSubState(states[elem.id]?.subStates, rowId, rowIndex));
        let childShape: Record<string, Schema> = {};

        for (const child of elem.children ?? []) {
            const childSchema = prosunaSchemaToYup(child, rowStates);
            childShape = {
                ...childShape,
                ...childSchema,
            };
        }

        return yup
            .object()
            .shape({
                id: yup.string().nullable(),
                values: yup
                    .object()
                    .shape(childShape),
            });
    });

    let elementShema: any = yup
        .array()
        .of(rowSchema);

    if (elem.required) {
        elementShema = elementShema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        elementShema = elementShema.nullable();
    }

    return elementShema;
}

function parseArrayIndex(path: string): number {
    const match = path.match(/\[(\d+)]$/);
    return match == null ? -1 : parseInt(match[1], 10);
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

    if (elem.required === true && (elem.minItems ?? 0) > 0) {
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
    const precision = dateModeToPrecision(elem.mode);
    return buildRangeSchema(
        elem,
        (value) => dateValueToDateTime(value, precision)?.toMillis() ?? null,
        compareNumbers,
    );
}

function timeRangeFieldToYup(elem: TimeRangeFieldElement): Schema {
    return buildRangeSchema(
        elem,
        (value) => localTimeIsoToDateTime(value)?.toMillis() ?? null,
        compareNumbers,
    );
}

function dateTimeRangeFieldToYup(elem: DateTimeRangeFieldElement): Schema {
    return buildRangeSchema(
        elem,
        (value) => isInstantIso(value) ? value : null,
        (start, end) => compareInstantIso(start, end)!,
    );
}

function dateModeToPrecision(mode: DateFieldComponentModelMode | null | undefined): CalendarDatePrecision {
    switch (mode) {
        case DateFieldComponentModelMode.Month:
            return 'month';
        case DateFieldComponentModelMode.Year:
            return 'year';
        default:
            return 'day';
    }
}

function compareNumbers(left: number, right: number): number {
    return left - right;
}

function buildRangeSchema<T>(
    elem: DateRangeFieldElement | TimeRangeFieldElement | DateTimeRangeFieldElement,
    parseValue: (value: string) => T | null,
    compareValues: (left: T, right: T) => number,
): Schema {
    let rangeSchema: Schema = yup
        .object()
        .shape({
            start: temporalRangeBoundarySchema(parseValue),
            end: temporalRangeBoundarySchema(parseValue),
        })
        .test(
            'range-complete',
            'Bitte geben Sie sowohl den Start- als auch den Endwert an.',
            (value: any) => {
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

                const startValue = parseValue(start);
                const endValue = parseValue(end);
                if (startValue == null || endValue == null) {
                    return true;
                }

                return compareValues(startValue, endValue) <= 0;
            },
        );

    if (elem.required) {
        rangeSchema = rangeSchema
            .required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`)
            .test(
                'range-filled',
                `${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`,
                (value: any) => value?.start != null && value.start.length > 0 && value?.end != null && value.end.length > 0,
            );
    } else {
        rangeSchema = rangeSchema
            .nullable();
    }

    return rangeSchema;
}

function temporalRangeBoundarySchema<T>(parseValue: (value: string) => T | null): Schema {
    return yup
        .string()
        .nullable()
        .test(
            'temporal-format',
            'Der Wert besitzt kein gültiges Datums- oder Zeitformat.',
            (value) => value == null || value.length === 0 || parseValue(value) != null,
        );
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

function noCodeInputFieldToYup(elem: NoCodeInputFieldElement): Schema {
    const requiredMessage = `${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`;

    let noCodeSchema: Schema = yup
        .object()
        .shape({
            noCode: yup.mixed().nullable(),
        });

    if (elem.required) {
        noCodeSchema = noCodeSchema
            .required(requiredMessage)
            .test(
                'no-code-required',
                requiredMessage,
                (value: any) => value?.noCode != null,
            );
    } else {
        noCodeSchema = noCodeSchema
            .nullable();
    }

    return noCodeSchema;
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
            generalAssigneePreference: yup.string().oneOf([
                'none',
                'previousProcessStepAssignee',
                'uninvolvedUser',
                'processInstanceAssignee',
            ]).nullable(),
            repeatExecutionAssigneePreference: yup.string().oneOf([
                'none',
                'previousIterationAssignee',
                'differentFromPreviousIterationAssignee',
            ]).nullable(),
        })
        .test(
            'assignment-context-preference-requires-selection',
            'Für eine Bevorzugung muss ein Personenkreis ausgewählt sein.',
            (value: unknown) => {
                if (value == null) {
                    return true;
                }

                const typedValue = value as AssignmentContextValue;
                const hasSelection = (typedValue.domainAndUserSelection ?? []).length > 0;
                const hasPreference =
                    typedValue.generalAssigneePreference === 'previousProcessStepAssignee' ||
                    typedValue.generalAssigneePreference === 'uninvolvedUser' ||
                    typedValue.generalAssigneePreference === 'processInstanceAssignee' ||
                    typedValue.repeatExecutionAssigneePreference === 'previousIterationAssignee' ||
                    typedValue.repeatExecutionAssigneePreference === 'differentFromPreviousIterationAssignee';

                return !hasPreference || hasSelection;
            },
        );

    if (elem.required) {
        assignmentContextSchema = assignmentContextSchema.required(`${elem.label || 'Dieses Feld'} ist ein Pflichtfeld.`);
    } else {
        assignmentContextSchema = assignmentContextSchema.nullable();
    }

    return assignmentContextSchema;
}

interface MapFormManagerErrorsToComputedErrorsOptions {
    rootPath?: string;
}

export function mapFormManagerErrorsToComputedErrors(
    rootElement: AnyElement,
    authoredElementValues: AuthoredElementValues,
    errors: Partial<Record<string, string>> | null | undefined,
    options?: MapFormManagerErrorsToComputedErrorsOptions,
): ComputedElementErrors {
    if (errors == null) {
        return {};
    }

    const normalizedErrors = Object.entries(errors)
        .filter((entry): entry is [string, string] => entry[1] != null && entry[1].length > 0);

    if (normalizedErrors.length === 0) {
        return {};
    }

    const rootPath = options?.rootPath ?? 'data';

    function buildPath(element: AnyElement, parents: Array<AnyElement | number>): string {
        let path = rootPath;

        for (const parent of parents) {
            if (typeof parent === 'number') {
                path += `[${parent}].values`;
            } else if (isAnyInputElement(parent)) {
                path += `.${parent.id}`;
            }
        }

        return `${path}.${element.id}`;
    }

    function findErrorForPath(path: string, includeDescendants: boolean): string | null {
        const exactMatch = normalizedErrors.find(([errorPath]) => errorPath === path);
        if (exactMatch != null) {
            return exactMatch[1];
        }

        if (!includeDescendants) {
            return null;
        }

        const descendantMatch = normalizedErrors.find(([errorPath]) => (
            errorPath.startsWith(`${path}.`) ||
            errorPath.startsWith(`${path}[`)
        ));

        return descendantMatch?.[1] ?? null;
    }

    function hasComputedElementErrors(computedErrors: ComputedElementErrors): boolean {
        return Object.values(computedErrors).some((computedError) => {
            if (computedError == null) {
                return false;
            }

            if (computedError.error != null) {
                return true;
            }

            if (computedError.subStates == null) {
                return false;
            }

            return computedError.subStates.some((subState) => {
                return Object.values(resolveComputedElementSubStateStates(subState)).some((subStateError) => {
                    return subStateError?.error != null || subStateError?.subStates != null;
                });
            });
        });
    }

    function mapErrors(
        element: AnyElement,
        currentAuthoredElementValues: AuthoredElementValues,
        parents: Array<AnyElement | number> = [],
    ): ComputedElementErrors {
        let nextComputedErrors: ComputedElementErrors = {};

        if (isAnyInputElement(element)) {
            const elementPath = buildPath(element, parents);
            const elementError = findErrorForPath(elementPath, !isReplicatingContainerLayout(element));

            if (isReplicatingContainerLayout(element)) {
                const childValues = currentAuthoredElementValues[element.id];
                const rowErrors = Array.isArray(childValues) ?
                    childValues.map((childValue, index) => {
                        const rowValues = resolveReplicatingContainerElementValues(childValue);
                        if (rowValues == null) {
                            return {};
                        }

                        let childComputedErrors: ComputedElementErrors = {};
                        for (const child of element.children ?? []) {
                            childComputedErrors = {
                                ...childComputedErrors,
                                ...mapErrors(child, rowValues, [...parents, element, index]),
                            };
                        }

                        return createComputedElementSubState(isReplicatingContainerElementValue(childValue) ? childValue.id : null, childComputedErrors);
                    }) :
                    [];

                if (elementError != null || rowErrors.some((rowError) => hasComputedElementErrors(rowError.states ?? {}))) {
                    nextComputedErrors[element.id] = {
                        ...(elementError != null ? {error: elementError} : {}),
                        subStates: rowErrors,
                    };
                }
            } else if (elementError != null) {
                nextComputedErrors[element.id] = {
                    error: elementError,
                };
            }
        }

        if (isAnyElementWithChildren(element) && !isReplicatingContainerLayout(element)) {
            for (const child of element.children ?? []) {
                nextComputedErrors = {
                    ...nextComputedErrors,
                    ...mapErrors(child, currentAuthoredElementValues, [...parents, element]),
                };
            }
        }

        return nextComputedErrors;
    }

    return mapErrors(rootElement, authoredElementValues);
}

export function applyYupErrorsToElementData(
    rootElement: AnyElement,
    authoredElementValues: AuthoredElementValues,
    errors: Partial<Record<string, string>>,
    baseDerivedData?: DerivedRuntimeElementData,
): DerivedRuntimeElementData {
    const nextDerivedData = createDerivedRuntimeElementData(baseDerivedData);
    nextDerivedData.elementStates = applyComputedErrors(
        mapFormManagerErrorsToComputedErrors(rootElement, authoredElementValues, errors, {rootPath: 'data'}),
        {
            ...nextDerivedData.elementStates,
        },
    );
    return nextDerivedData;
}

/**
 * @deprecated use prosunaSchemaToYup
 */
export const prosunaSchemaToYup2 = prosunaSchemaToYup;
